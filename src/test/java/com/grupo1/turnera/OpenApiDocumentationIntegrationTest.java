package com.grupo1.turnera;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deberiaPublicarTodosLosContratosActuales() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Turnero Médico API"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/doctores'].get").exists())
                .andExpect(jsonPath("$.paths['/api/pacientes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/reservar'].post").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/sobreturno'].post").exists())
                .andExpect(jsonPath("$.components.schemas.PacienteResponse.properties.password").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.PacienteResponse.properties.historiasClinicas").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.TurnoResponse.properties.historialEstados").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.TurnoResponse.properties.historiaClinica").doesNotExist());
    }

    @Test
    void deberiaExponerSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
