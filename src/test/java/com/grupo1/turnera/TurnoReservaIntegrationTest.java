package com.grupo1.turnera;

import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.DiaSemana;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.TurnoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TurnoReservaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TurnoRepository turnoRepository;

    private Long doctorId;
    private Long pacienteId;
    private LocalDateTime proximoLunes9am;

    @BeforeEach
    void setUp() {
        Especialidad especialidad = Especialidad.builder()
                .nombre("Clínica Médica")
                .descripcion("Especialidad de prueba")
                .build();
        entityManager.persist(especialidad);

        Doctor doctor = Doctor.builder()
                .nombre("Juan").apellido("Gomez").matriculaNacional("MN-999")
                .dni("55555555").email("juan.gomez@turnera.com")
                .password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.MEDICO).activo(true).especialidad(especialidad)
                .build();
        entityManager.persist(doctor);

        HorarioAtencion horario = HorarioAtencion.builder()
                .doctor(doctor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(12, 0))
                .duracionTurnoMinutos(30)
                .build();
        entityManager.persist(horario);

        Paciente paciente = Paciente.builder()
                .nombre("Ana").apellido("Perez").dni("30111222")
                .email("ana.perez@turnera.com")
                .password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1995, 4, 18))
                .build();
        entityManager.persist(paciente);

        entityManager.flush();
        entityManager.clear();

        doctorId = doctor.getId();
        pacienteId = paciente.getId();
        LocalDate proximoLunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        proximoLunes9am = LocalDateTime.of(proximoLunes, LocalTime.of(9, 0));
    }

    @Test
    void deberiaReservarUnTurnoValidoYPersistirTurnoEHistorial() throws Exception {
        mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(pacienteId, doctorId, proximoLunes9am)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.doctorId").value(doctorId))
                .andExpect(jsonPath("$.pacienteId").value(pacienteId))
                .andExpect(jsonPath("$.estado").value("RESERVADO"))
                .andExpect(jsonPath("$.fechaHoraFin").value(
                        proximoLunes9am.plusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        Turno turnoGuardado = turnoRepository.findAll().get(0);
        assertThat(turnoGuardado.getEstado().name()).isEqualTo("RESERVADO");
        assertThat(turnoGuardado.getHistorialEstados()).hasSize(1);
        assertThat(turnoGuardado.getHistorialEstados().get(0).getEstadoNuevo().name()).isEqualTo("RESERVADO");
    }

    @Test
    void deberiaResponder404SiElDoctorNoExiste() throws Exception {
        mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(pacienteId, 999999L, proximoLunes9am)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deberiaResponder404SiElPacienteNoExiste() throws Exception {
        mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(999999L, doctorId, proximoLunes9am)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deberiaResponder400ConFechaPasada() throws Exception {
        mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(pacienteId, doctorId, LocalDateTime.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fechaHoraInicio").exists());
    }

    @Test
    void deberiaResponder400SiElHorarioNoPerteneceALaAtencionDelMedico() throws Exception {
        LocalDateTime fueraDeHorario = proximoLunes9am.withHour(20);

        mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(pacienteId, doctorId, fueraDeHorario)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noDeberiaPermitirDosTurnosSuperpuestosParaElMismoMedico() throws Exception {
        mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(pacienteId, doctorId, proximoLunes9am)))
                .andExpect(status().isCreated());

        Paciente otroPaciente = Paciente.builder()
                .nombre("Bruno").apellido("Diaz").dni("40222333")
                .email("bruno.diaz@turnera.com")
                .password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();
        entityManager.persist(otroPaciente);
        entityManager.flush();

        // Mismo horario exacto.
        mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(otroPaciente.getId(), doctorId, proximoLunes9am)))
                .andExpect(status().isConflict());

        // Horario que arranca 15 min después (se solapa con el primero, que dura hasta las 9:30).
        mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(otroPaciente.getId(), doctorId, proximoLunes9am.plusMinutes(15))))
                .andExpect(status().isConflict());

        assertThat(turnoRepository.count()).isEqualTo(1);
    }

    private String requestBody(Long pacienteId, Long doctorId, LocalDateTime fechaHoraInicio) {
        return """
                {
                  "pacienteId": %d,
                  "doctorId": %d,
                  "fechaHoraInicio": "%s"
                }
                """.formatted(pacienteId, doctorId,
                fechaHoraInicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}