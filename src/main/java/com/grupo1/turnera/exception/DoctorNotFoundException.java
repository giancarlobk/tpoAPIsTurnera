package com.grupo1.turnera.exception;

public class DoctorNotFoundException extends RuntimeException {

    public DoctorNotFoundException(Long doctorId) {
        super("No existe un doctor con id " + doctorId);
    }
} 
