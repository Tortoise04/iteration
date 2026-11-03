package com.smartschedule.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试 — token 生成、验证、解析。
 */
class JwtUtilTest {

    // 需要一个足够长的 secret（HS256 要求 >= 256 bits）
    private static final String SECRET = "my-secret-key-for-smartschedule-jwt-testing-12345";
    private static final long EXPIRATION_MS = 3600_000L; // 1小时

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);

    @Test
    void testGenerateToken_NotNull() {
        String token = jwtUtil.generateToken(1L, "user1");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGetUserIdFromToken() {
        String token = jwtUtil.generateToken(42L, "bob");
        Long userId = jwtUtil.getUserIdFromToken(token);
        assertEquals(42L, userId);
    }

    @Test
    void testValidateToken_Valid() {
        String token = jwtUtil.generateToken(1L, "user1");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
        assertFalse(jwtUtil.validateToken("eyJhbGciOiJIUzI1NiJ9.xxx.yyy"));
    }

    @Test
    void testValidateToken_Expired() {
        // 创建一个已过期的 JwtUtil（expiration = 0）
        JwtUtil expiredUtil = new JwtUtil(SECRET, 0L);
        String token = expiredUtil.generateToken(1L, "user1");

        // 等待 1ms 确保过期
        try { Thread.sleep(1); } catch (InterruptedException e) {}

        assertFalse(expiredUtil.validateToken(token));
    }

    @Test
    void testGetUserIdFromToken_Tampered() {
        String token = jwtUtil.generateToken(1L, "user1");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(Exception.class, () -> jwtUtil.getUserIdFromToken(tampered));
    }
}
