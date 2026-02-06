package com.theinside.partii.security;

import com.theinside.partii.repository.EmailVerificationTokenRepository;
import com.theinside.partii.repository.PasswordResetTokenRepository;
import com.theinside.partii.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled task to clean up expired tokens.
 * Runs daily to maintain database hygiene.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Deletes expired and revoked refresh tokens daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        
        log.info("Starting cleanup of expired tokens");
        try {
            // Clean up refresh tokens
            refreshTokenRepository.deleteRevokedAndExpiredTokens();
            log.info("Successfully cleaned up expired refresh tokens");
            
            // Clean up email verification tokens
            int emailTokensDeleted = emailVerificationTokenRepository.deleteExpiredTokens(now);
            log.info("Deleted {} expired email verification tokens", emailTokensDeleted);
            
            // Clean up password reset tokens
            int resetTokensDeleted = passwordResetTokenRepository.deleteExpiredTokens(now);
            log.info("Deleted {} expired password reset tokens", resetTokensDeleted);
            
        } catch (Exception e) {
            log.error("Error during token cleanup", e);
        }
    }
}
