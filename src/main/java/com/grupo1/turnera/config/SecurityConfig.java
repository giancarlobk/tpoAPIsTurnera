package com.grupo1.turnera.config;

import com.grupo1.turnera.repository.UsuarioRepository;
import com.grupo1.turnera.security.JwtFilter;
import com.grupo1.turnera.security.JwtUtil;
import com.grupo1.turnera.security.SecurityErrorHandler;
import com.grupo1.turnera.security.SesionCookie;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmail(email.trim())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Email o contraseña incorrectos"));
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService users,
            PasswordEncoder encoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtUtil jwt,
            UserDetailsService users,
            SecurityErrorHandler errors,
            SesionCookie sesionCookie
    ) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository(sesionCookie))
                        // Token sin enmascarar: el front lo lee de la cookie XSRF-TOKEN y lo devuelve en X-XSRF-TOKEN.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(request -> requiereCsrf(request, sesionCookie))
                        // Por defecto Spring rota el token CSRF en cada autenticación. Con JWT el filtro autentica
                        // cada pedido, así que lo borraría siempre y el siguiente POST fallaría con 403.
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(errors)
                          .accessDeniedHandler(errors))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/auth/me")
                        .authenticated()

                        .requestMatchers(
                                "/api/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/pacientes")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/doctores",
                                "/api/especialidades"
                        ).hasRole("ADMIN")

                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/doctores",
                                "/api/especialidades",
                                "/api/turnos/disponibles"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/turnos")
                        .hasAnyRole("PACIENTE", "MEDICO", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/turnos/reservar")
                        .hasRole("PACIENTE")

                        .requestMatchers(HttpMethod.POST, "/api/turnos/sobreturno")
                        .hasAnyRole("MEDICO", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/carrito/items")
                        .hasRole("PACIENTE")

                        .requestMatchers("/api/carrito", "/api/carrito/**")
                        .hasAnyRole("PACIENTE", "ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtFilter(jwt, users, errors, sesionCookie),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    /**
     * Doble envío del token CSRF: Spring deja el token en la cookie XSRF-TOKEN (legible por JS, a diferencia de la
     * cookie de sesión) y exige que vuelva en el header X-XSRF-TOKEN. Otro sitio no puede leer esa cookie,
     * así que no puede armar el header.
     */
    private static CookieCsrfTokenRepository csrfTokenRepository(SesionCookie sesionCookie) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Strict").secure(sesionCookie.isSecure()));
        return repository;
    }

    /**
     * El CSRF solo es posible cuando el navegador manda la credencial solo (la cookie de sesión).
     * Los clientes con header Bearer no lo necesitan, los métodos de lectura no modifican nada, y login y
     * registro aceptan solo JSON, que otro sitio no puede enviar sin pasar por CORS.
     */
    private static boolean requiereCsrf(HttpServletRequest request, SesionCookie sesionCookie) {
        return !METODOS_SEGUROS.contains(request.getMethod())
                && request.getHeader(HttpHeaders.AUTHORIZATION) == null
                && !RUTAS_SIN_CSRF.contains(request.getRequestURI())
                && sesionCookie.leer(request).isPresent();
    }

    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> RUTAS_SIN_CSRF = Set.of("/api/auth/login", "/api/auth/register");
}