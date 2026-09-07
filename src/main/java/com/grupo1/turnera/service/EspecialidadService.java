package com.grupo1.turnera.service;

import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.repository.EspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspecialidadService {

    @Autowired
    private EspecialidadRepository especialidadRepository;

    public List<Especialidad> obtenerTodas() {
        //se usa el metodo findAll() de JpaRepository para obtener todas las especialidades
        return especialidadRepository.findAll();
    }
}
