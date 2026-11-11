package com.grupo1.turnera.dto.carrito;

import com.grupo1.turnera.dto.turno.UsuarioReferencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** El carrito pertenece al paciente autenticado: el request solo trae médico y horario. */
public record AgregarItemCarritoRequest(
        @NotNull @Valid UsuarioReferencia doctor,
        @NotNull @Future(message = "La fecha y hora del turno deben ser futuras") LocalDateTime fechaHoraInicio
) {
}
