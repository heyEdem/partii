package com.theinside.partii.service;

import com.theinside.partii.dto.*;
import com.theinside.partii.entity.User;
import com.theinside.partii.entity.UserBlock;
import com.theinside.partii.entity.UserReport;
import com.theinside.partii.enums.ReportStatus;
import com.theinside.partii.exception.BadRequestException;
import com.theinside.partii.exception.NotFoundException;
import com.theinside.partii.exception.ResourceNotFoundException;
import com.theinside.partii.mapper.UserMapper;
import com.theinside.partii.repository.UserBlockRepository;
import com.theinside.partii.repository.UserReportRepository;
import com.theinside.partii.repository.UserRepository;
import com.theinside.partii.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.theinside.partii.utils.CustomMessages.USER_NOT_FOUND;

/**
 * Implementation of UserService.
 * Handles profile management, blocking, and reporting functionality.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserReportRepository userReportRepository;
    private final UserMapper userMapper;

    // ===== Profile Management =====

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        validateAuthenticatedUser(userId);
        User user = findUserById(userId);
        return mapToProfileResponse(user, userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if users have blocked each other
        boolean hasBlocked = userBlockRepository.hasBlocked(currentUserId, userId);
        boolean isBlocked = userBlockRepository.isBlockedBy(currentUserId, userId);

        if (hasBlocked || isBlocked) {
            // Return minimal info if blocked
            int age = user.getDob() != null
                ? java.time.Period.between(user.getDob(), java.time.LocalDate.now()).getYears()
                : 0;
            return new UserProfileResponse(
                user.getId(),
                null,
                "[Blocked User]",
                null,
                null,
                null,
                null,
                null,
                null,
                age,
                user.getAccountStatus(),
                false,
                0,
                0,
                0,
                0,
                0,
                null,
                user.getCreatedAt()
            );
        }

        return mapToProfileResponse(user, currentUserId, false);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        validateAuthenticatedUser(userId);
        User user = findUserById(userId);

        // Validate age if dob is provided
        if (request.dob() != null) {
            int age = java.time.Period.between(request.dob(), java.time.LocalDate.now()).getYears();
            if (age < 18) {
                throw new BadRequestException("User must be at least 18 years old");
            }
        }

        userMapper.updateUserFromRequest(request, user);

        User savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", savedUser.getEmail());

        return mapToProfileResponse(savedUser, userId, true);
    }

    @Override
    @Transactional
    public UserProfileResponse completeProfile(Long userId, CompleteProfileRequest request) {
        validateAuthenticatedUser(userId);
        User user = findUserById(userId);

        if (user.isProfileCompleted()) {
            throw new BadRequestException("Profile already completed");
        }

        if (user.getDob() != null) {
            throw new BadRequestException("Date of birth already set");
        }

        userMapper.completeProfileFromRequest(request, user);
        user.setProfileCompleted(true);

        User savedUser = userRepository.save(user);
        log.info("Profile completed for user: {}", savedUser.getEmail());

        return mapToProfileResponse(savedUser, userId, true);
    }

    // ===== Blocking =====

    @Override
    public void blockUser(Long blockerId, Long blockedUserId) {
        validateAuthenticatedUser(blockerId);

        if (blockerId.equals(blockedUserId)) {
            throw new BadRequestException("Cannot block yourself");
        }

        User blocker = userRepository.findById(blockerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User blocked = userRepository.findById(blockedUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        // Check if already blocked
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            throw new BadRequestException("User is already blocked");
        }

        UserBlock block = UserBlock.builder()
            .blocker(blocker)
            .blocked(blocked)
            .build();

        userBlockRepository.save(block);
        log.info("User {} blocked user {}", blockerId, blockedUserId);
    }

    @Override
    public void unblockUser(Long blockerId, Long blockedUserId) {
        validateAuthenticatedUser(blockerId);

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            throw new ResourceNotFoundException("Block not found");
        }

        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedUserId);
        log.info("User {} unblocked user {}", blockerId, blockedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBlockedUser(Long userId1, Long userId2) {
        return userBlockRepository.hasBlocked(userId1, userId2);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedByUser(Long userId1, Long userId2) {
        return userBlockRepository.isBlockedBy(userId1, userId2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getBlockedUsers(Long userId) {
        return userBlockRepository.findByBlockerId(userId)
            .stream()
            .map(UserBlock::getBlocked)
            .toList();
    }

    // ===== Reporting =====

    @Override
    public void reportUser(Long reporterId, Long reportedUserId, ReportUserRequest request) {
        validateAuthenticatedUser(reporterId);

        if (reporterId.equals(reportedUserId)) {
            throw new BadRequestException("Cannot report yourself");
        }

        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User reported = userRepository.findById(reportedUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        // Check for duplicate pending reports
        if (userReportRepository.hasPendingReportBetweenUsers(reporterId, reportedUserId)) {
            throw new BadRequestException("You have already reported this user. Please wait for admin review.");
        }

        UserReport report = UserReport.builder()
            .reporter(reporter)
            .reported(reported)
            .reason(request.getReason())
            .description(request.getDescription())
            .status(ReportStatus.PENDING)
            .build();

        userReportRepository.save(report);
        log.info("User {} reported user {} for: {}", reporterId, reportedUserId, request.getReason());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingReportBetweenUsers(Long reporterId, Long reportedUserId) {
        return userReportRepository.hasPendingReportBetweenUsers(reporterId, reportedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnresolvedReportCount(Long userId) {
        return userReportRepository.countUnresolvedReportsAgainstUser(userId);
    }

    // ===== Privacy & Visibility =====

    @Override
    @Transactional(readOnly = true)
    public boolean canViewProfile(Long userId1, Long userId2) {
        // Can't view if either user has blocked the other
        return !hasBlockedUser(userId1, userId2) && !isBlockedByUser(userId1, userId2);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canMessage(Long userId1, Long userId2) {
        // Can't message if either user has blocked the other
        return !hasBlockedUser(userId1, userId2) && !isBlockedByUser(userId1, userId2);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canInviteToEvent(Long userId1, Long userId2) {
        // Can't invite if either user has blocked the other
        return !hasBlockedUser(userId1, userId2) && !isBlockedByUser(userId1, userId2);
    }

    // ===== Helper Methods =====

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
    }

    /**
     * Validates that the provided userId matches the currently authenticated user.
     * This provides defense-in-depth authorization checking at the service layer.
     *
     * @param userId the userId to validate
     * @throws AccessDeniedException if the userId doesn't match the authenticated user
     */
    private void validateAuthenticatedUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof SecurityUser securityUser)) {
            throw new AccessDeniedException("Invalid authentication principal");
        }

        if (!securityUser.getUserId().equals(userId)) {
            log.warn("Authorization violation: User {} attempted to perform action for user {}",
                    securityUser.getUserId(), userId);
            throw new AccessDeniedException("You are not authorized to perform this action for another user");
        }
    }

    private UserProfileResponse mapToProfileResponse(User user, Long currentUserId, boolean isOwnProfile) {
        int age = user.getDob() != null
            ? java.time.Period.between(user.getDob(), java.time.LocalDate.now()).getYears()
            : 0;

        return new UserProfileResponse(
            user.getId(),
            isOwnProfile ? user.getEmail() : null,
            user.getDisplayName(),
            user.getLegalName(),
            user.getBio(),
            user.getGeneralLocation(),
            isOwnProfile ? user.getPrimaryAddress() : null,
            isOwnProfile ? user.getPhoneNumber() : null,
            isOwnProfile ? user.getDob() : null,
            age,
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
}
