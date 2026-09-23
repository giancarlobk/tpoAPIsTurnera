package com.grupo1.turnera.model;

import com.grupo1.turnera.model.enums.DiaSemana;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "horarios_atencion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "doctor_id",
            nullable = false
    )
    private Doctor doctor;

    @NotNull(
            message = "El día de atención es obligatorio"
    )
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaSemana diaSemana;

    @NotNull(
            message = "La hora de inicio es obligatoria"
    )
    @Column(nullable = false)
    private LocalTime horaInicio;

    @NotNull(
            message = "La hora de fin es obligatoria"
    )
    @Column(nullable = false)
    private LocalTime horaFin;

    @NotNull(
            message = "La duración del turno es obligatoria"
    )
    @Positive(
            message = "La duración del turno debe ser mayor a 0"
    )
    @Builder.Default
    @Column(nullable = false)
    private Integer duracionTurnoMinutos = 15;

    /*
     * Política definida para este proyecto:
     *
     * una franja debe comenzar y terminar
     * dentro del mismo día.
     *
     * Por lo tanto:
     *
     * 09:00 - 12:00 -> válido
     * 23:45 - 00:15 -> inválido
     */
    @AssertTrue(
            message =
                    "La hora de fin debe ser posterior a la hora de inicio"
    )
    public boolean isFranjaCoherente() {

        if (horaInicio == null || horaFin == null) {
            return true;
        }

        return horaFin.isAfter(horaInicio);
    }
}
