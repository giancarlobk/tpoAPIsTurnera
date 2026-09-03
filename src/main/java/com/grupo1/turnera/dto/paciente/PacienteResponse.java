package com.grupo1.turnera.dto.paciente;

import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "PacienteResponse", description = "Paciente registrado, sin contraseña ni relaciones JPA")
public record PacienteResponse(
        Long id,
        String dni,
        String nombre,
        String apellido,
        String email,
        String telefono,
        Rol rol,
        Boolean activo,
        LocalDate fechaNacimiento,
        String obraSocial,
        String numeroAfiliado
) {

    public static PacienteResponse from(Paciente paciente) {
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
}
