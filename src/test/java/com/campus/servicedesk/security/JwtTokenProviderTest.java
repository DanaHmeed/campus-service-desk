package com.campus.servicedesk.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm-usage";
        tokenProvider = new JwtTokenProvider(secret, 3600000); // 1 hour
    }

    @Test
    @DisplayName("should generate and validate a token")
    void generateAndValidate() {
        String token = tokenProvider.generateToken(1L, "alice@u.edu", "STUDENT");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(1L);
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("should reject tampered token")
    void rejectTamperedToken() {
        String token = tokenProvider.generateToken(1L, "alice@u.edu", "STUDENT");
        String tampered = token + "x";

        assertThat(tokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("should reject null/empty token")
    void rejectNullToken() {
        assertThat(tokenProvider.validateToken(null)).isFalse();
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("should reject expired token")
    void rejectExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm-usage", 0);
        String token = shortLived.generateToken(1L, "alice@u.edu", "STUDENT");

        // Token with 0ms expiry should be expired immediately (or within ms)
        assertThat(shortLived.validateToken(token)).isFalse();
    }
}
