package com.grupo1.turnera.dto.carrito;

import com.grupo1.turnera.dto.turno.UsuarioReferencia;
import com.grupo1.turnera.model.Carrito;
import com.grupo1.turnera.model.ItemCarrito;

import java.util.Comparator;
import java.util.List;

/** Los ítems salen ordenados por fecha y hora del turno. */
public record CarritoResponse(Long id, UsuarioReferencia paciente, List<ItemCarritoResponse> items) {
    public static CarritoResponse fromEntity(Carrito carrito) {
        return new CarritoResponse(carrito.getId(), new UsuarioReferencia(carrito.getPaciente().getId()),
                carrito.getItems().stream()
                        .sorted(Comparator.comparing(ItemCarrito::getFechaHoraInicio))
                        .map(ItemCarritoResponse::fromEntity)
                        .toList());
    }
}
