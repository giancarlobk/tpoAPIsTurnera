package com.grupo1.turnera.dto.paciente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "PacienteCreateRequest", description = "Datos requeridos para registrar un paciente")
public record PacienteCreateRequest(
        @Schema(example = "30111222")
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos")
        String dni,

        @Schema(example = "Ana")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Schema(example = "Pérez")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @Schema(example = "ana.perez@example.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @Schema(example = "ClaveSegura123", format = "password")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Schema(example = "1122334455")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,

        @Schema(example = "1995-04-18")
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @Schema(example = "OSDE")
        @Size(max = 100, message = "La obra social no puede superar los 100 caracteres")
        String obraSocial,

        @Schema(example = "123456789")
        @Size(max = 50, message = "El número de afiliado no puede superar los 50 caracteres")
        String numeroAfiliado
) {
}
