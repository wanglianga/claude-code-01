package com.eldercare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(Long userId, String username, String realName, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("name", realName)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public AuthUser parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return new AuthUser(
                ((Number) c.get("uid")).longValue(),
                c.getSubject(),
                (String) c.get("name"),
                (String) c.get("role"));
    }
}
