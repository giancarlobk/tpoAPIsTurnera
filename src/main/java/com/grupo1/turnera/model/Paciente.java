package com.grupo1.turnera.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Paciente extends BaseUsuario {

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 100)
    private String obraSocial;

    @Column(length = 50)
    private String numeroAfiliado;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HistoriaClinica> historiasClinicas = new ArrayList<>();
}
