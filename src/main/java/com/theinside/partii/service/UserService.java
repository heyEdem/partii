package com.theinside.partii.service;

import com.theinside.partii.dto.*;
import com.theinside.partii.entity.User;

import java.util.List;

/**
 * Service interface for user operations including profile management, blocking, and reporting.
 */
public interface UserService {

    // ===== Profile Management =====

    /**
     * Get the current authenticated user's profile.
     *
     * @param userId the ID of the authenticated user
     * @return the user's profile information
     */
    UserProfileResponse getProfile(Long userId);

    /**
     * Get a user's public profile information.
     * Returns limited information that's safe to display publicly.
     *
     * @param userId        the ID of the user to view
     * @param currentUserId the ID of the current authenticated user
     * @return the user's public profile information
     */
    UserProfileResponse getPublicProfile(Long userId, Long currentUserId);

    /**
     * Complete the current authenticated user's profile.
     *
     * @param userId  the ID of the authenticated user
     * @param request the complete profile request
     * @return the completed profile information
     */
    UserProfileResponse completeProfile(Long userId, CompleteProfileRequest request);

    /**
     * Update the current authenticated user's profile.
     * Only non-null fields in the request will be updated.
     *
     * @param userId  the ID of the authenticated user
     * @param request the update request with optional fields
     * @return the updated profile information
     */
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    // ===== Blocking =====

    /**
     * Block a user.
     * Blocking is unidirectional but prevents both users from interacting.
     *
     * @param blockerId     the ID of the user performing the block
     * @param blockedUserId the ID of the user to block
     */
    void blockUser(Long blockerId, Long blockedUserId);

    /**
     * Unblock a previously blocked user.
     *
     * @param blockerId     the ID of the user performing the unblock
     * @param blockedUserId the ID of the user to unblock
     */
    void unblockUser(Long blockerId, Long blockedUserId);

    /**
     * Check if user1 has blocked user2.
     *
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return true if user1 has blocked user2
     */
    boolean hasBlockedUser(Long userId1, Long userId2);

    /**
     * Check if user1 is blocked by user2.
     *
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return true if user1 is blocked by user2
     */
    boolean isBlockedByUser(Long userId1, Long userId2);

    /**
     * Get list of users blocked by the given user.
     *
     * @param userId the ID of the user
     * @return list of blocked users
     */
    List<User> getBlockedUsers(Long userId);

    // ===== Reporting =====

    /**
     * Report a user for inappropriate behavior.
     *
     * @param reporterId     the ID of the user reporting
     * @param reportedUserId the ID of the user being reported
     * @param request        the report details
     */
    void reportUser(Long reporterId, Long reportedUserId, ReportUserRequest request);

    /**
     * Check if a pending report exists between two users.
     *
     * @param reporterId     the ID of the reporter
     * @param reportedUserId the ID of the reported user
     * @return true if a pending report exists
     */
    boolean hasPendingReportBetweenUsers(Long reporterId, Long reportedUserId);

    /**
     * Get count of unresolved reports against a user.
     *
     * @param userId the ID of the user
     * @return count of unresolved reports
     */
    long getUnresolvedReportCount(Long userId);

    // ===== Privacy & Visibility =====

    /**
     * Check if user1 can view user2's profile.
     * Returns false if either user has blocked the other.
     *
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return true if user1 can view user2's profile
     */
    boolean canViewProfile(Long userId1, Long userId2);

    /**
     * Check if user1 can message user2.
     * Returns false if either user has blocked the other.
     *
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return true if user1 can message user2
     */
    boolean canMessage(Long userId1, Long userId2);

    /**
     * Check if user1 can invite user2 to an event.
     * Returns false if either user has blocked the other.
     *
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return true if user1 can invite user2 to an event
     */
    boolean canInviteToEvent(Long userId1, Long userId2);
}
