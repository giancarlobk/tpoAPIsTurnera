package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.doctor.DoctorSummaryResponse;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public List<DoctorSummaryResponse> buscar(Long especialidadId, String nombre) {
        String nombreNormalizado = nombre == null ? null : nombre.trim();

        return doctorRepository.buscar(especialidadId, nombreNormalizado)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private DoctorSummaryResponse toSummary(Doctor doctor) {
        return new DoctorSummaryResponse(
                doctor.getId(),
                doctor.getNombre(),
                doctor.getApellido(),
                doctor.getMatriculaNacional(),
                doctor.getEspecialidad().getId(),
                doctor.getEspecialidad().getNombre()
        );
    }
}
