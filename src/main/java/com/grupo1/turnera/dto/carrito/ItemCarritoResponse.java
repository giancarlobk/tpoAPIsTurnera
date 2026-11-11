package com.grupo1.turnera.dto.carrito;

import com.grupo1.turnera.dto.turno.UsuarioReferencia;
import com.grupo1.turnera.model.ItemCarrito;

import java.time.LocalDateTime;

public record ItemCarritoResponse(
        Long id, UsuarioReferencia doctor, LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin
) {
    public static ItemCarritoResponse fromEntity(ItemCarrito item) {
        return new ItemCarritoResponse(item.getId(), new UsuarioReferencia(item.getDoctor().getId()),
                item.getFechaHoraInicio(), item.getFechaHoraFin());
    }
}
