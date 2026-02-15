package com.theinside.partii.service;

import com.theinside.partii.dto.*;
import com.theinside.partii.entity.ContributionItem;
import com.theinside.partii.entity.Event;
import com.theinside.partii.entity.User;
import com.theinside.partii.enums.ContributionStatus;
import com.theinside.partii.enums.EventStatus;
import com.theinside.partii.exception.NotFoundException;
import com.theinside.partii.exception.ResourceNotFoundException;
import com.theinside.partii.mapper.EventMapper;
import com.theinside.partii.repository.ContributionItemRepository;
import com.theinside.partii.repository.EventAttendeeRepository;
import com.theinside.partii.repository.EventRepository;
import com.theinside.partii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of EventService.
 * Handles event creation and management functionality.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventAttendeeRepository eventAttendeeRepository;
    private final ContributionItemRepository contributionItemRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    @Override
    @Transactional
    public EventResponse createEvent(Long userId, CreateEventRequest request) {
        User organizer = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        // Validate deadlines are before event date
        if (request.paymentDeadline() != null && request.paymentDeadline().isAfter(request.eventDate())) {
            throw new IllegalArgumentException("Payment deadline must be before event date");
        }
        if (request.joinDeadline() != null && request.joinDeadline().isAfter(request.eventDate())) {
            throw new IllegalArgumentException("Join deadline must be before event date");
        }

        // Build the event
        Event event = Event.builder()
            .organizer(organizer)
            .title(request.title())
            .description(request.description())
            .eventType(request.eventType())
            .locationAddress(request.locationAddress())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .eventDate(request.eventDate())
            .imageUrl(request.imageUrl())
            .estimatedBudget(request.estimatedBudget())
            .currency(request.currency() != null ? request.currency() : "GHS")
            .maxAttendees(request.maxAttendees() != null ? request.maxAttendees() : 10)
            .currentAttendees(0)
            .ageRestriction(request.ageRestriction())
            .paymentDeadline(request.paymentDeadline())
            .joinDeadline(request.joinDeadline())
            .visibility(request.visibility())
            .status(EventStatus.DRAFT)
            .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event created: {} by user: {}", savedEvent.getId(), userId);

        // Create contribution items if provided
        if (request.contributionItems() != null && !request.contributionItems().isEmpty()) {
            List<ContributionItem> items = new ArrayList<>();
            for (CreateContributionItemRequest itemRequest : request.contributionItems()) {
                ContributionItem item = buildContributionItem(savedEvent, itemRequest);
                items.add(item);
            }
            contributionItemRepository.saveAll(items);
            log.info("Created {} contribution items for event: {}", items.size(), savedEvent.getId());
        }

        return mapToEventResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return mapToEventResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventByPrivateLinkCode(String privateLinkCode) {
        Event event = eventRepository.findByPrivateLinkCode(privateLinkCode)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return mapToEventResponse(event);
    }

    private ContributionItem buildContributionItem(Event event, CreateContributionItemRequest request) {
        return ContributionItem.builder()
            .event(event)
            .name(request.name())
            .category(request.category())
            .type(request.type())
            .quantity(request.quantity() != null ? request.quantity() : 1)
            .timeCommitment(request.timeCommitment())
            .estimatedCost(request.estimatedCost())
            .priority(request.priority() != null ? request.priority() : com.theinside.partii.enums.Priority.NICE_TO_HAVE)
            .notes(request.notes())
            .status(ContributionStatus.AVAILABLE)
            .completed(false)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
            .map(this::mapToEventResponse);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long eventId, Long userId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Check if user is the organizer
        if (!event.getOrganizer().getId().equals(userId)) {
            throw new com.theinside.partii.exception.UnauthorizedException("Only the organizer can update this event");
        }

        if (request.eventDate() != null) {
            if (request.paymentDeadline() != null && request.paymentDeadline().isAfter(request.eventDate())) {
                throw new IllegalArgumentException("Payment deadline must be before event date");
            }
            if (request.joinDeadline() != null && request.joinDeadline().isAfter(request.eventDate())) {
                throw new IllegalArgumentException("Join deadline must be before event date");
            }
        }

        // Use MapStruct to apply partial updates (null values are ignored)
        eventMapper.updateEventFromDto(request, event);

        Event updatedEvent = eventRepository.save(event);
        log.info("Event patched: {} by user: {}", eventId, userId);

        return mapToEventResponse(updatedEvent);
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Check if user is the organizer
        if (!event.getOrganizer().getId().equals(userId)) {
            throw new com.theinside.partii.exception.UnauthorizedException("Only the organizer can delete this event");
        }

        // Only allow deletion if event is in DRAFT or CANCELLED status
        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.CANCELLED) {
            throw new IllegalStateException("Only draft or cancelled events can be deleted");
        }

        eventRepository.delete(event);
        log.info("Event deleted: {} by user: {}", eventId, userId);
    }

    @Override
    @Transactional
    public EventResponse publishEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Check if user is the organizer
        if (!event.getOrganizer().getId().equals(userId)) {
            throw new com.theinside.partii.exception.UnauthorizedException("Only the organizer can publish this event");
        }

        // Can only publish draft events
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException("Only draft events can be published");
        }

        event.setStatus(EventStatus.ACTIVE);
        Event updatedEvent = eventRepository.save(event);
        log.info("Event published: {} by user: {}", eventId, userId);

        return mapToEventResponse(updatedEvent);
    }

    @Override
    @Transactional
    public EventResponse cancelEvent(Long eventId, Long userId, String reason) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Check if user is the organizer
        if (!event.getOrganizer().getId().equals(userId)) {
            throw new com.theinside.partii.exception.UnauthorizedException("Only the organizer can cancel this event");
        }

        // Cannot cancel already cancelled events
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalStateException("Event is already cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);
        event.setCancellationReason(reason);
        Event updatedEvent = eventRepository.save(event);
        log.info("Event cancelled: {} by user: {} with reason: {}", eventId, userId, reason);

        return mapToEventResponse(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<EventResponse> getPublicEvents(String cursorString, int limit) {
        EventCursor cursor = cursorString != null ? EventCursor.decode(cursorString) : null;
        LocalDateTime afterDate = cursor != null ? cursor.eventDate() : null;
        Long afterId = cursor != null ? cursor.id() : null;

        // Fetch limit+1 to determine if there's a next page
        List<Event> events = eventRepository.findPublicEventsKeyset(
            LocalDateTime.now(),
            afterDate,
            afterId,
            Limit.of(limit + 1)
        );

        if (events.isEmpty()) {
            return CursorPage.empty();
        }

        // Generate next cursor from the last item
        String nextCursor = null;
        if (events.size() > limit) {
            Event lastEvent = events.get(limit - 1);
            nextCursor = new EventCursor(lastEvent.getEventDate(), lastEvent.getId()).encode();
        }

        List<EventResponse> responses = events.stream()
            .map(this::mapToEventResponse)
            .toList();

        return CursorPage.of(responses, nextCursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<EventResponse> getAllEventsKeyset(String cursorString, int limit) {
        // For admin view, we use createdAt as the sort key
        EventCursor cursor = cursorString != null ? EventCursor.decode(cursorString) : null;
        // Note: EventCursor uses eventDate, but for admin we need createdAt
        // We'll reuse the structure but interpret eventDate as createdAt timestamp
        java.time.Instant afterDate = cursor != null
            ? cursor.eventDate().atZone(java.time.ZoneId.systemDefault()).toInstant()
            : null;
        Long afterId = cursor != null ? cursor.id() : null;

        List<Event> events = eventRepository.findAllEventsKeyset(
            afterDate,
            afterId,
            Limit.of(limit + 1)
        );

        if (events.isEmpty()) {
            return CursorPage.empty();
        }

        String nextCursor = null;
        if (events.size() > limit) {
            Event lastEvent = events.get(limit - 1);
            // Convert createdAt (Instant) to LocalDateTime for cursor encoding
            LocalDateTime createdAtAsLocalDateTime = LocalDateTime.ofInstant(
                lastEvent.getCreatedAt(),
                java.time.ZoneId.systemDefault()
            );
            nextCursor = new EventCursor(createdAtAsLocalDateTime, lastEvent.getId()).encode();
        }

        List<EventResponse> responses = events.stream()
            .map(this::mapToEventResponse)
            .toList();

        return CursorPage.of(responses, nextCursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getMyOrganizedEvents(Long userId, Pageable pageable) {
        return eventRepository.findByOrganizerId(userId, pageable)
            .map(this::mapToEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getMyAttendingEvents(Long userId) {
        return eventAttendeeRepository.findActiveParticipationsByUser(userId).stream()
            .map(ea -> mapToEventResponse(ea.getEvent()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getMyPendingEvents(Long userId) {
        return eventAttendeeRepository.findPendingRequestsByUser(userId).stream()
            .map(ea -> mapToEventResponse(ea.getEvent()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getMyPastEvents(Long userId, Pageable pageable) {
        return eventAttendeeRepository.findPastParticipationsByUser(userId, pageable)
            .map(ea -> mapToEventResponse(ea.getEvent()));
    }

    private EventResponse mapToEventResponse(Event event) {
        return new EventResponse(
            event.getId(),
            event.getOrganizer().getId(),
            event.getOrganizer().getDisplayName(),
            event.getTitle(),
            event.getDescription(),
            event.getEventType(),
            event.getLocationAddress(),
            event.getLatitude(),
            event.getLongitude(),
            event.getEventDate(),
            event.getImageUrl(),
            event.getEstimatedBudget(),
            event.getCurrency(),
            event.getMaxAttendees(),
            event.getCurrentAttendees(),
            event.getAgeRestriction(),
            event.getPaymentDeadline(),
            event.getJoinDeadline(),
            event.getVisibility(),
            event.getStatus(),
            event.getPrivateLinkCode(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }
}
