package com.miguel_damasco.DoSafe.security.jwt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;


@Service
public class JwtUtil {
    
    private final String secretKey;

    public JwtUtil(@Value("${jwt.secret}") String pSecretKey) {
        this.secretKey = pSecretKey;
    }
    
    public String generateToken(String pUsername) {

        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                            .claims()
                            .add(claims)
                            .subject(pUsername)
                            .issuedAt(new Date(System.currentTimeMillis()))
                            .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 60 * 30))
                            .and()
                            .signWith(getKey())
                            .compact();

    }


    private Key getKey() {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    private SecretKey getSecretKey() {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);        
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {

        Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                            .verifyWith(getSecretKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
    }

     public String extractUsername(String token) {

        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {

        return extractClaim(token, Claims::getExpiration);
    }
    

    private boolean isTokenExpired(String token) {

        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {

        String myUsername = extractUsername(token);
        return (myUsername.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
