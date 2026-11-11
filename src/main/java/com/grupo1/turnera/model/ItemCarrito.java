package com.grupo1.turnera.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Un turno guardado en el carrito: médico y horario.
 * No ocupa el horario: la disponibilidad se valida al agregarlo y se vuelve a validar al reservar.
 */
@Entity
@Table(
        name = "items_carrito",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_item_carrito_doctor_inicio",
                columnNames = {"carrito_id", "doctor_id", "fecha_hora_inicio"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(nullable = false)
    private LocalDateTime fechaAgregado;
}
