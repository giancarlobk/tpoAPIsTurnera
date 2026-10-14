package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.doctor.DoctorSummaryResponse;
import com.grupo1.turnera.dto.doctor.DoctorCreateRequest;
import com.grupo1.turnera.exception.DniDuplicadoException;
import com.grupo1.turnera.exception.MatriculaDuplicadaException;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.exception.TelefonoDuplicadoException;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.EspecialidadRepository;
import com.grupo1.turnera.security.PasswordPolicyValidator;
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
    private final EmailUnicidadService emailUnicidadService;
    private final PasswordPolicyValidator passwordPolicyValidator;

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
        return crear(request.dni(), request.nombre(), request.apellido(), request.email(),
                request.password(), null, request.matriculaNacional(), request.especialidadId());
    }

    /** Alta desde /api/admin/doctores: mismas validaciones que el alta común, más el teléfono opcional. */
    @Transactional
    public DoctorSummaryResponse registrarDesdeAdministracion(
            com.grupo1.turnera.dto.admin.DoctorCreateRequest request) {
        return crear(request.dni(), request.nombre(), request.apellido(), request.email(),
                request.password(), request.telefono(), request.matriculaNacional(), request.especialidadId());
    }

    private DoctorSummaryResponse crear(String dniRecibido, String nombre, String apellido, String emailRecibido,
                                        String password, String telefonoRecibido, String matriculaRecibida,
                                        Long especialidadId) {
        passwordPolicyValidator.validateForEncoding(password);

        String dni = dniRecibido.trim();
        String email = emailUnicidadService.normalizar(emailRecibido);
        String matricula = matriculaRecibida.trim();
        String telefono = telefonoRecibido == null || telefonoRecibido.isBlank() ? null : telefonoRecibido.trim();

        if (doctorRepository.findByDni(dni).isPresent()) {
            throw new DniDuplicadoException(dni);
        }
        if (doctorRepository.findByMatriculaNacional(matricula).isPresent()) {
            throw new MatriculaDuplicadaException(matricula);
        }
        if (telefono != null && doctorRepository.findByTelefono(telefono).isPresent()) {
            throw new TelefonoDuplicadoException(telefono);
        }

        Especialidad especialidad = especialidadRepository.findById(especialidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad", especialidadId));

        emailUnicidadService.reservar(email);

        Doctor doctor = Doctor.builder()
                .dni(dni)
                .nombre(nombre.trim())
                .apellido(apellido.trim())
                .email(email)
                .password(passwordEncoder.encode(password))
                .telefono(telefono)
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
