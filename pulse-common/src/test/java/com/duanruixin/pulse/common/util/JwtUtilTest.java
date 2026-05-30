package com.duanruixin.pulse.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void should_generate_and_parse_token_correctly() {
        Long userId = 12345L;
        String token = JwtUtil.generateToken(userId);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT 应该是三段");

        Long parsed = JwtUtil.parseUserId(token);
        assertEquals(userId, parsed);
    }

    @Test
    void should_validate_valid_token() {
        String token = JwtUtil.generateToken(1L);
        assertTrue(JwtUtil.validate(token));
    }

    @Test
    void should_reject_invalid_token() {
        assertFalse(JwtUtil.validate("invalid.token.string"));
        assertFalse(JwtUtil.validate(""));
        assertFalse(JwtUtil.validate(null));
    }

    @Test
    void should_return_null_for_invalid_token() {
        Long result = JwtUtil.parseUserId("garbage");
        assertNull(result);
    }
}