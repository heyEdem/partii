package com.theinside.partii.service;

import com.theinside.partii.dto.ContributionItemResponse;
import com.theinside.partii.dto.CreateContributionItemRequest;
import com.theinside.partii.dto.UpdateContributionItemRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributionServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock ContributionItemRepository contributionItemRepository;
    @Mock EventAttendeeRepository attendeeRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ContributionServiceImpl service;

    private User organizer;
    private User attendee;
    private Event event;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).displayName("Organizer").build();
        attendee = User.builder().id(2L).displayName("Attendee").build();
        event = Event.builder().id(1L).organizer(organizer).status(EventStatus.ACTIVE).build();
    }

    private ContributionItem availableItem() {
        return ContributionItem.builder()
                .id(10L)
                .event(event)
                .name("Rice")
                .type(ContributionType.MATERIAL)
                .priority(Priority.NICE_TO_HAVE)
                .status(ContributionStatus.AVAILABLE)
                .quantity(1)
                .completed(false)
                .build();
    }

    private CreateContributionItemRequest createRequest() {
        return new CreateContributionItemRequest(
                "Rice", "Food", ContributionType.MATERIAL,
                2, null, new BigDecimal("15.00"),
                Priority.MUST_HAVE, "Bring basmati rice"
        );
    }

    // ===== createItem =====

    @Test
    void createItem_asOrganizer_success() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> {
                    ContributionItem item = invocation.getArgument(0);
                    item.setId(10L);
                    return item;
                });

        CreateContributionItemRequest request = createRequest();
        ContributionItemResponse response = service.createItem(1L, 1L, request);

        assertThat(response.name()).isEqualTo("Rice");
        assertThat(response.category()).isEqualTo("Food");
        assertThat(response.type()).isEqualTo(ContributionType.MATERIAL);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.priority()).isEqualTo(Priority.MUST_HAVE);
        assertThat(response.status()).isEqualTo(ContributionStatus.AVAILABLE);
        assertThat(response.completed()).isFalse();
        assertThat(response.eventId()).isEqualTo(1L);
        verify(contributionItemRepository).save(any(ContributionItem.class));
    }

    @Test
    void createItem_asNonOrganizer_throwsUnauthorized() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.createItem(1L, 2L, createRequest()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only the event organizer");
    }

    @Test
    void createItem_eventNotActive_throwsBadRequest() {
        Event draftEvent = Event.builder().id(2L).organizer(organizer).status(EventStatus.DRAFT).build();
        when(eventRepository.findById(2L)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> service.createItem(2L, 1L, createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not accepting contribution changes");
    }

    // ===== claimItem =====

    @Test
    void claimItem_asApprovedAttendee_success() {
        ContributionItem item = availableItem();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(attendeeRepository.existsByEventIdAndUserIdAndStatus(1L, 2L, AttendeeStatus.APPROVED))
                .thenReturn(true);
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(userRepository.findById(2L)).thenReturn(Optional.of(attendee));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContributionItemResponse response = service.claimItem(1L, 10L, 2L);

        assertThat(response.status()).isEqualTo(ContributionStatus.CLAIMED);
        assertThat(response.assignedToUserId()).isEqualTo(2L);
        assertThat(response.assignedToDisplayName()).isEqualTo("Attendee");
        assertThat(response.claimedAt()).isNotNull();
    }

    @Test
    void claimItem_asOrganizer_throwsBadRequest() {
        ContributionItem item = availableItem();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        // Organizer is also registered as approved attendee for the purpose of this check
        when(attendeeRepository.existsByEventIdAndUserIdAndStatus(1L, 1L, AttendeeStatus.APPROVED))
                .thenReturn(true);
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.claimItem(1L, 10L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Organizer cannot claim items");
    }

    @Test
    void claimItem_asNonAttendee_throwsUnauthorized() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(attendeeRepository.existsByEventIdAndUserIdAndStatus(1L, 3L, AttendeeStatus.APPROVED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.claimItem(1L, 10L, 3L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("approved attendee");
    }

    // ===== assignItem =====

    @Test
    void assignItem_asOrganizer_success() {
        ContributionItem item = availableItem();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(attendeeRepository.existsByEventIdAndUserIdAndStatus(1L, 2L, AttendeeStatus.APPROVED))
                .thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(attendee));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContributionItemResponse response = service.assignItem(1L, 10L, 1L, 2L);

        assertThat(response.status()).isEqualTo(ContributionStatus.ASSIGNED);
        assertThat(response.assignedToUserId()).isEqualTo(2L);
        assertThat(response.assignedToDisplayName()).isEqualTo("Attendee");
    }

    // ===== acceptAssignment =====

    @Test
    void acceptAssignment_asAssignedUser_success() {
        ContributionItem item = availableItem();
        item.assign(attendee); // transition to ASSIGNED

        when(userRepository.findById(2L)).thenReturn(Optional.of(attendee));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContributionItemResponse response = service.acceptAssignment(1L, 10L, 2L);

        assertThat(response.status()).isEqualTo(ContributionStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
        assertThat(response.assignedToUserId()).isEqualTo(2L);
    }

    @Test
    void acceptAssignment_asWrongUser_throwsUnauthorized() {
        ContributionItem item = availableItem();
        item.assign(attendee); // assigned to user 2

        User otherUser = User.builder().id(3L).displayName("Other").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.acceptAssignment(1L, 10L, 3L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only the assigned attendee can accept");
    }

    // ===== declineAssignment =====

    @Test
    void declineAssignment_asAssignedUser_success() {
        ContributionItem item = availableItem();
        item.assign(attendee);

        when(userRepository.findById(2L)).thenReturn(Optional.of(attendee));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContributionItemResponse response = service.declineAssignment(1L, 10L, 2L);

        assertThat(response.status()).isEqualTo(ContributionStatus.AVAILABLE);
        assertThat(response.assignedToUserId()).isNull();
    }

    // ===== releaseItem =====

    @Test
    void releaseItem_asOrganizer_success() {
        ContributionItem item = availableItem();
        item.claim(attendee);
        item.confirmClaim(); // now CONFIRMED

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContributionItemResponse response = service.releaseItem(1L, 10L, 1L);

        assertThat(response.status()).isEqualTo(ContributionStatus.AVAILABLE);
        assertThat(response.assignedToUserId()).isNull();
        assertThat(response.claimedAt()).isNull();
    }

    // ===== completeItem =====

    @Test
    void completeItem_asOrganizer_success() {
        ContributionItem item = availableItem();
        item.claim(attendee);
        item.confirmClaim(); // now CONFIRMED

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContributionItemResponse response = service.completeItem(1L, 10L, 1L);

        assertThat(response.completed()).isTrue();
        assertThat(response.status()).isEqualTo(ContributionStatus.CONFIRMED);
    }

    // ===== deleteItem =====

    @Test
    void deleteItem_whenNotAvailable_throwsBadRequest() {
        ContributionItem item = availableItem();
        item.claim(attendee); // now CLAIMED, not AVAILABLE

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.deleteItem(1L, 10L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only available items can be deleted");
    }

    @Test
    void deleteItem_whenAvailable_success() {
        ContributionItem item = availableItem();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));

        service.deleteItem(1L, 10L, 1L);

        verify(contributionItemRepository).delete(item);
    }

    // ===== updateItem =====

    @Test
    void updateItem_whenNotAvailable_throwsBadRequest() {
        ContributionItem item = availableItem();
        item.claim(attendee); // now CLAIMED

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));

        UpdateContributionItemRequest request = new UpdateContributionItemRequest(
                "Updated Rice", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateItem(1L, 10L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only available items can be updated");
    }

    @Test
    void updateItem_partialUpdate_success() {
        ContributionItem item = availableItem();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateContributionItemRequest request = new UpdateContributionItemRequest(
                "Basmati Rice", "Food", null, null, null, new BigDecimal("20.00"), null, null);

        ContributionItemResponse response = service.updateItem(1L, 10L, 1L, request);

        assertThat(response.name()).isEqualTo("Basmati Rice");
        assertThat(response.category()).isEqualTo("Food");
        assertThat(response.estimatedCost()).isEqualTo(new BigDecimal("20.00"));
        // Unchanged fields preserved
        assertThat(response.type()).isEqualTo(ContributionType.MATERIAL);
        assertThat(response.priority()).isEqualTo(Priority.NICE_TO_HAVE);
    }

    // ===== confirmItem =====

    @Test
    void confirmItem_asOrganizer_success() {
        ContributionItem item = availableItem();
        item.claim(attendee); // CLAIMED

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(contributionItemRepository.save(any(ContributionItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContributionItemResponse response = service.confirmItem(1L, 10L, 1L);

        assertThat(response.status()).isEqualTo(ContributionStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
    }

    // ===== getItem =====

    @Test
    void getItem_itemNotFoundForEvent_throwsResourceNotFound() {
        Event otherEvent = Event.builder().id(2L).organizer(organizer).status(EventStatus.ACTIVE).build();
        ContributionItem item = ContributionItem.builder()
                .id(10L)
                .event(otherEvent)
                .name("Rice")
                .type(ContributionType.MATERIAL)
                .priority(Priority.NICE_TO_HAVE)
                .status(ContributionStatus.AVAILABLE)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(contributionItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.getItem(1L, 10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found for this event");
    }
}
