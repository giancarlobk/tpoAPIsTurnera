package com.grupo1.turnera.security;

import com.grupo1.turnera.model.*;
import com.grupo1.turnera.model.enums.*;
import com.grupo1.turnera.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityHttpIntegrationTest {
    private static final String PASSWORD = "ClaveSegura123";
    @Value("${local.server.port}") int port;
    @Autowired ObjectMapper mapper;
    @Autowired PacienteRepository pacientes;
    @Autowired DoctorRepository doctores;
    @Autowired AdministradorRepository administradores;
    @Autowired EspecialidadRepository especialidades;
    @Autowired TurnoRepository turnos;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwt;

    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    Paciente paciente;
    Doctor medico;
    Administrador admin;

    @BeforeEach
    void fixtures() {
        // Este contexto usa exclusivamente H2 con nombre aleatorio.
        turnos.deleteAll();
        pacientes.deleteAll();
        doctores.deleteAll();
        administradores.deleteAll();
        especialidades.deleteAll();
        Especialidad especialidad = especialidades.saveAndFlush(Especialidad.builder().nombre("Clínica").build());
        paciente = pacientes.saveAndFlush(Paciente.builder().dni("22222222").nombre("Paciente").apellido("Prueba")
                .email("paciente@example.test").password(encoder.encode(PASSWORD)).rol(Rol.PACIENTE)
                .fechaNacimiento(LocalDate.of(1990, 1, 1)).activo(true).build());
        medico = doctores.saveAndFlush(Doctor.builder().dni("11111111").nombre("Médico").apellido("Prueba")
                .email("medico@example.test").password(encoder.encode(PASSWORD)).rol(Rol.MEDICO).activo(true)
                .matriculaNacional("MN-123").especialidad(especialidad).build());
        medico.getHorariosAtencion().add(HorarioAtencion.builder().doctor(medico).diaSemana(DiaSemana.MARTES)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(12, 0)).duracionTurnoMinutos(30).build());
        medico = doctores.saveAndFlush(medico);
        admin = administradores.saveAndFlush(Administrador.builder().dni("33333333").nombre("Admin").apellido("Prueba")
                .email("admin@example.test").password(encoder.encode(PASSWORD)).rol(Rol.ADMIN).activo(true).build());
    }

    @Test
    void registroLoginReservaYPersistenciaSinSuplantacion() throws Exception {
        Map<String, Object> registro = new HashMap<>(Map.of("dni", "44444444", "nombre", "Nueva",
                "apellido", "Paciente", "email", "nueva@example.test", "password", PASSWORD,
                "fechaNacimiento", "1995-01-01"));
        registro.put("rol", "ADMIN");
        registro.put("activo", false);
        var alta = send("POST", "/api/auth/register", registro, null);
        assertThat(alta.statusCode()).isEqualTo(201);
        assertThat(json(alta).path("rol").asText()).isEqualTo("PACIENTE");
        assertThat(json(alta).path("activo").asBoolean()).isTrue();
        assertThat(json(alta).has("password")).isFalse();
        var persistido = pacientes.findByEmail("nueva@example.test").orElseThrow();
        assertThat(persistido.getPassword()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, persistido.getPassword())).isTrue();

        String token = login("nueva@example.test");
        var request = reserva();
        request.put("paciente", Map.of("id", paciente.getId()));
        request.put("usuarioId", paciente.getId());
        request.put("rol", "ADMIN");
        request.put("id", 999L);
        request.put("estado", "ATENDIDO");
        request.put("historiaClinica", Map.of("id", 1));
        var response = send("POST", "/api/turnos/reservar", request, token);
        assertThat(response.statusCode()).isEqualTo(201);
        var body = json(response);
        assertThat(body.path("paciente").path("id").asLong()).isEqualTo(persistido.getId());
        assertThat(body.path("estado").asText()).isEqualTo("RESERVADO");
        assertThat(response.body()).doesNotContain("password", "historiasClinicas", "historiaClinica");
        var guardado = turnos.findById(body.path("id").asLong()).orElseThrow();
        assertThat(guardado.getPaciente().getId()).isEqualTo(persistido.getId());
        assertThat(guardado.getId()).isNotEqualTo(999L);
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertError(send("POST", "/api/turnos/reservar", request, null), 401, "/api/turnos/reservar");
    }

    @Test
    void tokenAusenteMalformadoManipuladoVencidoOFirmaAjenaDevuelve401() throws Exception {
        String token = login(paciente.getEmail());
        var parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(
                payload.replace("ROLE_PACIENTE", "ROLE_ADMIN").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String tampered = String.join(".", parts);
        String expired = Jwts.builder().subject(paciente.getEmail()).claim("userId", paciente.getId())
                .claim("roles", List.of("ROLE_PACIENTE")).issuedAt(Date.from(Instant.now().minusSeconds(100)))
                .expiration(Date.from(Instant.now().minusSeconds(1)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtUtilTest.SECRET)), Jwts.SIG.HS256).compact();
        String foreign = Jwts.builder().subject(paciente.getEmail()).signWith(Jwts.SIG.HS256.key().build()).compact();
        for (String invalid : Arrays.asList(null, "", "abc.def.ghi", tampered, expired, foreign)) {
            assertError(send("POST", "/api/turnos/reservar", reserva(), invalid), 401, "/api/turnos/reservar");
        }
        assertThat(turnos.count()).isZero();
    }

    @Test
    void rolesAutorizanYRechazanPorHttp() throws Exception {
        String patientToken = login(paciente.getEmail());
        String doctorToken = login(medico.getEmail());
        String adminToken = login(admin.getEmail());
        assertError(send("POST", "/api/turnos/sobreturno", sobreturno(), patientToken), 403, "/api/turnos/sobreturno");
        assertError(send("POST", "/api/turnos/reservar", reserva(), doctorToken), 403, "/api/turnos/reservar");
        assertError(send("POST", "/api/turnos/reservar", reserva(), adminToken), 403, "/api/turnos/reservar");
        assertError(send("GET", "/api/admin/prueba", null, patientToken), 403, "/api/admin/prueba");
        assertError(send("GET", "/api/admin/prueba", null, doctorToken), 403, "/api/admin/prueba");
        var doctorResponse = send("POST", "/api/turnos/sobreturno", sobreturno(), doctorToken);
        assertThat(doctorResponse.statusCode()).isEqualTo(201);
        assertThat(json(doctorResponse).path("doctor").path("id").asLong()).isEqualTo(medico.getId());
        assertThat(json(doctorResponse).path("esSobreturned").asBoolean()).isTrue();
        var historial = turnos.findById(json(doctorResponse).path("id").asLong()).orElseThrow().getHistorialEstados();
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).getMotivo()).isEqualTo("Control adicional");
        var adminRequest = sobreturno();
        adminRequest.put("fechaHoraInicio", "2030-01-01T10:30:00");
        adminRequest.put("fechaHoraFin", "2030-01-01T11:00:00");
        var adminResponse = send("POST", "/api/turnos/sobreturno", adminRequest, adminToken);
        assertThat(adminResponse.statusCode()).isEqualTo(201);
        assertThat(turnos.count()).isEqualTo(2);
    }

    @Test
    void sobreturnoRechazaJustificacionInvalidaYPacienteInexistente() throws Exception {
        String doctorToken = login(medico.getEmail());
        Map<String, Object> request = sobreturno();
        request.put("justificacionSobreturned", "   ");
        assertError(send("POST", "/api/turnos/sobreturno", request, doctorToken), 400, "/api/turnos/sobreturno");

        request = sobreturno();
        request.put("paciente", Map.of("id", 999999L));
        assertError(send("POST", "/api/turnos/sobreturno", request, doctorToken), 404, "/api/turnos/sobreturno");
        assertThat(turnos.count()).isZero();
    }

    @Test
    void medicoNoPuedeSuplantarOtraAgenda() throws Exception {
        Map<String, Object> request = sobreturno();
        request.put("doctor", Map.of("id", medico.getId() + 100));
        request.put("rol", "ADMIN");
        assertError(send("POST", "/api/turnos/sobreturno", request, login(medico.getEmail())),
                403, "/api/turnos/sobreturno");
        assertThat(turnos.count()).isZero();
        request.remove("doctor");
        assertThat(send("POST", "/api/turnos/sobreturno", request, login(medico.getEmail())).statusCode()).isEqualTo(201);
    }

    @Test
    void credencialesInvalidasCuentasInactivasYEmailsAmbiguosNoAutentican() throws Exception {
        for (Map<String, String> credentials : List.of(Map.of("email", paciente.getEmail(), "password", "incorrecta"),
                Map.of("email", "noexiste@example.test", "password", PASSWORD))) {
            assertError(send("POST", "/api/auth/login", credentials, null), 401, "/api/auth/login");
        }
        paciente.setActivo(false);
        pacientes.saveAndFlush(paciente);
        assertError(send("POST", "/api/auth/login", Map.of("email", paciente.getEmail(), "password", PASSWORD), null),
                401, "/api/auth/login");
        admin.setEmail(medico.getEmail());
        administradores.saveAndFlush(admin);
        assertError(send("POST", "/api/auth/login", Map.of("email", medico.getEmail(), "password", PASSWORD), null),
                401, "/api/auth/login");
    }

    @Test
    void tokenDeCuentaDesactivadaORolModificadoDejaDeServir() throws Exception {
        String token = login(paciente.getEmail());
        paciente.setActivo(false);
        pacientes.saveAndFlush(paciente);
        assertError(send("POST", "/api/turnos/reservar", reserva(), token), 401, "/api/turnos/reservar");
        paciente.setActivo(true);
        paciente.setRol(Rol.ADMIN);
        pacientes.saveAndFlush(paciente);
        assertError(send("POST", "/api/turnos/reservar", reserva(), token), 401, "/api/turnos/reservar");
    }

    @Test
    void ambosRegistrosRechazanEmailDeOtroTipoDeUsuarioYDatosInvalidos() throws Exception {
        for (String path : List.of("/api/auth/register", "/api/pacientes")) {
            assertError(send("POST", path, Map.of("dni", "44444444", "nombre", "Nueva", "apellido", "Prueba",
                    "email", "MEDICO@example.test", "password", PASSWORD, "fechaNacimiento", "1995-01-01"), null), 409, path);
            assertError(send("POST", path, Map.of("email", "invalido"), null), 400, path);
        }
        assertThat(pacientes.count()).isEqualTo(1);
    }

    @Test
    void consultasPublicasYSwaggerAccesiblesSinTokenNiDatosPrivados() throws Exception {
        turnos.saveAndFlush(Turno.builder().doctor(medico).estado(EstadoTurno.DISPONIBLE)
                .fechaHoraInicio(LocalDateTime.of(2030, 1, 1, 10, 0))
                .fechaHoraFin(LocalDateTime.of(2030, 1, 1, 10, 30)).build());
        for (String path : List.of("/api/doctores", "/api/especialidades", "/api/turnos/disponibles", "/v3/api-docs", "/swagger-ui/index.html")) {
            var response = send("GET", path, null, null);
            assertThat(response.statusCode()).as(path).isEqualTo(200);
            assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        }
        var disponibles = send("GET", "/api/turnos/disponibles", null, null);
        assertThat(disponibles.body()).doesNotContain("paciente", "password", "dni", "email", "historiaClinica");
        var api = json(send("GET", "/v3/api-docs", null, null));
        assertThat(api.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(api.at("/paths/~1api~1turnos~1reservar/post/security/0").has("bearerAuth")).isTrue();
        assertThat(api.at("/paths/~1api~1auth~1login/post/security").size()).isZero();
        assertThat(api.at("/paths/~1api~1turnos~1reservar/post/responses").has("401")).isTrue();
        assertThat(api.at("/paths/~1api~1turnos~1reservar/post/responses").has("403")).isTrue();
        assertThat(api.at("/paths/~1api~1turnos~1sobreturno/post/responses").has("201")).isTrue();
        assertThat(api.at("/paths/~1api~1turnos~1reservar/post/requestBody/content/application~1json/schema/$ref").asText())
                .endsWith("/ReservaTurnoRequest");
        assertThat(api.at("/components/schemas/ReservaTurnoRequest/properties").has("paciente")).isFalse();
    }

    @Test
    void passwordMayorAlLimiteBCryptSeRechazaSinErrorInterno() throws Exception {
        String passwordLarga = "ñ".repeat(37); // 74 bytes UTF-8.
        assertError(send("POST", "/api/auth/register",
                Map.of("dni", "44444444", "nombre", "Nueva", "apellido", "Prueba",
                        "email", "nueva@example.test", "password", passwordLarga, "fechaNacimiento", "1995-01-01"),
                null), 400, "/api/auth/register");
        assertError(send("POST", "/api/auth/login", Map.of("email", paciente.getEmail(), "password", passwordLarga),
                null), 401, "/api/auth/login");
        assertThat(pacientes.count()).isEqualTo(1);
    }

    @Test
    void despachoDeErrorPublicoConserva404YSesionNoAutentica() throws Exception {
        assertThat(send("GET", "/api/auth/no-existe", null, null).statusCode()).isEqualTo(404);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/turnos/reservar"))
                .header("Cookie", "JSESSIONID=sesion-falsa")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(reserva()))).build();
        assertError(client.send(request, HttpResponse.BodyHandlers.ofString()), 401, "/api/turnos/reservar");
    }

    private HashMap<String, Object> reserva() {
        return new HashMap<>(Map.of("doctor", Map.of("id", medico.getId()),
                "fechaHoraInicio", "2030-01-01T10:00:00", "fechaHoraFin", "2030-01-01T10:30:00"));
    }

    private HashMap<String, Object> sobreturno() {
        var request = reserva();
        request.put("paciente", Map.of("id", paciente.getId()));
        request.put("justificacionSobreturned", "Control adicional");
        return request;
    }

    private String login(String email) throws Exception {
        var response = send("POST", "/api/auth/login", Map.of("email", email, "password", PASSWORD), null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        var json = json(response);
        assertThat(json.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(json.path("expiresIn").asLong()).isEqualTo(3600);
        assertThat(json.has("password")).isFalse();
        String token = json.path("token").asText();
        assertThat(jwt.validateToken(token).getSubject()).isEqualTo(email);
        return token;
    }

    private HttpResponse<String> send(String method, String path, Object body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) { return mapper.readTree(response.body()); }

    private void assertError(HttpResponse<String> response, int status, String path) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        var json = json(response);
        assertThat(json.path("status").asInt()).isEqualTo(status);
        assertThat(json.path("path").asText()).isEqualTo(path);
        assertThat(json.has("timestamp")).isTrue();
        assertThat(json.has("fieldErrors")).isTrue();
        assertThat(response.body()).doesNotContain("stackTrace", PASSWORD);
        assertThat(json.has("password")).isFalse();
    }
}
