package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteResponse;
import com.grupo1.turnera.exception.DniDuplicadoException;
import com.grupo1.turnera.exception.EmailDuplicadoException;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.service.PacienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PacienteController.class)
class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PacienteService pacienteService;

    @Test
    void deberiaResponder201SinExponerPassword() throws Exception {
        when(pacienteService.registrarPaciente(any(PacienteCreateRequest.class)))
                .thenReturn(new PacienteResponse(
                        1L,
                        "30111222",
                        "Ana",
                        "Pérez",
                        "ana.perez@example.com",
                        "1122334455",
                        Rol.PACIENTE,
                        true,
                        LocalDate.of(1995, 4, 18),
                        "OSDE",
                        "123456789"
                ));

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValidoJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"))
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deberiaResponder400AnteRequestInvalido() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "abc",
                                  "nombre": "",
                                  "apellido": "",
                                  "email": "email-invalido",
                                  "password": "123",
                                  "fechaNacimiento": "2099-01-01"
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

        verifyNoInteractions(pacienteService);
    }

    @Test
    void deberiaResponder409AnteEmailDuplicado() throws Exception {
        when(pacienteService.registrarPaciente(any(PacienteCreateRequest.class)))
                .thenThrow(new EmailDuplicadoException("ana.perez@example.com"));

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValidoJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deberiaResponder409AnteDniDuplicado() throws Exception {
        when(pacienteService.registrarPaciente(any(PacienteCreateRequest.class)))
                .thenThrow(new DniDuplicadoException("30111222"));

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValidoJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private String requestValidoJson() {
        return """
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
                """;
    }
}
