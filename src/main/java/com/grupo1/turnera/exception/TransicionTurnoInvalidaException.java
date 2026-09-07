package com.grupo1.turnera.exception;

import com.grupo1.turnera.model.enums.EstadoTurno;

public class TransicionTurnoInvalidaException extends RuntimeException {
    public TransicionTurnoInvalidaException(EstadoTurno estado) {
        super("No se puede cancelar un turno en estado " + estado);
    }
}
