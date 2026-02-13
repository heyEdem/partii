package com.theinside.partii.controller;

import com.theinside.partii.dto.*;
import com.theinside.partii.security.SecurityUser;
import com.theinside.partii.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user operations.
 * Handles profile management, blocking, and reporting.
 */
@Slf4j
@RestController
@RequestMapping("/partii/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    /**
     * GET /api/users/me
     * Get the current user's own profile.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal SecurityUser user) {
        return userService.getProfile(user.getUserId());
    }

    /**
     * GET /api/users/{id}
     * Get a user's public profile.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getUserProfile(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser currentUser
    ) {
        log.debug("Fetching profile for user {}", id);
        UserProfileResponse profile = userService.getPublicProfile(id, currentUser.getUserId());
        return ResponseEntity.ok(profile);
    }

    /**
     * PATCH /api/users/me
     * Update the current user's profile.
     */
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileResponse updateMyProfile(
        @AuthenticationPrincipal SecurityUser user,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(user.getUserId(), request);
    }

    // ===== Blocking Endpoints =====

    /**
     * POST /api/users/{id}/block
     * Block a user.
     */
    @PostMapping("/{id}/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> blockUser(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser currentUser,
        @RequestBody(required = false) BlockUserRequest request
    ) {
        if (id.equals(currentUser.getUserId())) {
            return ResponseEntity.badRequest()
                    .body(new GenericMessageResponse("You cannot block yourself"));
        }

        log.info("User {} blocking user {}", currentUser.getUserId(), id);
        userService.blockUser(currentUser.getUserId(), id);
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
        userService.unblockUser(currentUser.getUserId(), id);
        return ResponseEntity.noContent().build();
    }


    /**
     * POST /api/users/{id}/report
     * Report a user for inappropriate behavior.
     */
    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reportUser(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser currentUser,
        @Valid @RequestBody ReportUserRequest request
    ) {
        if (id.equals(currentUser.getUserId())) {
            return ResponseEntity.badRequest()
                    .body(new GenericMessageResponse("You cannot report yourself"));
        }

        log.info("User {} reporting user {}", currentUser.getUserId(), id);
        userService.reportUser(currentUser.getUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    /**
     * GET /api/users/health
     * Health check endpoint for user service.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User service is running");
    }
}
