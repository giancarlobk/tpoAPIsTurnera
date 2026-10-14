package com.grupo1.turnera;

import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(HttpErrorsIntegrationTest.Probes.class)
class HttpErrorsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired WebApplicationContext context;

    @BeforeEach
    void configurarSeguridadDeMvc() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @TestConfiguration
    static class Probes {
        @Bean ProbeController probeController() { return new ProbeController(); }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/errors/unexpected-probe")
        String unexpected() {
            throw new IllegalStateException("password=secreto-interno");
        }
    }

    @Test
    @WithMockUser
    void metodoNoPermitidoDevuelve405ConAllowYApiErrorResponse() throws Exception {
        mvc.perform(delete("/api/doctores"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.path").value("/api/doctores"))
                .andExpect(jsonPath("$.message").value("Método HTTP no permitido"));
    }

    @Test
    void contentTypeNoSoportadoDevuelve415YApiErrorResponse() throws Exception {
        mvc.perform(post("/api/pacientes")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.path").value("/api/pacientes"))
                .andExpect(jsonPath("$.message").value("Content-Type no soportado"));
    }

    @Test
    void sortDesconocidoDevuelve400YSortPermitidoFunciona() throws Exception {
        // La búsqueda filtra por el principal, así que necesita un usuario real y no un mock de Spring.
        Paciente paciente = Paciente.builder().id(1L).dni("30111222").nombre("Ana").apellido("Pérez")
                .email("ana@example.test").password("x").rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1995, 4, 18)).build();
        mvc.perform(get("/api/turnos").param("sort", "campoInexistente,asc").with(user(paciente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/turnos"))
                .andExpect(jsonPath("$.message", containsString("campoInexistente")))
                .andExpect(jsonPath("$.message", containsString("fechaHoraInicio")));

        mvc.perform(get("/api/turnos").param("sort", "fechaHoraInicio,desc").with(user(paciente)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void errorInesperadoDevuelve500Sanitizado() throws Exception {
        var result = mvc.perform(get("/api/errors/unexpected-probe"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.path").value("/api/errors/unexpected-probe"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno inesperado"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("password", "secreto-interno", "IllegalStateException", "at com.grupo1");
    }
}
