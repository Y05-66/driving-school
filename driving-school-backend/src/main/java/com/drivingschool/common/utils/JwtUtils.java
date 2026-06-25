package com.drivingschool.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT（JSON Web Token）工具类。
 * <p>
 * 封装了 JWT 令牌的生成、解析、验证等操作。
 * 本系统使用两种类型的令牌：
 * - Access Token（访问令牌）：用于接口认证，有效期较短
 * - Refresh Token（刷新令牌）：用于刷新 Access Token，有效期较长
 * </p>
 * <p>
 * JWT 令牌中包含的声明（Claims）：
 * - userId：用户 ID
 * - username：用户名
 * - role：用户角色（仅 Access Token 包含）
 * - type：令牌类型（access 或 refresh）
 * - iat：签发时间
 * - exp：过期时间
 * </p>
 */
@Component
public class JwtUtils {

    /**
     * JWT 签名密钥，从配置文件 jwt.secret 读取。
     * 用于 HMAC-SHA 算法对令牌进行签名和验证。
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Access Token 有效期（毫秒），从配置文件 jwt.access-token-expiration 读取。
     */
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * Refresh Token 有效期（毫秒），从配置文件 jwt.refresh-token-expiration 读取。
     */
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 获取 HMAC 签名密钥。
     * <p>
     * 将配置文件中的密钥字符串转换为 SecretKey 对象，
     * 用于 JWT 的签名生成和验证。
     * </p>
     *
     * @return SecretKey HMAC 签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token（访问令牌）。
     * <p>
     * Access Token 用于接口请求的身份认证，包含用户 ID、用户名和角色信息。
     * 有效期较短，过期后需要使用 Refresh Token 刷新。
     * </p>
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     用户角色（如 ADMIN、STAFF、COACH、STUDENT）
     * @return 签名后的 JWT Access Token 字符串
     */
    public String generateAccessToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", "access");
        return createToken(claims, accessTokenExpiration);
    }

    /**
     * 生成 Refresh Token（刷新令牌）。
     * <p>
     * Refresh Token 用于在 Access Token 过期后获取新的 Access Token，
     * 有效期较长。不包含角色信息，不能直接用于接口认证。
     * </p>
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return 签名后的 JWT Refresh Token 字符串
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "refresh");
        return createToken(claims, refreshTokenExpiration);
    }

    /**
     * 创建 JWT 令牌。
     * <p>
     * 使用 JJWT 库构建令牌，设置声明、签发时间、过期时间并签名。
     * </p>
     *
     * @param claims     令牌中包含的声明信息
     * @param expiration 有效期（毫秒）
     * @return 签名后的 JWT 令牌字符串
     */
    private String createToken(Map<String, Object> claims, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())                                          // 签发时间
                .expiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(getSigningKey())                                     // 使用密钥签名
                .compact();
    }

    /**
     * 解析 JWT 令牌，获取所有声明信息。
     * <p>
     * 如果令牌签名无效或已过期，会抛出相应的 JwtException。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return Claims 对象，包含令牌中的所有声明
     * @throws JwtException 令牌解析失败时抛出（签名无效、过期等）
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从令牌中提取用户名。
     *
     * @param token JWT 令牌字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * 从令牌中提取用户 ID。
     *
     * @param token JWT 令牌字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Object userId = parseToken(token).get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    /**
     * 从令牌中提取用户角色。
     *
     * @param token JWT 令牌字符串
     * @return 用户角色字符串（如 ADMIN、STAFF 等）
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从令牌中提取令牌类型。
     *
     * @param token JWT 令牌字符串
     * @return 令牌类型（"access" 或 "refresh"）
     */
    public String getTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }

    /**
     * 解析令牌的所有声明信息。
     * <p>
     * 与 parseToken 功能相同，提供语义化的别名方法。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return Claims 对象
     */
    public Claims parseAllClaims(String token) {
        return parseToken(token);
    }

    /**
     * 从 HTTP 请求中提取 JWT 令牌。
     * <p>
     * 从请求头的 Authorization 字段中提取 Bearer Token。
     * 格式为："Bearer {token}"
     * </p>
     *
     * @param request HTTP 请求对象
     * @return JWT 令牌字符串，如果不存在则返回 null
     */
    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀，提取令牌部分
            return header.substring(7);
        }
        return null;
    }

    /**
     * 检查令牌是否已过期。
     *
     * @param token JWT 令牌字符串
     * @return true 表示已过期，false 表示未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            // 如果解析时抛出过期异常，说明已过期
            return true;
        }
    }

    /**
     * 验证令牌是否有效。
     * <p>
     * 检查令牌的签名是否正确且未过期。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return true 表示有效，false 表示无效（签名错误、已过期等）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 获取 Access Token 的有效期配置值。
     *
     * @return Access Token 有效期（毫秒）
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * 获取令牌的剩余有效时间。
     * <p>
     * 解析令牌的过期时间，计算距离当前时间的剩余毫秒数。
     * 如果令牌已过期，返回0。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 剩余有效时间（毫秒），已过期返回0
     */
    public long getRemainingTime(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (JwtException e) {
            return 0;
        }
    }

    /**
     * 获取 Refresh Token 的有效期配置值。
     *
     * @return Refresh Token 有效期（毫秒）
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
