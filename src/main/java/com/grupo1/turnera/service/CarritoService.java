package com.grupo1.turnera.service;

import com.grupo1.turnera.dto.carrito.AgregarItemCarritoRequest;
import com.grupo1.turnera.dto.carrito.CarritoResponse;
import com.grupo1.turnera.exception.RecursoNoEncontradoException;
import com.grupo1.turnera.exception.TurnoYaEnCarritoException;
import com.grupo1.turnera.model.*;
import com.grupo1.turnera.model.enums.Rol;
import com.grupo1.turnera.repository.CarritoRepository;
import com.grupo1.turnera.repository.DoctorRepository;
import com.grupo1.turnera.repository.ItemCarritoRepository;
import com.grupo1.turnera.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Carrito de turnos del paciente: getAll, getById, add y delete.
 * PACIENTE solo ve y modifica su carrito; ADMIN puede consultar y quitar ítems de cualquiera.
 */
@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final PacienteRepository pacienteRepository;
    private final DoctorRepository doctorRepository;
    private final TurnoService turnoService;
    private final Clock clock;

    // getAll: el paciente recibe una lista con su carrito (vacía si todavía no agregó nada); ADMIN, todos.
    @Transactional(readOnly = true)
    public List<CarritoResponse> obtenerCarritos(BaseUsuario actor) {
        exigirRol(actor, Rol.PACIENTE, Rol.ADMIN);
        if (actor.getRol() == Rol.ADMIN) {
            return carritoRepository.findAll().stream().map(CarritoResponse::fromEntity).toList();
        }
        return carritoRepository.findByPacienteId(actor.getId()).stream()
                .map(CarritoResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CarritoResponse obtenerCarrito(Long carritoId, BaseUsuario actor) {
        exigirRol(actor, Rol.PACIENTE, Rol.ADMIN);
        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Carrito", carritoId));
        validarDueno(carrito, actor);
        return CarritoResponse.fromEntity(carrito);
    }

    // add: agrega un turno (médico + horario) al carrito del paciente; si no tiene carrito, lo crea.
    @Transactional
    public CarritoResponse agregarItem(AgregarItemCarritoRequest request, BaseUsuario actor) {
        exigirRol(actor, Rol.PACIENTE);
        Paciente paciente = pacienteRepository.findById(actor.getId())
                .filter(p -> p.isEnabled() && p.getRol() == Rol.PACIENTE)
                .orElseThrow(() -> new AccessDeniedException("Paciente no habilitado"));
        Long doctorId = request.doctor().id();
        Doctor doctor = doctorRepository.findById(doctorId)
                .filter(d -> d.isEnabled() && d.getRol() == Rol.MEDICO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico", doctorId));

        // Mismas reglas que la reserva: slot de la agenda, alineado a la grilla y libre.
        LocalDateTime inicio = request.fechaHoraInicio();
        LocalDateTime fin = turnoService.validarSlotDisponible(doctor, inicio);

        Carrito carrito = carritoRepository.findByPacienteId(paciente.getId())
                .orElseGet(() -> Carrito.builder().paciente(paciente).build());
        boolean repetido = carrito.getItems().stream().anyMatch(item ->
                item.getDoctor().getId().equals(doctorId) && item.getFechaHoraInicio().equals(inicio));
        if (repetido) {
            throw new TurnoYaEnCarritoException(doctorId, inicio);
        }

        carrito.getItems().add(ItemCarrito.builder()
                .carrito(carrito)
                .doctor(doctor)
                .fechaHoraInicio(inicio)
                .fechaHoraFin(fin)
                .fechaAgregado(LocalDateTime.now(clock))
                .build());
        return CarritoResponse.fromEntity(carritoRepository.saveAndFlush(carrito));
    }

    // delete: quita un ítem del carrito y devuelve el carrito actualizado.
    @Transactional
    public CarritoResponse quitarItem(Long itemId, BaseUsuario actor) {
        exigirRol(actor, Rol.PACIENTE, Rol.ADMIN);
        ItemCarrito item = itemCarritoRepository.findById(itemId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem del carrito", itemId));
        Carrito carrito = item.getCarrito();
        validarDueno(carrito, actor);
        carrito.getItems().removeIf(actual -> actual.getId().equals(itemId));
        return CarritoResponse.fromEntity(carritoRepository.saveAndFlush(carrito));
    }

    private void validarDueno(Carrito carrito, BaseUsuario actor) {
        if (actor.getRol() != Rol.ADMIN && !carrito.getPaciente().getId().equals(actor.getId())) {
            throw new AccessDeniedException("El paciente solo puede ver y modificar su propio carrito");
        }
    }

    private void exigirRol(BaseUsuario actor, Rol... permitidos) {
        if (actor == null || !actor.isEnabled() || !List.of(permitidos).contains(actor.getRol())) {
            throw new AccessDeniedException("No tiene permisos para esta operación");
        }
    }
}
