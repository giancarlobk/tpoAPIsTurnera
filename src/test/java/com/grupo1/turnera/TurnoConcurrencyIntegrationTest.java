package com.grupo1.turnera;

import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.DiaSemana;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.EspecialidadRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import com.grupo1.turnera.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TurnoConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long doctorId;
    private Long pacienteUnoId;
    private Long pacienteDosId;
    private LocalDateTime inicio;

    @BeforeEach
    void setUp() {
        Especialidad especialidad = especialidadRepository.saveAndFlush(
                Especialidad.builder().nombre("Concurrencia " + System.nanoTime()).build());
        Doctor doctor = Doctor.builder()
                .nombre("Doctor").apellido("Concurrencia").matriculaNacional("MN-" + System.nanoTime())
                .dni("55" + Math.abs(System.nanoTime() % 1000000))
                .email("doctor-" + System.nanoTime() + "@turnera.com")
                .password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.MEDICO).activo(true).especialidad(especialidad)
                .build();
        HorarioAtencion horario = HorarioAtencion.builder()
                .doctor(doctor).diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(12, 0))
                .duracionTurnoMinutos(30).build();
        doctor.setHorariosAtencion(List.of(horario));
        doctor = doctorRepository.saveAndFlush(doctor);

        Paciente pacienteUno = pacienteRepository.saveAndFlush(paciente("Uno", "301" + Math.abs(System.nanoTime() % 10000000)));
        Paciente pacienteDos = pacienteRepository.saveAndFlush(paciente("Dos", "302" + Math.abs(System.nanoTime() % 10000000)));

        doctorId = doctor.getId();
        pacienteUnoId = pacienteUno.getId();
        pacienteDosId = pacienteDos.getId();
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        inicio = LocalDateTime.of(lunes, LocalTime.of(9, 0));
    }

    @Test
    void dosReservasSimultaneasConfirmanUnaSola() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> primera = executor.submit(() -> reservar(pacienteUnoId));
            Future<Integer> segunda = executor.submit(() -> reservar(pacienteDosId));

            List<Integer> estados = List.of(primera.get(), segunda.get());
            assertThat(estados).containsExactlyInAnyOrder(201, 409);
            assertThat(turnoRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private int reservar(Long pacienteId) throws Exception {
        return mockMvc.perform(post("/api/turnos/reservar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pacienteId":%d,"doctorId":%d,"fechaHoraInicio":"%s"}
                                """.formatted(pacienteId, doctorId, inicio)))
                .andReturn().getResponse().getStatus();
    }

    private Paciente paciente(String nombre, String dni) {
        return Paciente.builder()
                .nombre(nombre).apellido("Concurrencia").dni(dni)
                .email(nombre.toLowerCase() + "-" + System.nanoTime() + "@turnera.com")
                .password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .build();
    }
}