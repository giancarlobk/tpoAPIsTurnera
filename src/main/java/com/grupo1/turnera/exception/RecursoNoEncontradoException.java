package com.grupo1.turnera.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String tipoRecurso, Long id) {
        super(tipoRecurso + " con id " + id + " no encontrado"); // 404
    }
    
}
