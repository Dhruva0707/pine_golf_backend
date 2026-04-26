package com.pinewoods.score.tracker.config.security;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {private final SecretKey SECRET = Keys.hmacShaKeyFor("bm]hXshM!tD_(GoBkdo+Y;FXnhAi[rnTspHBdrE6_XU"
        .getBytes(StandardCharsets.UTF_8));

    private final JwtParser parser = Jwts.parser()
            .verifyWith(SECRET)
            .build();

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities())
                .issuedAt(now)
                .expiration(Date.from(now.toInstant().plusSeconds(6*60 * 60))) // 1-hour validity
                .signWith(SECRET, Jwts.SIG.HS256)
                .compact();
    }

    // method to fetch the username from the JWT token
    public String extractUsername(String token) {
        return parser.parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return System.currentTimeMillis() >
                parser.parseSignedClaims(token)
                        .getPayload()
                        .getExpiration()
                        .getTime();
    }
}
