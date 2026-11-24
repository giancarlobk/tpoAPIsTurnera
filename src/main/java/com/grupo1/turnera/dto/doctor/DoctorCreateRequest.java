package com.grupo1.turnera.dto.doctor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DoctorCreateRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos numéricos")
        String dni,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @NotBlank(message = "La matrícula nacional es obligatoria")
        @Size(max = 50, message = "La matrícula no puede superar los 50 caracteres")
        String matriculaNacional,

        
        @NotNull(message = "La especialidad es obligatoria")
        Long especialidadId,

        @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
        @Pattern(
                regexp = "^https://\\S+$",
                message = "La foto debe tener una URL HTTPS válida")
        String fotoUrl,
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion

) {
    public DoctorCreateRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
