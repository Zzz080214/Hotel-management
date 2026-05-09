package com.yueqi.hotel.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TokenService {

    public record TokenPayload(String type, String subject, long expiresAt, Map<String, String> claims) {}

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Long> revokedJtis = new ConcurrentHashMap<>();

    @Value("${auth.token.secret:local-dev-token-secret-change-before-production}")
    private String tokenSecret;

    public String issue(String type, String subject, Duration ttl, Map<String, String> claims) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + Math.max(60, ttl.toSeconds());
        String jti = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("typ", type);
        payload.put("sub", subject);
        payload.put("iat", now);
        payload.put("exp", expiresAt);
        payload.put("jti", jti);
        payload.put("claims", claims == null ? Map.of() : claims);

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        return signingInput + "." + sign(signingInput);
    }

    public Optional<TokenPayload> verify(String token, String expectedType) {
        Optional<VerifiedToken> verifiedToken = verifyInternal(token);
        if (verifiedToken.isEmpty()) {
            return Optional.empty();
        }
        TokenPayload payload = verifiedToken.get().payload();
        if (StringUtils.hasText(expectedType) && !expectedType.equals(payload.type())) {
            return Optional.empty();
        }
        return Optional.of(payload);
    }

    public void revoke(String token) {
        verifyInternal(token).ifPresent(verifiedToken -> {
            revokedJtis.put(verifiedToken.jti(), verifiedToken.payload().expiresAt());
            cleanupRevokedTokens();
        });
    }

    private Optional<VerifiedToken> verifyInternal(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            Map<String, Object> body = objectMapper.readValue(
                    new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8),
                    MAP_TYPE);
            String type = String.valueOf(body.getOrDefault("typ", ""));
            String subject = String.valueOf(body.getOrDefault("sub", ""));
            String jti = String.valueOf(body.getOrDefault("jti", ""));
            long expiresAt = asLong(body.get("exp"));
            long now = Instant.now().getEpochSecond();
            if (!StringUtils.hasText(type)
                    || !StringUtils.hasText(subject)
                    || !StringUtils.hasText(jti)
                    || expiresAt <= now
                    || revokedJtis.containsKey(jti)) {
                return Optional.empty();
            }
            return Optional.of(new VerifiedToken(new TokenPayload(type, subject, expiresAt, readClaims(body)), jti));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readClaims(Map<String, Object> body) {
        Object rawClaims = body.get("claims");
        if (!(rawClaims instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, String> claims = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> claims.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
        return claims;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private void cleanupRevokedTokens() {
        long now = Instant.now().getEpochSecond();
        revokedJtis.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private String encodeJson(Object value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encode token", exception);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(resolveSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign token", exception);
        }
    }

    private String resolveSecret() {
        return StringUtils.hasText(tokenSecret) ? tokenSecret : "local-dev-token-secret-change-before-production";
    }

    private record VerifiedToken(TokenPayload payload, String jti) {}
}
