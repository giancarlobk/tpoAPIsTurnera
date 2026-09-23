package com.grupo1.turnera.dto.turno;

import com.grupo1.turnera.model.enums.EstadoTurno;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambioEstadoTurnoRequest(
        @NotNull EstadoTurno estadoDestino,
        @Size(max = 2000) String motivo
) {
}