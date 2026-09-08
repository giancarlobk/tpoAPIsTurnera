package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.turno.ReservaTurnoRequest;
import com.grupo1.turnera.dto.turno.TurnoResponse;
import com.grupo1.turnera.dto.turno.UsuarioReferencia;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.exception.TurnoFueraDeHorarioException;
import com.grupo1.turnera.exception.TurnoNoDisponibleException;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.DiaSemana;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import com.grupo1.turnera.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private TurnoService turnoService;

    @Test
    void deberiaReservarUnTurnoValidoYRegistrarElHistorialInicial() {
        Doctor doctor = doctorConHorarioLunes9a12();
        Paciente paciente = paciente(2L);
        LocalDateTime inicio = proximoLunesA(LocalTime.of(9, 0));

        when(doctorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(doctor));
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente));
        when(turnoRepository.existsSolapamiento(1L, inicio, inicio.plusMinutes(30))).thenReturn(false);
        when(turnoRepository.saveAndFlush(any(Turno.class))).thenAnswer(invocation -> {
            Turno turno = invocation.getArgument(0);
            turno.setId(100L);
            return turno;
        });

        TurnoResponse response = turnoService.reservarTurno(reserva(inicio), paciente);

        ArgumentCaptor<Turno> captor = ArgumentCaptor.forClass(Turno.class);
        verify(turnoRepository).saveAndFlush(captor.capture());
        Turno turnoGuardado = captor.getValue();

        assertThat(turnoGuardado.getEstado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(turnoGuardado.getFechaHoraInicio()).isEqualTo(inicio);
        assertThat(turnoGuardado.getFechaHoraFin()).isEqualTo(inicio.plusMinutes(30));
        assertThat(turnoGuardado.getHistorialEstados()).hasSize(1);
        assertThat(turnoGuardado.getHistorialEstados().get(0).getEstadoAnterior()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(turnoGuardado.getHistorialEstados().get(0).getEstadoNuevo()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(turnoGuardado.getHistorialEstados().get(0).getUsuarioIdModificador()).isEqualTo(2L);
        assertThat(turnoGuardado.getHistorialEstados().get(0).getRolUsuarioModificador()).isEqualTo(Rol.PACIENTE);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(response.doctor().id()).isEqualTo(1L);
        assertThat(response.paciente().id()).isEqualTo(2L);
    }

    @Test
    void deberiaLanzar404SiElDoctorNoExiste() {
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente(2L)));
        when(doctorRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        Paciente paciente = paciente(2L);
        assertThatThrownBy(() -> turnoService.reservarTurno(
                reserva(proximoLunesA(LocalTime.of(9, 0))), paciente))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(turnoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deberiaLanzar404SiElPacienteNoExiste() {
        when(pacienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnoService.reservarTurno(
                reserva(proximoLunesA(LocalTime.of(9, 0))), paciente(2L)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(turnoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deberiaRechazarUnHorarioFueraDeLaAtencionDelMedico() {
        when(doctorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(doctorConHorarioLunes9a12()));
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente(2L)));

        // El doctor solo atiende lunes 9 a 12; pedimos un lunes a las 20hs.
        LocalDateTime fueraDeHorario = proximoLunesA(LocalTime.of(20, 0));

        assertThatThrownBy(() -> turnoService.reservarTurno(reserva(fueraDeHorario), paciente(2L)))
                .isInstanceOf(TurnoFueraDeHorarioException.class);

        verify(turnoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deberiaRechazarUnTurnoSolapadoConOtroExistente() {
        Doctor doctor = doctorConHorarioLunes9a12();
        LocalDateTime inicio = proximoLunesA(LocalTime.of(9, 0));

        when(doctorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(doctor));
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente(2L)));
        when(turnoRepository.existsSolapamiento(1L, inicio, inicio.plusMinutes(30))).thenReturn(true);

        assertThatThrownBy(() -> turnoService.reservarTurno(reserva(inicio), paciente(2L)))
                .isInstanceOf(TurnoNoDisponibleException.class);

        verify(turnoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deberiaTraducirUnaViolacionDeConstraintDeBaseEn409() {
        // Simula el caso de concurrencia real: dos requests pasan el chequeo de solapamiento
        // casi al mismo tiempo, y el segundo insert falla por la constraint UNIQUE de la tabla.
        Doctor doctor = doctorConHorarioLunes9a12();
        LocalDateTime inicio = proximoLunesA(LocalTime.of(9, 0));

        when(doctorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(doctor));
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente(2L)));
        when(turnoRepository.existsSolapamiento(1L, inicio, inicio.plusMinutes(30))).thenReturn(false);
        when(turnoRepository.saveAndFlush(any(Turno.class)))
                .thenThrow(new DataIntegrityViolationException("uk_turno_doctor_inicio"));

        assertThatThrownBy(() -> turnoService.reservarTurno(reserva(inicio), paciente(2L)))
                .isInstanceOf(TurnoNoDisponibleException.class);
    }

    private Doctor doctorConHorarioLunes9a12() {
        Doctor doctor = Doctor.builder()
                .id(1L)
                .nombre("Juan")
                .apellido("Gomez")
                .matriculaNacional("MN-999")
                .rol(Rol.MEDICO)
                .activo(true)
                .build();
        HorarioAtencion horario = HorarioAtencion.builder()
                .doctor(doctor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(12, 0))
                .duracionTurnoMinutos(30)
                .build();
        doctor.setHorariosAtencion(List.of(horario));
        return doctor;
    }

    private Paciente paciente(Long id) {
        return Paciente.builder()
                .id(id)
                .nombre("Ana")
                .apellido("Perez")
                .fechaNacimiento(LocalDate.of(1995, 4, 18))
                .rol(Rol.PACIENTE)
                .activo(true)
                .build();
    }

    private ReservaTurnoRequest reserva(LocalDateTime inicio) {
        return new ReservaTurnoRequest(new UsuarioReferencia(1L), inicio);
    }

    private LocalDateTime proximoLunesA(LocalTime hora) {
        LocalDate proximoLunes = LocalDate.now().with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
        return LocalDateTime.of(proximoLunes, hora);
    }
}
