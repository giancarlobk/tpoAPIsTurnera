package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// getAll, getById, add y delete vienen de JpaRepository: findAll, findById, save y delete.
@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    Optional<Carrito> findByPacienteId(Long pacienteId);
}
