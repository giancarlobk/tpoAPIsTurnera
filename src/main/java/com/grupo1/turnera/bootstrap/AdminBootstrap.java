package com.grupo1.turnera.bootstrap;

import com.grupo1.turnera.model.Administrador;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "turnera.bootstrap.admin.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${turnera.bootstrap.admin.dni:}")
    private String dni;
    @Value("${turnera.bootstrap.admin.nombre:}")
    private String nombre;
    @Value("${turnera.bootstrap.admin.apellido:}")
    private String apellido;
    @Value("${turnera.bootstrap.admin.email:}")
    private String email;
    @Value("${turnera.bootstrap.admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validarConfiguracion();
        if (administradorRepository.count() > 0) {
            return;
        }

        Administrador administrador = Administrador.builder()
                .dni(dni.trim())
                .nombre(nombre.trim())
                .apellido(apellido.trim())
                .email(email.trim())
                .password(passwordEncoder.encode(password))
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        administradorRepository.save(administrador);
    }

    private void validarConfiguracion() {
        if (estaVacio(dni) || estaVacio(nombre) || estaVacio(apellido) || estaVacio(email) || estaVacio(password)) {
            throw new IllegalStateException("El bootstrap del administrador requiere dni, nombre, apellido, email y password");
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}