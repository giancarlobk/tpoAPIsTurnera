package com.grupo1.turnera.exception;

public class MatriculaDuplicadaException extends RuntimeException {

    public MatriculaDuplicadaException(String matricula) {
        super("La matrícula nacional " + matricula + " ya está registrada");
    }
}