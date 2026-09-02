package com.grupo1.turnera.service;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;

    public Turno reservarTurno(Turno turno) {
        return turnoRepository.save(turno);
    }

    public Turno crearSobreturno(Turno sobreturno) {
        return turnoRepository.save(sobreturno);
    }

    public List<Turno> obtenerDisponibles(Long doctorId) {
        if (doctorId != null) {
            return turnoRepository.findByEstadoAndDoctorId(EstadoTurno.DISPONIBLE, doctorId);
        }else{
            return turnoRepository.findByEstado(EstadoTurno.DISPONIBLE);
        }
    }
}
