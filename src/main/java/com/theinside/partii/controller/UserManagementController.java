package com.theinside.partii.controller;

import com.theinside.partii.dto.*;
import com.theinside.partii.security.SecurityUser;
import com.theinside.partii.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user management endpoints.
 * Handles profile management, blocking, and reporting.
 */
@Slf4j
@RestController
@RequestMapping("/partii/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    // ===== Profile Endpoints =====

    /**
     * GET /api/users/{id}
     * Get a user's public profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserProfile(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser currentUser
    ) {
        log.debug("Fetching profile for user {}", id);
        UserProfileResponse profile = userManagementService.getPublicProfile(id, currentUser.getUserId());
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /api/users/me
     * Get the current authenticated user's own profile.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getOwnProfile(
        @AuthenticationPrincipal SecurityUser currentUser
    ) {
        log.debug("Fetching own profile for user {}", currentUser.getUserId());
        UserProfileResponse profile = userManagementService.getOwnProfile(currentUser.getUserId());
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/users/me
     * Update the current user's profile.
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(
        @AuthenticationPrincipal SecurityUser currentUser,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("User {} updating profile", currentUser.getUserId());
        UserProfileResponse updated = userManagementService.updateProfile(currentUser.getUserId(), request);
        return ResponseEntity.ok(updated);
    }

    // ===== Blocking Endpoints =====

    /**
     * POST /api/users/{id}/block
     * Block a user.
     */
    @PostMapping("/{id}/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> blockUser(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser currentUser,
        @RequestBody(required = false) BlockUserRequest request
    ) {
        if (id.equals(currentUser.getUserId())) {
            return ResponseEntity.badRequest().build();
        }

        log.info("User {} blocking user {}", currentUser.getUserId(), id);
        userManagementService.blockUser(currentUser.getUserId(), id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * DELETE /api/users/{id}/block
     * Unblock a previously blocked user.
     */
    @DeleteMapping("/{id}/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unblockUser(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser currentUser
    ) {
        log.info("User {} unblocking user {}", currentUser.getUserId(), id);
        userManagementService.unblockUser(currentUser.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    // ===== Reporting Endpoints =====

    /**
     * POST /api/users/{id}/report
     * Report a user for inappropriate behavior.
     */
    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> reportUser(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser currentUser,
        @Valid @RequestBody ReportUserRequest request
    ) {
        if (id.equals(currentUser.getUserId())) {
            return ResponseEntity.badRequest().build();
        }

        log.info("User {} reporting user {}", currentUser.getUserId(), id);
        userManagementService.reportUser(currentUser.getUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ===== Health Check =====

    /**
     * GET /api/users/health
     * Health check endpoint for user management service.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User management service is running");
    }
}
