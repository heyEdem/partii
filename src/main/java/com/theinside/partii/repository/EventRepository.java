package com.theinside.partii.repository;

import com.theinside.partii.entity.Event;
import com.theinside.partii.enums.EventStatus;
import com.theinside.partii.enums.EventType;
import com.theinside.partii.enums.EventVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Event entity with custom query methods.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    // ===== Basic Queries =====

    Optional<Event> findByPrivateLinkCode(String privateLinkCode);

    List<Event> findByOrganizerId(Long organizerId);

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    List<Event> findByStatus(EventStatus status);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByVisibility(EventVisibility visibility, Pageable pageable);

    // ===== Count Queries =====

    long countByOrganizerId(Long organizerId);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.organizer.id = :organizerId AND e.status IN :statuses")
    long countActiveEventsByOrganizer(
        @Param("organizerId") Long organizerId,
        @Param("statuses") List<EventStatus> statuses
    );

    default long countActiveEventsByOrganizer(Long organizerId) {
        return countActiveEventsByOrganizer(
            organizerId,
            List.of(EventStatus.DRAFT, EventStatus.ACTIVE, EventStatus.FULL)
        );
    }

    // ===== Public Event Discovery =====

    @Query("""
        SELECT e FROM Event e
        WHERE e.visibility = :visibility
        AND e.status IN :statuses
        AND e.eventDate > :now
        ORDER BY e.eventDate ASC
        """)
    Page<Event> findUpcomingPublicEvents(
        @Param("visibility") EventVisibility visibility,
        @Param("statuses") List<EventStatus> statuses,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    default Page<Event> findUpcomingPublicEvents(Pageable pageable) {
        return findUpcomingPublicEvents(
            EventVisibility.PUBLIC,
            List.of(EventStatus.ACTIVE, EventStatus.FULL),
            LocalDateTime.now(),
            pageable
        );
    }

    // ===== Location-Based Search =====

    /**
     * Find events within a radius using Haversine formula.
     * Distance is calculated in kilometers.
     */
    @Query("""
        SELECT e FROM Event e
        WHERE e.visibility = 'PUBLIC'
        AND e.status IN ('ACTIVE', 'FULL')
        AND e.latitude IS NOT NULL
        AND e.longitude IS NOT NULL
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(e.latitude))
            * cos(radians(e.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(e.latitude)))) <= :radiusKm
        AND e.eventDate > :now
        ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(e.latitude))
            * cos(radians(e.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(e.latitude)))) ASC
        """)
    Page<Event> findNearbyEvents(
        @Param("lat") Double latitude,
        @Param("lng") Double longitude,
        @Param("radiusKm") Double radiusKm,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    default Page<Event> findNearbyEvents(Double latitude, Double longitude, Double radiusKm, Pageable pageable) {
        return findNearbyEvents(latitude, longitude, radiusKm, LocalDateTime.now(), pageable);
    }

    // ===== Search and Filter =====

    @Query("""
        SELECT e FROM Event e
        WHERE e.visibility = 'PUBLIC'
        AND e.status IN ('ACTIVE', 'FULL')
        AND e.eventDate > :now
        AND (:eventType IS NULL OR e.eventType = :eventType)
        AND (:minBudget IS NULL OR e.estimatedBudget >= :minBudget)
        AND (:maxBudget IS NULL OR e.estimatedBudget <= :maxBudget)
        AND (:hasSpots = false OR e.currentAttendees < e.maxAttendees)
        ORDER BY e.eventDate ASC
        """)
    Page<Event> searchEvents(
        @Param("now") LocalDateTime now,
        @Param("eventType") EventType eventType,
        @Param("minBudget") java.math.BigDecimal minBudget,
        @Param("maxBudget") java.math.BigDecimal maxBudget,
        @Param("hasSpots") boolean hasSpots,
        Pageable pageable
    );

    @Query("""
        SELECT e FROM Event e
        WHERE e.visibility = 'PUBLIC'
        AND e.status IN ('ACTIVE', 'FULL')
        AND e.eventDate > :now
        AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(e.locationAddress) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY e.eventDate ASC
        """)
    Page<Event> searchByKeyword(
        @Param("query") String query,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    // ===== Feed Queries =====

    /**
     * Find newly created events (for "New Events" feed section).
     */
    @Query("""
        SELECT e FROM Event e
        WHERE e.visibility = 'PUBLIC'
        AND e.status = 'ACTIVE'
        AND e.eventDate > :now
        AND e.createdAt > :since
        ORDER BY e.createdAt DESC
        """)
    Page<Event> findNewlyCreatedEvents(
        @Param("now") LocalDateTime now,
        @Param("since") java.time.Instant since,
        Pageable pageable
    );

    /**
     * Find events filling up fast (> 75% capacity).
     */
    @Query("""
        SELECT e FROM Event e
        WHERE e.visibility = 'PUBLIC'
        AND e.status = 'ACTIVE'
        AND e.eventDate > :now
        AND (CAST(e.currentAttendees AS double) / CAST(e.maxAttendees AS double)) >= 0.75
        ORDER BY (CAST(e.currentAttendees AS double) / CAST(e.maxAttendees AS double)) DESC
        """)
    Page<Event> findEventsFillingFast(
        @Param("now") LocalDateTime now,
        Pageable pageable
    );

    // ===== Status Updates =====

    /**
     * Update events that have passed to PAST status.
     */
    @Modifying
    @Query("""
        UPDATE Event e
        SET e.status = 'PAST'
        WHERE e.status IN ('ACTIVE', 'FULL')
        AND e.eventDate < :now
        """)
    int updatePastEvents(@Param("now") LocalDateTime now);

    /**
     * Archive events that are 30+ days past.
     */
    @Modifying
    @Query("""
        UPDATE Event e
        SET e.status = 'ARCHIVED'
        WHERE e.status = 'PAST'
        AND e.eventDate < :archiveDate
        """)
    int archiveOldEvents(@Param("archiveDate") LocalDateTime archiveDate);

    // ===== Date Range Queries =====

    @Query("""
        SELECT e FROM Event e
        WHERE e.visibility = 'PUBLIC'
        AND e.status IN ('ACTIVE', 'FULL')
        AND e.eventDate BETWEEN :startDate AND :endDate
        ORDER BY e.eventDate ASC
        """)
    Page<Event> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // ===== Existence Checks =====

    boolean existsByPrivateLinkCode(String privateLinkCode);

    // ===== Keyset Pagination (Cursor-Based) =====

    /**
     * Find public events using keyset pagination (significantly faster than offset).
     * Uses composite key (eventDate, id) for stable ordering.
     * Fetches limit+1 to determine if there's a next page.
     */
    @Query("""
        SELECT e FROM Event e
        WHERE e.visibility = 'PUBLIC'
        AND e.status IN ('ACTIVE', 'FULL')
        AND e.eventDate > :now
        AND (CAST(:afterDate AS timestamp) IS NULL OR e.eventDate > :afterDate
            OR (e.eventDate = :afterDate AND e.id > :afterId))
        ORDER BY e.eventDate ASC, e.id ASC
        """)
    List<Event> findPublicEventsKeyset(
        @Param("now") LocalDateTime now,
        @Param("afterDate") LocalDateTime afterDate,
        @Param("afterId") Long afterId,
        org.springframework.data.domain.Limit limit
    );

    /**
     * Find all events using keyset pagination (admin only).
     */
    @Query("""
        SELECT e FROM Event e
        WHERE (CAST(:afterDate AS timestamp) IS NULL OR e.createdAt > :afterDate
            OR (e.createdAt = :afterDate AND e.id > :afterId))
        ORDER BY e.createdAt DESC, e.id ASC
        """)
    List<Event> findAllEventsKeyset(
        @Param("afterDate") java.time.Instant afterDate,
        @Param("afterId") Long afterId,
        org.springframework.data.domain.Limit limit
    );
}
