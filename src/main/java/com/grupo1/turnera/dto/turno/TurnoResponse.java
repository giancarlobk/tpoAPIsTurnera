package com.grupo1.turnera.dto.turno;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.EstadoTurno;

import java.time.LocalDateTime;

public record TurnoResponse(
        Long id, UsuarioReferencia doctor, UsuarioReferencia paciente,
        LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
        EstadoTurno estado, Boolean esSobreturned, String justificacionSobreturned
) {
    public static TurnoResponse fromEntity(Turno turno) {
        return new TurnoResponse(turno.getId(), new UsuarioReferencia(turno.getDoctor().getId()),
                turno.getPaciente() == null ? null : new UsuarioReferencia(turno.getPaciente().getId()),
                turno.getFechaHoraInicio(), turno.getFechaHoraFin(), turno.getEstado(),
                turno.getEsSobreturned(), turno.getJustificacionSobreturned());
    }
}
