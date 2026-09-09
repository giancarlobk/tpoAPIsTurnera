package com.grupo1.turnera.dto.turno;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** MEDICO usa su propia identidad; ADMIN debe indicar el doctor. */
public record SobreturnoRequest(
        @Valid UsuarioReferencia doctor,
        @NotNull @Valid UsuarioReferencia paciente,
        @NotNull @Future(message = "La fecha y hora de inicio deben ser futuras") LocalDateTime fechaHoraInicio,
        @NotNull @Future(message = "La fecha y hora de fin deben ser futuras") LocalDateTime fechaHoraFin,
        @NotBlank @Size(max = 2000) String justificacionSobreturned
) {
}
