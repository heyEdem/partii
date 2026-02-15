package com.theinside.partii.controller;

import com.theinside.partii.dto.CreateEventRequest;
import com.theinside.partii.dto.CursorPage;
import com.theinside.partii.dto.EventResponse;
import com.theinside.partii.dto.UpdateEventRequest;
import com.theinside.partii.security.SecurityUser;
import com.theinside.partii.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * REST controller for event operations.
 * Handles event creation, retrieval, and management.
 */
@Slf4j
@RestController
@RequestMapping("/partii/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * GET /api/events/public
     * List all public events using keyset pagination (significantly faster).
     * Use cursor from response to fetch next page.
     */
    @GetMapping("/public")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CursorPage<EventResponse>> listPublicEvents(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        log.debug("Fetching public events with cursor pagination, cursor: {}, limit: {}", cursor, limit);
        CursorPage<EventResponse> events = eventService.getPublicEvents(cursor, limit);
        return ResponseEntity.ok(events);
    }

    /**
     * POST /api/events
     * Create a new event.
     */
    @PostMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> createEvent(
        @AuthenticationPrincipal SecurityUser user,
        @Valid @RequestBody CreateEventRequest request
    ) {
        log.info("Creating new event by user: {}", user.getUserId());
        EventResponse response = eventService.createEvent(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/events/{id}
     * Get an event by its ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        log.debug("Fetching event: {}", id);
        EventResponse response = eventService.getEvent(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/events/{id}
     * Partial update of an existing event (only non-null fields are updated).
     */
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> updateEvent(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser user,
        @Valid @RequestBody UpdateEventRequest request
    ) {
        log.info("Patching event: {} by user: {}", id, user.getUserId());
        EventResponse response = eventService.updateEvent(id, user.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/events/{id}
     * Delete an event (only draft or cancelled events).
     */
    @DeleteMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEvent(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("Deleting event: {} by user: {}", id, user.getUserId());
        eventService.deleteEvent(id, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/events/{id}/publish
     * Publish an event (change status from DRAFT to ACTIVE).
     */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> publishEvent(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("Publishing event: {} by user: {}", id, user.getUserId());
        EventResponse response = eventService.publishEvent(id, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/events/{id}/cancel
     * Cancel an event.
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> cancelEvent(
        @PathVariable Long id,
        @AuthenticationPrincipal SecurityUser user,
        @RequestBody(required = false) CancelEventRequest cancelRequest
    ) {
        String reason = (cancelRequest != null && cancelRequest.reason() != null)
            ? cancelRequest.reason()
            : "No reason provided";
        log.info("Cancelling event: {} by user: {} with reason: {}", id, user.getUserId(), reason);
        EventResponse response = eventService.cancelEvent(id, user.getUserId(), reason);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/events/by-code/{code}
     * Get an event by its private link code.
     */
    @GetMapping("/by-code/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventResponse> getEventByPrivateCode(@PathVariable String code) {
        log.debug("Fetching event by private code");
        EventResponse response = eventService.getEventByPrivateLinkCode(code);
        return ResponseEntity.ok(response);
    }

    // ===== My Events Endpoints =====

    @GetMapping("/my-events/organized")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EventResponse>> getMyOrganizedEvents(
        @AuthenticationPrincipal SecurityUser user,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<EventResponse> events = eventService.getMyOrganizedEvents(user.getUserId(), pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-events/attending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventResponse>> getMyAttendingEvents(
        @AuthenticationPrincipal SecurityUser user
    ) {
        List<EventResponse> events = eventService.getMyAttendingEvents(user.getUserId());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-events/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventResponse>> getMyPendingEvents(
        @AuthenticationPrincipal SecurityUser user
    ) {
        List<EventResponse> events = eventService.getMyPendingEvents(user.getUserId());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/my-events/past")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EventResponse>> getMyPastEvents(
        @AuthenticationPrincipal SecurityUser user,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<EventResponse> events = eventService.getMyPastEvents(user.getUserId(), pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/health.
     * Health check endpoint for event service.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Event service is running");
    }

    /**
     * Simple request DTO for cancelling events.
     */
    private record CancelEventRequest(String reason) {}
}
