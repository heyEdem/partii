package com.theinside.partii.controller;

import com.theinside.partii.dto.AuthResponse;
import com.theinside.partii.dto.ForgotPasswordRequest;
import com.theinside.partii.dto.GenericMessageResponse;
import com.theinside.partii.dto.LoginRequest;
import com.theinside.partii.dto.LogoutRequest;
import com.theinside.partii.dto.RefreshTokenRequest;
import com.theinside.partii.dto.ResendOtpRequest;
import com.theinside.partii.dto.ResetPasswordRequest;
import com.theinside.partii.dto.SignupRequest;
import com.theinside.partii.dto.VerifyEmailRequest;
import com.theinside.partii.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.theinside.partii.utils.CustomMessages.LOGGED_OUT_SUCCESSFULLY;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/partii/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup request received for email: {}", request.email());
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<GenericMessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Email verification request received for email: {}", request.email());
        GenericMessageResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<GenericMessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("Resend OTP request received for email: {}", request.email());
        GenericMessageResponse response = authService.resendOtp(request.email());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<GenericMessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        log.info("Logout request received");
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(new GenericMessageResponse(LOGGED_OUT_SUCCESSFULLY));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<GenericMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request received for email: {}", request.email());
        GenericMessageResponse response = authService.forgotPassword(request.email());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<GenericMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received for email: {}", request.email());
        GenericMessageResponse response = authService.resetPassword(
                request.email(), 
                request.token(), 
                request.newPassword()
        );
        return ResponseEntity.ok(response);
    }
}
