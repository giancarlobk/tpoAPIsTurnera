package com.grupo1.turnera.security;

import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sesión del navegador: JWT en cookie HttpOnly, recuperación con /me, logout y protección CSRF. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SesionCookieIntegrationTest {

    private static final String PASSWORD = "ClaveSegura123";
    private static final String EMAIL = "sesion.cookie@turnera.com";
    // Un turno para un médico inexistente: si pasa la seguridad, responde 404.
    private static final String ITEM_CARRITO = """
            {"doctor": {"id": 999999}, "fechaHoraInicio": "2099-01-05T09:00:00"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Paciente paciente;

    @BeforeEach
    void crearPaciente() {
        paciente = Paciente.builder()
                .nombre("Sesion").apellido("Cookie").dni("30999888").email(EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1995, 4, 18))
                .build();
        entityManager.persist(paciente);
        entityManager.flush();
    }

    @Test
    void elLoginEntregaLaSesionEnUnaCookieHttpOnlyYElTokenCsrfEnOtraLegible() throws Exception {
        MvcResult login = login();

        String sesion = setCookie(login, "turnera_sesion");
        assertThat(sesion).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api", "Max-Age=3600");
        // Spring Security la agrega como Cookie del servlet; los atributos se revisan sobre el objeto.
        Cookie csrf = login.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrf).isNotNull();
        assertThat(csrf.isHttpOnly()).isFalse();
        assertThat(csrf.getSecure()).isTrue();
        assertThat(csrf.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(login.getResponse().getHeaders("Set-Cookie")).noneMatch(cookie -> cookie.startsWith("JSESSIONID="));
    }

    @Test
    void conLaCookieSeRecuperaLaSesionSinExponerElToken() throws Exception {
        Cookie sesion = cookie(login(), "turnera_sesion");

        MvcResult me = mockMvc.perform(get("/api/auth/me").cookie(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paciente.getId()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();
        assertThat(me.getResponse().getContentAsString()).doesNotContain(sesion.getValue());
        assertThat(Long.parseLong(me.getResponse().getContentAsString()
                .replaceAll(".*\"expiresIn\":([0-9]+).*", "$1"))).isBetween(1L, 3600L);

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void unPedidoQueModificaDatosConLaCookieExigeElTokenCsrf() throws Exception {
        MvcResult login = login();
        Cookie sesion = cookie(login, "turnera_sesion");
        Cookie csrf = cookie(login, "XSRF-TOKEN");

        // Como lo haría otro sitio: la cookie viaja sola, sin el header.
        mockMvc.perform(post("/api/carrito/items").cookie(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM_CARRITO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Falta el token CSRF o no es válido"));
        mockMvc.perform(post("/api/carrito/items").cookie(sesion, csrf).header("X-XSRF-TOKEN", "otro-valor")
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM_CARRITO))
                .andExpect(status().isForbidden());

        // Como lo hace el front: devuelve en el header el valor de la cookie XSRF-TOKEN.
        mockMvc.perform(post("/api/carrito/items").cookie(sesion, csrf).header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM_CARRITO))
                .andExpect(status().isNotFound());
    }

    @Test
    void losClientesBearerNoNecesitanTokenCsrf() throws Exception {
        String token = login().getResponse().getContentAsString().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/carrito/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM_CARRITO))
                .andExpect(status().isNotFound());
    }

    @Test
    void elLogoutBorraLaCookieYUnaCookieInvalidaNoBloqueaLasRutasPublicas() throws Exception {
        MvcResult login = login();
        Cookie sesion = cookie(login, "turnera_sesion");
        Cookie csrf = cookie(login, "XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/logout").cookie(sesion)).andExpect(status().isForbidden());
        MvcResult logout = mockMvc.perform(post("/api/auth/logout").cookie(sesion, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(setCookie(logout, "turnera_sesion")).startsWith("turnera_sesion=;").contains("Max-Age=0");

        Cookie basura = new Cookie("turnera_sesion", "no-es-un-jwt");
        MvcResult publico = mockMvc.perform(get("/api/doctores").cookie(basura))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(setCookie(publico, "turnera_sesion")).contains("Max-Age=0");
        mockMvc.perform(get("/api/auth/me").cookie(basura)).andExpect(status().isUnauthorized());
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String setCookie(MvcResult result, String nombre) {
        List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
        return cookies.stream().filter(cookie -> cookie.startsWith(nombre + "=")).findFirst()
                .orElseThrow(() -> new AssertionError("Falta la cookie " + nombre + " en " + cookies));
    }

    private static Cookie cookie(MvcResult result, String nombre) {
        String header = setCookie(result, nombre);
        String valor = header.substring(nombre.length() + 1, header.indexOf(';'));
        return new Cookie(nombre, valor);
    }
}
