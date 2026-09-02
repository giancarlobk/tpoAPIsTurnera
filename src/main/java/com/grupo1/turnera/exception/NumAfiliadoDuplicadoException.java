package com.grupo1.turnera.exception;

public class NumAfiliadoDuplicadoException extends RuntimeException {
    public NumAfiliadoDuplicadoException(String numeroAfiliado) {
        super("Ya existe un paciente registrado con el número de afiliado: " + numeroAfiliado);
    }
}