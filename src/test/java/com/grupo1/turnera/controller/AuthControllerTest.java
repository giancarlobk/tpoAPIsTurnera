package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.auth.LoginRequest;
import com.grupo1.turnera.dto.auth.LoginResponse;
import com.grupo1.turnera.exception.CredencialesInvalidasException;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@org.springframework.context.annotation.Import({
        com.grupo1.turnera.config.SecurityConfig.class, com.grupo1.turnera.config.PasswordConfig.class,
        com.grupo1.turnera.security.SecurityErrorHandler.class})
class AuthControllerTest {

    @MockitoBean
    private com.grupo1.turnera.repository.UsuarioRepository usuarios;
    @MockitoBean
    private com.grupo1.turnera.security.JwtUtil jwtUtil;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authService;

    @Test
    void deberiaResponder200SinExponerPassword() throws Exception {
        LoginResponse response = new LoginResponse(
                1L,
                "Pablo",
                "Paciente",
                "usuario@turnera.com",
                Rol.PACIENTE, "jwt-de-prueba", "Bearer", 3600
        );
        when(authService.login(new LoginRequest("usuario@turnera.com", "ClaveSegura123")))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "usuario@turnera.com",
                                  "password": "ClaveSegura123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("usuario@turnera.com"))
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deberiaResponder400AnteRequestInvalido() throws Exception {
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

        verifyNoInteractions(authService);
    }

    @Test
    void deberiaResponder401AnteCredencialesInvalidas() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new CredencialesInvalidasException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "usuario@turnera.com",
                                  "password": "incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }
}
