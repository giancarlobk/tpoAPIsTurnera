package com.grupo1.turnera;

import com.grupo1.turnera.model.Administrador;
import com.grupo1.turnera.model.BaseUsuario;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.security.JwtUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdministracionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    private static final String ADMIN_EMAIL = "admin@turnera.com";
    private static final String ADMIN_PASSWORD = "ClaveAdmin123";
    private static final String PACIENTE_EMAIL = "paciente-admin-test@turnera.com";
    private Long especialidadId;
    private Administrador administrador;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        Especialidad especialidad = Especialidad.builder().nombre("Cardiología").build();
        entityManager.persist(especialidad);
        especialidadId = especialidad.getId();

        administrador = Administrador.builder()
                .dni("20000001").nombre("Admin").apellido("Prueba").email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD)).rol(Rol.ADMIN).activo(true).build();
        entityManager.persist(administrador);
        paciente = Paciente.builder()
                .dni("30000001").nombre("Paciente").apellido("Prueba").email(PACIENTE_EMAIL)
                .password(passwordEncoder.encode("ClavePaciente123")).rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1990, 1, 1)).build();
        entityManager.persist(paciente);
        entityManager.flush();
    }

    @Test
    void noExponeOperacionesAdministrativasSinCredenciales() throws Exception {
        mockMvc.perform(post("/api/admin/especialidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Neurología\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaPacienteEnOperacionesAdministrativas() throws Exception {
        mockMvc.perform(post("/api/admin/especialidades")
                        .header("Authorization", bearer(paciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Neurología\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteAlAdminCrearEspecialidadYRechazaAgendaInvalida() throws Exception {
        mockMvc.perform(post("/api/admin/especialidades")
                        .header("Authorization", bearer(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Neurología\"}"))
                .andExpect(status().isCreated());

        String doctor = """
                {
                  "dni":"25555555", "nombre":"Dra.", "apellido":"Lopez",
                  "email":"doctora@turnera.com", "password":"ClaveDoctor123",
                  "matriculaNacional":"MN-123", "especialidadId":%d
                }
                """.formatted(especialidadId);
        String doctorResponse = mockMvc.perform(post("/api/admin/doctores")
                        .header("Authorization", bearer(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long doctorId = Long.parseLong(doctorResponse.replaceAll(".*\\\"id\\\":([0-9]+).*", "$1"));
        mockMvc.perform(put("/api/admin/doctores/{id}/horarios", doctorId)
                        .header("Authorization", bearer(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"diaSemana\":\"LUNES\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"09:00:00\",\"duracionTurnoMinutos\":30}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void altaDeMedicoNormalizaEmailYRechazaElDeOtroTipoDeUsuario() throws Exception {
        String doctor = """
                {
                  "dni":"25555556", "nombre":"Dr.", "apellido":"Gomez",
                  "email":"%s", "password":"ClaveDoctor123",
                  "matriculaNacional":"MN-456", "especialidadId":%d
                }
                """;
        mockMvc.perform(post("/api/admin/doctores")
                        .header("Authorization", bearer(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctor.formatted(PACIENTE_EMAIL.toUpperCase(), especialidadId)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/doctores")
                        .header("Authorization", bearer(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctor.formatted("Dr.Gomez@Turnera.com", especialidadId)))
                .andExpect(status().isCreated());

        Doctor guardado = entityManager.createQuery(
                        "select d from Doctor d where d.matriculaNacional = 'MN-456'", Doctor.class)
                .getSingleResult();
        assertThat(guardado.getEmail()).isEqualTo("dr.gomez@turnera.com");
    }

    @Test
    void altaDeEspecialidadRechazaNombreRepetido() throws Exception {
        mockMvc.perform(post("/api/admin/especialidades")
                        .header("Authorization", bearer(administrador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cardiología\"}"))
                .andExpect(status().isConflict());
    }

    private String bearer(BaseUsuario usuario) {
        return "Bearer " + jwtUtil.generateToken(usuario);
    }
}
