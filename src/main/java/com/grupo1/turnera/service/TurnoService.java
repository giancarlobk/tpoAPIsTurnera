package com.grupo1.turnera.service;

import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.BaseUsuario;
import com.grupo1.turnera.model.HistorialEstadoTurno;
import com.grupo1.turnera.dto.turno.CancelacionTurnoRequest;
import com.grupo1.turnera.dto.turno.TurnoResponse;
import com.grupo1.turnera.exception.CancelacionNoPermitidaException;
import com.grupo1.turnera.exception.TransicionTurnoInvalidaException;
import com.grupo1.turnera.exception.TurnoNoEncontradoException;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import com.grupo1.turnera.repository.HistorialEstadoTurnoRepository;
import com.grupo1.turnera.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final HistorialEstadoTurnoRepository historialEstadoTurnoRepository;
    private final PacienteRepository pacienteRepository;
    private final DoctorRepository doctorRepository;

    @Transactional
    public TurnoResponse cancelarTurno(Long id, CancelacionTurnoRequest request) {
        Turno turno = turnoRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new TurnoNoEncontradoException(id));
        validarActor(turno, request);

        EstadoTurno anterior = turno.getEstado();
        if (anterior != EstadoTurno.RESERVADO && anterior != EstadoTurno.CONFIRMADO) {
            throw new TransicionTurnoInvalidaException(anterior);
        }
        EstadoTurno nuevo = request.rol() == Rol.PACIENTE
                ? EstadoTurno.CANCELADO_PACIENTE : EstadoTurno.CANCELADO_MEDICO;
        turno.setEstado(nuevo);
        turnoRepository.saveAndFlush(turno);
        historialEstadoTurnoRepository.saveAndFlush(HistorialEstadoTurno.builder()
                .turno(turno)
                .estadoAnterior(anterior)
                .estadoNuevo(nuevo)
                .fechaCambio(LocalDateTime.now())
                .usuarioIdModificador(request.usuarioId())
                .rolUsuarioModificador(request.rol())
                .motivo(request.motivo().trim())
                .build());
        return TurnoResponse.from(turno);
    }

    private void validarActor(Turno turno, CancelacionTurnoRequest request) {
        BaseUsuario actor;
        if (request.rol() == Rol.PACIENTE) {
            actor = pacienteRepository.findById(request.usuarioId())
                    .orElseThrow(CancelacionNoPermitidaException::new);
            if (turno.getPaciente() == null || !actor.getId().equals(turno.getPaciente().getId())) {
                throw new CancelacionNoPermitidaException();
            }
        } else if (request.rol() == Rol.MEDICO) {
            actor = doctorRepository.findById(request.usuarioId())
                    .orElseThrow(CancelacionNoPermitidaException::new);
            if (!actor.getId().equals(turno.getDoctor().getId())) {
                throw new CancelacionNoPermitidaException();
            }
        } else {
            throw new CancelacionNoPermitidaException();
        }
        if (!Boolean.TRUE.equals(actor.getActivo()) || actor.getRol() != request.rol()) {
            throw new CancelacionNoPermitidaException();
        }
    }

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
