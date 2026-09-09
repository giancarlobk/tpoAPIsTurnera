package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.auth.LoginRequest;
import com.grupo1.turnera.dto.auth.LoginResponse;
import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteResponse;
import com.grupo1.turnera.exception.CredencialesInvalidasException;
import com.grupo1.turnera.model.BaseUsuario;
import com.grupo1.turnera.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final PacienteService pacienteService;
    private final JwtUtil jwtUtil;

    @Transactional
    public PacienteResponse register(PacienteCreateRequest request) {
        return pacienteService.registrarPaciente(request);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new CredencialesInvalidasException();
        }
        try {
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
            BaseUsuario usuario = (BaseUsuario) authentication.getPrincipal();
            return new LoginResponse(usuario.getId(), usuario.getNombre(), usuario.getApellido(),
                    usuario.getEmail(), usuario.getRol(), jwtUtil.generateToken(usuario),
                    "Bearer", jwtUtil.getExpirationSeconds());
        } catch (AuthenticationException exception) {
            throw new CredencialesInvalidasException();
        }
    }
}
