package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    // Hereda los métodos CRUD estándar de JpaRepository (findById, save, etc.)
}