package com.grupo1.turnera.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emails_registrados")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailRegistro {
    @Id
    @Column(nullable = false, length = 150)
    private String email;
}
