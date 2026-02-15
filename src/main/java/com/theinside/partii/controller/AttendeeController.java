package com.theinside.partii.controller;

import com.theinside.partii.dto.AttendeeResponse;
import com.theinside.partii.security.SecurityUser;
import com.theinside.partii.service.AttendeeService;
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



/**
 * REST controller for event attendee management.
 * Handles join requests, approvals, and attendee listing.
 */
@Slf4j
@RestController
@RequestMapping("/partii/api/v1/events/{eventId}/attendees")
@RequiredArgsConstructor
public class AttendeeController {

    private final AttendeeService attendeeService;

    /**
     * POST /events/{eventId}/attendees/join
     * Request to join an event. Auto-waitlists if event is full.
     */
    @PostMapping("/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendeeResponse> requestToJoin(
        @PathVariable Long eventId,
        @AuthenticationPrincipal SecurityUser user
    ) {
        log.info("User {} requesting to join event {}", user.getUserId(), eventId);
        AttendeeResponse response = attendeeService.requestToJoin(eventId, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /events/{eventId}/attendees
     * List attendees for an event. Optional status filter.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AttendeeResponse>> getAttendees(
        @PathVariable Long eventId,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("Listing attendees for event {}, status filter: {}", eventId, status);
        Page<AttendeeResponse> attendees = attendeeService.getAttendees(eventId, status, pageable);
        return ResponseEntity.ok(attendees);
    }

    /**
     * POST /events/{eventId}/attendees/{userId}/approve
     * Approve a pending join request. Organizer only.
     */
    @PostMapping("/{userId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendeeResponse> approveRequest(
        @PathVariable Long eventId,
        @PathVariable Long userId,
        @AuthenticationPrincipal SecurityUser organizer
    ) {
        log.info("Organizer {} approving user {} for event {}", organizer.getUserId(), userId, eventId);
        AttendeeResponse response = attendeeService.approveRequest(eventId, userId, organizer.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /events/{eventId}/attendees/{userId}/decline
     * Decline a pending join request. Organizer only.
     */
    @PostMapping("/{userId}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendeeResponse> declineRequest(
        @PathVariable Long eventId,
        @PathVariable Long userId,
        @AuthenticationPrincipal SecurityUser organizer
    ) {
        log.info("Organizer {} declining user {} for event {}", organizer.getUserId(), userId, eventId);
        AttendeeResponse response = attendeeService.declineRequest(eventId, userId, organizer.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /events/{eventId}/attendees/{userId}/remove
     * Remove an approved attendee. Organizer only.
     * Promotes first waitlisted user to PENDING.
     */
    @PostMapping("/{userId}/remove")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeAttendee(
        @PathVariable Long eventId,
        @PathVariable Long userId,
        @AuthenticationPrincipal SecurityUser organizer
    ) {
        log.info("Organizer {} removing user {} from event {}", organizer.getUserId(), userId, eventId);
        attendeeService.removeAttendee(eventId, userId, organizer.getUserId());
        return ResponseEntity.noContent().build();
    }
}
