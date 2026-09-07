package com.grupo1.turnera;

import com.grupo1.turnera.model.*;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.HistorialEstadoTurnoRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.open-in-view=false")
@ActiveProfiles("test")
class TurnoCancelacionIntegrationTest {
    @LocalServerPort int port;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean HistorialEstadoTurnoRepository historiales;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private Long turnoId;
    private Long pacienteId;
    private Long doctorId;
    private Long especialidadId;

    @BeforeEach
    void prepararDatosConfirmados() {
        tx.executeWithoutResult(status -> {
            String unique = UUID.randomUUID().toString();
            Especialidad especialidad = Especialidad.builder().nombre("Prueba-" + unique).build();
            em.persist(especialidad);
            especialidadId = especialidad.getId();
            Doctor doctor = Doctor.builder().dni("doc-" + unique.substring(0, 12)).nombre("Medico")
                    .apellido("Prueba").email("doc-" + unique + "@example.com").password("hash-privado")
                    .rol(Rol.MEDICO).activo(true).matriculaNacional(unique).especialidad(especialidad).build();
            em.persist(doctor);
            doctorId = doctor.getId();
            Paciente paciente = Paciente.builder().dni("pac-" + unique.substring(0, 12)).nombre("Paciente")
                    .apellido("Prueba").email("pac-" + unique + "@example.com").password("hash-privado")
                    .rol(Rol.PACIENTE).activo(true).fechaNacimiento(LocalDate.of(1990, 1, 1)).build();
            em.persist(paciente);
            pacienteId = paciente.getId();
            Turno turno = Turno.builder().doctor(doctor).paciente(paciente).estado(EstadoTurno.RESERVADO)
                    .fechaHoraInicio(LocalDateTime.of(2026, 10, 1, 10, 0))
                    .fechaHoraFin(LocalDateTime.of(2026, 10, 1, 10, 30)).build();
            em.persist(turno);
            turnoId = turno.getId();
            em.persist(HistorialEstadoTurno.builder().turno(turno).estadoAnterior(EstadoTurno.DISPONIBLE)
                    .estadoNuevo(EstadoTurno.RESERVADO).fechaCambio(LocalDateTime.now())
                    .usuarioIdModificador(pacienteId).rolUsuarioModificador(Rol.PACIENTE)
                    .motivo("Reserva inicial").build());
        });
    }

    @AfterEach
    void limpiarDatos() {
        tx.executeWithoutResult(status -> {
            jdbc.update("delete from historial_estados_turno where turno_id = ?", turnoId);
            jdbc.update("delete from turnos where id = ?", turnoId);
            jdbc.update("delete from pacientes where id = ?", pacienteId);
            jdbc.update("delete from doctores where id = ?", doctorId);
            jdbc.update("delete from especialidades where id = ?", especialidadId);
        });
    }

    @Test
    void cancelaPorHttpYPersisteActorMotivoEHistorialSinExponerEntidades() throws Exception {
        HttpResponse<String> response = cancelar(turnoId.toString(), body(pacienteId, "PACIENTE", "  No puedo asistir  "));
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> json = JsonPath.read(response.body(), "$");
        assertThat(json).containsEntry("estado", "CANCELADO_PACIENTE")
                .doesNotContainKeys("historialEstados", "historiaClinica", "password");
        Map<String, Object> doctor = JsonPath.read(response.body(), "$.doctor");
        Map<String, Object> paciente = JsonPath.read(response.body(), "$.paciente");
        assertThat(doctor).containsOnlyKeys("id");
        assertThat(paciente).containsOnlyKeys("id");
        assertThat(estado()).isEqualTo("CANCELADO_PACIENTE");
        assertThat(cantidadHistorial()).isEqualTo(2);
        Map<String, Object> audit = jdbc.queryForMap("select * from historial_estados_turno where turno_id = ? and estado_nuevo = ?",
                turnoId, "CANCELADO_PACIENTE");
        assertThat(audit.get("estado_anterior")).isEqualTo("RESERVADO");
        assertThat(audit.get("motivo")).isEqualTo("No puedo asistir");
        assertThat(((Number) audit.get("usuario_id_modificador")).longValue()).isEqualTo(pacienteId);
        assertThat(audit.get("rol_usuario_modificador")).isEqualTo("PACIENTE");
        assertThat(audit.get("fecha_cambio")).isNotNull();
    }

    @Test
    void medicoCancelaTurnoConfirmado() throws Exception {
        jdbc.update("update turnos set estado = 'CONFIRMADO' where id = ?", turnoId);
        HttpResponse<String> response = cancelar(turnoId.toString(), body(doctorId, "MEDICO", "No atiendo ese día"));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(estado()).isEqualTo("CANCELADO_MEDICO");
        assertThat(cantidadHistorial()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select estado_anterior from historial_estados_turno where turno_id = ? and estado_nuevo = 'CANCELADO_MEDICO'",
                String.class, turnoId)).isEqualTo("CONFIRMADO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDIDO", "CANCELADO_PACIENTE", "CANCELADO_MEDICO", "AUSENTE", "DISPONIBLE"})
    void transicionInvalidaResponde409SinEscrituras(String estado) throws Exception {
        jdbc.update("update turnos set estado = ? where id = ?", estado, turnoId);
        HttpResponse<String> response = cancelar(turnoId.toString(), body(pacienteId, "PACIENTE", "Motivo"));
        assertError(response, 409);
        assertThat(estado()).isEqualTo(estado);
        assertThat(cantidadHistorial()).isEqualTo(1);
    }

    @Test
    void turnoInexistenteResponde404() throws Exception {
        assertError(cancelar(Long.toString(Long.MAX_VALUE), body(pacienteId, "PACIENTE", "Motivo")), 404);
        verificarSinCambios();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "", "{\"usuarioId\":1,\"rol\":\"OTRO\",\"motivo\":\"Motivo\"}",
            "{\"usuarioId\":0,\"rol\":\"PACIENTE\",\"motivo\":\"Motivo\"}",
            "{\"usuarioId\":1,\"rol\":\"PACIENTE\",\"motivo\":\"  \"}",
            "{\"usuarioId\":1,\"rol\":null,\"motivo\":\"Motivo\"}"})
    void cuerpoInvalidoResponde400(String body) throws Exception {
        assertError(cancelar(turnoId.toString(), body), 400);
        verificarSinCambios();
    }

    @Test
    void motivoDemasiadoLargoResponde400() throws Exception {
        assertError(cancelar(turnoId.toString(), body(pacienteId, "PACIENTE", "x".repeat(1001))), 400);
        verificarSinCambios();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void idInvalidoResponde400(String id) throws Exception {
        assertError(cancelar(id, body(pacienteId, "PACIENTE", "Motivo")), 400);
        verificarSinCambios();
    }

    @Test
    void actorInexistenteOAdministradorResponde403() throws Exception {
        assertError(cancelar(turnoId.toString(), body(Long.MAX_VALUE, "PACIENTE", "Motivo")), 403);
        assertError(cancelar(turnoId.toString(), body(pacienteId, "ADMIN", "Motivo")), 403);
        verificarSinCambios();
    }

    @Test
    void actorInactivoResponde403() throws Exception {
        jdbc.update("update pacientes set activo = false where id = ?", pacienteId);
        assertError(cancelar(turnoId.toString(), body(pacienteId, "PACIENTE", "Motivo")), 403);
        verificarSinCambios();
    }

    @Test
    void fallaDelHistorialRevierteActualizacionDelTurno() throws Exception {
        doThrow(new DataIntegrityViolationException("Fallo de historial simulado"))
                .when(historiales).saveAndFlush(any(HistorialEstadoTurno.class));
        HttpResponse<String> response = cancelar(turnoId.toString(), body(pacienteId, "PACIENTE", "Motivo"));
        assertThat(response.statusCode()).isEqualTo(500);
        verificarSinCambios();
    }

    @Test
    void dosCancelacionesConcurrentesPersistenUnaSolaTransicion() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Integer> accion = () -> {
            inicio.await();
            return cancelar(turnoId.toString(), body(pacienteId, "PACIENTE", "Motivo")).statusCode();
        };
        try {
            Future<Integer> primera = executor.submit(accion);
            Future<Integer> segunda = executor.submit(accion);
            inicio.countDown();
            assertThat(List.of(primera.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(estado()).isEqualTo("CANCELADO_PACIENTE");
            assertThat(cantidadHistorial()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    private HttpResponse<String> cancelar(String id, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/turnos/" + id + "/cancelacion"))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String body(Long id, String rol, String motivo) {
        return "{\"usuarioId\":%d,\"rol\":\"%s\",\"motivo\":\"%s\"}".formatted(id, rol, motivo);
    }

    private void assertError(HttpResponse<String> response, int code) {
        assertThat(response.statusCode()).isEqualTo(code);
        Number status = JsonPath.read(response.body(), "$.status");
        assertThat(status.intValue()).isEqualTo(code);
    }

    private String estado() {
        return jdbc.queryForObject("select estado from turnos where id = ?", String.class, turnoId);
    }

    private int cantidadHistorial() {
        return jdbc.queryForObject("select count(*) from historial_estados_turno where turno_id = ?", Integer.class, turnoId);
    }

    private void verificarSinCambios() {
        assertThat(estado()).isEqualTo("RESERVADO");
        assertThat(cantidadHistorial()).isEqualTo(1);
    }
}
