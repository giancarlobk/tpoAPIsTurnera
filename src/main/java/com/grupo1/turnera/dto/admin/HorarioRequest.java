package com.grupo1.turnera.dto.admin;

import com.grupo1.turnera.model.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

public record HorarioRequest(
        @NotNull DiaSemana diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @NotNull @Positive Integer duracionTurnoMinutos
) {
}