package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.turno.CambioEstadoTurnoRequest;
import com.grupo1.turnera.exception.TransicionEstadoTurnoInvalidaException;
import com.grupo1.turnera.model.*;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TurnoServiceTemporalTest {

    private TurnoRepository turnoRepository;
    private TurnoService turnoService;

    private BaseUsuario actor;
    private Turno turno;

    private static final ZoneId ZONA =
            ZoneId.of("America/Argentina/Buenos_Aires");

    private static final LocalDateTime INICIO =
            LocalDateTime.of(2026, 11, 10, 10, 0);

    private static final LocalDateTime FIN =
            INICIO.plusMinutes(30);

    @BeforeEach
    void preparar() {

        turnoRepository = mock(TurnoRepository.class);

        DoctorRepository doctorRepository =
                mock(DoctorRepository.class);

        PacienteRepository pacienteRepository =
                mock(PacienteRepository.class);

        // ADMIN simplifica las pruebas de reglas temporales.
        // Las validaciones de otros roles siguen existiendo.
        actor = mock(BaseUsuario.class);

        when(actor.isEnabled()).thenReturn(true);
        when(actor.getRol()).thenReturn(Rol.ADMIN);
        when(actor.getId()).thenReturn(1L);

        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(10L);

        turno = Turno.builder()
                .id(100L)
                .doctor(doctor)
                .fechaHoraInicio(INICIO)
                .fechaHoraFin(FIN)
                .estado(EstadoTurno.CONFIRMADO)
                .ocupacionActiva(true)
                .build();

        when(turnoRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(turno));
    }

    private void configurarReloj(LocalDateTime momento) {

        Clock clock = Clock.fixed(
                momento.atZone(ZONA).toInstant(),
                ZONA
        );

        turnoService = new TurnoService(
                turnoRepository,
                mock(DoctorRepository.class),
                mock(PacienteRepository.class),
                clock
        );
    }

    private CambioEstadoTurnoRequest solicitud(
            EstadoTurno destino) {

        return new CambioEstadoTurnoRequest(
                destino,
                "Motivo de prueba"
        );
    }

    private void verificarRechazo(EstadoTurno destino) {

        EstadoTurno estadoOriginal = turno.getEstado();
        int historialOriginal =
                turno.getHistorialEstados().size();

        assertThrows(
                TransicionEstadoTurnoInvalidaException.class,
                () -> turnoService.cambiarEstado(
                        100L, solicitud(destino), actor)
        );

        // No debe cambiar el estado.
        assertEquals(estadoOriginal, turno.getEstado());

        // No debe agregarse historial.
        assertEquals(
                historialOriginal,
                turno.getHistorialEstados().size()
        );

        // No debe guardarse el turno.
        verify(turnoRepository, never())
                .saveAndFlush(any(Turno.class));
    }

    private void verificarExito(EstadoTurno destino) {

        when(turnoRepository.saveAndFlush(any(Turno.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int historialOriginal =
                turno.getHistorialEstados().size();

        var respuesta = turnoService.cambiarEstado(
                100L, solicitud(destino), actor);

        assertEquals(destino, respuesta.estado());
        assertEquals(destino, turno.getEstado());

        assertEquals(
                historialOriginal + 1,
                turno.getHistorialEstados().size()
        );

        verify(turnoRepository, times(1))
                .saveAndFlush(turno);
    }

    @Test
    void debeRechazarAusenteAntesDelInicio() {

        configurarReloj(INICIO.minusMinutes(5));

        verificarRechazo(EstadoTurno.AUSENTE);
    }

    @Test
    void debeRechazarAusenteDuranteElTurno() {

        configurarReloj(INICIO.plusMinutes(10));

        verificarRechazo(EstadoTurno.AUSENTE);
    }

    @Test
    void debePermitirAusenteAlFinalizar() {

        configurarReloj(FIN);

        verificarExito(EstadoTurno.AUSENTE);
    }

    @Test
    void debeRechazarCancelacionDeTurnoPasado() {

        configurarReloj(FIN.plusMinutes(1));

        verificarRechazo(
                EstadoTurno.CANCELADO_PACIENTE);
    }

    @Test
    void debeRechazarCancelacionDuranteElTurno() {

        configurarReloj(INICIO.plusMinutes(1));

        verificarRechazo(
                EstadoTurno.CANCELADO_MEDICO);
    }

    @Test
    void debePermitirCancelacionAntesDelInicio() {

        configurarReloj(INICIO.minusMinutes(1));

        verificarExito(
                EstadoTurno.CANCELADO_PACIENTE);

        assertNull(turno.getOcupacionActiva());
    }

    @Test
    void debeRechazarCancelacionEnInicioExacto() {

        configurarReloj(INICIO);

        verificarRechazo(
                EstadoTurno.CANCELADO_MEDICO);
    }

    @Test
    void debeRechazarAtendidoAntesDelFin() {

        configurarReloj(FIN.minusNanos(1));

        verificarRechazo(EstadoTurno.ATENDIDO);
    }

    @Test
    void debePermitirAtendidoAlFinalizar() {

        configurarReloj(FIN);

        verificarExito(EstadoTurno.ATENDIDO);
    }

    @Test
    void debePermitirConfirmarAntesDelInicio() {

        turno.setEstado(EstadoTurno.RESERVADO);

        configurarReloj(INICIO.minusMinutes(5));

        verificarExito(EstadoTurno.CONFIRMADO);
    }

    @Test
    void debeRechazarConfirmarAlInicio() {

        turno.setEstado(EstadoTurno.RESERVADO);

        configurarReloj(INICIO);

        verificarRechazo(EstadoTurno.CONFIRMADO);
    }

    @Test
    void debeRechazarTransicionDeEstadoInvalida() {

        turno.setEstado(EstadoTurno.RESERVADO);

        configurarReloj(FIN.plusMinutes(5));

        verificarRechazo(EstadoTurno.AUSENTE);
    }
}
