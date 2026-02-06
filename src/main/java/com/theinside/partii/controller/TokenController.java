package com.theinside.partii.controller;

import com.theinside.partii.dto.GenericMessageResponse;
import com.theinside.partii.security.KeyManager;
import com.theinside.partii.security.TokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.theinside.partii.utils.CustomMessages.KEYS_ROTATED_SUCCESSFULLY;

/**
 * Controller for managing JWT tokens and key rotation.
 * Provides endpoints to retrieve the JWK set and rotate keys.
 */

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final KeyManager keyManager;
    private final TokenManager tokenManager;

    @GetMapping("/.well-known/jwks.json")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getJwk() {

        return keyManager.getJWKSet().toPublicJWKSet().toJSONObject();
    }

    @PostMapping("/partii/api/v1/admin/keys/rotate")
    public GenericMessageResponse rotateKeys() {
        keyManager.rotateKeys();
        return new GenericMessageResponse(KEYS_ROTATED_SUCCESSFULLY);
    }

}
