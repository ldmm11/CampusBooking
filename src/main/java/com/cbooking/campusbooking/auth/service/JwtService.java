package com.cbooking.campusbooking.auth.service;

import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String HEADER = ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private final byte[] key;
    private final long ttlSeconds;
    private final Clock clock;
    private final ObjectMapper mapper;

    public JwtService(@Value("${auth.jwt.secret:}") String secret,
                      @Value("${auth.jwt.expires-in:7200}") long ttlSeconds,
                      Clock clock, ObjectMapper mapper) {
        if (ttlSeconds <= 0) throw new IllegalArgumentException("JWT lifetime must be positive");
        this.key = secret.isBlank() ? randomKey() : secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
        this.mapper = mapper;
    }

    public long expiresIn() { return ttlSeconds; }

    public String issue(long userId) {
        if (userId <= 0) throw new IllegalArgumentException("Invalid user ID");
        try {
            long now = clock.instant().getEpochSecond();
            String payload = ENCODER.encodeToString(mapper.writeValueAsBytes(
                    Map.of("sub", Long.toString(userId), "iat", now, "exp", Math.addExact(now, ttlSeconds))));
            String content = HEADER + "." + payload;
            return content + "." + ENCODER.encodeToString(sign(content));
        } catch (Exception ex) { throw new IllegalStateException("Cannot issue JWT", ex); }
    }

    public long verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !HEADER.equals(parts[0])) throw new IllegalArgumentException();
            String content = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(content), DECODER.decode(parts[2]))) throw new IllegalArgumentException();
            var claims = mapper.readTree(DECODER.decode(parts[1]));
            String subject = claims.path("sub").textValue();
            long id = Long.parseLong(subject);
            long issued = claims.path("iat").longValue();
            long expires = claims.path("exp").longValue();
            long now = clock.instant().getEpochSecond();
            if (id <= 0 || issued <= 0 || issued > now || expires <= now || expires - issued != ttlSeconds)
                throw new IllegalArgumentException();
            return id;
        } catch (Exception ex) { throw new com.cbooking.campusbooking.common.BusinessException(
                com.cbooking.campusbooking.common.ErrorCode.UNAUTHENTICATED); }
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] randomKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
