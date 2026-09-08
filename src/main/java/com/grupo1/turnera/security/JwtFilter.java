package com.grupo1.turnera.security;

import com.grupo1.turnera.model.BaseUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Se agrega solamente a SecurityFilterChain, no al registro de filtros del servlet. */
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final SecurityErrorHandler errors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    throw new BadCredentialsException("Esquema de autenticación inválido");
                }
                Claims claims = jwtUtil.validateToken(header.substring(7));
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
            } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
                SecurityContextHolder.clearContext();
                errors.commence(request, response, new BadCredentialsException("Token inválido"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
