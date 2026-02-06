package com.theinside.partii.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Represents a block between two users.
 * Blocking is mutual - both users cannot see each other.
 */
@Entity
@Table(
    name = "user_blocks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_blocks_blocker_blocked",
            columnNames = {"blocker_id", "blocked_id"}
        )
    },
    indexes = {
        @Index(name = "idx_user_blocks_blocker", columnList = "blocker_id"),
        @Index(name = "idx_user_blocks_blocked", columnList = "blocked_id"),
        @Index(name = "idx_user_blocks_created", columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who initiated the block.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    /**
     * The user being blocked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Checks if the given user is blocked by the other user.
     */
    public boolean isUserBlocked(User user) {
        return blocked.getId().equals(user.getId());
    }

    /**
     * Checks if the given user initiated this block.
     */
    public boolean isBlockedBy(User user) {
        return blocker.getId().equals(user.getId());
    }
}
