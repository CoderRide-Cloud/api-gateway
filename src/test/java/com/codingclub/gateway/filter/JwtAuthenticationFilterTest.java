package com.codingclub.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtFilter;

    @Mock
    private GatewayFilterChain filterChain;

    private String validToken;

    @BeforeEach
    void setUp() {
        // Since JwtAuthenticationFilter creates its own key based on a secret, 
        // we'd normally inject the JwtUtil or the secret. 
        // For testing the gateway logic, we can simulate the request.
        // Assuming we have a valid token (this requires the actual secret configured in filter)
    }

    @Test
    void testFilter_MissingAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/projects")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = jwtFilter.filter(exchange, filterChain);
        result.subscribe();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_InvalidAuthHeaderFormat() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/projects")
                .header(HttpHeaders.AUTHORIZATION, "InvalidFormat token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = jwtFilter.filter(exchange, filterChain);
        result.subscribe();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }
}
