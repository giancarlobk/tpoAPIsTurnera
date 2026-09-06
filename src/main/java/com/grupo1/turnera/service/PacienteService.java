package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteResponse;
import com.grupo1.turnera.exception.DniDuplicadoException;
import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.exception.NumAfiliadoDuplicadoException;
import com.grupo1.turnera.exception.TelefonoDuplicadoException;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PacienteResponse registrarPaciente(PacienteCreateRequest request) {
        String dni = request.dni().trim();
        String nombre = request.nombre().trim();
        String apellido = request.apellido().trim();
        String email = request.email().trim();
        String telefono = normalizar(request.telefono());
        String obraSocial = normalizar(request.obraSocial());
        String numeroAfiliado = normalizar(request.numeroAfiliado());

        if (pacienteRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new EmailDuplicadoException(email);
        }
        if (pacienteRepository.findByDni(dni).isPresent()) {
            throw new DniDuplicadoException(dni);
        }
        if (telefono != null && pacienteRepository.findByTelefono(telefono).isPresent()) {
            throw new TelefonoDuplicadoException(telefono);
        }
        if (numeroAfiliado != null && pacienteRepository.findByNumeroAfiliado(numeroAfiliado).isPresent()) {
            throw new NumAfiliadoDuplicadoException(numeroAfiliado);
        }

        Paciente paciente = Paciente.builder()
                .dni(dni)
                .nombre(nombre)
                .apellido(apellido)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .telefono(telefono)
                .rol(Rol.PACIENTE)
                .activo(true)
                .fechaNacimiento(request.fechaNacimiento())
                .obraSocial(obraSocial)
                .numeroAfiliado(numeroAfiliado)
                .build();

        Paciente pacienteGuardado = pacienteRepository.save(paciente);

        return PacienteResponse.fromEntity(pacienteGuardado);
    }

    private String normalizar(String valor) {
        return Optional.ofNullable(valor)
                .map(String::trim)
                .filter(texto -> !texto.isEmpty())
                .orElse(null);
    }
}
