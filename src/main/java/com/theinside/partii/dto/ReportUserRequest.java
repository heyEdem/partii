package com.theinside.partii.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for reporting a user.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportUserRequest {

    @NotBlank(message = "Report reason is required")
    @Size(min = 10, max = 100, message = "Reason must be between 10 and 100 characters")
    private String reason;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}
