package com.grupo1.turnera.security;

import com.grupo1.turnera.model.BaseUsuario;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityAuthorizationTest.Probes.class)
class SecurityAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired org.springframework.web.context.WebApplicationContext context;

    @org.junit.jupiter.api.BeforeEach
    void configurarSpringSecurityTest() {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // Endpoints exclusivos de prueba para verificar ADMIN y la regla final authenticated().
    @TestConfiguration
    static class Probes {
        @Bean ProbeController probeController() { return new ProbeController(); }
    }
    @RestController
    static class ProbeController {
        @GetMapping("/api/admin/security-probe") String admin() { return "admin"; }
        @GetMapping("/api/security-probe") String authenticated() { return "ok"; }
    }

    @Test
    void requiereTokenConContratoJsonYNoRedirigeAlLogin() throws Exception {
        mvc.perform(get("/api/admin/security-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/admin/security-probe"));
        mvc.perform(get("/api/security-probe")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorAccedeARutasAdministrativasYAutenticadas() throws Exception {
        mvc.perform(get("/api/admin/security-probe")).andExpect(status().isOk());
        mvc.perform(get("/api/security-probe")).andExpect(status().isOk());
    }

    @Test
    void pacienteYMedicoNoAccedenAAdministracion() throws Exception {
        for (String rol : new String[]{"PACIENTE", "MEDICO"}) {
            mvc.perform(get("/api/admin/security-probe").with(user("usuario").roles(rol)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
            mvc.perform(get("/api/security-probe").with(user("usuario").roles(rol)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void pacienteNoPuedeCrearSobreturnos() throws Exception {
        mvc.perform(post("/api/turnos/sobreturno")).andExpect(status().isForbidden());
    }

    @Test
    void medicoYAdministradorNoReservanComoPacientes() throws Exception {
        for (String rol : new String[]{"MEDICO", "ADMIN"}) {
            mvc.perform(post("/api/turnos/reservar").with(user("usuario").roles(rol)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void contratoUserDetailsReflejaRolYEstado() {
        BaseUsuario usuario = Paciente.builder().email("p@example.test").rol(Rol.PACIENTE).activo(false).build();
        org.assertj.core.api.Assertions.assertThat(usuario.getUsername()).isEqualTo("p@example.test");
        org.assertj.core.api.Assertions.assertThat(usuario.getAuthorities()).extracting("authority").containsExactly("ROLE_PACIENTE");
        org.assertj.core.api.Assertions.assertThat(usuario.isEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThat(usuario.isAccountNonExpired()).isTrue();
        org.assertj.core.api.Assertions.assertThat(usuario.isAccountNonLocked()).isTrue();
        org.assertj.core.api.Assertions.assertThat(usuario.isCredentialsNonExpired()).isTrue();
    }
}
