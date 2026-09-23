package com.grupo1.turnera.exception;

/**
 * Representa un argumento inválido o una regla de negocio
 * incumplida por los datos enviados por el cliente.
 *
 * El GlobalExceptionHandler la transforma en HTTP 400.
 */
public class ArgumentoInvalidoException extends RuntimeException {

    public ArgumentoInvalidoException(String message) {
        super(message);
    }
}
