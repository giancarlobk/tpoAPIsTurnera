package com.grupo1.turnera.bootstrap;

import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.model.Administrador;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.AdministradorRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import com.grupo1.turnera.service.EmailUnicidadService;
import com.grupo1.turnera.service.PacienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regresión: el primer administrador creado por AdminBootstrap debe respetar la misma
 * unicidad global de email que las altas de médicos y pacientes.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminBootstrapIntegrationTest {

    @Autowired AdministradorRepository administradores;
    @Autowired PacienteRepository pacientes;
    @Autowired PacienteService pacienteService;
    @Autowired EmailUnicidadService emailUnicidadService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void sinAdministradores() {
        // El bootstrap solo actúa cuando todavía no hay administradores.
        administradores.deleteAll();
    }

    @Test
    void rechazaEmailDeUnPacienteExistenteSinCrearAdministrador() {
        String email = unico("paciente");
        Paciente paciente = pacientes.saveAndFlush(Paciente.builder()
                .dni("51111222").nombre("Ana").apellido("Paciente")
                .email(email).password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1990, 1, 1)).build());

        AdminBootstrap bootstrap = bootstrap("  " + email.toUpperCase() + "  ");

        assertThatThrownBy(() -> ejecutar(bootstrap)).isInstanceOf(EmailDuplicadoException.class);
        assertThat(administradores.count()).isZero();
        assertThat(pacientes.findById(paciente.getId())).isPresent()
                .get().extracting(Paciente::getEmail).isEqualTo(email);
    }

    @Test
    void creaAdministradorConEmailNormalizadoYReservado() {
        String email = unico("admin");

        ejecutar(bootstrap("  " + email.toUpperCase() + "  "));

        assertThat(administradores.findAll()).singleElement()
                .extracting(Administrador::getEmail).isEqualTo(email);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emails_registrados WHERE email = ?", Integer.class, email))
                .isEqualTo(1);
        // Ninguna otra alta puede reutilizar ese email.
        assertThatThrownBy(() -> pacienteService.registrarPaciente(new PacienteCreateRequest(
                "52111222", "Beto", "Paciente", email.toUpperCase(),
                "ClaveSegura123", null, LocalDate.of(1990, 1, 1), null, null)))
                .isInstanceOf(EmailDuplicadoException.class);
    }

    @Test
    void conAdministradoresExistentesNoHaceNada() {
        String emailExistente = unico("existente");
        administradores.saveAndFlush(Administrador.builder()
                .dni("53111222").nombre("Admin").apellido("Original")
                .email(emailExistente).password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.ADMIN).activo(true).build());

        ejecutar(bootstrap(unico("otro")));

        assertThat(administradores.findAll()).singleElement()
                .extracting(Administrador::getEmail).isEqualTo(emailExistente);
    }

    private AdminBootstrap bootstrap(String email) {
        AdminBootstrap bootstrap = new AdminBootstrap(administradores, passwordEncoder, emailUnicidadService);
        ReflectionTestUtils.setField(bootstrap, "dni", "50111222");
        ReflectionTestUtils.setField(bootstrap, "nombre", "Admin");
        ReflectionTestUtils.setField(bootstrap, "apellido", "Inicial");
        ReflectionTestUtils.setField(bootstrap, "email", email);
        ReflectionTestUtils.setField(bootstrap, "password", "ClaveSegura123");
        return bootstrap;
    }

    private void ejecutar(AdminBootstrap bootstrap) {
        // Fuera de Spring no hay proxy transaccional; la reserva de email exige una transacción activa.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> bootstrap.run(null));
    }

    private String unico(String prefijo) {
        return prefijo + UUID.randomUUID().toString().replace("-", "") + "@turnera.test";
    }
}
