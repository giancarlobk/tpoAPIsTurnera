package com.grupo1.turnera;

import com.grupo1.turnera.dto.doctor.DoctorCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.model.Administrador;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.AdministradorRepository;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.EspecialidadRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import com.grupo1.turnera.service.DoctorService;
import com.grupo1.turnera.service.PacienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailGlobalIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DoctorService doctorService;
    @Autowired PacienteService pacienteService;
    @Autowired DoctorRepository doctores;
    @Autowired PacienteRepository pacientes;
    @Autowired AdministradorRepository administradores;
    @Autowired EspecialidadRepository especialidades;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void pacienteExistenteImpideAltaMedicaYSigueAutenticandose() throws Exception {
        String email = unico("paciente");
        pacientes.saveAndFlush(Paciente.builder()
                .dni("34111222").nombre("Ana").apellido("Paciente")
                .email(email).password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1990, 1, 1)).build());
        long totalDoctores = doctores.count();
        long totalPacientes = pacientes.count();
        Especialidad especialidad = especialidad();

        mvc.perform(post("/api/doctores").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorJson("35111222", "MN-PAC", "  " + email.toUpperCase() + "  ", especialidad.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/doctores"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(doctores.count()).isEqualTo(totalDoctores);
        assertThat(pacientes.count()).isEqualTo(totalPacientes);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void medicoExistenteImpideAltaPacienteYAdministradorImpideAmbas() throws Exception {
        Especialidad especialidad = especialidad();
        String emailMedico = unico("medico");
        doctores.saveAndFlush(Doctor.builder()
                .dni("36111222").nombre("Beto").apellido("Medico")
                .email(emailMedico).password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.MEDICO).activo(true).matriculaNacional("MN-EXISTENTE")
                .especialidad(especialidad).build());
        long totalPacientes = pacientes.count();

        mvc.perform(post("/api/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content(pacienteJson("37111222", emailMedico.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/pacientes"));
        assertThat(pacientes.count()).isEqualTo(totalPacientes);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(emailMedico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(emailMedico));

        String emailAdmin = unico("admin");
        administradores.saveAndFlush(Administrador.builder()
                .dni("38111222").nombre("Admin").apellido("Original")
                .email(emailAdmin).password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.ADMIN).activo(true).build());
        mvc.perform(post("/api/doctores").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorJson("39111222", "MN-ADMIN", emailAdmin, especialidad.getId())))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content(pacienteJson("40111222", emailAdmin)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(emailAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(emailAdmin));
    }

    @Test
    void altaNuevaNormalizaEmailYRechazaLaOtraTabla() throws Exception {
        Especialidad especialidad = especialidad();
        String email = unico("nuevo");
        long totalPacientes = pacientes.count();
        mvc.perform(post("/api/doctores").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorJson("41111222", "MN-NUEVO", "  " + email.toUpperCase() + "  ", especialidad.getId())))
                .andExpect(status().isCreated());
        assertThat(doctores.findByEmailIgnoreCase(email)).isPresent()
                .get().extracting(Doctor::getEmail).isEqualTo(email);
        mvc.perform(post("/api/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content(pacienteJson("42111222", email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        assertThat(pacientes.count()).isEqualTo(totalPacientes);
    }

    @Test
    void dosAltasSimultaneasCompartenUnaSolaReserva() throws Exception {
        Especialidad especialidad = especialidad();
        String email = unico("concurrente");
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> medico = executor.submit(() -> resultado(inicio, () ->
                    doctorService.registrar(new DoctorCreateRequest(
                            "43111222", "Cora", "Medica", email.toUpperCase(),
                            "ClaveSegura123", "MN-CONC", especialidad.getId()))));
            Future<String> paciente = executor.submit(() -> resultado(inicio, () ->
                    pacienteService.registrarPaciente(new PacienteCreateRequest(
                            "44111222", "Dina", "Paciente", email,
                            "ClaveSegura123", null, LocalDate.of(1990, 1, 1), null, null))));
            inicio.countDown();
            assertThat(java.util.List.of(medico.get(15, TimeUnit.SECONDS), paciente.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("creado", "duplicado");
        } finally {
            executor.shutdownNow();
        }
        assertThat(doctores.findByEmailIgnoreCase(email).isPresent() ? 1 : 0)
                .isEqualTo(pacientes.findByEmailIgnoreCase(email).isPresent() ? 0 : 1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emails_registrados WHERE email = ?", Integer.class, email))
                .isEqualTo(1);
    }

    private String resultado(CountDownLatch inicio, Runnable alta) {
        try {
            inicio.await();
            alta.run();
            return "creado";
        } catch (EmailDuplicadoException exception) {
            return "duplicado";
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Especialidad especialidad() {
        return especialidades.saveAndFlush(Especialidad.builder()
                .nombre("Especialidad " + UUID.randomUUID()).build());
    }

    private String unico(String prefijo) {
        return prefijo + UUID.randomUUID().toString().replace("-", "") + "@turnera.test";
    }

    private String doctorJson(String dni, String matricula, String email, Long especialidadId) {
        return """
                {"dni":"%s","nombre":"Eva","apellido":"Medica","email":"%s",
                 "password":"ClaveSegura123","matriculaNacional":"%s","especialidadId":%d}
                """.formatted(dni, email, matricula, especialidadId);
    }

    private String pacienteJson(String dni, String email) {
        return """
                {"dni":"%s","nombre":"Eva","apellido":"Paciente","email":"%s",
                 "password":"ClaveSegura123","fechaNacimiento":"1990-01-01"}
                """.formatted(dni, email);
    }

    private String loginJson(String email) {
        return """
                {"email":"%s","password":"ClaveSegura123"}
                """.formatted(email);
    }
}
