package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.turno.*;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.exception.TurnoFueraDeHorarioException;
import com.grupo1.turnera.exception.TurnoNoDisponibleException;
import com.grupo1.turnera.model.*;
import com.grupo1.turnera.model.enums.DiaSemana;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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
        Doctor doctor = doctorRepository.findByIdForUpdate(request.doctor().id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico", request.doctor().id()));
        if (!doctor.isEnabled() || doctor.getRol() != Rol.MEDICO) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico activo no encontrado");
        }

        LocalDateTime inicio = request.fechaHoraInicio();
        HorarioAtencion horario = buscarHorario(doctor, inicio)
                .orElseThrow(() -> new TurnoFueraDeHorarioException(doctor.getId(), inicio.toString()));
        LocalDateTime fin = inicio.plusMinutes(horario.getDuracionTurnoMinutos());
        if (fin.toLocalTime().isAfter(horario.getHoraFin())) {
            throw new TurnoFueraDeHorarioException(doctor.getId(), inicio.toString());
        }
        if (turnoRepository.existsSolapamiento(doctor.getId(), inicio, fin)) {
            throw new TurnoNoDisponibleException(doctor.getId(), inicio);
        }

        Turno turno = nuevoTurno(doctor, paciente, inicio, fin);
        turno.getHistorialEstados().add(HistorialEstadoTurno.builder()
                .turno(turno).estadoAnterior(EstadoTurno.RESERVADO).estadoNuevo(EstadoTurno.RESERVADO)
                .fechaCambio(LocalDateTime.now()).usuarioIdModificador(paciente.getId())
                .rolUsuarioModificador(Rol.PACIENTE).build());
        try {
            return TurnoResponse.fromEntity(turnoRepository.saveAndFlush(turno));
        } catch (DataIntegrityViolationException exception) {
            throw new TurnoNoDisponibleException(doctor.getId(), inicio);
        }
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
        if (!request.fechaHoraInicio().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha y hora de inicio deben ser futuras");
        }
        if (!request.fechaHoraFin().isAfter(request.fechaHoraInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de fin debe ser posterior al inicio");
        }
        String justificacion = request.justificacionSobreturned().trim();
        if (justificacion.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La justificación es obligatoria");
        }
        Turno turno = nuevoTurno(doctor, paciente, request.fechaHoraInicio(), request.fechaHoraFin());
        turno.setEsSobreturned(true);
        turno.setJustificacionSobreturned(justificacion);
        turno.getHistorialEstados().add(HistorialEstadoTurno.builder()
                .turno(turno).estadoAnterior(EstadoTurno.RESERVADO).estadoNuevo(EstadoTurno.RESERVADO)
                .fechaCambio(LocalDateTime.now()).usuarioIdModificador(actor.getId())
                .rolUsuarioModificador(actor.getRol()).motivo(justificacion).build());
        return TurnoResponse.fromEntity(turnoRepository.saveAndFlush(turno));
    }

    @Transactional(readOnly = true)
    public List<TurnoDisponibleResponse> obtenerDisponibles(Long doctorId) {
        List<Turno> turnos = doctorId == null
                ? turnoRepository.findByEstado(EstadoTurno.DISPONIBLE)
                : turnoRepository.findByEstadoAndDoctorId(EstadoTurno.DISPONIBLE, doctorId);
        return turnos.stream().map(TurnoDisponibleResponse::fromEntity).toList();
    }

    private Optional<HorarioAtencion> buscarHorario(Doctor doctor, LocalDateTime inicio) {
        DiaSemana dia = convertirDia(inicio.getDayOfWeek());
        LocalTime hora = inicio.toLocalTime();
        return doctor.getHorariosAtencion().stream()
                .filter(horario -> horario.getDiaSemana() == dia)
                .filter(horario -> !hora.isBefore(horario.getHoraInicio()))
                .filter(horario -> horario.getDuracionTurnoMinutos() != null)
                .findFirst();
    }

    private DiaSemana convertirDia(java.time.DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
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
