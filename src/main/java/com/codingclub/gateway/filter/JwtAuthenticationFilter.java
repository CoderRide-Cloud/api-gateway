package com.codingclub.gateway.filter;

import com.codingclub.gateway.util.GatewayJwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private GatewayJwtUtil jwtUtil;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (path.startsWith("/api/v1/auth/github") && !path.endsWith("/repos")) {
                return chain.filter(exchange);
            }

            boolean isPublic = isPublicReadPath(exchange.getRequest().getMethod().name(), path);

            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                if (isPublic) {
                    return chain.filter(exchange);
                }
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            } else {
                if (isPublic) {
                    return chain.filter(exchange);
                }
                return onError(exchange, "Invalid authorization header format", HttpStatus.UNAUTHORIZED);
            }

            // OPTIMIZED: Parse JWT claims exactly once per request.
            // Previously: validateToken() parsed claims, then extractAllClaims() parsed them again.
            Claims claims;
            try {
                claims = jwtUtil.extractAllClaims(authHeader);
                // Check expiry from the already-parsed claims (no second parse)
                if (claims.getExpiration().before(new java.util.Date())) {
                    if (isPublic) return chain.filter(exchange);
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }
            } catch (Exception e) {
                if (isPublic) {
                    return chain.filter(exchange);
                }
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String permissions = claims.get("permissions", String.class);
            Object position = claims.get("position");
            Object isLead = claims.get("isLead");
            Object isActive = claims.get("isActive");
            Object customRoleId = claims.get("customRoleId");

            ServerWebExchange mutatedExchange = exchange.mutate().request(
                    exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role != null ? role : "")
                            .header("X-User-Permissions", permissions != null ? permissions : "")
                            .header("X-User-Position", position != null ? String.valueOf(position) : "0")
                            .header("X-User-Is-Lead", isLead != null ? String.valueOf(isLead) : "false")
                            .header("X-User-Is-Active", isActive != null ? String.valueOf(isActive) : "true")
                            .header("X-User-Custom-Role-Id", customRoleId != null ? String.valueOf(customRoleId) : "")
                            .build()
            ).build();

            return chain.filter(mutatedExchange);
        };
    }

    private boolean isPublicReadPath(String method, String path) {
        if (!"GET".equals(method)) {
            return false;
        }

        if (path.equals("/api/v1/members") || path.matches("/api/v1/members/\\d+")
                || path.matches("/api/v1/members/user/\\d+")) {
            return true;
        }

        if (path.startsWith("/api/v1/tags")) {
            return true;
        }

        if (path.startsWith("/api/v1/skills")) {
            return true;
        }

        if (path.startsWith("/api/v1/events")) {
            return true;
        }

        if (path.equals("/api/v1/projects") || path.matches("/api/v1/projects/\\d+")) {
            return true;
        }

        if (path.equals("/api/v1/roles") || path.matches("/api/v1/roles/\\d+")) {
            return true;
        }

        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"status\":" + httpStatus.value()
                + ",\"error\":\"" + httpStatus.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {
    }
}
