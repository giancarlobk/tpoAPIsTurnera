package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.especialidad.EspecialidadCreateRequest;
import com.grupo1.turnera.dto.especialidad.EspecialidadResponse;
import com.grupo1.turnera.exception.EspecialidadDuplicadaException;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.repository.EspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspecialidadService {

    @Autowired
    private EspecialidadRepository especialidadRepository;

    public List<EspecialidadResponse> obtenerTodas() {
        return especialidadRepository.findAll().stream()
                .map(EspecialidadResponse::fromEntity)
                .toList();
    }

    
/**
 * Obtiene una especialidad por su identificador.
 */
public EspecialidadResponse obtenerPorId(Long id) {
    Especialidad especialidad =
            especialidadRepository.findById(id)
                    .orElseThrow(() ->
                            new RecursoNoEncontradoException(
                                    "Especialidad", id
                            )
                    );

    return EspecialidadResponse.fromEntity(especialidad);
}


    public EspecialidadResponse crear(EspecialidadCreateRequest request) {
        String nombre = request.nombre().trim();
        String descripcion = request.descripcion() == null
                ? null
                : request.descripcion().trim();

        if (especialidadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new EspecialidadDuplicadaException(nombre);
        }

        Especialidad especialidad = Especialidad.builder()
                .nombre(nombre)
                .descripcion(descripcion == null || descripcion.isEmpty() ? null : descripcion)
                .build();

        return EspecialidadResponse.fromEntity(especialidadRepository.save(especialidad));
    }
}
