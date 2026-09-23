package com.grupo1.turnera.service;

import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailUnicidadService {
    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;

    public String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reservar(String emailNormalizado) {
        // La consulta cubre cuentas anteriores a la tabla de reservas.
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new EmailDuplicadoException(emailNormalizado);
        }
        try {
            // La clave primaria serializa dos altas simultáneas entre tablas distintas.
            jdbcTemplate.update("INSERT INTO emails_registrados (email) VALUES (?)", emailNormalizado);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailDuplicadoException(emailNormalizado);
        }
    }
}
