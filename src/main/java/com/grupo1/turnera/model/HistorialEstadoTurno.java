package com.grupo1.turnera.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;

@Entity
@Table(name = "historial_estados_turno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEstadoTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTurno estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTurno estadoNuevo;

    @Column(nullable = false)
    private LocalDateTime fechaCambio;

    @Column(nullable = false)
    private Long usuarioIdModificador; // ID del usuario que realizó el cambio

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rolUsuarioModificador; // Rol del usuario (ADMIN, MEDICO, PACIENTE)

    @Column(columnDefinition = "TEXT")
    private String motivo;
}
