package com.grupo1.turnera.exception;

public class EspecialidadDuplicadaException extends RuntimeException {

    public EspecialidadDuplicadaException(String nombre) {
        super("La especialidad " + nombre + " ya esta registrada");
    }
}