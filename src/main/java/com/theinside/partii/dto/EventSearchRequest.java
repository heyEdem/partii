package com.theinside.partii.dto;

import com.theinside.partii.enums.EventStatus;
import com.theinside.partii.enums.EventType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for searching events with dynamic filters.
 * All fields are optional - null values are ignored in search.
 */
public record EventSearchRequest(

    List<EventType> eventTypes,

    List<EventStatus> statuses,

    LocalDateTime startDate,

    LocalDateTime endDate,

    @DecimalMin(value = "0.0", message = "Minimum budget cannot be negative")
    BigDecimal minBudget,

    @DecimalMin(value = "0.0", message = "Maximum budget cannot be negative")
    BigDecimal maxBudget,

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double latitude,

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double longitude,

    @Min(value = 1, message = "Radius must be at least 1 km")
    Double radiusKm,

    String keyword,

    @Min(value = 0, message = "Minimum age restriction cannot be negative")
    Integer maxAgeRestriction,

    Boolean hasAvailableSpots,

    Long organizerId
) {
    /**
     * Builder-style constructor with defaults.
     */
    public EventSearchRequest {
        // Default statuses to ACTIVE and FULL if not specified
        if (statuses == null || statuses.isEmpty()) {
            statuses = List.of(EventStatus.ACTIVE, EventStatus.FULL);
        }
    }
}
