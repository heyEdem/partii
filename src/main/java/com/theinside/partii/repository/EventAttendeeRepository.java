package com.theinside.partii.repository;

import com.theinside.partii.entity.EventAttendee;
import com.theinside.partii.enums.AttendeeStatus;
import com.theinside.partii.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for EventAttendee entity with custom query methods.
 */
@Repository
public interface EventAttendeeRepository extends JpaRepository<EventAttendee, Long> {

    // ===== Basic Queries =====

    List<EventAttendee> findByEventId(Long eventId);

    Page<EventAttendee> findByEventId(Long eventId, Pageable pageable);

    List<EventAttendee> findByUserId(Long userId);

    Page<EventAttendee> findByUserId(Long userId, Pageable pageable);

    Optional<EventAttendee> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, AttendeeStatus status);

    // ===== Status Queries =====

    List<EventAttendee> findByEventIdAndStatus(Long eventId, AttendeeStatus status);

    Page<EventAttendee> findByEventIdAndStatus(Long eventId, AttendeeStatus status, Pageable pageable);

    List<EventAttendee> findByUserIdAndStatus(Long userId, AttendeeStatus status);

    @Query("""
        SELECT ea FROM EventAttendee ea
        WHERE ea.event.id = :eventId
        AND ea.status IN :statuses
        ORDER BY ea.joinedAt ASC
        """)
    List<EventAttendee> findByEventIdAndStatusIn(
        @Param("eventId") Long eventId,
        @Param("statuses") List<AttendeeStatus> statuses
    );

    // ===== Count Queries =====

    long countByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, AttendeeStatus status);

    @Query("SELECT COUNT(ea) FROM EventAttendee ea WHERE ea.event.id = :eventId AND ea.status = 'APPROVED'")
    long countApprovedAttendees(@Param("eventId") Long eventId);

    // ===== Payment Queries =====

    List<EventAttendee> findByEventIdAndPaymentStatus(Long eventId, PaymentStatus paymentStatus);

    @Query("""
        SELECT ea FROM EventAttendee ea
        WHERE ea.event.id = :eventId
        AND ea.status = 'APPROVED'
        AND ea.paymentStatus != 'PAID'
        ORDER BY ea.joinedAt ASC
        """)
    List<EventAttendee> findUnpaidAttendees(@Param("eventId") Long eventId);

    @Query("""
        SELECT SUM(ea.amountPaid) FROM EventAttendee ea
        WHERE ea.event.id = :eventId
        AND ea.status = 'APPROVED'
        """)
    BigDecimal sumAmountPaidByEvent(@Param("eventId") Long eventId);

    @Query("""
        SELECT SUM(ea.paymentAmount) FROM EventAttendee ea
        WHERE ea.event.id = :eventId
        AND ea.status = 'APPROVED'
        """)
    BigDecimal sumPaymentAmountByEvent(@Param("eventId") Long eventId);

    // ===== Waitlist Queries =====

    @Query("""
        SELECT ea FROM EventAttendee ea
        WHERE ea.event.id = :eventId
        AND ea.status = 'WAITLIST'
        ORDER BY ea.joinedAt ASC
        """)
    List<EventAttendee> findWaitlistByEventId(@Param("eventId") Long eventId);

    @Query("""
        SELECT ea FROM EventAttendee ea
        WHERE ea.event.id = :eventId
        AND ea.status = 'WAITLIST'
        ORDER BY ea.joinedAt ASC
        LIMIT 1
        """)
    Optional<EventAttendee> findFirstInWaitlist(@Param("eventId") Long eventId);

    // ===== User Event Participation =====

    /**
     * Find all events a user is actively participating in.
     */
    @Query("""
        SELECT ea FROM EventAttendee ea
        JOIN FETCH ea.event e
        WHERE ea.user.id = :userId
        AND ea.status = 'APPROVED'
        AND e.status IN ('ACTIVE', 'FULL')
        ORDER BY e.eventDate ASC
        """)
    List<EventAttendee> findActiveParticipationsByUser(@Param("userId") Long userId);

    /**
     * Find all pending join requests for a user.
     */
    @Query("""
        SELECT ea FROM EventAttendee ea
        JOIN FETCH ea.event e
        WHERE ea.user.id = :userId
        AND ea.status = 'PENDING'
        ORDER BY ea.joinedAt DESC
        """)
    List<EventAttendee> findPendingRequestsByUser(@Param("userId") Long userId);

    /**
     * Find all past events a user attended.
     */
    @Query("""
        SELECT ea FROM EventAttendee ea
        JOIN FETCH ea.event e
        WHERE ea.user.id = :userId
        AND ea.status = 'APPROVED'
        AND e.status IN ('PAST', 'ARCHIVED')
        ORDER BY e.eventDate DESC
        """)
    Page<EventAttendee> findPastParticipationsByUser(@Param("userId") Long userId, Pageable pageable);

    // ===== Organizer Queries =====

    /**
     * Find all pending requests for events organized by a user.
     */
    @Query("""
        SELECT ea FROM EventAttendee ea
        JOIN FETCH ea.event e
        JOIN FETCH ea.user u
        WHERE e.organizer.id = :organizerId
        AND ea.status = 'PENDING'
        ORDER BY ea.joinedAt ASC
        """)
    List<EventAttendee> findPendingRequestsForOrganizer(@Param("organizerId") Long organizerId);

    /**
     * Count pending requests for all events organized by a user.
     */
    @Query("""
        SELECT COUNT(ea) FROM EventAttendee ea
        WHERE ea.event.organizer.id = :organizerId
        AND ea.status = 'PENDING'
        """)
    long countPendingRequestsForOrganizer(@Param("organizerId") Long organizerId);

    // ===== Deletion =====

    void deleteByEventId(Long eventId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);
}
