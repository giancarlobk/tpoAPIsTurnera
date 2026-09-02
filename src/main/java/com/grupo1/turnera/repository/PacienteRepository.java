package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByEmail(String email);
    Optional<Paciente> findByDni(String dni);    
    Optional<Paciente> findByEmailIgnoreCase(String email);
    Optional<Paciente> findByTelefono(String telefono);
    Optional<Paciente> findByNumeroAfiliado(String numeroAfiliado);
}
