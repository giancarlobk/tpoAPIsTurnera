package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("""
            SELECT d
            FROM Doctor d
            JOIN FETCH d.especialidad e
            WHERE d.activo = true
              AND (:especialidadId IS NULL OR e.id = :especialidadId)
              AND (:nombre IS NULL OR LOWER(d.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
            ORDER BY d.apellido ASC, d.nombre ASC, d.id ASC
            """)
    List<Doctor> buscar(
            @Param("especialidadId") Long especialidadId,
            @Param("nombre") String nombre
    );
}
