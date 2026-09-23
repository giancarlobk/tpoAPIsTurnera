package com.grupo1.turnera;

import com.grupo1.turnera.model.Administrador;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.AdministradorRepository;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.EspecialidadRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import com.grupo1.turnera.security.JwtUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordLimitIntegrationTest {

    private static final String LIMIT_MESSAGE = "La contraseña no puede superar 72 bytes UTF-8";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PacienteRepository pacienteRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired AdministradorRepository administradorRepository;
    @Autowired EspecialidadRepository especialidadRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

    @ParameterizedTest(name = "paciente: {0} de {1} bytes")
    @MethodSource("passwordsEnElLimite")
    void altaPacienteRespetaLimiteBCrypt(String tipo, int bytes, String password, boolean valido) throws Exception {
        assertThat(password.getBytes(StandardCharsets.UTF_8)).hasSize(bytes);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("dni", "30111222");
        request.put("nombre", "Ana");
        request.put("apellido", "Pérez");
        request.put("email", "ana.perez@example.test");
        request.put("password", password);
        request.put("fechaNacimiento", "1995-04-18");

        var result = mockMvc.perform(post("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        if (valido) {
            result.andExpect(status().isCreated());
            var paciente = pacienteRepository.findByDni("30111222").orElseThrow();
            assertThat(passwordEncoder.matches(password, paciente.getPassword())).isTrue();
        } else {
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(LIMIT_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/api/pacientes"));
            assertThat(pacienteRepository.count()).isZero();
        }
    }

    @ParameterizedTest(name = "médico: {0} de {1} bytes")
    @MethodSource("passwordsEnElLimite")
    void altaMedicoRespetaLimiteBCrypt(String tipo, int bytes, String password, boolean valido) throws Exception {
        assertThat(password.getBytes(StandardCharsets.UTF_8)).hasSize(bytes);
        Administrador admin = administradorRepository.saveAndFlush(Administrador.builder()
                .dni("10111222")
                .nombre("Admin")
                .apellido("Prueba")
                .email("admin@example.test")
                .password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.ADMIN)
                .activo(true)
                .build());
        Especialidad especialidad = especialidadRepository.saveAndFlush(
                Especialidad.builder().nombre("Clínica médica").build());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("dni", "20111222");
        request.put("nombre", "Bruno");
        request.put("apellido", "Gómez");
        request.put("email", "bruno.gomez@example.test");
        request.put("password", password);
        request.put("matriculaNacional", "MN-100");
        request.put("especialidadId", especialidad.getId());

        var result = mockMvc.perform(post("/api/doctores")
                .header("Authorization", "Bearer " + jwtUtil.generateToken(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        if (valido) {
            result.andExpect(status().isCreated());
            var medico = doctorRepository.findByDni("20111222").orElseThrow();
            assertThat(passwordEncoder.matches(password, medico.getPassword())).isTrue();
        } else {
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(LIMIT_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/api/doctores"));
            assertThat(doctorRepository.count()).isZero();
        }
    }

    private static Stream<Arguments> passwordsEnElLimite() {
        return Stream.of(
                Arguments.of("ASCII", 71, "a".repeat(71), true),
                Arguments.of("ASCII", 72, "a".repeat(72), true),
                Arguments.of("ASCII", 73, "a".repeat(73), false),
                Arguments.of("UTF-8 multibyte", 71, passwordMultibyte(71), true),
                Arguments.of("UTF-8 multibyte", 72, passwordMultibyte(72), true),
                Arguments.of("UTF-8 multibyte", 73, passwordMultibyte(73), false)
        );
    }

    private static String passwordMultibyte(int bytes) {
        return "a".repeat(bytes - 2) + "ñ";
    }
}
