package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.admin.DoctorCreateRequest;
import com.grupo1.turnera.dto.admin.HorarioRequest;
import com.grupo1.turnera.dto.admin.HorarioResponse;
import com.grupo1.turnera.dto.doctor.DoctorSummaryResponse;
import com.grupo1.turnera.dto.especialidad.EspecialidadCreateRequest;
import com.grupo1.turnera.dto.especialidad.EspecialidadResponse;
import com.grupo1.turnera.exception.AgendaInvalidaException;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdministracionService {

    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;
    private final EspecialidadService especialidadService;

    // Las altas delegan en los mismos servicios que /api/doctores y /api/especialidades
    // para aplicar las mismas validaciones (email único entre usuarios, duplicados, contraseña).
    @Transactional
    public EspecialidadResponse crearEspecialidad(EspecialidadCreateRequest request) {
        return especialidadService.crear(request);
    }

    @Transactional
    public DoctorSummaryResponse crearDoctor(DoctorCreateRequest request) {
        return doctorService.registrarDesdeAdministracion(request);
    }

    @Transactional
    public List<HorarioResponse> reemplazarHorarios(Long doctorId, List<HorarioRequest> requests) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico", doctorId));
        validarHorarios(requests);
        doctor.getHorariosAtencion().clear();
        requests.stream()
                .map(request -> HorarioAtencion.builder()
                        .doctor(doctor)
                        .diaSemana(request.diaSemana())
                        .horaInicio(request.horaInicio())
                        .horaFin(request.horaFin())
                        .duracionTurnoMinutos(request.duracionTurnoMinutos())
                        .build())
                .forEach(doctor.getHorariosAtencion()::add);
        doctorRepository.save(doctor);
        return doctor.getHorariosAtencion().stream().map(HorarioResponse::fromEntity).toList();
    }

    private void validarHorarios(List<HorarioRequest> requests) {
        Map<Object, List<HorarioRequest>> porDia = new HashMap<>();
        for (HorarioRequest request : requests) {
            if (!request.horaInicio().isBefore(request.horaFin())) {
                throw new AgendaInvalidaException("La hora de inicio debe ser anterior a la hora de fin");
            }
            long minutos = Duration.between(request.horaInicio(), request.horaFin()).toMinutes();
            if (request.duracionTurnoMinutos() > minutos || minutos % request.duracionTurnoMinutos() != 0) {
                throw new AgendaInvalidaException("La duración debe dividir exactamente la franja de atención");
            }
            porDia.computeIfAbsent(request.diaSemana(), ignored -> new java.util.ArrayList<>()).add(request);
        }
        porDia.values().forEach(this::validarSolapamientos);
    }

    private void validarSolapamientos(List<HorarioRequest> horarios) {
        List<HorarioRequest> ordenados = horarios.stream()
            .sorted(Comparator.comparing(request -> request.horaInicio()))
                .toList();
        for (int index = 1; index < ordenados.size(); index++) {
            if (ordenados.get(index).horaInicio().isBefore(ordenados.get(index - 1).horaFin())) {
                throw new AgendaInvalidaException("Las franjas del mismo día no pueden solaparse");
            }
        }
    }
}