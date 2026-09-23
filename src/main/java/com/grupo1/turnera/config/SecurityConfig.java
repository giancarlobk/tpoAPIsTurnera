package com.grupo1.turnera.config;

import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.AdministradorRepository;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final PacienteRepository pacienteRepository;
    private final DoctorRepository doctorRepository;
    private final AdministradorRepository administradorRepository;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**", "/api/pacientes", "/api/doctores/**",
                                "/api/especialidades/**", "/api/turnos/**", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(Rol.ADMIN.name())
                        .anyRequest().authenticated())
                .httpBasic(basic -> {})
                .formLogin(form -> form.disable());
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> administradorRepository.findByEmailIgnoreCase(username)
                .map(usuario -> User.withUsername(usuario.getEmail())
                        .password(usuario.getPassword())
                        .roles(usuario.getRol().name())
                        .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                        .build())
                .or(() -> doctorRepository.findByEmailIgnoreCase(username)
                        .map(usuario -> User.withUsername(usuario.getEmail())
                                .password(usuario.getPassword())
                                .roles(usuario.getRol().name())
                                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                                .build()))
                .or(() -> pacienteRepository.findByEmailIgnoreCase(username)
                        .map(usuario -> User.withUsername(usuario.getEmail())
                                .password(usuario.getPassword())
                                .roles(usuario.getRol().name())
                                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                                .build()))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}