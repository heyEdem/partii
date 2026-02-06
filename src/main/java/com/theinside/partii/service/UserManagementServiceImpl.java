package com.theinside.partii.service;

import com.theinside.partii.dto.*;
import com.theinside.partii.entity.User;
import com.theinside.partii.entity.UserBlock;
import com.theinside.partii.entity.UserReport;
import com.theinside.partii.enums.ReportStatus;
import com.theinside.partii.exception.BadRequestException;
import com.theinside.partii.exception.ResourceNotFoundException;
import com.theinside.partii.mapper.UserMapper;
import com.theinside.partii.repository.UserBlockRepository;
import com.theinside.partii.repository.UserReportRepository;
import com.theinside.partii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of UserManagementService.
 * Handles profile management, blocking, and reporting functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserReportRepository userReportRepository;
    private final UserMapper userMapper;

    // ===== Profile Management =====

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
    @Transactional(readOnly = true)
    public UserProfileResponse getOwnProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToProfileResponse(user, userId, true);
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate age if dob is provided
        if (request.dob() != null) {
            int age = java.time.Period.between(request.dob(), java.time.LocalDate.now()).getYears();
            if (age < 18) {
                throw new BadRequestException("User must be at least 18 years old");
            }
        }

        // Update fields (only non-null values)
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
        if (request.primaryAddress() != null) {
            user.setPrimaryAddress(request.primaryAddress());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.dob() != null) {
            user.setDob(request.dob());
        }

        User updated = userRepository.save(user);
        log.info("User {} profile updated", userId);

        return mapToProfileResponse(updated, userId, true);
    }

    // ===== Blocking =====

    @Override
    public void blockUser(Long blockerId, Long blockedUserId) {
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
