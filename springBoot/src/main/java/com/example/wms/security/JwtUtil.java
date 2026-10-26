package com.example.wms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（jjwt 0.12.x 新 API）。
 * 设计要点：
 * 1. 无状态：token 自带用户信息（userId/username/role），服务端不存 session，便于水平扩展；
 * 2. 前端路由权限需要 role，所以把 role 放进 claims 而不是只放 username；
 * 3. 密钥从配置注入，支持环境变量覆盖，避免硬编码。
 */
@Component
public class JwtUtil {

    @Value("${wms.jwt.secret}")
    private String secret;

    @Value("${wms.jwt.expiration}")
    private Long expiration;

    /**
     * 生成 token：claims 中携带 userId / username / role。
     * subject 用 username（便于调试），但真正取用户身份靠 userId claim。
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 token：jjwt 0.12.x 用 Jwts.parser().verifyWith(key).build()。
     * 解析失败（签名错误/格式错误）会抛异常，由调用方统一处理。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** token 是否已过期 */
    public boolean isExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    /** 从 claims 取 userId（Long 型） */
    public Long getUserId(String token) {
        Object userId = parseToken(token).get("userId");
        return userId == null ? null : ((Number) userId).longValue();
    }

    /** 从 claims 取 role */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /** 构造签名密钥：HS256 要求密钥长度 >= 256 位（32 字节） */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
