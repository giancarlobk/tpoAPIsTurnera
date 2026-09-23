package com.grupo1.turnera.config.openapi;

import com.grupo1.turnera.dto.turno.TurnoResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Solo describe la serialización actual de Page<TurnoResponse> en OpenAPI. */
@Schema(name = "TurnoPageResponse", description = "Página de turnos con metadatos de paginación")
public record TurnoPageResponse(
        List<TurnoResponse> content,
        PageableInfo pageable,
        boolean last,
        int totalPages,
        long totalElements,
        int size,
        int number,
        SortInfo sort,
        boolean first,
        int numberOfElements,
        boolean empty
) {
    public record PageableInfo(int pageNumber, int pageSize, SortInfo sort,
                               long offset, boolean paged, boolean unpaged) {
    }

    public record SortInfo(boolean empty, boolean sorted, boolean unsorted) {
    }
}
