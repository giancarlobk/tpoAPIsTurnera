package com.grupo1.turnera.dto.doctor;

public record DoctorSummaryResponse(
        Long id,
        String nombre,
        String apellido,
        String matriculaNacional,
        Long especialidadId,
        String especialidadNombre
) {
}
