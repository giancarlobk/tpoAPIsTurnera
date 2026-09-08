package com.grupo1.turnera.dto.turno;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** La identidad del paciente se obtiene exclusivamente del principal. */
public record ReservaTurnoRequest(
        @NotNull @Valid UsuarioReferencia doctor,
        @NotNull LocalDateTime fechaHoraInicio,
        @NotNull LocalDateTime fechaHoraFin
) {
}
