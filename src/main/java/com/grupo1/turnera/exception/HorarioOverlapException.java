package com.grupo1.turnera.exception;

public class HorarioOverlapException extends RuntimeException {

    public HorarioOverlapException() {
        super("El horario se superpone con otro horario existente del doctor seleccionado.");
    }
} 
