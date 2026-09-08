package com.grupo1.turnera.dto.turno;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** MEDICO usa su propia identidad; ADMIN debe indicar el doctor. */
public record SobreturnoRequest(
        @Valid UsuarioReferencia doctor,
        @NotNull @Valid UsuarioReferencia paciente,
        @NotNull LocalDateTime fechaHoraInicio,
        @NotNull LocalDateTime fechaHoraFin,
        @NotBlank @Size(max = 2000) String justificacionSobreturned
) {
}
