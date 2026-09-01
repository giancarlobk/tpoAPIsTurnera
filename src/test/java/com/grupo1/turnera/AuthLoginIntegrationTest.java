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
class AuthLoginIntegrationTest {

    private static final String EMAIL = "paciente@turnera.com";
    private static final String PASSWORD = "ClaveSegura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void deberiaAutenticarEndToEndContraH2ConPasswordBCrypt() throws Exception {
        Paciente paciente = guardarPacienteActivo();

        assertThat(paciente.getPassword())
                .startsWith("$2")
                .isNotEqualTo(PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "paciente@turnera.com",
                                  "password": "ClaveSegura123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paciente.getId()))
                .andExpect(jsonPath("$.nombre").value("Pablo"))
                .andExpect(jsonPath("$.apellido").value("Paciente"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deberiaResponder401EndToEndConPasswordIncorrecta() throws Exception {
        guardarPacienteActivo();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "paciente@turnera.com",
                                  "password": "incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void deberiaResponder400EndToEndSinConsultarLaBase() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "email-invalido",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        assertThat(pacienteRepository.count()).isZero();
    }

    private Paciente guardarPacienteActivo() {
        Paciente paciente = Paciente.builder()
                .dni("11111111")
                .nombre("Pablo")
                .apellido("Paciente")
                .email(EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .telefono("1122334455")
                .rol(Rol.PACIENTE)
                .activo(true)
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();
        return pacienteRepository.saveAndFlush(paciente);
    }
}
