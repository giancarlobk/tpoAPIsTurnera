package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.auth.LoginRequest;
import com.grupo1.turnera.dto.auth.LoginResponse;
import com.grupo1.turnera.exception.CredencialesInvalidasException;
import com.grupo1.turnera.model.BaseUsuario;
import com.grupo1.turnera.repository.AdministradorRepository;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PacienteRepository pacienteRepository;
    private final DoctorRepository doctorRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        BaseUsuario usuario = buscarUsuarioUnico(request.email().trim());

        if (!Boolean.TRUE.equals(usuario.getActivo()) || !passwordValida(request.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }

        return new LoginResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol()
        );
    }

    private BaseUsuario buscarUsuarioUnico(String email) {
        List<BaseUsuario> coincidencias = Stream.of(
                        pacienteRepository.findByEmailIgnoreCase(email).map(BaseUsuario.class::cast),
                        doctorRepository.findByEmailIgnoreCase(email).map(BaseUsuario.class::cast),
                        administradorRepository.findByEmailIgnoreCase(email).map(BaseUsuario.class::cast)
                )
                .flatMap(Optional::stream)
                .toList();

        if (coincidencias.size() != 1) {
            throw new CredencialesInvalidasException();
        }

        return coincidencias.get(0);
    }

    private boolean passwordValida(String passwordPlano, String passwordHash) {
        try {
            return passwordEncoder.matches(passwordPlano, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
