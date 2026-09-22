package com.grupo1.turnera.model;

import com.grupo1.turnera.model.enums.EstadoTurno;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "turnos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_turno_doctor_inicio_activo",
                columnNames = {"doctor_id","fecha_hora_inicio","es_sobreturned","ocupacion_activa"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTurno estado;

    @Builder.Default
    @Column(name = "es_sobreturned", nullable = false)
    private Boolean esSobreturned = false;

    /*
     * TRUE:
     * el turno continúa activo.
     *
     * NULL:
     * el turno fue cancelado y queda solamente
     * como registro histórico.
     *
     * Se utiliza NULL porque MySQL permite varias
     * filas NULL dentro de un índice UNIQUE.
     */
    @Builder.Default
    @Column(name = "ocupacion_activa")
    private Boolean ocupacionActiva = true;

    @Column(columnDefinition = "TEXT")
    private String justificacionSobreturned;

    @OneToMany(
            mappedBy = "turno",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<HistorialEstadoTurno> historialEstados =
            new ArrayList<>();

    @OneToOne(
            mappedBy = "turno",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private HistoriaClinica historiaClinica;
}

    @OneToOne(mappedBy = "turno", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private HistoriaClinica historiaClinica;
}
