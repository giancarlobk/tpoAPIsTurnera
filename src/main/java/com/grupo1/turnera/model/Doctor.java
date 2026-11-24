package com.grupo1.turnera.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Doctor extends BaseUsuario {

    @Column(nullable = false, unique = true, length = 50)
    private String matriculaNacional;

    
    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HorarioAtencion> horariosAtencion = new ArrayList<>();
}
