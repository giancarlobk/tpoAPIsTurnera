package com.grupo1.turnera.dto.turno;

import com.grupo1.turnera.model.Turno;
import java.time.LocalDateTime;

/** Consulta pública: no contiene datos del paciente ni información clínica. */
public record TurnoDisponibleResponse(
        Long id, UsuarioReferencia doctor, LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin
) {
    public static TurnoDisponibleResponse fromEntity(Turno turno) {
        return new TurnoDisponibleResponse(turno.getId(), new UsuarioReferencia(turno.getDoctor().getId()),
                turno.getFechaHoraInicio(), turno.getFechaHoraFin());
    }
}
