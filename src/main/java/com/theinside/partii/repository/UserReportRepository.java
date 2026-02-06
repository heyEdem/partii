package com.theinside.partii.repository;

import com.theinside.partii.entity.UserReport;
import com.theinside.partii.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for UserReport entity.
 */
@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    // ===== Find by Reporter =====

    List<UserReport> findByReporterId(Long reporterId);

    Page<UserReport> findByReporterId(Long reporterId, Pageable pageable);

    // ===== Find by Reported =====

    List<UserReport> findByReportedId(Long reportedId);

    Page<UserReport> findByReportedId(Long reportedId, Pageable pageable);

    /**
     * Find all reports against a user with a specific status.
     */
    List<UserReport> findByReportedIdAndStatus(Long reportedId, ReportStatus status);

    // ===== Find by Status =====

    List<UserReport> findByStatus(ReportStatus status);

    Page<UserReport> findByStatus(ReportStatus status, Pageable pageable);

    /**
     * Find pending reports (for admin review queue).
     */
    @Query("SELECT ur FROM UserReport ur WHERE ur.status IN ('PENDING', 'UNDER_REVIEW') ORDER BY ur.createdAt ASC")
    Page<UserReport> findPendingReports(Pageable pageable);

    // ===== Count Queries =====

    long countByReportedId(Long reportedId);

    long countByReportedIdAndStatus(Long reportedId, ReportStatus status);

    long countByStatus(ReportStatus status);

    @Query("SELECT COUNT(ur) FROM UserReport ur WHERE ur.status IN ('PENDING', 'UNDER_REVIEW')")
    long countPendingReports();

    /**
     * Count unresolved reports against a user.
     */
    @Query("""
        SELECT COUNT(ur) FROM UserReport ur
        WHERE ur.reported.id = :userId
        AND ur.status IN ('PENDING', 'UNDER_REVIEW', 'ON_HOLD')
        """)
    long countUnresolvedReportsAgainstUser(@Param("userId") Long userId);

    // ===== Find by Reason =====

    List<UserReport> findByReasonContainingIgnoreCase(String reason);

    // ===== Find by Reviewer =====

    /**
     * Find all reports reviewed by a specific admin.
     */
    List<UserReport> findByReviewedById(Long reviewerId);

    /**
     * Find reports reviewed within a date range.
     */
    List<UserReport> findByReviewedAtBetween(Instant startDate, Instant endDate);

    // ===== Check Duplicate =====

    /**
     * Check if a report already exists from reporter to reported with status not DISMISSED.
     */
    @Query("""
        SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
        FROM UserReport ur
        WHERE ur.reporter.id = :reporterId
        AND ur.reported.id = :reportedId
        AND ur.status != 'DISMISSED'
        """)
    boolean hasPendingReportBetweenUsers(@Param("reporterId") Long reporterId, @Param("reportedId") Long reportedId);

    // ===== Deletion =====

    void deleteByReporterId(Long reporterId);

    void deleteByReportedId(Long reportedId);
}
