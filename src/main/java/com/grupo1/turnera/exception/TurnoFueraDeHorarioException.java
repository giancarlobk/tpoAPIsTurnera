package com.grupo1.turnera.exception;

public class TurnoFueraDeHorarioException extends RuntimeException {

    public TurnoFueraDeHorarioException(Long doctorId, String fechaHorarioInicio) {
        super("El medico con id " + doctorId + " no tiene disponibilidad para la fecha y hora " + fechaHorarioInicio); // 400
    }
    
}
