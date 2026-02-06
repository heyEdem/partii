package com.theinside.partii.service;

import com.theinside.partii.dto.CompleteProfileRequest;
import com.theinside.partii.dto.UpdateProfileRequest;
import com.theinside.partii.dto.UserProfileResponse;

/**
 * Service interface for user profile operations.
 */
public interface UserService {

    /**
     * Get the current authenticated user's profile.
     *
     * @param userId the ID of the authenticated user
     * @return the user's profile information
     */
    UserProfileResponse getProfile(Long userId);

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
}
