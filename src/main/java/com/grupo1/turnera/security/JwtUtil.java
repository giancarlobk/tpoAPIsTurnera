package com.grupo1.turnera.security;

import com.grupo1.turnera.model.BaseUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final JwtParser parser;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        if (secret == null || secret.isBlank() || expiration < 1000) {
            throw new IllegalArgumentException("Configurar jwt.secret en Base64 y jwt.expiration >= 1000 ms");
        }
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        parser = Jwts.parser().verifyWith(key).build();
        this.expiration = expiration;
    }

    public String generateToken(BaseUsuario usuario) {
        Instant now = Instant.now();
        return Jwts.builder().subject(usuario.getEmail())
                .claim("userId", usuario.getId())
                .claim("roles", usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expiration)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Claims validateToken(String token) {
        var signed = parser.parseSignedClaims(token);
        Claims claims = signed.getPayload();
        if (!"HS256".equals(signed.getHeader().getAlgorithm())
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getIssuedAt() == null || claims.getExpiration() == null
                || !claims.getExpiration().after(new Date())
                || !claims.getExpiration().after(claims.getIssuedAt())
                || claims.getIssuedAt().after(new Date())
                || !(claims.get("userId") instanceof Number)
                || !(claims.get("roles") instanceof List<?> roles) || roles.size() != 1
                || !List.of("ROLE_PACIENTE", "ROLE_MEDICO", "ROLE_ADMIN").contains(roles.get(0))) {
            throw new JwtException("Token inválido");
        }
        return claims;
    }

    public long getExpirationSeconds() { return expiration / 1000; }
}
