package com.theinside.partii.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.theinside.partii.utils.CustomMessages.EMAIL_NOT_BLANK;

public record VerifyEmailRequest(
        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email
        String email,

        @NotBlank(message = "OTP cannot be blank")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
        String otp
) {
}
