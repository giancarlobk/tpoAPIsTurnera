package com.grupo1.turnera.exception;

public class DniDuplicadoException extends RuntimeException {
    public DniDuplicadoException(String dni){
        super("Ya existe un usuario con el mismo Dni: " + dni);
    }
    
}
