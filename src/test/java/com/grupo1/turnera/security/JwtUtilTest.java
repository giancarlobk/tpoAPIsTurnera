package com.grupo1.turnera.security;

import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {
    static final String SECRET = "c2VjcmV0by1zb2xvLXBhcmEtcHJ1ZWJhcy1qd3QtdHVybmVyYS0xMjM0NTY=";
    private final JwtUtil jwt = new JwtUtil(SECRET, 3600000);

    @Test
    void firmaSubjectRolEmisionYExpiracion() {
        var user = Paciente.builder().id(1L).email("p@example.test").rol(Rol.PACIENTE).build();
        var claims = jwt.validateToken(jwt.generateToken(user));
        assertThat(claims.getSubject()).isEqualTo(user.getEmail());
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_PACIENTE");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(3600000);
        assertThat(claims).doesNotContainKeys("password", "dni");
    }

    @Test
    void rechazaTokenVencidoAlteradoOFirmadoConOtraClave() {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String expired = Jwts.builder().subject("p@example.test").claim("roles", List.of("ROLE_PACIENTE"))
                .claim("userId", 1L).issuedAt(Date.from(Instant.now().minusSeconds(100)))
                .expiration(Date.from(Instant.now().minusSeconds(1))).signWith(key, Jwts.SIG.HS256).compact();
        String foreign = Jwts.builder().subject("p@example.test").signWith(Jwts.SIG.HS256.key().build()).compact();
        for (String token : List.of(expired, foreign, "abc.def.ghi")) {
            assertThatThrownBy(() -> jwt.validateToken(token)).isInstanceOf(JwtException.class);
        }
    }

    @Test
    void rechazaClaimsObligatoriosAusentesYRolDesconocido() {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        var now = new Date();
        var later = Date.from(Instant.now().plusSeconds(3600));
        var valid = Jwts.builder().subject("p@example.test").claim("userId", 1L)
                .claim("roles", List.of("ROLE_PACIENTE")).issuedAt(now).expiration(later);
        String missingExpiration = valid.expiration(null).signWith(key, Jwts.SIG.HS256).compact();
        String unknownRole = valid.expiration(later).claim("roles", List.of("ADMIN")).signWith(key, Jwts.SIG.HS256).compact();
        String missingSubject = valid.subject(null).signWith(key, Jwts.SIG.HS256).compact();
        for (String token : List.of(missingExpiration, unknownRole, missingSubject)) {
            assertThatThrownBy(() -> jwt.validateToken(token)).isInstanceOf(JwtException.class);
        }
    }

    @Test
    void exigeSecretoFuerteYExpiracionPositiva() {
        assertThatThrownBy(() -> new JwtUtil("", 3600000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtUtil("Y2xh dmU=", 3600000)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new JwtUtil(SECRET, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtUtil("Y2xhdmU=", 3600000)).isInstanceOf(RuntimeException.class);
    }
}
