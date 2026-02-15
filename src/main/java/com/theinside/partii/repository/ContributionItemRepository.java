package com.theinside.partii.repository;

import com.theinside.partii.entity.ContributionItem;
import com.theinside.partii.enums.ContributionStatus;
import com.theinside.partii.enums.ContributionType;
import com.theinside.partii.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for ContributionItem entity with custom query methods.
 */
@Repository
public interface ContributionItemRepository extends JpaRepository<ContributionItem, Long> {

    // ===== Basic Queries =====

    List<ContributionItem> findByEventId(Long eventId);

    Page<ContributionItem> findByEventId(Long eventId, Pageable pageable);

    List<ContributionItem> findByAssignedToId(Long userId);

    Page<ContributionItem> findByAssignedToId(Long userId, Pageable pageable);

    // ===== Status Queries =====

    List<ContributionItem> findByEventIdAndStatus(Long eventId, ContributionStatus status);

    List<ContributionItem> findByEventIdAndStatusIn(Long eventId, List<ContributionStatus> statuses);

    @Query("""
        SELECT ci FROM ContributionItem ci
        WHERE ci.event.id = :eventId
        AND ci.status = 'AVAILABLE'
        ORDER BY ci.priority ASC, ci.createdAt ASC
        """)
    List<ContributionItem> findAvailableByEventId(@Param("eventId") Long eventId);

    // ===== Category Queries =====

    List<ContributionItem> findByEventIdAndCategory(Long eventId, String category);

    @Query("SELECT DISTINCT ci.category FROM ContributionItem ci WHERE ci.event.id = :eventId AND ci.category IS NOT NULL")
    List<String> findDistinctCategoriesByEventId(@Param("eventId") Long eventId);

    // ===== Type Queries =====

    List<ContributionItem> findByEventIdAndType(Long eventId, ContributionType type);

    // ===== Priority Queries =====

    List<ContributionItem> findByEventIdAndPriority(Long eventId, Priority priority);

    @Query("""
        SELECT ci FROM ContributionItem ci
        WHERE ci.event.id = :eventId
        AND ci.priority = 'MUST_HAVE'
        AND ci.status = 'AVAILABLE'
        ORDER BY ci.createdAt ASC
        """)
    List<ContributionItem> findUnclaimedMustHaveItems(@Param("eventId") Long eventId);

    // ===== Count Queries =====

    long countByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, ContributionStatus status);

    long countByEventIdAndCompleted(Long eventId, boolean completed);

    @Query("""
        SELECT COUNT(ci) FROM ContributionItem ci
        WHERE ci.event.id = :eventId
        AND ci.priority = 'MUST_HAVE'
        AND ci.status = 'AVAILABLE'
        """)
    long countUnclaimedMustHaveItems(@Param("eventId") Long eventId);

    // ===== User's Contributions =====

    @Query("""
        SELECT ci FROM ContributionItem ci
        JOIN FETCH ci.event e
        WHERE ci.assignedTo.id = :userId
        AND ci.status IN ('CLAIMED', 'CONFIRMED')
        AND e.status IN ('ACTIVE', 'FULL')
        ORDER BY e.eventDate ASC
        """)
    List<ContributionItem> findActiveContributionsByUser(@Param("userId") Long userId);

    @Query("""
        SELECT ci FROM ContributionItem ci
        WHERE ci.event.id = :eventId
        AND ci.assignedTo.id = :userId
        """)
    List<ContributionItem> findByEventIdAndAssignedTo(
        @Param("eventId") Long eventId,
        @Param("userId") Long userId
    );

    // ===== Cost Queries =====

    @Query("SELECT SUM(ci.estimatedCost) FROM ContributionItem ci WHERE ci.event.id = :eventId")
    BigDecimal sumEstimatedCostByEventId(@Param("eventId") Long eventId);

    @Query("""
        SELECT SUM(ci.estimatedCost) FROM ContributionItem ci
        WHERE ci.event.id = :eventId
        AND ci.status IN ('CLAIMED', 'CONFIRMED')
        """)
    BigDecimal sumClaimedCostByEventId(@Param("eventId") Long eventId);

    // ===== Completion Queries =====

    List<ContributionItem> findByEventIdAndCompletedTrue(Long eventId);

    List<ContributionItem> findByEventIdAndCompletedFalse(Long eventId);

    @Query("""
        SELECT ci FROM ContributionItem ci
        WHERE ci.event.id = :eventId
        AND ci.status = 'CONFIRMED'
        AND ci.completed = false
        ORDER BY ci.priority ASC
        """)
    List<ContributionItem> findPendingCompletionByEventId(@Param("eventId") Long eventId);

    // ===== Alert Queries =====

    /**
     * Find MUST_HAVE items that are not yet claimed or confirmed,
     * for events happening within the specified number of days.
     */
    @Query("""
        SELECT ci FROM ContributionItem ci
        JOIN ci.event e
        WHERE ci.priority = 'MUST_HAVE'
        AND ci.status = 'AVAILABLE'
        AND e.status = 'ACTIVE'
        AND e.eventDate <= :deadline
        ORDER BY e.eventDate ASC
        """)
    List<ContributionItem> findUnclaimedMustHaveItemsBeforeDeadline(
        @Param("deadline") java.time.LocalDateTime deadline
    );

    // ===== Deletion =====

    void deleteByEventId(Long eventId);
}
