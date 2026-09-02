package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.EstadoTurno;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    // Hereda los métodos CRUD estándar de JpaRepository (findById, save, etc.)
    // Obtener todos los turnos por estado
    List<Turno> findByEstado(EstadoTurno estado);
    // Obtener turnos por estado y doctor
    List<Turno> findByEstadoAndDoctorId(EstadoTurno estado, Long doctorId);

}