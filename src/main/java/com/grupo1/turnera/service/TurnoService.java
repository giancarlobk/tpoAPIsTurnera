package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.turno.*;
import com.grupo1.turnera.model.*;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoService {
    private final TurnoRepository turnoRepository;
    private final DoctorRepository doctorRepository;
    private final PacienteRepository pacienteRepository;

    @Transactional
    public TurnoResponse reservarTurno(ReservaTurnoRequest request, BaseUsuario actor) {
        exigirRol(actor, Rol.PACIENTE);
        Paciente paciente = pacienteRepository.findById(actor.getId())
                .filter(p -> p.isEnabled() && p.getRol() == Rol.PACIENTE)
                .orElseThrow(() -> new AccessDeniedException("Paciente no habilitado"));
        Doctor doctor = doctorActivo(request.doctor().id());
        Turno turno = nuevoTurno(doctor, paciente, request.fechaHoraInicio(), request.fechaHoraFin());
        return TurnoResponse.fromEntity(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoResponse crearSobreturno(SobreturnoRequest request, BaseUsuario actor) {
        exigirRol(actor, Rol.MEDICO, Rol.ADMIN);
        Long doctorId;
        if (actor.getRol() == Rol.MEDICO) {
            if (request.doctor() != null && !actor.getId().equals(request.doctor().id())) {
                throw new AccessDeniedException("Solo puede crear sobreturnos en su propia agenda");
            }
            doctorId = actor.getId();
        } else {
            if (request.doctor() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El administrador debe indicar el doctor");
            }
            doctorId = request.doctor().id();
        }
        Doctor doctor = doctorActivo(doctorId);
        Paciente paciente = pacienteRepository.findById(request.paciente().id())
                .filter(p -> p.isEnabled() && p.getRol() == Rol.PACIENTE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente activo no encontrado"));
        Turno turno = nuevoTurno(doctor, paciente, request.fechaHoraInicio(), request.fechaHoraFin());
        turno.setEsSobreturned(true);
        turno.setJustificacionSobreturned(request.justificacionSobreturned().trim());
        return TurnoResponse.fromEntity(turnoRepository.save(turno));
    }

    @Transactional(readOnly = true)
    public List<TurnoDisponibleResponse> obtenerDisponibles(Long doctorId) {
        List<Turno> turnos = doctorId == null
                ? turnoRepository.findByEstado(EstadoTurno.DISPONIBLE)
                : turnoRepository.findByEstadoAndDoctorId(EstadoTurno.DISPONIBLE, doctorId);
        return turnos.stream().map(TurnoDisponibleResponse::fromEntity).toList();
    }

    private Doctor doctorActivo(Long id) {
        return doctorRepository.findById(id).filter(d -> d.isEnabled() && d.getRol() == Rol.MEDICO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico activo no encontrado"));
    }

    private Turno nuevoTurno(Doctor doctor, Paciente paciente, LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de fin debe ser posterior al inicio");
        }
        return Turno.builder().doctor(doctor).paciente(paciente).fechaHoraInicio(inicio).fechaHoraFin(fin)
                .estado(EstadoTurno.RESERVADO).esSobreturned(false).build();
    }

    private void exigirRol(BaseUsuario actor, Rol... permitidos) {
        if (actor == null || !actor.isEnabled() || !List.of(permitidos).contains(actor.getRol())) {
            throw new AccessDeniedException("No tiene permisos para esta operación");
        }
    }
}
