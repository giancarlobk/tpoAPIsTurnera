package com.grupo1.turnera.service;

import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.HorarioCreateRequest;
import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.HorarioResponse;
import com.grupo1.turnera.exception.DoctorNotFoundException;
import com.grupo1.turnera.exception.HorarioInvalidoException;
import com.grupo1.turnera.exception.HorarioOverlapException;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.HorarioAtencionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioAtencionService {

    private static final int DURACION_TURNO_MINUTOS = 15;

    private final DoctorRepository doctorRepository;
    private final HorarioAtencionRepository horarioAtencionRepository;

    @Transactional
    public HorarioResponse crearHorario(
            Long doctorId,
            HorarioCreateRequest request
    ) {

        validarHorario(request);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));

        List<HorarioAtencion> horariosExistentes =
                horarioAtencionRepository.findByDoctorAndDiaForUpdate(
                        doctorId,
                        request.diaSemana()
                );

        verificarSuperposicion(request, horariosExistentes);

        HorarioAtencion horario = HorarioAtencion.builder()
                .doctor(doctor)
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .duracionTurnoMinutos(request.duracionTurnoMinutos())
                .build();

        HorarioAtencion horarioGuardado =
                horarioAtencionRepository.save(horario);

        return convertirAResponse(horarioGuardado);
    }

    private void validarHorario(HorarioCreateRequest request) {

        if (request == null) {
            throw new HorarioInvalidoException("Los datos del horario son obligatorios");
        }

        if (request.diaSemana() == null) {
            throw new HorarioInvalidoException("El día de la semana es obligatorio");
        }

        if (request.horaInicio() == null) {
            throw new HorarioInvalidoException(
                    "La hora de inicio es obligatoria"
            );
        }

        if (request.horaFin() == null) {
            throw new HorarioInvalidoException(
                    "La hora de fin es obligatoria"
            );
        }

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new HorarioInvalidoException(
                    "La hora de fin debe ser posterior a la hora de inicio"
            );
        }

        if (request.duracionTurnoMinutos() == null) {
            throw new HorarioInvalidoException("La duración del turno es obligatoria");
        }

        if (request.duracionTurnoMinutos() != DURACION_TURNO_MINUTOS) {
            throw new HorarioInvalidoException("La duración del turno debe ser de " + DURACION_TURNO_MINUTOS + " minutos");
        }
    }

    private void verificarSuperposicion(
            HorarioCreateRequest request,
            List<HorarioAtencion> horariosExistentes
    ) {

        boolean existeSuperposicion = horariosExistentes.stream()
                .anyMatch(horario ->
                        request.horaInicio().isBefore(horario.getHoraFin())
                                && request.horaFin().isAfter(horario.getHoraInicio())
                );

        if (existeSuperposicion) {
            throw new HorarioOverlapException();
        }
    }

    private HorarioResponse convertirAResponse(
            HorarioAtencion horario
    ) {

        return new HorarioResponse(
                horario.getId(),
                horario.getDoctor().getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getDuracionTurnoMinutos()
        );
    }
}
