package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.turno.CancelacionTurnoRequest;
import com.grupo1.turnera.dto.turno.TurnoResponse;
import com.grupo1.turnera.exception.CancelacionNoPermitidaException;
import com.grupo1.turnera.exception.TransicionTurnoInvalidaException;
import com.grupo1.turnera.exception.TurnoNoEncontradoException;
import com.grupo1.turnera.model.*;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoCancelacionServiceTest {
    @Mock TurnoRepository turnos;
    @Mock HistorialEstadoTurnoRepository historiales;
    @Mock PacienteRepository pacientes;
    @Mock DoctorRepository doctores;
    @InjectMocks TurnoService service;
    private Turno turno;
    private Paciente paciente;
    private Doctor doctor;

    @BeforeEach
    void preparar() {
        paciente = Paciente.builder().id(10L).rol(Rol.PACIENTE).activo(true).build();
        doctor = Doctor.builder().id(20L).rol(Rol.MEDICO).activo(true).build();
        turno = Turno.builder().id(1L).paciente(paciente).doctor(doctor)
                .estado(EstadoTurno.RESERVADO).fechaHoraInicio(LocalDateTime.of(2026, 10, 1, 10, 0))
                .fechaHoraFin(LocalDateTime.of(2026, 10, 1, 10, 30)).build();
        when(turnos.findByIdParaActualizar(1L)).thenReturn(Optional.of(turno));
    }

    @ParameterizedTest
    @CsvSource({"PACIENTE,RESERVADO,CANCELADO_PACIENTE", "PACIENTE,CONFIRMADO,CANCELADO_PACIENTE",
            "MEDICO,RESERVADO,CANCELADO_MEDICO", "MEDICO,CONFIRMADO,CANCELADO_MEDICO"})
    void cancelaYRegistraTransicion(Rol rol, EstadoTurno anterior, EstadoTurno nuevo) {
        turno.setEstado(anterior);
        Long actorId = prepararActor(rol);
        LocalDateTime antes = LocalDateTime.now();
        TurnoResponse respuesta = service.cancelarTurno(1L,
                new CancelacionTurnoRequest(actorId, rol, "  No puedo asistir  "));
        assertThat(respuesta.estado()).isEqualTo(nuevo);
        assertThat(respuesta.doctor().id()).isEqualTo(20L);
        assertThat(respuesta.paciente().id()).isEqualTo(10L);
        assertThat(turno.getEstado()).isEqualTo(nuevo);
        ArgumentCaptor<HistorialEstadoTurno> captor = ArgumentCaptor.forClass(HistorialEstadoTurno.class);
        verify(historiales).saveAndFlush(captor.capture());
        HistorialEstadoTurno historial = captor.getValue();
        assertThat(historial.getTurno()).isSameAs(turno);
        assertThat(historial.getEstadoAnterior()).isEqualTo(anterior);
        assertThat(historial.getEstadoNuevo()).isEqualTo(nuevo);
        assertThat(historial.getUsuarioIdModificador()).isEqualTo(actorId);
        assertThat(historial.getRolUsuarioModificador()).isEqualTo(rol);
        assertThat(historial.getMotivo()).isEqualTo("No puedo asistir");
        assertThat(historial.getFechaCambio()).isBetween(antes, LocalDateTime.now());
        verify(turnos).saveAndFlush(turno);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"DISPONIBLE", "ATENDIDO", "AUSENTE",
            "CANCELADO_PACIENTE", "CANCELADO_MEDICO"})
    void rechazaEstadosNoCancelables(EstadoTurno estado) {
        turno.setEstado(estado);
        prepararActor(Rol.PACIENTE);
        assertThatThrownBy(() -> service.cancelarTurno(1L, request()))
                .isInstanceOf(TransicionTurnoInvalidaException.class);
        assertThat(turno.getEstado()).isEqualTo(estado);
        verificarSinEscrituras();
    }

    @Test
    void rechazaTurnoInexistente() {
        when(turnos.findByIdParaActualizar(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelarTurno(1L, request()))
                .isInstanceOf(TurnoNoEncontradoException.class);
        verifyNoInteractions(pacientes, doctores);
        verificarSinEscrituras();
    }

    @Test
    void rechazaAdministrador() {
        assertThatThrownBy(() -> service.cancelarTurno(1L,
                new CancelacionTurnoRequest(10L, Rol.ADMIN, "Motivo")))
                .isInstanceOf(CancelacionNoPermitidaException.class);
        verifyNoInteractions(pacientes, doctores);
        verificarSinEscrituras();
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"PACIENTE", "MEDICO"})
    void rechazaActorInexistente(Rol rol) {
        assertThatThrownBy(() -> service.cancelarTurno(1L,
                new CancelacionTurnoRequest(99L, rol, "Motivo")))
                .isInstanceOf(CancelacionNoPermitidaException.class);
        verificarSinEscrituras();
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"PACIENTE", "MEDICO"})
    void rechazaActorAjeno(Rol rol) {
        Long id = prepararActor(rol);
        if (rol == Rol.PACIENTE) turno.setPaciente(Paciente.builder().id(99L).build());
        else turno.setDoctor(Doctor.builder().id(99L).build());
        assertThatThrownBy(() -> service.cancelarTurno(1L,
                new CancelacionTurnoRequest(id, rol, "Motivo")))
                .isInstanceOf(CancelacionNoPermitidaException.class);
        verificarSinEscrituras();
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"PACIENTE", "MEDICO"})
    void rechazaActorInactivo(Rol rol) {
        Long id = prepararActor(rol);
        (rol == Rol.PACIENTE ? paciente : doctor).setActivo(false);
        assertThatThrownBy(() -> service.cancelarTurno(1L,
                new CancelacionTurnoRequest(id, rol, "Motivo")))
                .isInstanceOf(CancelacionNoPermitidaException.class);
        verificarSinEscrituras();
    }

    @Test
    void rechazaRolDistintoAlPersistido() {
        prepararActor(Rol.PACIENTE);
        paciente.setRol(Rol.ADMIN);
        assertThatThrownBy(() -> service.cancelarTurno(1L, request()))
                .isInstanceOf(CancelacionNoPermitidaException.class);
        verificarSinEscrituras();
    }

    @Test
    void rechazaPacienteSinReservaAsignada() {
        prepararActor(Rol.PACIENTE);
        turno.setPaciente(null);
        assertThatThrownBy(() -> service.cancelarTurno(1L, request()))
                .isInstanceOf(CancelacionNoPermitidaException.class);
        verificarSinEscrituras();
    }

    private Long prepararActor(Rol rol) {
        if (rol == Rol.PACIENTE) {
            when(pacientes.findById(10L)).thenReturn(Optional.of(paciente));
            return 10L;
        }
        when(doctores.findById(20L)).thenReturn(Optional.of(doctor));
        return 20L;
    }

    private CancelacionTurnoRequest request() {
        return new CancelacionTurnoRequest(10L, Rol.PACIENTE, "Motivo");
    }

    private void verificarSinEscrituras() {
        verify(turnos, never()).saveAndFlush(any());
        verifyNoInteractions(historiales);
    }
}
