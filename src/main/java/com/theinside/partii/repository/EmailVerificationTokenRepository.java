package com.theinside.partii.repository;

import com.theinside.partii.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Find the most recent unused, non-expired token for an email.
     */
    Optional<EmailVerificationToken> findFirstByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, Instant now);

    /**
     * Count recent tokens for rate limiting (tokens created in last N minutes).
     */
    long countByEmailAndCreatedAtAfter(String email, Instant since);

    /**
     * Find all unused tokens for an email (for invalidating on successful verification).
     */
    List<EmailVerificationToken> findByEmailAndUsedFalse(String email);

    /**
     * Delete expired tokens (for scheduled cleanup).
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(Instant now);
}
