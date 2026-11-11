package com.grupo1.turnera.exception;

import java.time.LocalDateTime;

public class TurnoYaEnCarritoException extends RuntimeException {

    public TurnoYaEnCarritoException(Long doctorId, LocalDateTime fechaHoraInicio) {
        super("El turno del medico con id " + doctorId + " para la fecha y hora " + fechaHoraInicio
                + " ya está en el carrito"); // 409
    }
}
