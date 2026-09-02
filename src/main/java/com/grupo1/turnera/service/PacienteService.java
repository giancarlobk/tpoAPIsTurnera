package com.grupo1.turnera.service;

import com.grupo1.turnera.exception.DniDuplicadoException;
import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class PacienteService {
    // Declaramos a PacienteReposity y Usamos sus metodos para validar si ya existe
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;

    // Metodo para validar y guardar el nuevo paciente
    public Paciente registraPaciente(Paciente paciente){
        if (pacienteRepository.findByEmail(paciente.getEmail()).isPresent()){
            throw new EmailDuplicadoException(paciente.getEmail());
        }
        if (pacienteRepository.findByDni(paciente.getDni()).isPresent()){
            throw new DniDuplicadoException(paciente.getDni());
        }

        paciente.setRol(Rol.PACIENTE);
        paciente.setActivo(true);
        paciente.setPassword(passwordEncoder.encode(paciente.getPassword())); // Se hashea la Password
        
        return pacienteRepository.save(paciente);
    }
}
