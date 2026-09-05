package com.grupo1.turnera;

import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.enums.DiaSemana;
import com.grupo1.turnera.model.enums.Rol;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HorarioAtencionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private Long doctorId;

    @BeforeEach
    void setUp() {

        Especialidad especialidad =
                Especialidad.builder()
                        .nombre("Cardiología")
                        .descripcion("Especialidad de prueba")
                        .build();

        entityManager.persist(especialidad);

        Doctor doctor =
                Doctor.builder()
                        .nombre("Ana")
                        .apellido("Alvarez")
                        .matriculaNacional("MN-HORARIO-100")
                        .dni("55555555")
                        .email("horario@turnera.com")
                        .password("hash-test")
                        .telefono("1199999999")
                        .rol(Rol.MEDICO)
                        .activo(true)
                        .especialidad(especialidad)
                        .build();

        entityManager.persist(doctor);

        entityManager.flush();

        doctorId = doctor.getId();

        entityManager.clear();
    }

    @Test
    void deberiaCrearHorarioYResponder201() throws Exception {

        String json = """
                {
                  "diaSemana": "LUNES",
                  "horaInicio": "09:00",
                  "horaFin": "13:00",
                  "duracionTurnoMinutos": 15
                }
                """;

        mockMvc.perform(
                        post("/api/doctores/{doctorId}/horarios", doctorId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.doctorId").value(doctorId))
                .andExpect(jsonPath("$.diaSemana").value("LUNES"))
                .andExpect(jsonPath("$.horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.horaFin").value("13:00:00"))
                .andExpect(jsonPath("$.duracionTurnoMinutos").value(15));
    }

    @Test
    void deberiaResponder400CuandoFinEsMenorQueInicio() throws Exception {

        String json = """
                {
                  "diaSemana": "LUNES",
                  "horaInicio": "10:00",
                  "horaFin": "09:00",
                  "duracionTurnoMinutos": 15
                }
                """;

        mockMvc.perform(
                        post("/api/doctores/{doctorId}/horarios", doctorId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("La hora de fin debe ser posterior a la hora de inicio"));
    }

    @Test
    void deberiaResponder404SiDoctorNoExiste() throws Exception {

        String json = """
                {
                  "diaSemana": "LUNES",
                  "horaInicio": "09:00",
                  "horaFin": "13:00",
                  "duracionTurnoMinutos": 15
                }
                """;

        mockMvc.perform(
                        post("/api/doctores/{doctorId}/horarios", 999999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deberiaResponder409CuandoExisteSuperposicion() throws Exception {

        Doctor doctor =
                entityManager.find(Doctor.class, doctorId);

        HorarioAtencion horarioExistente =
                HorarioAtencion.builder()
                        .doctor(doctor)
                        .diaSemana(DiaSemana.LUNES)
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(12, 0))
                        .duracionTurnoMinutos(15)
                        .build();

        entityManager.persist(horarioExistente);

        entityManager.flush();
        entityManager.clear();

        String json = """
                {
                  "diaSemana": "LUNES",
                  "horaInicio": "10:00",
                  "horaFin": "13:00",
                  "duracionTurnoMinutos": 15
                }
                """;

        mockMvc.perform(
                        post("/api/doctores/{doctorId}/horarios", doctorId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("El horario se superpone con otro horario existente del doctor seleccionado."));
    }
}
