package com.grupo1.turnera.exception;

public class EmailDuplicadoException extends RuntimeException {
    public EmailDuplicadoException(String email){
        super("Ya existe un usuario con el mismo Email: " + email);
    }
    
}
