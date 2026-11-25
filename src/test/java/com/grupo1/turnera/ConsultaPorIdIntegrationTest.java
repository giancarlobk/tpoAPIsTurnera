
package com.grupo1.turnera;

import com.grupo1.turnera.model.*;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsultaPorIdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private Long doctorId;
    private Long especialidadId;
    private Long turnoId;

    private Paciente pacientePropietario;
    private Paciente pacienteAjeno;
    private Doctor medicoPropietario;
    private Doctor medicoAjeno;
    private Administrador administrador;

    @BeforeEach
    void prepararDatos() {

        // Especialidad de prueba.
        Especialidad especialidad = Especialidad.builder()
                .nombre("Especialidad test consulta ID")
                .descripcion("Especialidad de integración")
                .build();

        entityManager.persist(especialidad);
        especialidadId = especialidad.getId();

        // Médico propietario del turno.
        medicoPropietario = Doctor.builder()
                .nombre("Ana")
                .apellido("Medica")
                .dni("40111222")
                .email("medica.id@test.local")
                .password("hash")
                .matriculaNacional("MN-ID-101")
                .especialidad(especialidad)
                .rol(Rol.MEDICO)
                .activo(true)
                .build();

        entityManager.persist(medicoPropietario);
        doctorId = medicoPropietario.getId();

        // Otro médico para comprobar acceso denegado.
        medicoAjeno = Doctor.builder()
                .nombre("Bruno")
                .apellido("Medico")
                .dni("40222333")
                .email("medico.ajeno.id@test.local")
                .password("hash")
                .matriculaNacional("MN-ID-102")
                .especialidad(especialidad)
                .rol(Rol.MEDICO)
                .activo(true)
                .build();

        entityManager.persist(medicoAjeno);

        // Paciente propietario del turno.
        pacientePropietario = Paciente.builder()
                .nombre("Carla")
                .apellido("Paciente")
                .dni("40333444")
                .email("paciente.id@test.local")
                .password("hash")
                .fechaNacimiento(LocalDate.of(1995, 1, 1))
                .rol(Rol.PACIENTE)
                .activo(true)
                .build();

        entityManager.persist(pacientePropietario);

        // Otro paciente para comprobar acceso denegado.
        pacienteAjeno = Paciente.builder()
                .nombre("Diego")
                .apellido("Paciente")
                .dni("40444555")
                .email("paciente.ajeno.id@test.local")
                .password("hash")
                .fechaNacimiento(LocalDate.of(1996, 1, 1))
                .rol(Rol.PACIENTE)
                .activo(true)
                .build();

        entityManager.persist(pacienteAjeno);

        // Administrador.
        administrador = Administrador.builder()
                .nombre("Admin")
                .apellido("Prueba")
                .dni("40555666")
                .email("admin.id@test.local")
                .password("hash")
                .rol(Rol.ADMIN)
                .activo(true)
                .build();

        entityManager.persist(administrador);

        // Turno reservado.
        LocalDateTime inicio =
                LocalDateTime.of(2030, 1, 10, 9, 0);

        Turno turno = Turno.builder()
                .doctor(medicoPropietario)
                .paciente(pacientePropietario)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(inicio.plusMinutes(30))
                .estado(EstadoTurno.RESERVADO)
                .esSobreturned(false)
                .ocupacionActiva(true)
                .build();

        entityManager.persist(turno);
        turnoId = turno.getId();

        entityManager.flush();
    }

    @Test
    void medicoPorIdExistenteDevuelve200() throws Exception {
        mockMvc.perform(get("/api/doctores/{id}", doctorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctorId))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void medicoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/doctores/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path")
                        .value("/api/doctores/999999"));
    }

    @Test
    void especialidadPorIdDevuelve200() throws Exception {
        mockMvc.perform(get("/api/especialidades/{id}", especialidadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(especialidadId))
                .andExpect(jsonPath("$.nombre")
                        .value("Especialidad test consulta ID"));
    }

    @Test
    void especialidadInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/especialidades/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void pacientePuedeConsultarSuTurno() throws Exception {
        mockMvc.perform(
                get("/api/turnos/{id}", turnoId)
                        .with(user(pacientePropietario))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(turnoId))
                .andExpect(jsonPath("$.paciente.id")
                        .value(pacientePropietario.getId()));
    }

    @Test
    void pacienteNoPuedeConsultarTurnoAjeno() throws Exception {
        mockMvc.perform(
                get("/api/turnos/{id}", turnoId)
                        .with(user(pacienteAjeno))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void medicoPuedeConsultarSuAgenda() throws Exception {
        mockMvc.perform(
                get("/api/turnos/{id}", turnoId)
                        .with(user(medicoPropietario))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctor.id")
                        .value(medicoPropietario.getId()));
    }

    @Test
    void medicoNoPuedeConsultarAgendaAjena() throws Exception {
        mockMvc.perform(
                get("/api/turnos/{id}", turnoId)
                        .with(user(medicoAjeno))
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void administradorPuedeConsultarCualquierTurno() throws Exception {
        mockMvc.perform(
                get("/api/turnos/{id}", turnoId)
                        .with(user(administrador))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(turnoId));
    }

    @Test
    void turnoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(
                get("/api/turnos/{id}", 999999L)
                        .with(user(administrador))
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void turnoSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/turnos/{id}", turnoId))
                .andExpect(status().isUnauthorized());
    }
}
