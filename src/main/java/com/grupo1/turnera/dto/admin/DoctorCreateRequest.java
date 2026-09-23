package com.grupo1.turnera.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DoctorCreateRequest(
        @NotBlank @Size(max = 20) String dni,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8) String password,
        @Size(max = 20) String telefono,
        @NotBlank @Size(max = 50) String matriculaNacional,
        @NotNull @Positive Long especialidadId
) {
}