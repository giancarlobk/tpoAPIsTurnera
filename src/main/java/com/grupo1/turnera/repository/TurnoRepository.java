package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.EstadoTurno;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    @Query("select distinct t from Turno t left join fetch t.historialEstados where t.id = :id")
    Optional<Turno> findByIdWithHistorial(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turno t where t.id = :id")
    Optional<Turno> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
            FROM Turno t
            WHERE t.doctor.id = :doctorId
              AND t.estado NOT IN (
                  com.grupo1.turnera.model.enums.EstadoTurno.CANCELADO_PACIENTE,
                  com.grupo1.turnera.model.enums.EstadoTurno.CANCELADO_MEDICO
              )
              AND t.fechaHoraInicio < :fechaHoraFin
              AND t.fechaHoraFin > :fechaHoraInicio
            """)
    boolean existsSolapamiento(
        @Param("doctorId") Long doctorId,
        @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
        @Param("fechaHoraFin") LocalDateTime fechaHoraFin
    );

    // Obtener todos los turnos por estado
    List<Turno> findByEstado(EstadoTurno estado);
    // Obtener turnos por estado y doctor
    List<Turno> findByEstadoAndDoctorId(EstadoTurno estado, Long doctorId);
}