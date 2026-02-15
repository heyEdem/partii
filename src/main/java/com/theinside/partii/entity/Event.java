package com.theinside.partii.entity;

import com.theinside.partii.enums.EventStatus;
import com.theinside.partii.enums.EventType;
import com.theinside.partii.enums.EventVisibility;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Represents an event that users can organize and attend.
 */
@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_organizer", columnList = "organizer_id"),
    @Index(name = "idx_events_status", columnList = "status"),
    @Index(name = "idx_events_visibility", columnList = "visibility"),
    @Index(name = "idx_events_event_date", columnList = "event_date"),
    @Index(name = "idx_events_location", columnList = "latitude, longitude")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @NotBlank(message = "Event title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Column(nullable = false, length = 100)
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Column(length = 2000)
    private String description;

    @NotNull(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType eventType;

    @Size(max = 500, message = "Location address cannot exceed 500 characters")
    @Column(length = 500)
    private String locationAddress;

    /**
     * Latitude coordinate for location-based search.
     */
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    /**
     * Longitude coordinate for location-based search.
     */
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Column(length = 500)
    private String imageUrl;

    @DecimalMin(value = "0.0", message = "Budget cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid budget format")
    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedBudget;

    @Size(max = 3, message = "Currency code must be 3 characters")
    @Column(length = 3)
    @Builder.Default
    private String currency = "GHS";

    @Min(value = 2, message = "Event must have at least 2 attendees")
    @Max(value = 10000, message = "Event cannot exceed 10000 attendees")
    @Column(nullable = false)
    @Builder.Default
    private Integer maxAttendees = 10;

    @Min(value = 0, message = "Current attendees cannot be negative")
    @Column(nullable = false)
    @Builder.Default
    private Integer currentAttendees = 0;

    @Min(value = 0, message = "Age restriction cannot be negative")
    private Integer ageRestriction;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Column(name = "join_deadline")
    private LocalDateTime joinDeadline;

    @NotNull(message = "Event visibility is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EventVisibility visibility = EventVisibility.PUBLIC;

    @NotNull(message = "Event status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    /**
     * Unique code for private event access (6-char alphanumeric).
     */
    @Column(name = "private_link_code", unique = true, length = 10)
    private String privateLinkCode;

    @Column(name = "link_expiration")
    private LocalDateTime linkExpiration;

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (visibility == EventVisibility.PRIVATE && privateLinkCode == null) {
            privateLinkCode = generatePrivateLinkCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Generates a 6-character alphanumeric code for private event access.
     */
    private String generatePrivateLinkCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.security.SecureRandom();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    /**
     * Checks if the event has available spots.
     */
    public boolean hasAvailableSpots() {
        return currentAttendees < maxAttendees;
    }

    /**
     * Checks if the event is accepting join requests.
     */
    public boolean isAcceptingJoinRequests() {
        if (status != EventStatus.ACTIVE) {
            return false;
        }
        if (joinDeadline != null && LocalDateTime.now().isAfter(joinDeadline)) {
            return false;
        }
        return hasAvailableSpots();
    }

    /**
     * Increments the current attendee count.
     * @throws IllegalStateException if event is at capacity
     */
    public void incrementAttendees() {
        if (!hasAvailableSpots()) {
            throw new IllegalStateException("Event is at capacity");
        }
        this.currentAttendees++;
        if (this.currentAttendees >= this.maxAttendees) {
            this.status = EventStatus.FULL;
        }
    }

    /**
     * Decrements the current attendee count.
     */
    public void decrementAttendees() {
        if (this.currentAttendees > 0) {
            this.currentAttendees--;
            if (this.status == EventStatus.FULL && hasAvailableSpots()) {
                this.status = EventStatus.ACTIVE;
            }
        }
    }
}
