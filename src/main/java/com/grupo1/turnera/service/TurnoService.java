package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.turno.TurnoReservaRequest;
import com.grupo1.turnera.dto.turno.TurnoResponse;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.exception.TurnoFueraDeHorarioException;
import com.grupo1.turnera.exception.TurnoNoDisponibleException;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.HistorialEstadoTurno;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.DiaSemana;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import com.grupo1.turnera.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final DoctorRepository doctorRepository;
    private final PacienteRepository pacienteRepository;

    @Transactional
    public TurnoResponse reservarTurno(TurnoReservaRequest request) {
        Doctor doctor = doctorRepository.findByIdForUpdate(request.doctorId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Médico", request.doctorId()));
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Paciente", request.pacienteId()));

        LocalDateTime inicio = request.fechaHoraInicio();
        HorarioAtencion horario = buscarHorario(doctor, inicio)
                .orElseThrow(() -> new TurnoFueraDeHorarioException(request.doctorId(), inicio.toString()));
        LocalDateTime fin = inicio.plusMinutes(horario.getDuracionTurnoMinutos());

        if (fin.toLocalTime().isAfter(horario.getHoraFin())) {
            throw new TurnoFueraDeHorarioException(request.doctorId(), inicio.toString());
        }

        if (turnoRepository.existsSolapamiento(request.doctorId(), inicio, fin)) {
            throw new TurnoNoDisponibleException(request.doctorId(), inicio);
        }

        Turno turno = Turno.builder()
                .doctor(doctor)
                .paciente(paciente)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(fin)
                .estado(EstadoTurno.RESERVADO)
                .esSobreturned(false)
                .build();

        HistorialEstadoTurno historial = HistorialEstadoTurno.builder()
                .turno(turno)
                .estadoAnterior(EstadoTurno.RESERVADO)
                .estadoNuevo(EstadoTurno.RESERVADO)
                .fechaCambio(LocalDateTime.now())
                .usuarioIdModificador(paciente.getId())
                .rolUsuarioModificador(Rol.PACIENTE)
                .build();
        turno.getHistorialEstados().add(historial);

        try {
            return TurnoResponse.fromEntity(turnoRepository.saveAndFlush(turno));
        } catch (DataIntegrityViolationException exception) {
            throw new TurnoNoDisponibleException(request.doctorId(), inicio);
        }
    }

    private Optional<HorarioAtencion> buscarHorario(Doctor doctor, LocalDateTime inicio) {
        DiaSemana dia = DiaSemana.valueOf(inicio.getDayOfWeek().name());
        LocalTime hora = inicio.toLocalTime();
        return doctor.getHorariosAtencion().stream()
                .filter(horario -> horario.getDiaSemana() == dia)
                .filter(horario -> !hora.isBefore(horario.getHoraInicio()))
                .filter(horario -> horario.getDuracionTurnoMinutos() != null)
                .findFirst();
    }

    public Turno crearSobreturno(Turno sobreturno) {
        return turnoRepository.save(sobreturno);
    }
}
