package com.theinside.partii.dto;

import com.theinside.partii.enums.EventStatus;
import com.theinside.partii.enums.EventType;
import com.theinside.partii.enums.EventVisibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Response DTO for event information.
 */
public record EventResponse(
    Long id,
    Long organizerId,
    String organizerDisplayName,
    String title,
    String description,
    EventType eventType,
    String locationAddress,
    Double latitude,
    Double longitude,
    LocalDateTime eventDate,
    String imageUrl,
    BigDecimal estimatedBudget,
    String currency,
    Integer maxAttendees,
    Integer currentAttendees,
    Integer ageRestriction,
    LocalDateTime paymentDeadline,
    LocalDateTime joinDeadline,
    EventVisibility visibility,
    EventStatus status,
    String privateLinkCode,
    Instant createdAt,
    Instant updatedAt
) {}
