package com.grupo1.turnera.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EspecialidadCreateRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 255) String descripcion
) {
}