package com.theinside.partii.repository;

import com.theinside.partii.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find the most recent unused, non-expired token for an email.
     */
    Optional<PasswordResetToken> findFirstByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, Instant now);

    /**
     * Count recent tokens for rate limiting.
     */
    long countByEmailAndCreatedAtAfter(String email, Instant since);

    /**
     * Delete expired tokens (for scheduled cleanup).
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(Instant now);
}
