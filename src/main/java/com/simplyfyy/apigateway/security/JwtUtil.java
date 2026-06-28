package com.simplyfyy.apigateway.security;

import com.simplyfyy.apigateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Stateless JWT parser — no I/O, no Spring Security, trivially unit-testable.
 * Claim names match exactly what AuthUtil.generateAccessToken() embeds.
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Parse and validation.
     * Throws typed JJWT exceptions (ExpiredJwtException, MalformedJwtException,
     * SignatureException) — the filter handles each one specifically.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}