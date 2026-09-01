package com.grupo1.turnera.service;

import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class PacienteService {
    // Declaramos a PacienteReposity y Usamos sus metodos para validar si ya existe
    private final PacienteRepository pacienteRepository;

    // Metodo para validar y guardar el nuevo paciente
    public Paciente registraPaciente(Paciente paciente){
        if (pacienteRepository.findByEmail(paciente.getEmail()).isPresent()){
            throw new RuntimeException("El email ya se encuentra registrado");
        }
        if (pacienteRepository.findByDni(paciente.getDni()).isPresent()){
            throw new RuntimeException("El DNI ya se encuentra registrado");
        }

        paciente.setRol(Rol.PACIENTE);
        paciente.setActivo(true);
        // password sin Hashear por ahora(Se agrega con spring segurity)
        return pacienteRepository.save(paciente);
    }
}
