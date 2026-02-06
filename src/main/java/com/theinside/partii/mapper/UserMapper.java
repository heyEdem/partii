package com.theinside.partii.mapper;

import com.theinside.partii.dto.CompleteProfileRequest;
import com.theinside.partii.dto.UpdateProfileRequest;
import com.theinside.partii.dto.UserProfileResponse;
import com.theinside.partii.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for User entity conversions.
 * Manual implementation to avoid external dependencies.
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to UserProfileResponse DTO.
     */
    public UserProfileResponse toProfileResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getLegalName(),
                user.getBio(),
                user.getGeneralLocation(),
                user.getPrimaryAddress(),
                user.getPhoneNumber(),
                user.getDob(),
                0, // age - computed field, flagged as pre-existing issue
                user.getAccountStatus(),
                user.isVerified(),
                user.getTotalRatings(),
                user.getAverageRating(),
                user.getEventsAttended(),
                user.getEventsOrganized(),
                user.getActiveEventsCount(),
                user.getProfilePictureUrl(),
                user.getCreatedAt()
        );
    }

    /**
     * Update user from update request - only non-null fields are updated.
     */
    public void updateUserFromRequest(UpdateProfileRequest request, User user) {
        if (request == null || user == null) {
            return;
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.legalName() != null) {
            user.setLegalName(request.legalName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.generalLocation() != null) {
            user.setGeneralLocation(request.generalLocation());
        }
    }

    /**
     * Complete user profile from request.
     */
    public void completeProfileFromRequest(CompleteProfileRequest request, User user) {
        if (request == null || user == null) {
            return;
        }
        if (request.dob() != null) {
            user.setDob(request.dob());
        }
        if (request.legalName() != null) {
            user.setLegalName(request.legalName());
        }
        if (request.generalLocation() != null) {
            user.setGeneralLocation(request.generalLocation());
        }
    }
}
