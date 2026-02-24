package com.theinside.partii.service;

import com.theinside.partii.dto.AttendeeResponse;
import com.theinside.partii.entity.Event;
import com.theinside.partii.entity.EventAttendee;
import com.theinside.partii.entity.User;
import com.theinside.partii.enums.AttendeeStatus;
import com.theinside.partii.enums.EventStatus;
import com.theinside.partii.exception.BadRequestException;
import com.theinside.partii.exception.ResourceNotFoundException;
import com.theinside.partii.exception.UnauthorizedException;
import com.theinside.partii.repository.EventAttendeeRepository;
import com.theinside.partii.repository.EventRepository;
import com.theinside.partii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AttendeeServiceImpl implements AttendeeService {

    private final EventRepository eventRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final UserRepository userRepository;

    @Override
    public AttendeeResponse requestToJoin(Long eventId, Long userId) {
        Event event = findEventOrThrow(eventId);
        User user = findUserOrThrow(userId);

        // Cannot join your own event
        if (event.getOrganizer().getId().equals(userId)) {
            throw new BadRequestException("Organizer cannot join their own event");
        }

        // Check for duplicate request
        if (attendeeRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new IllegalStateException("You have already requested to join this event");
        }

        // Event must be ACTIVE or FULL (waitlist)
        if (event.getStatus() != EventStatus.ACTIVE && event.getStatus() != EventStatus.FULL) {
            throw new BadRequestException("This event is not accepting join requests");
        }

        // Check join deadline
        if (event.getJoinDeadline() != null
                && java.time.LocalDateTime.now().isAfter(event.getJoinDeadline())) {
            throw new BadRequestException("The join deadline for this event has passed");
        }

        // Determine initial status: PENDING if spots available, WAITLIST if full
        AttendeeStatus initialStatus = event.hasAvailableSpots()
            ? AttendeeStatus.PENDING
            : AttendeeStatus.WAITLIST;

        EventAttendee attendee = EventAttendee.builder()
            .event(event)
            .user(user)
            .status(initialStatus)
            .build();

        EventAttendee saved = attendeeRepository.save(attendee);
        log.info("User {} requested to join event {} with status {}", userId, eventId, initialStatus);

        return toResponse(saved);
    }

    @Override
    public AttendeeResponse approveRequest(Long eventId, Long userId, Long organizerId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);

        EventAttendee attendee = findAttendeeOrThrow(eventId, userId);

        if (attendee.getStatus() != AttendeeStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be approved");
        }

        if (!event.hasAvailableSpots()) {
            throw new BadRequestException("Event is at capacity. Increase max attendees first.");
        }

        attendee.approve();
        event.incrementAttendees();

        attendeeRepository.save(attendee);
        eventRepository.save(event);
        log.info("Organizer {} approved user {} for event {}", organizerId, userId, eventId);

        return toResponse(attendee);
    }

    @Override
    public AttendeeResponse declineRequest(Long eventId, Long userId, Long organizerId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);

        EventAttendee attendee = findAttendeeOrThrow(eventId, userId);

        if (attendee.getStatus() != AttendeeStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be declined");
        }

        attendee.decline();
        attendeeRepository.save(attendee);
        log.info("Organizer {} declined user {} for event {}", organizerId, userId, eventId);

        return toResponse(attendee);
    }

    @Override
    public void removeAttendee(Long eventId, Long userId, Long organizerId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);

        EventAttendee attendee = findAttendeeOrThrow(eventId, userId);

        if (attendee.getStatus() != AttendeeStatus.APPROVED) {
            throw new BadRequestException("Only approved attendees can be removed");
        }

        attendee.remove();
        event.decrementAttendees();

        attendeeRepository.save(attendee);
        eventRepository.save(event);
        log.info("Organizer {} removed user {} from event {}", organizerId, userId, eventId);

        // Promote first waitlisted user to PENDING
        attendeeRepository.findFirstInWaitlist(eventId).ifPresent(waitlisted -> {
            waitlisted.setStatus(AttendeeStatus.PENDING);
            attendeeRepository.save(waitlisted);
            log.info("Promoted waitlisted user {} to PENDING for event {}", waitlisted.getUser().getId(), eventId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendeeResponse> getAttendees(Long eventId, String status, Pageable pageable) {
        findEventOrThrow(eventId);

        Page<EventAttendee> attendees;
        if (status != null && !status.isBlank()) {
            AttendeeStatus attendeeStatus = AttendeeStatus.valueOf(status.toUpperCase());
            attendees = attendeeRepository.findByEventIdAndStatus(eventId, attendeeStatus, pageable);
        } else {
            attendees = attendeeRepository.findByEventId(eventId, pageable);
        }

        return attendees.map(this::toResponse);
    }

    // ===== Helper methods =====

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private EventAttendee findAttendeeOrThrow(Long eventId, Long userId) {
        return attendeeRepository.findByEventIdAndUserId(eventId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Attendee not found for this event"));
    }

    private void verifyOrganizer(Event event, Long userId) {
        if (!event.getOrganizer().getId().equals(userId)) {
            throw new UnauthorizedException("Only the organizer can perform this action");
        }
    }

    private AttendeeResponse toResponse(EventAttendee attendee) {
        User user = attendee.getUser();
        return new AttendeeResponse(
            attendee.getId(),
            user.getId(),
            user.getDisplayName(),
            user.getProfilePictureUrl(),
            attendee.getStatus(),
            attendee.getPaymentAmount(),
            attendee.getPaymentStatus(),
            attendee.getAmountPaid(),
            attendee.getJoinedAt(),
            attendee.getApprovedAt()
        );
    }
}
