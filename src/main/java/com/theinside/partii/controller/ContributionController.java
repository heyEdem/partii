package com.theinside.partii.controller;

import com.theinside.partii.dto.*;
import com.theinside.partii.security.SecurityUser;
import com.theinside.partii.service.ContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for event contribution management.
 * Handles CRUD, lifecycle transitions, and read-only queries for contribution items.
 */
@Slf4j
@RestController
@RequestMapping("/partii/api/v1/events/{eventId}/contributions")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;

    // ===== CRUD Endpoints =====

    /**
     * POST /events/{eventId}/contributions
     * Create a new contribution item for an event.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> createItem(
        @PathVariable Long eventId,
        @Valid @RequestBody CreateContributionItemRequest request,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} creating contribution item for event {}", user.getUserId(), eventId);
        ContributionItemResponse response = contributionService.createItem(eventId, user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /events/{eventId}/contributions
     * List contribution items for an event with optional filters.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ContributionItemResponse>> listItems(
        @PathVariable Long eventId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String priority,
        @PageableDefault(size = 20) Pageable pageable,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.debug("Listing contributions for event {}, status={}, category={}, type={}, priority={}",
                eventId, status, category, type, priority);
        Page<ContributionItemResponse> items = contributionService.listItems(
                eventId, user.getUserId(), status, category, type, priority, pageable);
        return ResponseEntity.ok(items);
    }

    /**
     * GET /events/{eventId}/contributions/{itemId}
     * Get a single contribution item by ID.
     */
    @GetMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> getItem(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.debug("Getting contribution item {} for event {}", itemId, eventId);
        ContributionItemResponse response = contributionService.getItem(eventId, itemId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /events/{eventId}/contributions/{itemId}
     * Update an existing contribution item.
     */
    @PutMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> updateItem(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateContributionItemRequest request,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} updating contribution item {} for event {}", user.getUserId(), itemId, eventId);
        ContributionItemResponse response = contributionService.updateItem(eventId, itemId, user.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /events/{eventId}/contributions/{itemId}
     * Delete a contribution item.
     */
    @DeleteMapping("/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteItem(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} deleting contribution item {} for event {}", user.getUserId(), itemId, eventId);
        contributionService.deleteItem(eventId, itemId, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ===== Lifecycle Endpoints =====

    /**
     * POST /events/{eventId}/contributions/{itemId}/claim
     * Claim a contribution item as the current user.
     */
    @PostMapping("/{itemId}/claim")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> claimItem(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} claiming contribution item {} for event {}", user.getUserId(), itemId, eventId);
        ContributionItemResponse response = contributionService.claimItem(eventId, itemId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /events/{eventId}/contributions/{itemId}/confirm
     * Confirm a claimed contribution item. Organizer only.
     */
    @PostMapping("/{itemId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> confirmItem(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} confirming contribution item {} for event {}", user.getUserId(), itemId, eventId);
        ContributionItemResponse response = contributionService.confirmItem(eventId, itemId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /events/{eventId}/contributions/{itemId}/assign/{userId}
     * Assign a contribution item to a specific user. Organizer only.
     */
    @PostMapping("/{itemId}/assign/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> assignItem(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @PathVariable Long userId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("Organizer {} assigning contribution item {} to user {} for event {}",
                user.getUserId(), itemId, userId, eventId);
        ContributionItemResponse response = contributionService.assignItem(eventId, itemId, user.getUserId(), userId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /events/{eventId}/contributions/{itemId}/accept
     * Accept a contribution assignment.
     */
    @PostMapping("/{itemId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> acceptAssignment(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} accepting assignment for contribution item {} in event {}", user.getUserId(), itemId, eventId);
        ContributionItemResponse response = contributionService.acceptAssignment(eventId, itemId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /events/{eventId}/contributions/{itemId}/decline
     * Decline a contribution assignment.
     */
    @PostMapping("/{itemId}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> declineAssignment(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} declining assignment for contribution item {} in event {}", user.getUserId(), itemId, eventId);
        ContributionItemResponse response = contributionService.declineAssignment(eventId, itemId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /events/{eventId}/contributions/{itemId}/release
     * Release a contribution item back to available. Organizer only.
     */
    @PostMapping("/{itemId}/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> releaseItem(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} releasing contribution item {} for event {}", user.getUserId(), itemId, eventId);
        ContributionItemResponse response = contributionService.releaseItem(eventId, itemId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /events/{eventId}/contributions/{itemId}/complete
     * Mark a contribution item as complete. Organizer only.
     */
    @PostMapping("/{itemId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionItemResponse> completeItem(
        @PathVariable Long eventId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} completing contribution item {} for event {}", user.getUserId(), itemId, eventId);
        ContributionItemResponse response = contributionService.completeItem(eventId, itemId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    // ===== Read-only Endpoints =====

    /**
     * GET /events/{eventId}/contributions/summary
     * Get a summary of contributions for an event.
     */
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContributionSummaryResponse> getSummary(
        @PathVariable Long eventId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.debug("Getting contribution summary for event {}", eventId);
        ContributionSummaryResponse response = contributionService.getSummary(eventId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /events/{eventId}/contributions/categories
     * Get all categories for contributions in an event.
     */
    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getCategories(
        @PathVariable Long eventId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.debug("Getting contribution categories for event {}", eventId);
        List<String> categories = contributionService.getCategories(eventId, user.getUserId());
        return ResponseEntity.ok(categories);
    }
}
