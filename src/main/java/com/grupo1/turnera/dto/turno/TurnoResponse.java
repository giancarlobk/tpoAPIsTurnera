package com.grupo1.turnera.dto.turno;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.EstadoTurno;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Nombramos el schema de OpenAPI "TurnoReservaResponse" (distinto del nombre de la clase Java)
// para no chocar con el schema "TurnoResponse" ya documentado para /api/turnos/sobreturno
// en TurneraOpenApiSchemas.
@Schema(name = "TurnoReservaResponse", description = "Turno reservado, sin exponer el historial completo ni la historia clínica")
public record TurnoResponse(
        Long id,
        Long doctorId,
        String doctorNombreCompleto,
        Long pacienteId,
        String pacienteNombreCompleto,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        EstadoTurno estado,
        Boolean esSobreturno
) {

    public static TurnoResponse fromEntity(Turno turno) {
        return new TurnoResponse(
                turno.getId(),
                turno.getDoctor().getId(),
                turno.getDoctor().getNombre() + " " + turno.getDoctor().getApellido(),
                turno.getPaciente().getId(),
                turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido(),
                turno.getFechaHoraInicio(),
                turno.getFechaHoraFin(),
                turno.getEstado(),
                turno.getEsSobreturned()
        );
    }
}