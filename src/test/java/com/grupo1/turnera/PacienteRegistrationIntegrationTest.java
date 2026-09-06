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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PacienteRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void deberiaRegistrarUnPacienteValidoYPersistirloEnLaBase() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "30111222",
                                  "nombre": "Ana",
                                  "apellido": "Pérez",
                                  "email": "ana.perez@example.com",
                                  "password": "ClaveSegura123",
                                  "telefono": "1122334455",
                                  "fechaNacimiento": "1995-04-18",
                                  "obraSocial": "OSDE",
                                  "numeroAfiliado": "123456789"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.dni").value("30111222"))
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"))
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());

        Optional<Paciente> pacienteGuardado = pacienteRepository.findByDni("30111222");
        assertThat(pacienteGuardado).isPresent();
        assertThat(pacienteGuardado.get().getPassword())
                .startsWith("$2")
                .isNotEqualTo("ClaveSegura123");
        assertThat(pacienteGuardado.get().getRol()).isEqualTo(Rol.PACIENTE);
    }

    @Test
    void deberiaResponder400ConDatosInvalidosYNoPersistirNada() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "",
                                  "nombre": "",
                                  "apellido": "",
                                  "email": "no-es-un-email",
                                  "password": "123",
                                  "fechaNacimiento": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.dni").exists())
                .andExpect(jsonPath("$.fieldErrors.nombre").exists())
                .andExpect(jsonPath("$.fieldErrors.apellido").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.fechaNacimiento").exists());

        assertThat(pacienteRepository.count()).isZero();
    }

    @Test
    void deberiaResponder409ConEmailDuplicado() throws Exception {
        guardarPaciente("30111222", "ana.perez@example.com", "11122233");

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "40222333",
                                  "nombre": "Bruno",
                                  "apellido": "Gómez",
                                  "email": "ana.perez@example.com",
                                  "password": "ClaveSegura123",
                                  "fechaNacimiento": "1990-01-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/pacientes"));
    }

    @Test
    void deberiaResponder409ConDniDuplicado() throws Exception {
        guardarPaciente("30111222", "ana.perez@example.com", "11122233");

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "30111222",
                                  "nombre": "Bruno",
                                  "apellido": "Gómez",
                                  "email": "bruno.gomez@example.com",
                                  "password": "ClaveSegura123",
                                  "fechaNacimiento": "1990-01-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deberiaPermitirDosPacientesSinTelefonoNiNumeroAfiliado() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "10000001",
                                  "nombre": "Carla",
                                  "apellido": "Diaz",
                                  "email": "carla@example.com",
                                  "password": "ClaveSegura123",
                                  "fechaNacimiento": "1992-05-05"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "10000002",
                                  "nombre": "Dario",
                                  "apellido": "Lopez",
                                  "email": "dario@example.com",
                                  "password": "ClaveSegura123",
                                  "fechaNacimiento": "1993-06-06"
                                }
                                """))
                .andExpect(status().isCreated());

        assertThat(pacienteRepository.count()).isEqualTo(2);
    }

    private void guardarPaciente(String dni, String email, String telefono) {
        Paciente paciente = Paciente.builder()
                .dni(dni)
                .nombre("Ana")
                .apellido("Pérez")
                .email(email)
                .password(passwordEncoder.encode("ClaveSegura123"))
                .telefono(telefono)
                .rol(Rol.PACIENTE)
                .activo(true)
                .fechaNacimiento(LocalDate.of(1995, 4, 18))
                .build();
        pacienteRepository.saveAndFlush(paciente);
    }
}
