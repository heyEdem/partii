package com.theinside.partii.entity;

import com.theinside.partii.enums.ReportStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

/**
 * Represents a report submitted by a user against another user.
 * Used for moderation and safety purposes.
 */
@Entity
@Table(
    name = "user_reports",
    indexes = {
        @Index(name = "idx_user_reports_reporter", columnList = "reporter_id"),
        @Index(name = "idx_user_reports_reported", columnList = "reported_id"),
        @Index(name = "idx_user_reports_status", columnList = "status"),
        @Index(name = "idx_user_reports_created", columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user submitting the report.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /**
     * The user being reported.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id", nullable = false)
    private User reported;

    /**
     * Reason for the report (e.g., "Harassment", "Spam", "Inappropriate behavior", "Scam").
     */
    @NotBlank(message = "Report reason is required")
    @Size(min = 10, max = 100, message = "Reason must be between 10 and 100 characters")
    @Column(nullable = false, length = 100)
    private String reason;

    /**
     * Detailed description of the issue.
     */
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(length = 1000)
    private String description;

    @NotNull(message = "Report status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /**
     * Admin notes about the report resolution.
     */
    @Size(max = 500, message = "Admin notes cannot exceed 500 characters")
    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    /**
     * The admin who reviewed this report (if resolved).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Marks this report as reviewed with the given status.
     */
    public void markReviewed(ReportStatus newStatus, User reviewer, String notes) {
        this.status = newStatus;
        this.reviewedBy = reviewer;
        this.adminNotes = notes;
        this.reviewedAt = Instant.now();
    }

    /**
     * Checks if this report has been reviewed.
     */
    public boolean isReviewed() {
        return status != ReportStatus.PENDING && status != ReportStatus.UNDER_REVIEW;
    }

    /**
     * Checks if the given user is the reporter.
     */
    public boolean isReportedBy(User user) {
        return reporter.getId().equals(user.getId());
    }

    /**
     * Checks if the given user is being reported.
     */
    public boolean isReportingUser(User user) {
        return reported.getId().equals(user.getId());
    }
}
