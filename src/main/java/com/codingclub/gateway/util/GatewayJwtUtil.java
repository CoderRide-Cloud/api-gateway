package com.codingclub.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class GatewayJwtUtil {

    @Value("${jwt.secret:thisisaverysecuresecretkeythatisverylongandsecure!}")
    private String secret;

    // OPTIMIZED: Cache derived key once at startup — not re-derived on every request
    private Key cachedSigningKey;

    @PostConstruct
    public void init() {
        this.cachedSigningKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(cachedSigningKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            // OPTIMIZED: parse once — validate expiry from same Claims object
            // Previously: validateToken -> extractAllClaims, then filter called extractAllClaims again
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
