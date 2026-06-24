package com.codingclub.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtFilterFactory; // This is the factory

    @Mock
    private GatewayFilterChain filterChain;

    private GatewayFilter actualFilter; // This will hold the actual filter

    @BeforeEach
    void setUp() {
        // Extract the actual filter from the factory by calling apply()
        actualFilter = jwtFilterFactory.apply(new JwtAuthenticationFilter.Config());
    }

    @Test
    void testFilter_MissingAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/projects")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Call .filter() on the extracted actualFilter, not the factory
        Mono<Void> result = actualFilter.filter(exchange, filterChain);
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

        // Call .filter() on the extracted actualFilter, not the factory
        Mono<Void> result = actualFilter.filter(exchange, filterChain);
        result.subscribe();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }
}