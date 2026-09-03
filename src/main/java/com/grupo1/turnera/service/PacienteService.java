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

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PacienteResponse registrarPaciente(PacienteCreateRequest request) {
        String dni = blankToNull(request.dni());
        String nombre = blankToNull(request.nombre());
        String apellido = blankToNull(request.apellido());
        String email = blankToNull(request.email());
        String telefono = blankToNull(request.telefono());
        String obraSocial = blankToNull(request.obraSocial());
        String numeroAfiliado = blankToNull(request.numeroAfiliado());

        if (email != null && pacienteRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new EmailDuplicadoException(email);
        }
        if (dni != null && pacienteRepository.findByDni(dni).isPresent()) {
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
                .email(email.toLowerCase(Locale.ROOT))
                .password(passwordEncoder.encode(request.password()))
                .telefono(telefono)
                .rol(Rol.PACIENTE)
                .activo(true)
                .fechaNacimiento(request.fechaNacimiento())
                .obraSocial(obraSocial)
                .numeroAfiliado(numeroAfiliado)
                .build();

        Paciente guardado = pacienteRepository.save(paciente);
        return toResponse(guardado);
    }

    @Transactional(readOnly = true)
    private PacienteResponse toResponse(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getDni(),
                paciente.getNombre(),
                paciente.getApellido(),
                paciente.getEmail(),
                paciente.getTelefono(),
                paciente.getRol(),
                paciente.getActivo(),
                paciente.getFechaNacimiento(),
                paciente.getObraSocial(),
                paciente.getNumeroAfiliado()
        );
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}