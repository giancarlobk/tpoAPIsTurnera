package com.grupo1.turnera.exception;
import java.time.LocalDateTime;

public class TurnoNoDisponibleException extends RuntimeException {

    public TurnoNoDisponibleException(Long doctorId, LocalDateTime fechaHorarioInicio) {
        super("El medico con id " + doctorId + " ya tiene un turno reservado para la fecha y hora " + fechaHorarioInicio); // 409
    }

}
