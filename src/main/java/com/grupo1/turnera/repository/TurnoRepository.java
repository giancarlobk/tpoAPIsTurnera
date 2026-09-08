package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.EstadoTurno;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
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

    @Query("SELECT t FROM Turno t WHERE " +
           "(:pacienteId IS NULL OR t.paciente.id = :pacienteId) AND " +
           "(:doctorId IS NULL OR t.doctor.id = :doctorId) AND " +
           "(:estado IS NULL OR t.estado = :estado) AND " +
           "(:fechaDesde IS NULL OR t.fechaHoraInicio >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR t.fechaHoraInicio <= :fechaHasta)")
    Page<Turno> buscarConFiltros(
            @Param("pacienteId") Long pacienteId,
            @Param("doctorId") Long doctorId,
            @Param("estado") EstadoTurno estado,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);
}