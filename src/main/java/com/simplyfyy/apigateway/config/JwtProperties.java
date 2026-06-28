package com.simplyfyy.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secretKey) {
    public JwtProperties {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("jwt.secretKey must be set via JWT_SECRET_KEY env var");
        }
    }
}