package com.example.railway.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.railway.domain.AppUser;
import com.example.railway.domain.UserRole;
import com.example.railway.security.AuthPrincipal;
import com.example.railway.security.AuthenticationException;

@Service
public class AuthTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long TOKEN_TTL_SECONDS = 8 * 60 * 60;

    private final String secret;

    public AuthTokenService(@Value("${auth.token.secret:railway-demo-secret-change-me}") String secret) {
        this.secret = secret;
    }

    public String generate(AppUser user) {
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        String payload = encode(user.getUsername()) + ":" + encode(user.getDisplayName()) + ":" + user.getRole().name() + ":" + expiresAt;
        String body = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + sign(body);
    }

    public AuthPrincipal parse(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("请先登录");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new AuthenticationException("登录令牌格式不正确");
        }
        if (!constantTimeEquals(sign(parts[0]), parts[1])) {
            throw new AuthenticationException("登录令牌签名无效");
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split(":");
            if (values.length != 4) {
                throw new AuthenticationException("登录令牌内容不正确");
            }

            long expiresAt = Long.parseLong(values[3]);
            if (expiresAt < Instant.now().getEpochSecond()) {
                throw new AuthenticationException("登录已过期，请重新登录");
            }

            return new AuthPrincipal(
                    decode(values[0]),
                    decode(values[1]),
                    UserRole.valueOf(values[2]),
                    expiresAt
            );
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationException("登录令牌内容不正确");
        }
    }

    public long expiresAt(String token) {
        return parse(token).getExpiresAt();
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return base64Url(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign auth token", exception);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String encode(String value) {
        return base64Url(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
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
}
