package com.theinside.partii.service;

import com.theinside.partii.dto.*;
import com.theinside.partii.entity.User;

/**
 * Service interface for user profile management, blocking, and reporting.
 */
public interface UserManagementService {

    // ===== Profile Management =====

    /**
     * Get a user's public profile information.
     * Returns limited information that's safe to display publicly.
     */
    UserProfileResponse getPublicProfile(Long userId, Long currentUserId);

    /**
     * Get the current user's own profile (full details).
     */
    UserProfileResponse getOwnProfile(Long userId);

    /**
     * Update the current user's profile.
     */
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    // ===== Blocking =====

    /**
     * Block a user.
     * Blocking is unidirectional but prevents both users from interacting.
     */
    void blockUser(Long blockerId, Long blockedUserId);

    /**
     * Unblock a previously blocked user.
     */
    void unblockUser(Long blockerId, Long blockedUserId);

    /**
     * Check if user1 has blocked user2.
     */
    boolean hasBlockedUser(Long userId1, Long userId2);

    /**
     * Check if user1 is blocked by user2.
     */
    boolean isBlockedByUser(Long userId1, Long userId2);

    /**
     * Get list of users blocked by the given user.
     */
    java.util.List<User> getBlockedUsers(Long userId);

    // ===== Reporting =====

    /**
     * Report a user for inappropriate behavior.
     */
    void reportUser(Long reporterId, Long reportedUserId, ReportUserRequest request);

    /**
     * Check if a pending report exists between two users.
     */
    boolean hasPendingReportBetweenUsers(Long reporterId, Long reportedUserId);

    /**
     * Get count of unresolved reports against a user.
     */
    long getUnresolvedReportCount(Long userId);

    // ===== Privacy & Visibility =====

    /**
     * Check if user1 can view user2's profile.
     * Returns false if either user has blocked the other.
     */
    boolean canViewProfile(Long userId1, Long userId2);

    /**
     * Check if user1 can message user2.
     * Returns false if either user has blocked the other.
     */
    boolean canMessage(Long userId1, Long userId2);

    /**
     * Check if user1 can invite user2 to an event.
     * Returns false if either user has blocked the other.
     */
    boolean canInviteToEvent(Long userId1, Long userId2);
}
