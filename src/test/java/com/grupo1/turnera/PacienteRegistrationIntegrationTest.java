package com.grupo1.turnera;

import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PacienteRegistrationIntegrationTest {

    private static final String PASSWORD = "ClaveSegura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void deberiaPersistirPacienteValidoYResponder201SinPassword() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("30111222", "ana.perez@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.dni").value("30111222"))
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"))
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.historiasClinicas").doesNotExist());

        Paciente persistido = pacienteRepository.findByDni("30111222").orElseThrow();
        assertThat(persistido.getRol()).isEqualTo(Rol.PACIENTE);
        assertThat(persistido.getPassword())
                .startsWith("$2")
                .isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, persistido.getPassword())).isTrue();
    }

    @Test
    void deberiaResponder400ConDatosInvalidos() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "12",
                                  "nombre": "",
                                  "apellido": "",
                                  "email": "no-es-un-email",
                                  "password": "corta",
                                  "fechaNacimiento": "2099-12-31"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deberiaResponder409SiElEmailYaExiste() throws Exception {
        guardarPaciente("30111222", "ana.perez@example.com");

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("30888999", "ana.perez@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deberiaResponder409SiElDniYaExiste() throws Exception {
        guardarPaciente("30111222", "ana.perez@example.com");

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("30111222", "otra.ana@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private void guardarPaciente(String dni, String email) {
        pacienteRepository.save(Paciente.builder()
                .dni(dni)
                .nombre("Ana")
                .apellido("Pérez")
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .rol(Rol.PACIENTE)
                .activo(true)
                .fechaNacimiento(LocalDate.of(1995, 4, 18))
                .build());
    }

    private String requestJson(String dni, String email) {
        return """
                {
                  "dni": "%s",
                  "nombre": "Ana",
                  "apellido": "Pérez",
                  "email": "%s",
                  "password": "ClaveSegura123",
                  "telefono": "1122334455",
                  "fechaNacimiento": "1995-04-18",
                  "obraSocial": "OSDE",
                  "numeroAfiliado": "123456789"
                }
                """.formatted(dni, email);
    }
}
