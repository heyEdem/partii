package com.theinside.partii.service;

import com.theinside.partii.dto.AuthResponse;
import com.theinside.partii.dto.CompleteProfileRequest;
import com.theinside.partii.dto.ForgotPasswordRequest;
import com.theinside.partii.dto.GenericMessageResponse;
import com.theinside.partii.dto.LoginRequest;
import com.theinside.partii.dto.ResetPasswordRequest;
import com.theinside.partii.dto.SignupRequest;
import com.theinside.partii.dto.VerifyEmailRequest;

/**
 * Service interface for managing authentication-related operations.
 * Provides methods for user signup, login, email verification, token refresh, logout, and password reset.
 */
public interface AuthService {

    /**
     * Register a new user account.
     *
     * @param request the signup request containing user details
     * @return response with tokens and user info
     */
    AuthResponse signup(SignupRequest request);

    /**
     * Authenticate a user and issue tokens.
     *
     * @param request the login request containing credentials
     * @return response with tokens and user info
     */
    AuthResponse login(LoginRequest request);

    /**
     * Verify user's email with OTP.
     *
     * @param request the verification request containing email and OTP
     * @return success message response
     */
    GenericMessageResponse verifyEmail(VerifyEmailRequest request);

    /**
     * Resend OTP to user's email.
     *
     * @param email the email address to send OTP to
     * @return success message response
     */
    GenericMessageResponse resendOtp(String email);

    /**
     * Refresh access token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return response with new tokens
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Logout a user by revoking their refresh token.
     *
     * @param refreshToken the refresh token to revoke
     */
    void logout(String refreshToken);

    /**
     * Initiate password reset flow.
     *
     * @param email the email address to send reset link to
     * @return success message response
     */
    GenericMessageResponse forgotPassword(String email);

    /**
     * Complete password reset with token and new password.
     *
     * @param email the user's email
     * @param token the reset token
     * @param newPassword the new password
     * @return success message response
     */
    GenericMessageResponse resetPassword(String email, String token, String newPassword);

    /**
     * Complete user profile after OAuth signup.
     *
     * @param userId the user ID
     * @param request the profile completion request
     * @return response with tokens and user info
     */
    AuthResponse completeProfile(Long userId, CompleteProfileRequest request);
}
