package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.EstadoTurno;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turno t where t.id = :id")
    Optional<Turno> findByIdParaActualizar(@Param("id") Long id);

    // Hereda los métodos CRUD estándar de JpaRepository (findById, save, etc.)
    // Obtener todos los turnos por estado
    List<Turno> findByEstado(EstadoTurno estado);
    // Obtener turnos por estado y doctor
    List<Turno> findByEstadoAndDoctorId(EstadoTurno estado, Long doctorId);

}
