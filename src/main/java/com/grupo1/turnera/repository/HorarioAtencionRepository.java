package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.enums.DiaSemana;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT h
            FROM HorarioAtencion h
            WHERE h.doctor.id = :doctorId
              AND h.diaSemana = :diaSemana
            """)
    List<HorarioAtencion> findByDoctorAndDiaForUpdate(
            @Param("doctorId") Long doctorId,
            @Param("diaSemana") DiaSemana diaSemana
    );
}
