package com.grupo1.turnera.exception;

public class TurnoNoEncontradoException extends RuntimeException {
    public TurnoNoEncontradoException(Long id) {
        super("No existe el turno " + id);
    }
}
