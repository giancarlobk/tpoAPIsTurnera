package com.grupo1.turnera.security;

import com.grupo1.turnera.model.BaseUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Se agrega solamente a SecurityFilterChain, no al registro de filtros del servlet.
 * Toma el JWT del header Authorization (clientes como Swagger) o, si no viene, de la cookie de sesión del navegador.
 */
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final SecurityErrorHandler errors;
    private final SesionCookie sesionCookie;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    throw new BadCredentialsException("Esquema de autenticación inválido");
                }
                autenticar(header.substring(7));
            } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
                SecurityContextHolder.clearContext();
                errors.commence(request, response, new BadCredentialsException("Token inválido"));
                return;
            }
        } else {
            Optional<String> cookie = sesionCookie.leer(request);
            if (cookie.isPresent()) {
                try {
                    autenticar(cookie.get());
                } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
                    // Una cookie vencida o inválida no bloquea las rutas públicas: se borra y el pedido
                    // sigue como anónimo. Las rutas protegidas responden 401 desde el entry point.
                    SecurityContextHolder.clearContext();
                    response.addHeader(HttpHeaders.SET_COOKIE, sesionCookie.borrar().toString());
                }
            }
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String token) {
        Claims claims = jwtUtil.validateToken(token);
        BaseUsuario usuario = (BaseUsuario) userDetailsService.loadUserByUsername(claims.getSubject());
        String role = ((List<?>) claims.get("roles")).get(0).toString();
        if (!usuario.isEnabled() || !usuario.isAccountNonLocked() || !usuario.isAccountNonExpired()
                || !usuario.isCredentialsNonExpired()
                || !usuario.getId().equals(((Number) claims.get("userId")).longValue())
                || !usuario.getAuthorities().contains(new SimpleGrantedAuthority(role))) {
            throw new BadCredentialsException("El usuario o su rol ya no están habilitados");
        }
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                usuario, null, List.of(new SimpleGrantedAuthority(role)));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
