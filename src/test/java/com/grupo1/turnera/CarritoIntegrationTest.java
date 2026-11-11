package com.grupo1.turnera;

import com.grupo1.turnera.model.Administrador;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.model.HorarioAtencion;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.model.enums.DiaSemana;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.security.JwtUtil;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CarritoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    private Long doctorId;
    private Doctor doctor;
    private Paciente paciente;
    private String tokenPaciente;
    private String tokenOtroPaciente;
    private String tokenMedico;
    private String tokenAdmin;
    private LocalDateTime proximoLunes9am;

    @BeforeEach
    void setUp() {
        Especialidad especialidad = Especialidad.builder().nombre("Clínica Médica").build();
        entityManager.persist(especialidad);

        doctor = Doctor.builder()
                .nombre("Juan").apellido("Gomez").matriculaNacional("MN-777")
                .dni("55555556").email("carrito.medico@turnera.com")
                .password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.MEDICO).activo(true).especialidad(especialidad)
                .build();
        entityManager.persist(doctor);
        entityManager.persist(HorarioAtencion.builder()
                .doctor(doctor).diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(12, 0))
                .duracionTurnoMinutos(30).build());

        paciente = paciente("30111223", "carrito.ana@turnera.com");
        Paciente otroPaciente = paciente("30111224", "carrito.bruno@turnera.com");
        Administrador admin = Administrador.builder()
                .dni("20111225").nombre("Admin").apellido("Prueba").email("carrito.admin@turnera.com")
                .password(passwordEncoder.encode("ClaveSegura123")).rol(Rol.ADMIN).activo(true).build();
        entityManager.persist(admin);

        entityManager.flush();
        entityManager.clear();

        doctorId = doctor.getId();
        tokenPaciente = "Bearer " + jwtUtil.generateToken(paciente);
        tokenOtroPaciente = "Bearer " + jwtUtil.generateToken(otroPaciente);
        tokenMedico = "Bearer " + jwtUtil.generateToken(doctor);
        tokenAdmin = "Bearer " + jwtUtil.generateToken(admin);
        LocalDate proximoLunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        proximoLunes9am = LocalDateTime.of(proximoLunes, LocalTime.of(9, 0));
    }

    @Test
    void elPacienteAgregaTurnosConsultaSuCarritoYQuitaUno() throws Exception {
        agregar(tokenPaciente, proximoLunes9am.plusMinutes(30))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paciente.id").value(paciente.getId()))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].doctor.id").value(doctorId))
                .andExpect(jsonPath("$.items[0].fechaHoraFin").value(
                        proximoLunes9am.plusMinutes(60).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        // Los ítems salen ordenados por horario aunque se agreguen desordenados.
        String carrito = agregar(tokenPaciente, proximoLunes9am)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].fechaHoraInicio").value(
                        proximoLunes9am.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .andReturn().getResponse().getContentAsString();
        long carritoId = Long.parseLong(carrito.replaceAll("^\\{\"id\":([0-9]+).*", "$1"));
        long primerItemId = Long.parseLong(carrito.replaceAll(".*\"items\":\\[\\{\"id\":([0-9]+).*", "$1"));

        mockMvc.perform(get("/api/carrito").header("Authorization", tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(carritoId));
        mockMvc.perform(get("/api/carrito/{id}", carritoId).header("Authorization", tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));

        mockMvc.perform(delete("/api/carrito/items/{id}", primerItemId).header("Authorization", tokenPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].fechaHoraInicio").value(
                        proximoLunes9am.plusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    @Test
    void aplicaLasReglasDeLaReservaYRechazaDuplicados() throws Exception {
        agregar(tokenPaciente, proximoLunes9am).andExpect(status().isCreated());
        // El mismo turno dos veces.
        agregar(tokenPaciente, proximoLunes9am).andExpect(status().isConflict());
        // 09:15 no es inicio de slot con turnos de 30 minutos.
        agregar(tokenPaciente, proximoLunes9am.plusMinutes(15)).andExpect(status().isBadRequest());
        // Fuera de la franja de atención.
        agregar(tokenPaciente, proximoLunes9am.plusHours(5)).andExpect(status().isBadRequest());

        // Un horario ya reservado por otro paciente no se puede agregar.
        entityManager.persist(Turno.builder()
                .doctor(entityManager.getReference(Doctor.class, doctorId))
                .paciente(entityManager.getReference(Paciente.class, paciente.getId()))
                .fechaHoraInicio(proximoLunes9am.plusHours(1))
                .fechaHoraFin(proximoLunes9am.plusHours(1).plusMinutes(30))
                .estado(EstadoTurno.RESERVADO)
                .build());
        entityManager.flush();
        agregar(tokenOtroPaciente, proximoLunes9am.plusHours(1)).andExpect(status().isConflict());

        // Médico inexistente.
        mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(999999L, proximoLunes9am)))
                .andExpect(status().isNotFound());
    }

    @Test
    void permisosPorRolYCarritoAjeno() throws Exception {
        String carrito = agregar(tokenPaciente, proximoLunes9am)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long carritoId = Long.parseLong(carrito.replaceAll("^\\{\"id\":([0-9]+).*", "$1"));
        long itemId = Long.parseLong(carrito.replaceAll(".*\"items\":\\[\\{\"id\":([0-9]+).*", "$1"));

        mockMvc.perform(get("/api/carrito")).andExpect(status().isUnauthorized());
        agregar(tokenMedico, proximoLunes9am).andExpect(status().isForbidden());
        agregar(tokenAdmin, proximoLunes9am).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/carrito").header("Authorization", tokenMedico))
                .andExpect(status().isForbidden());

        // Otro paciente no ve ni modifica el carrito ajeno, y su lista está vacía.
        mockMvc.perform(get("/api/carrito/{id}", carritoId).header("Authorization", tokenOtroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/carrito/items/{id}", itemId).header("Authorization", tokenOtroPaciente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/carrito").header("Authorization", tokenOtroPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // ADMIN ve todos los carritos.
        mockMvc.perform(get("/api/carrito").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/carrito/{id}", 999999L).header("Authorization", tokenAdmin))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions agregar(String token, LocalDateTime inicio)
            throws Exception {
        return mockMvc.perform(post("/api/carrito/items")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(doctorId, inicio)));
    }

    private String body(Long doctorId, LocalDateTime inicio) {
        return """
                {"doctor": {"id": %d}, "fechaHoraInicio": "%s"}
                """.formatted(doctorId, inicio);
    }

    private Paciente paciente(String dni, String email) {
        Paciente nuevo = Paciente.builder()
                .nombre("Paciente").apellido("Carrito").dni(dni).email(email)
                .password(passwordEncoder.encode("ClaveSegura123"))
                .rol(Rol.PACIENTE).activo(true)
                .fechaNacimiento(LocalDate.of(1995, 4, 18))
                .build();
        entityManager.persist(nuevo);
        return nuevo;
    }
}
