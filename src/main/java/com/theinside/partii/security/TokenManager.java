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

    public AuthToken refreshAccessToken(String refreshTokenValue) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(UUID.fromString(refreshTokenValue))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        User account = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        return issueToken(account);
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
        UUID refreshTokenId = UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenId)
                .userId(user.getId())
                .issuedAt(currentTime)
                .expiresAt(currentTime.plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshTokenId.toString();
    }
}
