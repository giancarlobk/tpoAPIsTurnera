package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

	boolean existsByNombreIgnoreCase(String nombre);
}
