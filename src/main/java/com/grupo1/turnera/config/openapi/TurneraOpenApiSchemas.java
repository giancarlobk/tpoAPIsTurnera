package com.grupo1.turnera.config.openapi;

import com.grupo1.turnera.model.enums.EstadoTurno;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import com.grupo1.turnera.model.enums.DiaSemana;
import java.time.LocalTime;

public final class TurneraOpenApiSchemas {

    private TurneraOpenApiSchemas() {
    }

    @Schema(name = "EntityReference", description = "Referencia por identificador a una entidad existente")
    public record EntityReference(
            @Schema(example = "1") Long id
    ) {
    }

    @Schema(name = "TurnoRequest", description = "Datos de un turno; doctor y paciente se referencian por id")
    public record TurnoRequest(
            EntityReference doctor,
            EntityReference paciente,
            @Schema(example = "2026-09-10T10:00:00") LocalDateTime fechaHoraInicio,
            @Schema(example = "2026-09-10T10:30:00") LocalDateTime fechaHoraFin,
            EstadoTurno estado,
            @Schema(example = "false") Boolean esSobreturned,
            String justificacionSobreturned
    ) {
    }

    @Schema(name = "TurnoResponse", description = "Turno persistido, sin historial ni historia clínica")
    public record TurnoResponse(
            Long id,
            EntityReference doctor,
            EntityReference paciente,
            LocalDateTime fechaHoraInicio,
            LocalDateTime fechaHoraFin,
            EstadoTurno estado,
            Boolean esSobreturned,
            String justificacionSobreturned
    ) {
    }
    @Schema(
        name = "HorarioCreateRequest",
        description = "Datos necesarios para registrar un bloque semanal de atención de un doctor"
)
public record HorarioCreateRequest(

        @Schema(example = "LUNES")
        DiaSemana diaSemana,

        @Schema(example = "09:00")
        LocalTime horaInicio,

        @Schema(example = "13:00")
        LocalTime horaFin,

        @Schema(example = "15")
        Integer duracionTurnoMinutos

) {
}

@Schema(
        name = "HorarioResponse",
        description = "Bloque semanal de atención registrado para un doctor"
)
public record HorarioResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "1")
        Long doctorId,

        @Schema(example = "LUNES")
        DiaSemana diaSemana,

        @Schema(example = "09:00")
        LocalTime horaInicio,

        @Schema(example = "13:00")
        LocalTime horaFin,

        @Schema(example = "15")
        Integer duracionTurnoMinutos

) {
}
}
