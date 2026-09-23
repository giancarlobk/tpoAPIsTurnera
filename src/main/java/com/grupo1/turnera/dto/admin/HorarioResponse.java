package com.grupo1.turnera.dto.admin;

import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.enums.DiaSemana;

import java.time.LocalTime;

public record HorarioResponse(
        Long id,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer duracionTurnoMinutos
) {
    public static HorarioResponse fromEntity(HorarioAtencion horario) {
        return new HorarioResponse(horario.getId(), horario.getDiaSemana(), horario.getHoraInicio(),
                horario.getHoraFin(), horario.getDuracionTurnoMinutos());
    }
}