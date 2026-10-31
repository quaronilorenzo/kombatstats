package com.example.demo.security;

import com.example.demo.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class JwtSecurity {
    private final int exparationTime;
    private final SecretKey jwtKey;

    public JwtSecurity(
            @Value("${app.jwt.expiration-minutes}")
            int exparationTime,
            @Value("${app.jwt.secret}")
            String jwtSecret) {
        this.exparationTime = exparationTime;
        byte[] decodedBytes = Decoders.BASE64.decode(jwtSecret);
        this.jwtKey = Keys.hmacShaKeyFor(decodedBytes);
    }

    public String generateToken(User user){
        return Jwts.builder().subject(Long.toString(user.getId()))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(exparationTime))))
                .signWith(jwtKey)
                .compact();
    }

    public Long extractUserId(String token){
        return Long.valueOf(Jwts.parser()
            .verifyWith(jwtKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject());
    }
}
