package com.theinside.partii.service;

import com.theinside.partii.dto.AuthResponse;
import com.theinside.partii.dto.CompleteProfileRequest;
import com.theinside.partii.dto.GenericMessageResponse;
import com.theinside.partii.dto.LoginRequest;
import com.theinside.partii.dto.SignupRequest;
import com.theinside.partii.dto.VerifyEmailRequest;
import com.theinside.partii.entity.AccountStatus;
import com.theinside.partii.entity.EmailVerificationToken;
import com.theinside.partii.entity.PasswordResetToken;
import com.theinside.partii.entity.User;
import com.theinside.partii.exception.BadRequestException;
import com.theinside.partii.exception.NotFoundException;
import com.theinside.partii.exception.UnauthorizedException;
import com.theinside.partii.exception.ValidationException;
import com.theinside.partii.exception.VerificationFailedException;
import com.theinside.partii.repository.EmailVerificationTokenRepository;
import com.theinside.partii.repository.PasswordResetTokenRepository;
import com.theinside.partii.repository.RefreshTokenRepository;
import com.theinside.partii.repository.UserRepository;
import com.theinside.partii.security.TokenManager;
import com.theinside.partii.service.notification.NotificationService;
import com.theinside.partii.utils.validators.CustomValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.theinside.partii.utils.CustomMessages.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenManager tokenManager;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final CustomValidator customValidator;

    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_MINUTES = 15;
    private static final int MAX_OTP_REQUESTS_PER_HOUR = 5;
    private static final int MAX_RESET_REQUESTS_PER_HOUR = 3;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Processing signup for email: {}", request.email());

        // Check age requirement (18+)
        int age = Period.between(request.dob(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new ValidationException(UNDER_AGE);
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException(EMAIL_ALREADY_EXISTS);
        }

        // Create new user - for simple signup, set required fields to empty/default
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .provider("local")
                .providerId(UUID.randomUUID().toString())
                .dob(request.dob())
                .generalLocation("")  // Required field, set to empty
                .primaryAddress("")   // Required field, set to empty  
                .phoneNumber("")      // Required field, set to empty
                .accountStatus(AccountStatus.PENDING)
                .isVerified(false)
                .isEnabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        // Send verification email
        sendVerificationEmail(savedUser);

        // Issue tokens (user can login but some features may be restricted until verified)
        var authToken = tokenManager.issueToken(savedUser);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getDisplayName(),
                authToken.getAccessToken(),
                authToken.getRefreshToken(),
                authToken.getAccessTokenExpiresAt(),
                authToken.getRefreshTokenExpiresAt()
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS_MESSAGE));

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw new UnauthorizedException(ACCOUNT_DISABLED);
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException(BAD_CREDENTIALS_MESSAGE);
        }

        // Check if account is verified
        if (!user.isVerified()) {
            // Resend OTP if not verified
            sendVerificationEmail(user);
            throw new UnauthorizedException(UNVERIFIED_ACCOUNT);
        }

        var authToken = tokenManager.issueToken(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                authToken.getAccessToken(),
                authToken.getRefreshToken(),
                authToken.getAccessTokenExpiresAt(),
                authToken.getRefreshTokenExpiresAt()
        );
    }

    @Override
    @Transactional
    public GenericMessageResponse verifyEmail(VerifyEmailRequest request) {
        log.info("Verifying email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        EmailVerificationToken token = emailVerificationTokenRepository
                .findFirstByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        request.email(), Instant.now())
                .orElseThrow(() -> new VerificationFailedException(OTP_NOT_VERIFIED));

        // Verify OTP hash
        if (!passwordEncoder.matches(request.otp(), token.getTokenHash())) {
            throw new VerificationFailedException(OTP_VERIFICATION_FAILED_MESSAGE);
        }

        // Mark token as used
        token.setUsed(true);
        emailVerificationTokenRepository.save(token);

        // Mark user as verified
        user.setVerified(true);
        user.setAccountStatus(AccountStatus.VERIFIED);
        userRepository.save(user);

        // Send welcome email
        notificationService.sendWelcomeEmail(user.getEmail(), user.getDisplayName());

        return new GenericMessageResponse(ACCOUNT_VERIFIED_SUCCESSFULLY);
    }

    @Override
    @Transactional
    public GenericMessageResponse resendOtp(String email) {
        log.info("Resending OTP for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        if (user.isVerified()) {
            throw new BadRequestException("Account is already verified");
        }

        // Check rate limit
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentRequests = emailVerificationTokenRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        if (recentRequests >= MAX_OTP_REQUESTS_PER_HOUR) {
            throw new ValidationException("Too many requests. Please try again later.");
        }

        sendVerificationEmail(user);

        return new GenericMessageResponse(TOKEN_SENT_MSG);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing access token");
        var authToken = tokenManager.refreshAccessToken(refreshToken);

        // Get user info from the token
        UUID tokenId = UUID.fromString(refreshToken);
        var tokenEntity = refreshTokenRepository.findByToken(tokenId)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                authToken.getAccessToken(),
                authToken.getRefreshToken(),
                authToken.getAccessTokenExpiresAt(),
                authToken.getRefreshTokenExpiresAt()
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.info("Processing logout");

        UUID tokenId;
        try {
            tokenId = UUID.fromString(refreshToken);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid refresh token format");
        }

        var tokenEntity = refreshTokenRepository.findByToken(tokenId)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Revoke the entire token family
        tokenManager.revokeTokenFamily(tokenEntity.getFamilyId());
    }

    @Override
    @Transactional
    public GenericMessageResponse forgotPassword(String email) {
        log.info("Processing forgot password for email: {}", email);

        // Always return success to prevent email enumeration
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return new GenericMessageResponse(PASSWORD_RESET_EMAIL_SENT);
        }

        // Check rate limit (stricter than OTP: 3 per hour)
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentRequests = passwordResetTokenRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        if (recentRequests >= MAX_RESET_REQUESTS_PER_HOUR) {
            log.warn("Rate limit exceeded for password reset: {}", email);
            // Still return success to prevent enumeration
            return new GenericMessageResponse(PASSWORD_RESET_EMAIL_SENT);
        }

        // Generate secure random token
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Hash the token for storage
        String tokenHash = passwordEncoder.encode(rawToken);

        // Save token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(email)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Send email
        notificationService.sendPasswordResetEmail(email, rawToken, user.getDisplayName());

        return new GenericMessageResponse(PASSWORD_RESET_EMAIL_SENT);
    }

    @Override
    @Transactional
    public GenericMessageResponse resetPassword(String email, String token, String newPassword) {
        log.info("Processing password reset for email: {}", email);

        // Find the most recent valid token for this email
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findFirstByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, Instant.now())
                .orElseThrow(() -> new VerificationFailedException(INVALID_OR_EXPIRED_RESET_TOKEN));

        // Verify token hash
        if (!passwordEncoder.matches(token, resetToken.getTokenHash())) {
            throw new VerificationFailedException(INVALID_OR_EXPIRED_RESET_TOKEN);
        }

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Update user password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all user tokens (password change = invalidate all sessions)
        tokenManager.revokeAllUserTokens(user.getId());

        return new GenericMessageResponse(RESET_PASSWORD_SUCCESS);
    }

    @Override
    @Transactional
    public AuthResponse completeProfile(Long userId, CompleteProfileRequest request) {
        log.info("Completing profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        if (user.isProfileCompleted()) {
            throw new BadRequestException("Profile already completed");
        }

        if (user.getDob() != null) {
            throw new BadRequestException("Date of birth already set");
        }

        // Check age requirement (18+)
        int age = Period.between(request.dob(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new ValidationException(UNDER_AGE);
        }

        // Update user fields
        user.setDob(request.dob());
        if (request.legalName() != null) {
            user.setLegalName(request.legalName());
        }
        if (request.generalLocation() != null) {
            user.setGeneralLocation(request.generalLocation());
        }
        user.setProfileCompleted(true);

        User savedUser = userRepository.save(user);
        log.info("Profile completed for user: {}", savedUser.getEmail());

        // Issue tokens
        var authToken = tokenManager.issueToken(savedUser);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getDisplayName(),
                authToken.getAccessToken(),
                authToken.getRefreshToken(),
                authToken.getAccessTokenExpiresAt(),
                authToken.getRefreshTokenExpiresAt()
        );
    }

    private void sendVerificationEmail(User user) {
        // Generate OTP
        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        // Invalidate any existing unused tokens
        List<EmailVerificationToken> existingTokens = emailVerificationTokenRepository
                .findByEmailAndUsedFalse(user.getEmail());
        existingTokens.forEach(t -> t.setUsed(true));
        emailVerificationTokenRepository.saveAll(existingTokens);

        // Save new token
        EmailVerificationToken token = EmailVerificationToken.builder()
                .email(user.getEmail())
                .tokenHash(otpHash)
                .expiresAt(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(token);

        // Send email
        notificationService.sendOtpEmail(user.getEmail(), otp, user.getDisplayName());
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
