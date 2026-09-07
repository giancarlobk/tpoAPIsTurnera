package com.grupo1.turnera.dto.turno;
 
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
 
import java.time.LocalDateTime;
 
@Schema(name = "TurnoReservaRequest", description = "Datos requeridos para reservar un turno")
public record TurnoReservaRequest(
 
        @NotNull(message = "El id del paciente es obligatorio")
        @Schema(example = "2")
        Long pacienteId,
 
        @NotNull(message = "El id del médico es obligatorio")
        @Schema(example = "1")
        Long doctorId,
 
        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        @Future(message = "La fecha y hora del turno deben ser futuras")
        @Schema(example = "2026-09-14T09:00:00")
        LocalDateTime fechaHoraInicio
) {
}
 