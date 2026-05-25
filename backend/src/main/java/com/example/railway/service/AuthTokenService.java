package com.example.railway.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.railway.domain.AppUser;
import com.example.railway.domain.UserRole;
import com.example.railway.security.AuthPrincipal;
import com.example.railway.security.AuthenticationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuthTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String secret;
    private final long expireSeconds;
    private final ObjectMapper objectMapper;

    public AuthTokenService(@Value("${security.jwt.secret:${auth.token.secret:railway-demo-secret-change-me}}") String secret,
                            @Value("${security.jwt.expire-seconds:28800}") long expireSeconds,
                            ObjectMapper objectMapper) {
        this.secret = secret;
        this.expireSeconds = expireSeconds;
        this.objectMapper = objectMapper;
    }

    public String generate(AppUser user) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expireSeconds;

        Map<String, Object> header = new LinkedHashMap<String, Object>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("userId", user.getId());
        payload.put("username", user.getUsername());
        payload.put("displayName", user.getDisplayName());
        payload.put("role", user.getRole().name());
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);

        String headerPart = base64Url(toJsonBytes(header));
        String payloadPart = base64Url(toJsonBytes(payload));
        String body = headerPart + "." + payloadPart;
        return body + "." + sign(body);
    }

    public AuthPrincipal parse(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("Please login first");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new AuthenticationException("Invalid token format");
        }
        String body = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(body), parts[2])) {
            throw new AuthenticationException("Invalid token signature");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            long expiresAt = asLong(payload.get("exp"));
            if (expiresAt < Instant.now().getEpochSecond()) {
                throw new AuthenticationException("Token expired");
            }
            return new AuthPrincipal(
                    asLong(payload.get("userId")),
                    String.valueOf(payload.get("username")),
                    String.valueOf(payload.get("displayName")),
                    UserRole.valueOf(String.valueOf(payload.get("role"))),
                    expiresAt
            );
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthenticationException("Invalid token content");
        }
    }

    public long expiresAt(String token) {
        return parse(token).getExpiresAt();
    }

    private byte[] toJsonBytes(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build JWT payload", exception);
        }
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return base64Url(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }

    private long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
