package com.theinside.partii.dto;

import java.math.BigDecimal;

public record ContributionSummaryResponse(
    long totalItems,
    long availableCount,
    long claimedCount,
    long assignedCount,
    long confirmedCount,
    long completedCount,
    long unclaimedMustHaveCount,
    BigDecimal totalEstimatedCost,
    BigDecimal claimedCost
) {}
