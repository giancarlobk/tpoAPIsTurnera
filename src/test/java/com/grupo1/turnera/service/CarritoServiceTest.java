package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.carrito.AgregarItemCarritoRequest;
import com.grupo1.turnera.dto.carrito.CarritoResponse;
import com.grupo1.turnera.dto.turno.UsuarioReferencia;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.exception.TurnoNoDisponibleException;
import com.grupo1.turnera.exception.TurnoYaEnCarritoException;
import com.grupo1.turnera.model.Administrador;
import com.grupo1.turnera.model.Carrito;
import com.grupo1.turnera.model.Doctor;
import com.grupo1.turnera.model.ItemCarrito;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.CarritoRepository;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.ItemCarritoRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 8, 0);
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 16, 9, 0);

    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private ItemCarritoRepository itemCarritoRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private TurnoService turnoService;

    private CarritoService carritoService;

    @BeforeEach
    void crearServicio() {
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(AHORA.atZone(zona).toInstant(), zona);
        carritoService = new CarritoService(carritoRepository, itemCarritoRepository,
                pacienteRepository, doctorRepository, turnoService, reloj);
    }

    @Test
    void deberiaCrearElCarritoConElPrimerTurnoYCalcularElFinDesdeLaAgenda() {
        Paciente paciente = paciente(2L);
        Doctor doctor = medico(1L);
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(turnoService.validarSlotDisponible(doctor, INICIO)).thenReturn(INICIO.plusMinutes(30));
        when(carritoRepository.findByPacienteId(2L)).thenReturn(Optional.empty());
        when(carritoRepository.saveAndFlush(any(Carrito.class))).thenAnswer(invocation -> {
            Carrito carrito = invocation.getArgument(0);
            carrito.setId(50L);
            carrito.getItems().get(0).setId(500L);
            return carrito;
        });

        CarritoResponse response = carritoService.agregarItem(agregar(1L, INICIO), paciente);

        ArgumentCaptor<Carrito> captor = ArgumentCaptor.forClass(Carrito.class);
        verify(carritoRepository).saveAndFlush(captor.capture());
        ItemCarrito item = captor.getValue().getItems().get(0);
        assertThat(captor.getValue().getPaciente()).isSameAs(paciente);
        assertThat(item.getFechaHoraFin()).isEqualTo(INICIO.plusMinutes(30));
        assertThat(item.getFechaAgregado()).isEqualTo(AHORA);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.paciente().id()).isEqualTo(2L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).doctor().id()).isEqualTo(1L);
    }

    @Test
    void deberiaRechazarUnTurnoQueYaEstaEnElCarrito() {
        Paciente paciente = paciente(2L);
        Doctor doctor = medico(1L);
        Carrito carrito = carritoCon(paciente, item(10L, doctor, INICIO));
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(turnoService.validarSlotDisponible(doctor, INICIO)).thenReturn(INICIO.plusMinutes(30));
        when(carritoRepository.findByPacienteId(2L)).thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> carritoService.agregarItem(agregar(1L, INICIO), paciente))
                .isInstanceOf(TurnoYaEnCarritoException.class);
        verify(carritoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deberiaPropagarLasReglasDeLaReservaSiElHorarioEstaOcupado() {
        Paciente paciente = paciente(2L);
        Doctor doctor = medico(1L);
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(turnoService.validarSlotDisponible(doctor, INICIO))
                .thenThrow(new TurnoNoDisponibleException(1L, INICIO));

        assertThatThrownBy(() -> carritoService.agregarItem(agregar(1L, INICIO), paciente))
                .isInstanceOf(TurnoNoDisponibleException.class);
        verify(carritoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deberiaLanzar404SiElMedicoNoEstaActivo() {
        Paciente paciente = paciente(2L);
        Doctor inactivo = medico(1L);
        inactivo.setActivo(false);
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> carritoService.agregarItem(agregar(1L, INICIO), paciente))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void soloElPacientePuedeAgregarTurnos() {
        assertThatThrownBy(() -> carritoService.agregarItem(agregar(1L, INICIO), medico(1L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> carritoService.agregarItem(agregar(1L, INICIO), admin()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void elPacienteSoloVeSuCarritoYElAdminVeTodos() {
        Paciente duenio = paciente(2L);
        Carrito carrito = carritoCon(duenio);
        carrito.setId(50L);
        when(carritoRepository.findById(50L)).thenReturn(Optional.of(carrito));
        when(carritoRepository.findAll()).thenReturn(List.of(carrito, carritoCon(paciente(3L))));
        when(carritoRepository.findByPacienteId(2L)).thenReturn(Optional.of(carrito));

        assertThat(carritoService.obtenerCarrito(50L, duenio).id()).isEqualTo(50L);
        assertThat(carritoService.obtenerCarrito(50L, admin()).id()).isEqualTo(50L);
        assertThatThrownBy(() -> carritoService.obtenerCarrito(50L, paciente(3L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> carritoService.obtenerCarritos(medico(1L)))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(carritoService.obtenerCarritos(duenio)).hasSize(1);
        assertThat(carritoService.obtenerCarritos(admin())).hasSize(2);
    }

    @Test
    void deberiaQuitarUnItemSoloDelCarritoPropio() {
        Paciente duenio = paciente(2L);
        ItemCarrito item = item(10L, medico(1L), INICIO);
        Carrito carrito = carritoCon(duenio, item, item(11L, medico(1L), INICIO.plusMinutes(30)));
        when(itemCarritoRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> carritoService.quitarItem(10L, paciente(3L)))
                .isInstanceOf(AccessDeniedException.class);

        when(carritoRepository.saveAndFlush(carrito)).thenReturn(carrito);
        CarritoResponse response = carritoService.quitarItem(10L, duenio);
        assertThat(response.items()).extracting(itemResponse -> itemResponse.id()).containsExactly(11L);
    }

    @Test
    void deberiaLanzar404SiElItemNoExiste() {
        when(itemCarritoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carritoService.quitarItem(99L, paciente(2L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private AgregarItemCarritoRequest agregar(Long doctorId, LocalDateTime inicio) {
        return new AgregarItemCarritoRequest(new UsuarioReferencia(doctorId), inicio);
    }

    private Carrito carritoCon(Paciente paciente, ItemCarrito... items) {
        Carrito carrito = Carrito.builder().paciente(paciente).build();
        for (ItemCarrito item : items) {
            item.setCarrito(carrito);
            carrito.getItems().add(item);
        }
        return carrito;
    }

    private ItemCarrito item(Long id, Doctor doctor, LocalDateTime inicio) {
        return ItemCarrito.builder().id(id).doctor(doctor).fechaHoraInicio(inicio)
                .fechaHoraFin(inicio.plusMinutes(30)).fechaAgregado(AHORA).build();
    }

    private Paciente paciente(Long id) {
        return Paciente.builder().id(id).rol(Rol.PACIENTE).activo(true).build();
    }

    private Doctor medico(Long id) {
        return Doctor.builder().id(id).rol(Rol.MEDICO).activo(true).build();
    }

    private Administrador admin() {
        return Administrador.builder().id(99L).rol(Rol.ADMIN).activo(true).build();
    }
}
