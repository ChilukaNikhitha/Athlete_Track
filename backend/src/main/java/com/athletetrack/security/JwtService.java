package com.athletetrack.security;

import com.athletetrack.user.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiryMs;
    public JwtService(@Value("${athletetrack.jwt.secret}") String secret,
                      @Value("${athletetrack.jwt.expiration-ms}") long expiryMs) {
        byte[] decoded = Decoders.BASE64.decode(secret);
        if (decoded.length < 32) throw new IllegalArgumentException("JWT_SECRET must be Base64 for a key of at least 32 bytes.");
        this.key = Keys.hmacShaKeyFor(decoded);
        this.expiryMs = expiryMs;
    }
    public String create(UserAccount user) {
        Date now = new Date();
        return Jwts.builder().subject(user.getUsername()).claim("role", user.getRole().name())
                .issuedAt(now).expiration(new Date(now.getTime() + expiryMs)).signWith(key).compact();
    }
    public String subject(String token) { return claims(token).getSubject(); }
    public boolean valid(String token, String email) {
        Claims claims = claims(token);
        return email.equalsIgnoreCase(claims.getSubject()) && claims.getExpiration().after(new Date());
    }
    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
