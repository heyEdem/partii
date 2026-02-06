package com.theinside.partii.security;

import com.theinside.partii.entity.AuthToken;
import com.theinside.partii.entity.RefreshToken;
import com.theinside.partii.entity.User;
import com.theinside.partii.exception.NotFoundException;
import com.theinside.partii.exception.UnauthorizedException;
import com.theinside.partii.repository.RefreshTokenRepository;
import com.theinside.partii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenManager {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthToken issueToken (User user){
        Instant currentTime = Instant.now();
        Instant accessExpiry = currentTime.plus(30, ChronoUnit.MINUTES);
        Instant refreshExpiry = currentTime.plus(7, ChronoUnit.DAYS);

        JwsHeader jwsHeader = buildJwsHeader();
        JwtClaimsSet claimSet = buildJwtClaimSet(user, currentTime);

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimSet)).getTokenValue();
        String refreshToken = saveRefreshToken(user, currentTime);

        return AuthToken.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .refreshTokenExpiresAt(refreshExpiry)
                .accessTokenExpiresAt(accessExpiry)
                .build();
    }

    @Transactional
    public AuthToken refreshAccessToken(String refreshTokenValue) {
        UUID tokenId;
        try {
            tokenId = UUID.fromString(refreshTokenValue);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid refresh token format");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenId)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            // Token reuse detected - revoke entire family
            revokeTokenFamily(refreshToken.getFamilyId());
            throw new UnauthorizedException("Token reuse detected. All tokens in this family have been revoked.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        User account = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        // Rotate the token: mark old one as used (we'll track via family and issue new one with same family)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Issue new token with rotation (same family for continuity detection)
        return issueTokenWithFamily(account, refreshToken.getFamilyId());
    }

    /**
     * Revokes all tokens in a token family (used for token reuse detection).
     */
    @Transactional
    public void revokeTokenFamily(UUID familyId) {
        refreshTokenRepository.revokeByFamilyId(familyId);
    }

    /**
     * Revokes all refresh tokens for a user (used on password change/logout all).
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeByUserId(userId);
    }

    private JwsHeader buildJwsHeader() {
        return JwsHeader.with(SignatureAlgorithm.RS256)
                .header("type", "JWT")
                .build();
    }

    private JwtClaimsSet buildJwtClaimSet(User user, Instant currentTime) {
        return JwtClaimsSet.builder()
                .subject(user.getEmail())
                .issuer("http://localhost:8080")
                .issuedAt(currentTime)
                .expiresAt(currentTime.plus(30, ChronoUnit.MINUTES))
                .claim("email", user.getEmail())
                .claim("userId", user.getId())
                .build();
    }

    private String saveRefreshToken(User user, Instant currentTime) {
        UUID familyId = UUID.randomUUID();
        return saveRefreshTokenWithFamily(user, currentTime, familyId);
    }

    private String saveRefreshTokenWithFamily(User user, Instant currentTime, UUID familyId) {
        UUID refreshTokenId = UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenId)
                .userId(user.getId())
                .issuedAt(currentTime)
                .expiresAt(currentTime.plus(7, ChronoUnit.DAYS))
                .familyId(familyId)
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshTokenId.toString();
    }

    private AuthToken issueTokenWithFamily(User user, UUID familyId) {
        Instant currentTime = Instant.now();
        Instant accessExpiry = currentTime.plus(30, ChronoUnit.MINUTES);
        Instant refreshExpiry = currentTime.plus(7, ChronoUnit.DAYS);

        JwsHeader jwsHeader = buildJwsHeader();
        JwtClaimsSet claimSet = buildJwtClaimSet(user, currentTime);

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimSet)).getTokenValue();
        String refreshToken = saveRefreshTokenWithFamily(user, currentTime, familyId);

        return AuthToken.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .refreshTokenExpiresAt(refreshExpiry)
                .accessTokenExpiresAt(accessExpiry)
                .build();
    }
}
