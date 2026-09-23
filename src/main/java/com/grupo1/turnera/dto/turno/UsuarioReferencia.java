package com.grupo1.turnera.dto.turno;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UsuarioReferencia(@NotNull @Positive Long id) {
}
