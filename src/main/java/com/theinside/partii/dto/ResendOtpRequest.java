package com.theinside.partii.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static com.theinside.partii.utils.CustomMessages.EMAIL_NOT_BLANK;

public record ResendOtpRequest(
        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email
        String email
) {
}
