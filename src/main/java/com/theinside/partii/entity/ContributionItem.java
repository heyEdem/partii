package com.theinside.partii.entity;

import com.theinside.partii.enums.ContributionStatus;
import com.theinside.partii.enums.ContributionType;
import com.theinside.partii.enums.Priority;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents an item or service that attendees can contribute to an event.
 */
@Entity
@Table(
    name = "contribution_items",
    indexes = {
        @Index(name = "idx_contributions_event", columnList = "event_id"),
        @Index(name = "idx_contributions_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_contributions_status", columnList = "status"),
        @Index(name = "idx_contributions_category", columnList = "category")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Category for grouping items (e.g., "Food", "Drinks", "Decorations", "Equipment").
     */
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    @Column(length = 50)
    private String category;

    @NotNull(message = "Contribution type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContributionType type;

    /**
     * Quantity needed (for MATERIAL type).
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Time commitment in minutes (for SERVICE type).
     */
    @Min(value = 0, message = "Time commitment cannot be negative")
    @Column(name = "time_commitment")
    private Integer timeCommitment;

    /**
     * Estimated cost of this item.
     */
    @DecimalMin(value = "0.0", message = "Estimated cost cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid estimated cost format")
    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private Priority priority = Priority.NICE_TO_HAVE;

    /**
     * Additional notes or instructions for this item.
     */
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @NotNull(message = "Contribution status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ContributionStatus status = ContributionStatus.AVAILABLE;

    /**
     * The user who claimed/is assigned to this item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    /**
     * Whether the contribution has been completed/delivered.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * When the item was claimed.
     */
    @Column(name = "claimed_at")
    private Instant claimedAt;

    /**
     * When the claim was confirmed by the organizer.
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Claims this item for the specified user.
     * @param user The user claiming the item
     * @throws IllegalStateException if item is not available
     */
    public void claim(User user) {
        if (status != ContributionStatus.AVAILABLE) {
            throw new IllegalStateException("Item is not available for claiming");
        }
        this.assignedTo = user;
        this.status = ContributionStatus.CLAIMED;
        this.claimedAt = Instant.now();
    }

    /**
     * Confirms the claim for this item.
     * @throws IllegalStateException if item is not claimed
     */
    public void confirmClaim() {
        if (status != ContributionStatus.CLAIMED) {
            throw new IllegalStateException("Item must be claimed before confirming");
        }
        this.status = ContributionStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    /**
     * Releases the claim on this item (backout).
     */
    public void releaseClaim() {
        this.assignedTo = null;
        this.status = ContributionStatus.AVAILABLE;
        this.claimedAt = null;
        this.confirmedAt = null;
        this.completed = false;
    }

    /**
     * Marks the contribution as completed.
     * @throws IllegalStateException if item is not confirmed
     */
    public void markCompleted() {
        if (status != ContributionStatus.CONFIRMED) {
            throw new IllegalStateException("Item must be confirmed before marking complete");
        }
        this.completed = true;
    }

    /**
     * Checks if this item is available for claiming.
     */
    public boolean isAvailable() {
        return status == ContributionStatus.AVAILABLE;
    }

    /**
     * Checks if this item is assigned to a specific user.
     */
    public boolean isAssignedTo(User user) {
        return assignedTo != null && assignedTo.getId().equals(user.getId());
    }
}
