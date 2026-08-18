package com.grupo1.turnera.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "administradores")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Administrador extends BaseUsuario {
    // Hereda los atributos básicos para administradores del sistema
}
