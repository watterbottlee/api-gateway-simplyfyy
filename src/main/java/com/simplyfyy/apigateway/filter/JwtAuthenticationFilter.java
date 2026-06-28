package com.simplyfyy.apigateway.filter;

import com.simplyfyy.apigateway.security.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final int ORDER = -100;

    private final JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    //public paths
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/user/create",
            "/auth/login",
            "/auth/google",
            "/auth/email/send-otp",
            "/auth/email/verify-otp",
            "/auth/email/forgot-password/send-otp",
            "/auth/email/forgot-password/verify-otp",
            "/user/reset-password",
            "/swagger-ui",
            "/v3/api-docs",
            "/ai/test/submission"
    );

    private static final List<MethodPath> PUBLIC_METHOD_PATHS = List.of(
            new MethodPath(HttpMethod.POST,   "/submission/operation/create"),
            new MethodPath(HttpMethod.POST,   "/api/files/upload"),
            new MethodPath(HttpMethod.POST,   "/api/files/upload/resume"),
            new MethodPath(HttpMethod.GET,    "/api/files/resume"),
            new MethodPath(HttpMethod.GET,    "/form/operation/get")
    );

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path   = request.getPath().value();
        HttpMethod method = request.getMethod();

        if (isPublic(path, method)) {
            log.debug("Public path bypass: {} {}", method, path);
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (token == null) {
            log.debug("No Bearer token on {} {}", method, path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        try {
            jwtUtil.parseAndValidate(token);
            return chain.filter(exchange);

        } catch (ExpiredJwtException e) {
            log.debug("Expired token on {} {}", method, path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Token has expired");

        } catch (SignatureException e) {
            log.warn("Invalid signature on {} {}", method, path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token signature");

        } catch (MalformedJwtException e) {
            log.warn("Malformed token on {} {}", method, path);
            return reject(exchange, HttpStatus.BAD_REQUEST, "Malformed token");

        } catch (Exception e) {
            log.error("Unexpected token error on {} {}: {}", method, path, e.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Authentication failed");
        }
    }

    private boolean isPublic(String path, HttpMethod method) {
        for (String pub : PUBLIC_PATHS) {
            if (path.equals(pub) || path.startsWith(pub + "/")) return true;
        }
        for (MethodPath mp : PUBLIC_METHOD_PATHS) {
            if (mp.method().equals(method) &&
                    (path.equals(mp.path()) || path.startsWith(mp.path() + "/"))) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (headers == null || headers.isEmpty()) return null;
        String value = headers.get(0);
        if (value == null || !value.startsWith("Bearer ")) return null;
        String token = value.substring(7).strip();
        return token.isEmpty() ? null : token;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> body = Map.of(
                    "success",    false,
                    "message",    message,
                    "body",       Map.of(),
                    "httpStatus", status.value()
            );
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize error response", e);
        }
    }

    private record MethodPath(HttpMethod method, String path) {}
}