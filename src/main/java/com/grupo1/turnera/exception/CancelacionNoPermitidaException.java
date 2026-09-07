package com.grupo1.turnera.exception;

public class CancelacionNoPermitidaException extends RuntimeException {
    public CancelacionNoPermitidaException() {
        super("Solo el paciente o médico activo asociado al turno puede cancelarlo");
    }
}
