package com.grupo1.turnera.exception;

public class TransicionEstadoTurnoInvalidaException extends RuntimeException {
    public TransicionEstadoTurnoInvalidaException(String mensaje) {
        super(mensaje);
    }
}