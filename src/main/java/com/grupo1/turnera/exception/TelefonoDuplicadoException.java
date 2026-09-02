package com.grupo1.turnera.exception;

public class TelefonoDuplicadoException extends RuntimeException {
    public TelefonoDuplicadoException(String telefono) {
        super("Ya existe un usuario registrado con el teléfono: " + telefono);
    }
    
}
