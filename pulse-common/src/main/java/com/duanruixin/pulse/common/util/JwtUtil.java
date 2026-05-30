package com.duanruixin.pulse.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 用于生成、解析、验证 JSON Web Token
 */
@Slf4j
public class JwtUtil {


    /** 密钥(实际项目应从配置文件读取,demo 这里写死) */
    private static final String SECRET_KEY = "pulse-platform-secret-key-must-be-at-least-32-bytes";

    /** Token 默认过期时间:2 小时 */
    private static final long DEFAULT_EXPIRE_MILLIS = 2 * 60 * 60 * 1000L;

    /** 签名密钥对象 */
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成 Token
     *
     * @param userId 用户 ID
     * @return JWT 字符串
     */
    public static String generateToken(Long userId) {
        return generateToken(userId, DEFAULT_EXPIRE_MILLIS);
    }

    /**
     * 生成 Token(指定过期时间)
     */
    public static String generateToken(Long userId, long expireMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireMillis);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析 Token,获取 userId
     *
     * @param token JWT 字符串
     * @return userId(解析失败返回 null)
     */
    public static Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验 Token 是否有效(签名 + 过期时间)
     */
    public static boolean validate(String token) {
        try {
            Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }





}
