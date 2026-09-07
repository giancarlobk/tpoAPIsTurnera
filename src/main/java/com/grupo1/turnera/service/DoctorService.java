package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.doctor.DoctorSummaryResponse;
import com.grupo1.turnera.dto.doctor.DoctorCreateRequest;
import com.grupo1.turnera.exception.DniDuplicadoException;
import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.exception.MatriculaDuplicadaException;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<DoctorSummaryResponse> buscar(Long especialidadId, String nombre) {
        String nombreNormalizado = nombre == null ? null : nombre.trim();

        return doctorRepository.buscar(especialidadId, nombreNormalizado)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public DoctorSummaryResponse registrar(DoctorCreateRequest request) {
        String dni = request.dni().trim();
        String email = request.email().trim();
        String matricula = request.matriculaNacional().trim();

        if (doctorRepository.findByDni(dni).isPresent()) {
            throw new DniDuplicadoException(dni);
        }
        if (doctorRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new EmailDuplicadoException(email);
        }
        if (doctorRepository.findByMatriculaNacional(matricula).isPresent()) {
            throw new MatriculaDuplicadaException(matricula);
        }

        Especialidad especialidad = especialidadRepository.findById(request.especialidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad", request.especialidadId()));

        Doctor doctor = Doctor.builder()
                .dni(dni)
                .nombre(request.nombre().trim())
                .apellido(request.apellido().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .rol(Rol.MEDICO)
                .activo(true)
                .matriculaNacional(matricula)
                .especialidad(especialidad)
                .build();

        return toSummary(doctorRepository.save(doctor));
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
