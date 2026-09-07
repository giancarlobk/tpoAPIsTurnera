package com.grupo1.turnera.controller;


import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.TurnoRequest;
import com.grupo1.turnera.dto.turno.TurnoResponse;
import com.grupo1.turnera.dto.turno.CancelacionTurnoRequest;
import com.grupo1.turnera.exception.ApiErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Reserva de turnos regulares y sobreturnos")
public class TurnoController {

    private final TurnoService turnoService;

    @PatchMapping("/{id}/cancelacion")
    @Operation(summary = "Cancelar turno",
            description = "Solo RESERVADO o CONFIRMADO pueden pasar a CANCELADO_PACIENTE o CANCELADO_MEDICO. "
                    + "Registra actor, motivo y transición atómicamente. El actor declarado se valida contra "
                    + "la base y su relación con el turno; todavía no hay autenticación por sesión o token.")
    @ApiResponse(responseCode = "200", description = "Turno cancelado e historial persistido",
            content = @Content(schema = @Schema(implementation = TurnoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Identificador, rol o motivo inválido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Actor inexistente, inactivo, ajeno al turno o rol no permitido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Turno inexistente",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El estado actual no admite cancelación",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TurnoResponse> cancelarTurno(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CancelacionTurnoRequest request) {
        return ResponseEntity.ok(turnoService.cancelarTurno(id, request));
    }

    @PostMapping("/reservar")
    @Operation(summary = "Reservar turno")
    @ApiResponse(responseCode = "200", description = "Turno reservado",
            content = @Content(schema = @Schema(implementation = TurnoResponse.class)))
    public ResponseEntity<Turno> reservarTurno(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = TurnoRequest.class)))
            @RequestBody Turno turno) {
        Turno resultado = turnoService.reservarTurno(turno);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/sobreturno")
    @Operation(summary = "Crear sobreturno")
    @ApiResponse(responseCode = "201", description = "Sobreturno creado",
            content = @Content(schema = @Schema(implementation = TurnoResponse.class)))
    public ResponseEntity<Turno> crearSobreturno(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = TurnoRequest.class)))
            @RequestBody Turno sobreturno) {
        Turno resultado = turnoService.crearSobreturno(sobreturno);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<Turno>> obtenerTurnosDisponibles(
            @RequestParam(required = false) Long doctorId) {
        List<Turno> disponibles = turnoService.obtenerDisponibles(doctorId);
        return ResponseEntity.ok(disponibles);
    }

}
