package com.theinside.partii.dto;

import com.theinside.partii.enums.AttendeeStatus;
import com.theinside.partii.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for event attendee information.
 */
public record AttendeeResponse(
    Long id,
    Long userId,
    String displayName,
    String profilePictureUrl,
    AttendeeStatus status,
    BigDecimal paymentAmount,
    PaymentStatus paymentStatus,
    BigDecimal amountPaid,
    Instant joinedAt,
    Instant approvedAt
) {}
