package com.theinside.partii.service;

import com.theinside.partii.dto.AttendeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


/**
 * Service interface for managing event attendees.
 * Handles join requests, approvals, removals, and waitlist management.
 */
public interface AttendeeService {

    /**
     * Request to join an event.
     * If the event is full, the user is placed on the waitlist.
     */
    AttendeeResponse requestToJoin(Long eventId, Long userId);

    /**
     * Approve a pending join request. Only the organizer can approve.
     * Increments event attendee count and transitions event to FULL if at capacity.
     */
    AttendeeResponse approveRequest(Long eventId, Long userId, Long organizerId);

    /**
     * Decline a pending join request. Only the organizer can decline.
     */
    AttendeeResponse declineRequest(Long eventId, Long userId, Long organizerId);

    /**
     * Remove an approved attendee from the event. Only the organizer can remove.
     * Decrements event attendee count and promotes first waitlisted user if any.
     */
    void removeAttendee(Long eventId, Long userId, Long organizerId);

    /**
     * List attendees for an event with optional status filtering.
     */
    Page<AttendeeResponse> getAttendees(Long eventId, String status, Pageable pageable);
}
