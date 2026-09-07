package com.grupo1.turnera.dto.turno;

import com.grupo1.turnera.model.enums.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Actor declarado y motivo de la cancelación. No reemplaza la autenticación.")
public record CancelacionTurnoRequest(
        @NotNull @Positive @Schema(example = "1") Long usuarioId,
        @NotNull @Schema(allowableValues = {"PACIENTE", "MEDICO"}, example = "PACIENTE") Rol rol,
        @NotBlank @Size(max = 1000) @Schema(example = "No puedo asistir") String motivo
) {
}
