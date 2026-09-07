package com.grupo1.turnera.dto.turno;

import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.EntityReference;
import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.EstadoTurno;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

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
    public static TurnoResponse from(Turno turno) {
        return new TurnoResponse(turno.getId(), new EntityReference(turno.getDoctor().getId()),
                turno.getPaciente() == null ? null : new EntityReference(turno.getPaciente().getId()),
                turno.getFechaHoraInicio(), turno.getFechaHoraFin(), turno.getEstado(),
                turno.getEsSobreturned(), turno.getJustificacionSobreturned());
    }
}
