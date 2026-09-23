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
    void deberiaPublicarLosContratosDeTurnosYEndpointsBase() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Turnero Médico API"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/doctores'].get").exists())
                .andExpect(jsonPath("$.paths['/api/pacientes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/especialidades'].get.responses['200'].content['application/json'].schema.items['$ref']")
                        .value("#/components/schemas/EspecialidadResponse"))
                .andExpect(jsonPath("$.paths['/api/turnos/reservar'].post").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/reservar'].post.responses['201'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/TurnoResponse"))
                .andExpect(jsonPath("$.paths['/api/turnos/sobreturno'].post").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/sobreturno'].post.responses['201'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/TurnoResponse"))
                .andExpect(jsonPath("$.paths['/api/turnos/sobreturno'].post.responses['409'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/turnos/disponibles'].get.responses['200'].content['application/json'].schema.type")
                        .value("array"))
                .andExpect(jsonPath("$.paths['/api/turnos/disponibles'].get.responses['200'].content['application/json'].schema.items['$ref']")
                        .value("#/components/schemas/TurnoDisponibleResponse"))
                .andExpect(jsonPath("$.paths['/api/turnos'].get.responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/TurnoPageResponse"))
                .andExpect(jsonPath("$.components.schemas.TurnoPageResponse.properties.content.items['$ref']")
                        .value("#/components/schemas/TurnoResponse"))
                .andExpect(jsonPath("$.components.schemas.TurnoPageResponse.properties.totalElements.type")
                        .value("integer"))
                .andExpect(jsonPath("$.paths['/api/turnos'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos'].get.responses['403']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/turnos'].get.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/disponibles'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/disponibles'].get.responses['401']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/turnos/disponibles'].get.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch.requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/CambioEstadoTurnoRequest"))
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch.responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/TurnoResponse"))
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{turnoId}/estado'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.PacienteResponse.properties.password").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.PacienteResponse.properties.historiasClinicas").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.TurnoResponse.properties.historialEstados").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.TurnoResponse.properties.historiaClinica").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.Turno").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.TurnoDisponibleResponse.properties.paciente").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.TurnoDisponibleResponse.properties.historiaClinica").doesNotExist());
    }

    @Test
    void deberiaResponderDisponibilidadComoArrayYValidarSusFiltros() throws Exception {
        mockMvc.perform(get("/api/turnos/disponibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/turnos/disponibles").param("doctorId", "no-es-un-id"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/turnos"))
                .andExpect(status().isUnauthorized());
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
