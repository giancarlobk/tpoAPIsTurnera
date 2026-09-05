package com.grupo1.turnera.service;

import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.HorarioCreateRequest;
import com.grupo1.turnera.exception.DoctorNotFoundException;
import com.grupo1.turnera.exception.HorarioInvalidoException;
import com.grupo1.turnera.exception.HorarioOverlapException;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.enums.DiaSemana;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.HorarioAtencionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioAtencionServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private HorarioAtencionRepository horarioAtencionRepository;

    private HorarioAtencionService horarioAtencionService;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        horarioAtencionService =
                new HorarioAtencionService(
                        doctorRepository,
                        horarioAtencionRepository
                );

        doctor = Doctor.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Pérez")
                .build();
    }

    @Test
    void deberiaCrearHorarioValido() {

        HorarioCreateRequest request =
                new HorarioCreateRequest(
                        DiaSemana.LUNES,
                        LocalTime.of(9, 0),
                        LocalTime.of(13, 0),
                        15
                );

        when(doctorRepository.findById(1L))
                .thenReturn(Optional.of(doctor));

        when(horarioAtencionRepository.findByDoctorAndDiaForUpdate(
                1L,
                DiaSemana.LUNES
        )).thenReturn(List.of());

        when(horarioAtencionRepository.save(any(HorarioAtencion.class)))
                .thenAnswer(invocation -> {
                    HorarioAtencion horario = invocation.getArgument(0);
                    horario.setId(10L);
                    return horario;
                });

        var response =
                horarioAtencionService.crearHorario(1L, request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.doctorId());
        assertEquals(DiaSemana.LUNES, response.diaSemana());
        assertEquals(LocalTime.of(9, 0), response.horaInicio());
        assertEquals(LocalTime.of(13, 0), response.horaFin());
        assertEquals(15, response.duracionTurnoMinutos());

        verify(horarioAtencionRepository).save(
                any(HorarioAtencion.class)
        );
    }

    @Test
    void deberiaRechazarFinMenorQueInicio() {

        HorarioCreateRequest request =
                new HorarioCreateRequest(
                        DiaSemana.LUNES,
                        LocalTime.of(10, 0),
                        LocalTime.of(9, 0),
                        15
                );

        assertThrows(
                HorarioInvalidoException.class,
                () -> horarioAtencionService.crearHorario(1L, request)
        );

        verifyNoInteractions(doctorRepository);
        verifyNoInteractions(horarioAtencionRepository);
    }

    @Test
    void deberiaRechazarFinIgualAInicio() {

        HorarioCreateRequest request =
                new HorarioCreateRequest(
                        DiaSemana.LUNES,
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 0),
                        15
                );

        assertThrows(
                HorarioInvalidoException.class,
                () -> horarioAtencionService.crearHorario(1L, request)
        );

        verifyNoInteractions(doctorRepository);
        verifyNoInteractions(horarioAtencionRepository);
    }

    @Test
    void deberiaRechazarDuracionDistintaDeQuinceMinutos() {

        HorarioCreateRequest request =
                new HorarioCreateRequest(
                        DiaSemana.LUNES,
                        LocalTime.of(9, 0),
                        LocalTime.of(13, 0),
                        30
                );

        assertThrows(
                HorarioInvalidoException.class,
                () -> horarioAtencionService.crearHorario(1L, request)
        );

        verifyNoInteractions(doctorRepository);
        verifyNoInteractions(horarioAtencionRepository);
    }

    @Test
    void deberiaLanzar404LogicoSiDoctorNoExiste() {

        HorarioCreateRequest request =
                new HorarioCreateRequest(
                        DiaSemana.LUNES,
                        LocalTime.of(9, 0),
                        LocalTime.of(13, 0),
                        15
                );

        when(doctorRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                DoctorNotFoundException.class,
                () -> horarioAtencionService.crearHorario(999L, request)
        );

        verify(
                horarioAtencionRepository,
                never()
        ).save(any());
    }

    @Test
    void deberiaRechazarHorarioSuperpuesto() {

        HorarioCreateRequest request =
                new HorarioCreateRequest(
                        DiaSemana.LUNES,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        15
                );

        HorarioAtencion existente =
                HorarioAtencion.builder()
                        .doctor(doctor)
                        .diaSemana(DiaSemana.LUNES)
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .duracionTurnoMinutos(15)
                        .build();

        when(doctorRepository.findById(1L))
                .thenReturn(Optional.of(doctor));

        when(horarioAtencionRepository.findByDoctorAndDiaForUpdate(
                1L,
                DiaSemana.LUNES
        )).thenReturn(List.of(existente));

        assertThrows(
                HorarioOverlapException.class,
                () -> horarioAtencionService.crearHorario(1L, request)
        );

        verify(
                horarioAtencionRepository,
                never()
        ).save(any());
    }

    @Test
    void deberiaPermitirHorarioConsecutivo() {

        HorarioCreateRequest request =
                new HorarioCreateRequest(
                        DiaSemana.LUNES,
                        LocalTime.of(11, 0),
                        LocalTime.of(13, 0),
                        15
                );

        HorarioAtencion existente =
                HorarioAtencion.builder()
                        .doctor(doctor)
                        .diaSemana(DiaSemana.LUNES)
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .duracionTurnoMinutos(15)
                        .build();

        when(doctorRepository.findById(1L))
                .thenReturn(Optional.of(doctor));

        when(horarioAtencionRepository.findByDoctorAndDiaForUpdate(
                1L,
                DiaSemana.LUNES
        )).thenReturn(List.of(existente));

        when(horarioAtencionRepository.save(any(HorarioAtencion.class)))
                .thenAnswer(invocation -> {
                    HorarioAtencion horario = invocation.getArgument(0);
                    horario.setId(20L);
                    return horario;
                });

        assertDoesNotThrow(
                () -> horarioAtencionService.crearHorario(
                        1L,
                        request
                )
        );
    }
}
