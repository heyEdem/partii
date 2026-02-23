package com.theinside.partii.service;

import com.theinside.partii.dto.*;
import com.theinside.partii.entity.ContributionItem;
import com.theinside.partii.entity.Event;
import com.theinside.partii.entity.User;
import com.theinside.partii.enums.*;
import com.theinside.partii.exception.BadRequestException;
import com.theinside.partii.exception.ResourceNotFoundException;
import com.theinside.partii.exception.UnauthorizedException;
import com.theinside.partii.repository.ContributionItemRepository;
import com.theinside.partii.repository.EventAttendeeRepository;
import com.theinside.partii.repository.EventRepository;
import com.theinside.partii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ContributionServiceImpl implements ContributionService {

    private final EventRepository eventRepository;
    private final ContributionItemRepository contributionItemRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final UserRepository userRepository;

    // ===== CRUD =====

    @Override
    public ContributionItemResponse createItem(Long eventId, Long organizerId, CreateContributionItemRequest request) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);
        verifyEventActive(event);

        ContributionItem item = ContributionItem.builder()
                .event(event)
                .name(request.name())
                .category(request.category())
                .type(request.type())
                .quantity(request.quantity() != null ? request.quantity() : 1)
                .timeCommitment(request.timeCommitment())
                .estimatedCost(request.estimatedCost())
                .priority(request.priority() != null ? request.priority() : Priority.NICE_TO_HAVE)
                .notes(request.notes())
                .status(ContributionStatus.AVAILABLE)
                .completed(false)
                .build();

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("Organizer {} created contribution item {} for event {}", organizerId, saved.getId(), eventId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContributionItemResponse> listItems(Long eventId, Long userId, String status, String category,
                                                     String type, String priority, Pageable pageable) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizerOrApprovedAttendee(event, userId);

        Page<ContributionItem> items;

        if (status != null && !status.isBlank()) {
            ContributionStatus contributionStatus = ContributionStatus.valueOf(status.toUpperCase());
            items = contributionItemRepository.findByEventIdAndStatus(eventId, contributionStatus, pageable);
        } else if (category != null && !category.isBlank()) {
            items = contributionItemRepository.findByEventIdAndCategory(eventId, category, pageable);
        } else if (type != null && !type.isBlank()) {
            ContributionType contributionType = ContributionType.valueOf(type.toUpperCase());
            items = contributionItemRepository.findByEventIdAndType(eventId, contributionType, pageable);
        } else if (priority != null && !priority.isBlank()) {
            Priority priorityEnum = Priority.valueOf(priority.toUpperCase());
            items = contributionItemRepository.findByEventIdAndPriority(eventId, priorityEnum, pageable);
        } else {
            items = contributionItemRepository.findByEventId(eventId, pageable);
        }

        return items.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionItemResponse getItem(Long eventId, Long itemId, Long userId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizerOrApprovedAttendee(event, userId);

        ContributionItem item = findItemOrThrow(eventId, itemId);
        return toResponse(item);
    }

    @Override
    public ContributionItemResponse updateItem(Long eventId, Long itemId, Long organizerId,
                                                UpdateContributionItemRequest request) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);
        verifyEventActive(event);

        ContributionItem item = findItemOrThrow(eventId, itemId);

        if (item.getStatus() != ContributionStatus.AVAILABLE) {
            throw new BadRequestException("Only available items can be updated");
        }

        // Partial update: apply non-null fields
        if (request.name() != null) item.setName(request.name());
        if (request.category() != null) item.setCategory(request.category());
        if (request.type() != null) item.setType(request.type());
        if (request.quantity() != null) item.setQuantity(request.quantity());
        if (request.timeCommitment() != null) item.setTimeCommitment(request.timeCommitment());
        if (request.estimatedCost() != null) item.setEstimatedCost(request.estimatedCost());
        if (request.priority() != null) item.setPriority(request.priority());
        if (request.notes() != null) item.setNotes(request.notes());

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("Organizer {} updated contribution item {} for event {}", organizerId, itemId, eventId);
        return toResponse(saved);
    }

    @Override
    public void deleteItem(Long eventId, Long itemId, Long organizerId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);
        verifyEventActive(event);

        ContributionItem item = findItemOrThrow(eventId, itemId);

        if (item.getStatus() != ContributionStatus.AVAILABLE) {
            throw new BadRequestException("Only available items can be deleted");
        }

        contributionItemRepository.delete(item);
        log.info("Organizer {} deleted contribution item {} from event {}", organizerId, itemId, eventId);
    }

    // ===== Lifecycle =====

    @Override
    public ContributionItemResponse claimItem(Long eventId, Long itemId, Long userId) {
        Event event = findEventOrThrow(eventId);
        verifyApprovedAttendee(eventId, userId);
        verifyEventActive(event);

        ContributionItem item = findItemOrThrow(eventId, itemId);

        if (event.getOrganizer().getId().equals(userId)) {
            throw new BadRequestException("Organizer cannot claim items on their own event");
        }

        User user = findUserOrThrow(userId);
        item.claim(user); // throws IllegalStateException if not available

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("User {} claimed contribution item {} for event {}", userId, itemId, eventId);
        return toResponse(saved);
    }

    @Override
    public ContributionItemResponse confirmItem(Long eventId, Long itemId, Long organizerId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);

        ContributionItem item = findItemOrThrow(eventId, itemId);
        item.confirmClaim(); // throws IllegalStateException if not claimed

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("Organizer {} confirmed contribution item {} for event {}", organizerId, itemId, eventId);
        return toResponse(saved);
    }

    @Override
    public ContributionItemResponse assignItem(Long eventId, Long itemId, Long organizerId, Long assigneeId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);
        verifyEventActive(event);

        ContributionItem item = findItemOrThrow(eventId, itemId);

        // Verify assignee is an approved attendee
        if (!attendeeRepository.existsByEventIdAndUserIdAndStatus(eventId, assigneeId, AttendeeStatus.APPROVED)) {
            throw new BadRequestException("Assignee must be an approved attendee of this event");
        }

        User assignee = findUserOrThrow(assigneeId);
        item.assign(assignee); // throws IllegalStateException if not available

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("Organizer {} assigned contribution item {} to user {} for event {}", organizerId, itemId, assigneeId, eventId);
        return toResponse(saved);
    }

    @Override
    public ContributionItemResponse acceptAssignment(Long eventId, Long itemId, Long userId) {
        User user = findUserOrThrow(userId);
        ContributionItem item = findItemOrThrow(eventId, itemId);

        if (!item.isAssignedTo(user)) {
            throw new UnauthorizedException("Only the assigned attendee can accept");
        }

        item.acceptAssignment(); // throws IllegalStateException if not assigned

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("User {} accepted assignment of contribution item {} for event {}", userId, itemId, eventId);
        return toResponse(saved);
    }

    @Override
    public ContributionItemResponse declineAssignment(Long eventId, Long itemId, Long userId) {
        User user = findUserOrThrow(userId);
        ContributionItem item = findItemOrThrow(eventId, itemId);

        if (!item.isAssignedTo(user)) {
            throw new UnauthorizedException("Only the assigned attendee can decline");
        }

        item.declineAssignment(); // throws IllegalStateException if not assigned

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("User {} declined assignment of contribution item {} for event {}", userId, itemId, eventId);
        return toResponse(saved);
    }

    @Override
    public ContributionItemResponse releaseItem(Long eventId, Long itemId, Long organizerId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);

        ContributionItem item = findItemOrThrow(eventId, itemId);
        item.releaseClaim();

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("Organizer {} released contribution item {} for event {}", organizerId, itemId, eventId);
        return toResponse(saved);
    }

    @Override
    public ContributionItemResponse completeItem(Long eventId, Long itemId, Long organizerId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizer(event, organizerId);

        ContributionItem item = findItemOrThrow(eventId, itemId);
        item.markCompleted(); // throws IllegalStateException if not confirmed

        ContributionItem saved = contributionItemRepository.save(item);
        log.info("Organizer {} marked contribution item {} as completed for event {}", organizerId, itemId, eventId);
        return toResponse(saved);
    }

    // ===== Read-only =====

    @Override
    @Transactional(readOnly = true)
    public ContributionSummaryResponse getSummary(Long eventId, Long userId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizerOrApprovedAttendee(event, userId);

        return new ContributionSummaryResponse(
                contributionItemRepository.countByEventId(eventId),
                contributionItemRepository.countByEventIdAndStatus(eventId, ContributionStatus.AVAILABLE),
                contributionItemRepository.countByEventIdAndStatus(eventId, ContributionStatus.CLAIMED),
                contributionItemRepository.countByEventIdAndStatus(eventId, ContributionStatus.ASSIGNED),
                contributionItemRepository.countByEventIdAndStatus(eventId, ContributionStatus.CONFIRMED),
                contributionItemRepository.countByEventIdAndCompleted(eventId, true),
                contributionItemRepository.countUnclaimedMustHaveItems(eventId),
                Optional.ofNullable(contributionItemRepository.sumEstimatedCostByEventId(eventId)).orElse(BigDecimal.ZERO),
                Optional.ofNullable(contributionItemRepository.sumClaimedCostByEventId(eventId)).orElse(BigDecimal.ZERO)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategories(Long eventId, Long userId) {
        Event event = findEventOrThrow(eventId);
        verifyOrganizerOrApprovedAttendee(event, userId);

        return contributionItemRepository.findDistinctCategoriesByEventId(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributionItemResponse> getMyContributions(Long userId) {
        return contributionItemRepository.findActiveContributionsByUser(userId)
                .stream()
                .map(this::toResponse)
                .toList();
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

    private ContributionItem findItemOrThrow(Long eventId, Long itemId) {
        ContributionItem item = contributionItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution item not found"));
        if (!item.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException("Contribution item not found for this event");
        }
        return item;
    }

    private void verifyOrganizer(Event event, Long userId) {
        if (!event.getOrganizer().getId().equals(userId)) {
            throw new UnauthorizedException("Only the event organizer can perform this action");
        }
    }

    private void verifyOrganizerOrApprovedAttendee(Event event, Long userId) {
        if (event.getOrganizer().getId().equals(userId)) return;
        if (!attendeeRepository.existsByEventIdAndUserIdAndStatus(event.getId(), userId, AttendeeStatus.APPROVED)) {
            throw new UnauthorizedException("You must be an approved attendee to view contributions");
        }
    }

    private void verifyApprovedAttendee(Long eventId, Long userId) {
        if (!attendeeRepository.existsByEventIdAndUserIdAndStatus(eventId, userId, AttendeeStatus.APPROVED)) {
            throw new UnauthorizedException("You must be an approved attendee to claim items");
        }
    }

    private void verifyEventActive(Event event) {
        if (event.getStatus() != EventStatus.ACTIVE && event.getStatus() != EventStatus.FULL) {
            throw new BadRequestException("Event is not accepting contribution changes");
        }
    }

    private ContributionItemResponse toResponse(ContributionItem item) {
        User assigned = item.getAssignedTo();
        return new ContributionItemResponse(
                item.getId(),
                item.getEvent().getId(),
                item.getName(),
                item.getCategory(),
                item.getType(),
                item.getQuantity(),
                item.getTimeCommitment(),
                item.getEstimatedCost(),
                item.getPriority(),
                item.getNotes(),
                item.getStatus(),
                item.isCompleted(),
                assigned != null ? assigned.getId() : null,
                assigned != null ? assigned.getDisplayName() : null,
                assigned != null ? assigned.getProfilePictureUrl() : null,
                item.getCreatedAt(),
                item.getClaimedAt(),
                item.getConfirmedAt()
        );
    }
}
