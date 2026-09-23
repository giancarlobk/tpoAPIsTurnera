package com.grupo1.turnera.dto.turno;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** La identidad del paciente se obtiene exclusivamente del principal. */
public record ReservaTurnoRequest(
        @NotNull @Valid UsuarioReferencia doctor,
        @NotNull @Future(message = "La fecha y hora del turno deben ser futuras") LocalDateTime fechaHoraInicio
) {
}
