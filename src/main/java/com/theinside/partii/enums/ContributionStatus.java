package com.theinside.partii.enums;

/**
 * Tracks the claim status of a contribution item.
 */
public enum ContributionStatus {
    /**
     * Item is available for attendees to claim.
     */
    AVAILABLE,

    /**
     * An attendee has claimed the item, awaiting organizer confirmation.
     */
    CLAIMED,

    /**
     * Organizer assigned item to an attendee, awaiting attendee acceptance.
     */
    ASSIGNED,

    /**
     * Organizer confirmed the claim, attendee is responsible for this item.
     */
    CONFIRMED
}
