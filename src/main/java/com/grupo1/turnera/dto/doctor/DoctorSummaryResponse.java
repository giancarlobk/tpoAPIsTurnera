
package com.grupo1.turnera.dto.doctor;

public record DoctorSummaryResponse(
        Long id,
        String nombre,
        String apellido,
        String matriculaNacional,
        Long especialidadId,
        String especialidadNombre,
        String fotoUrl,
        String descripcion
) {

    // Constructor compatible con las llamadas anteriores.
    public DoctorSummaryResponse(
            Long id,
            String nombre,
            String apellido,
            String matriculaNacional,
            Long especialidadId,
            String especialidadNombre
    ) {
        this(
                id,
                nombre,
                apellido,
                matriculaNacional,
                especialidadId,
                especialidadNombre,
                null,
                null
        );
    }
}
