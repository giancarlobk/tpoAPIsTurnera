package com.grupo1.turnera.dto.especialidad;

import com.grupo1.turnera.model.Especialidad;

public record EspecialidadResponse(Long id, String nombre, String descripcion) {

    public static EspecialidadResponse fromEntity(Especialidad especialidad) {
        return new EspecialidadResponse(
                especialidad.getId(),
                especialidad.getNombre(),
                especialidad.getDescripcion()
        );
    }
}