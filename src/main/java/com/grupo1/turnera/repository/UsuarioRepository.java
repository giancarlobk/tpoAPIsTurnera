package com.grupo1.turnera.repository;

import com.grupo1.turnera.model.BaseUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Acceso común a las tres tablas que heredan de BaseUsuario. */
@Repository
@RequiredArgsConstructor
public class UsuarioRepository {
    private final PacienteRepository pacientes;
    private final DoctorRepository doctores;
    private final AdministradorRepository administradores;

    public Optional<BaseUsuario> findByEmail(String email) {
        List<BaseUsuario> coincidencias = Stream.of(
                pacientes.findByEmailIgnoreCase(email).map(BaseUsuario.class::cast),
                doctores.findByEmailIgnoreCase(email).map(BaseUsuario.class::cast),
                administradores.findByEmailIgnoreCase(email).map(BaseUsuario.class::cast))
                .flatMap(Optional::stream).toList();
        // Un email ambiguo nunca puede autenticarse, aunque una contraseña coincida.
        return coincidencias.size() == 1 ? Optional.of(coincidencias.get(0)) : Optional.empty();
    }

    public boolean existsByEmail(String email) {
        return pacientes.findByEmailIgnoreCase(email).isPresent()
                || doctores.findByEmailIgnoreCase(email).isPresent()
                || administradores.findByEmailIgnoreCase(email).isPresent();
    }
}
