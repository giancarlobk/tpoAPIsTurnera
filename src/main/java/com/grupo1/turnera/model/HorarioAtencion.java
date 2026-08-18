package com.grupo1.turnera.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

import com.grupo1.turnera.model.enums.DiaSemana;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaSemana diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Builder.Default
    @Column(nullable = false)
    private Integer duracionTurnoMinutos = 15; // Fijo en 15 minutos por regla
}