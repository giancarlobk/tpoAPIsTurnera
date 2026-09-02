package com.grupo1.turnera.model;

import com.grupo1.turnera.model.enums.Rol;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20, unique = true)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}