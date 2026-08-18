package com.grupo1.turnera.service;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
