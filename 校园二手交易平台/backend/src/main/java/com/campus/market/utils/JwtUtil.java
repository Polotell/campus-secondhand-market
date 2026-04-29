package com.campus.market.utils;

import com.campus.market.common.Constants;
import com.campus.market.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类（HS256 对称签名）
 * <p>
 * 这是通过 {@code @Component} 注入 IoC 容器的无状态工具 Bean，
 * 所有拦截器 / 过滤器 / 服务类通过依赖注入获得实例。
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours}")
    private long expireHours;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 Token */
    public String generate(Long userId, String username, UserRole role) {
        long nowMs = System.currentTimeMillis();
        long expMs = nowMs + TimeUnit.HOURS.toMillis(expireHours);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(Constants.JWT_CLAIM_USER_ID,  userId)
                .claim(Constants.JWT_CLAIM_USERNAME, username)
                .claim(Constants.JWT_CLAIM_ROLE,     role.getCode())
                .setIssuedAt(new Date(nowMs))
                .setExpiration(new Date(expMs))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 Token。解析失败返回 null，由拦截器决定是否报 401。 */
    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
