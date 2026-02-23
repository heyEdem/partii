package com.theinside.partii.entity;

import com.theinside.partii.enums.ContributionStatus;
import com.theinside.partii.enums.ContributionType;
import com.theinside.partii.enums.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContributionItemTest {

    private User organizer;
    private User attendee;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).displayName("Organizer").build();
        attendee = User.builder().id(2L).displayName("Attendee").build();
    }

    private ContributionItem availableItem() {
        return ContributionItem.builder()
                .id(1L)
                .name("Chips")
                .type(ContributionType.MATERIAL)
                .priority(Priority.NICE_TO_HAVE)
                .status(ContributionStatus.AVAILABLE)
                .build();
    }

    // ── claim() ─────────────────────────────────────────────────────────

    @Nested
    class Claim {

        @Test
        void claim_happyPath_setsClaimedStatusAndUser() {
            ContributionItem item = availableItem();

            item.claim(attendee);

            assertThat(item.getStatus()).isEqualTo(ContributionStatus.CLAIMED);
            assertThat(item.getAssignedTo()).isEqualTo(attendee);
            assertThat(item.getClaimedAt()).isNotNull();
        }

        @Test
        void claim_whenNotAvailable_throws() {
            ContributionItem item = availableItem();
            item.claim(attendee); // now CLAIMED

            assertThatThrownBy(() -> item.claim(organizer))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not available");
        }
    }

    // ── assign() ────────────────────────────────────────────────────────

    @Nested
    class Assign {

        @Test
        void assign_happyPath_setsAssignedStatusAndUser() {
            ContributionItem item = availableItem();

            item.assign(attendee);

            assertThat(item.getStatus()).isEqualTo(ContributionStatus.ASSIGNED);
            assertThat(item.getAssignedTo()).isEqualTo(attendee);
            assertThat(item.getClaimedAt()).isNotNull();
        }

        @Test
        void assign_whenNotAvailable_throws() {
            ContributionItem item = availableItem();
            item.claim(attendee); // now CLAIMED

            assertThatThrownBy(() -> item.assign(organizer))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not available for assignment");
        }
    }

    // ── acceptAssignment() ──────────────────────────────────────────────

    @Nested
    class AcceptAssignment {

        @Test
        void acceptAssignment_happyPath_confirmsItem() {
            ContributionItem item = availableItem();
            item.assign(attendee);

            item.acceptAssignment();

            assertThat(item.getStatus()).isEqualTo(ContributionStatus.CONFIRMED);
            assertThat(item.getConfirmedAt()).isNotNull();
            assertThat(item.getAssignedTo()).isEqualTo(attendee);
        }

        @Test
        void acceptAssignment_whenNotAssigned_throws() {
            ContributionItem item = availableItem();

            assertThatThrownBy(() -> item.acceptAssignment())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be assigned");
        }
    }

    // ── declineAssignment() ─────────────────────────────────────────────

    @Nested
    class DeclineAssignment {

        @Test
        void declineAssignment_happyPath_resetsToAvailable() {
            ContributionItem item = availableItem();
            item.assign(attendee);

            item.declineAssignment();

            assertThat(item.getStatus()).isEqualTo(ContributionStatus.AVAILABLE);
            assertThat(item.getAssignedTo()).isNull();
            assertThat(item.getClaimedAt()).isNull();
            assertThat(item.getConfirmedAt()).isNull();
        }

        @Test
        void declineAssignment_whenNotAssigned_throws() {
            ContributionItem item = availableItem();

            assertThatThrownBy(() -> item.declineAssignment())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be assigned");
        }
    }

    // ── confirmClaim() ──────────────────────────────────────────────────

    @Nested
    class ConfirmClaim {

        @Test
        void confirmClaim_happyPath_confirmsClaimedItem() {
            ContributionItem item = availableItem();
            item.claim(attendee);

            item.confirmClaim();

            assertThat(item.getStatus()).isEqualTo(ContributionStatus.CONFIRMED);
            assertThat(item.getConfirmedAt()).isNotNull();
        }
    }

    // ── releaseClaim() ──────────────────────────────────────────────────

    @Nested
    class ReleaseClaim {

        @Test
        void releaseClaim_resetsToAvailable() {
            ContributionItem item = availableItem();
            item.claim(attendee);
            item.confirmClaim();

            item.releaseClaim();

            assertThat(item.getStatus()).isEqualTo(ContributionStatus.AVAILABLE);
            assertThat(item.getAssignedTo()).isNull();
            assertThat(item.getClaimedAt()).isNull();
            assertThat(item.getConfirmedAt()).isNull();
            assertThat(item.isCompleted()).isFalse();
        }
    }

    // ── markCompleted() ─────────────────────────────────────────────────

    @Nested
    class MarkCompleted {

        @Test
        void markCompleted_happyPath_setsCompletedTrue() {
            ContributionItem item = availableItem();
            item.claim(attendee);
            item.confirmClaim();

            item.markCompleted();

            assertThat(item.isCompleted()).isTrue();
        }

        @Test
        void markCompleted_whenNotConfirmed_throws() {
            ContributionItem item = availableItem();

            assertThatThrownBy(() -> item.markCompleted())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be confirmed");
        }
    }
}
