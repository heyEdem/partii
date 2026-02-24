package com.theinside.partii.dto;

import com.theinside.partii.enums.ContributionStatus;
import com.theinside.partii.enums.ContributionType;
import com.theinside.partii.enums.Priority;

import java.math.BigDecimal;
import java.time.Instant;

public record ContributionItemResponse(
    Long id,
    Long eventId,
    String name,
    String category,
    ContributionType type,
    Integer quantity,
    Integer timeCommitment,
    BigDecimal estimatedCost,
    Priority priority,
    String notes,
    ContributionStatus status,
    boolean completed,
    Long assignedToUserId,
    String assignedToDisplayName,
    String assignedToProfilePictureUrl,
    Instant createdAt,
    Instant claimedAt,
    Instant confirmedAt
) {}
