package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.turno.ReservaTurnoRequest;
import com.grupo1.turnera.dto.turno.CambioEstadoTurnoRequest;
import com.grupo1.turnera.dto.turno.SobreturnoRequest;
import com.grupo1.turnera.dto.turno.TurnoDisponibleResponse;
import com.grupo1.turnera.dto.turno.TurnoResponse;
import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.exception.ApiErrorResponse;
import com.grupo1.turnera.model.BaseUsuario;
import com.grupo1.turnera.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Reserva, sobreturnos y cambios de estado")
@ApiResponse(responseCode = "400", description = "Datos inválidos",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
public class TurnoController {
    private final TurnoService turnoService;

    @PostMapping("/reservar")
    @ApiResponse(responseCode = "201", description = "Turno reservado",
            content = @Content(schema = @Schema(implementation = TurnoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Médico activo no encontrado",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El horario ya está ocupado",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(summary = "Reservar turno",
            description = "PACIENTE: la reserva pertenece al principal autenticado y respeta la agenda del médico.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TurnoResponse> reservarTurno(@Valid @RequestBody ReservaTurnoRequest request,
            @AuthenticationPrincipal BaseUsuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.reservarTurno(request, actor));
    }

    @PostMapping("/sobreturno")
    @ApiResponse(responseCode = "201", description = "Sobreturno creado",
            content = @Content(schema = @Schema(implementation = TurnoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Médico o paciente activo no encontrado",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol o la agenda no están autorizados",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(summary = "Crear sobreturno",
            description = "MEDICO: solo su agenda. ADMIN: indica el doctor. El paciente es el destinatario del turno.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TurnoResponse> crearSobreturno(@Valid @RequestBody SobreturnoRequest request,
            @AuthenticationPrincipal BaseUsuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.crearSobreturno(request, actor));
    }

    @PatchMapping("/{turnoId}/estado")
    @ApiResponse(responseCode = "200", description = "Estado actualizado",
            content = @Content(schema = @Schema(implementation = TurnoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Turno no encontrado",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Transición de estado no permitida",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(summary = "Actualizar parcialmente el estado de un turno",
            description = "Modifica solo el estado y el motivo del turno. PACIENTE cancela sus turnos; "
                    + "MEDICO actualiza su agenda; ADMIN puede actualizar cualquier turno.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TurnoResponse> cambiarEstado(@PathVariable Long turnoId,
            @Valid @RequestBody CambioEstadoTurnoRequest request,
            @AuthenticationPrincipal BaseUsuario actor) {
        return ResponseEntity.ok(turnoService.cambiarEstado(turnoId, request, actor));
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Consultar turnos disponibles", description = "Público: solo horarios e identificador del médico.")
    public ResponseEntity<List<TurnoDisponibleResponse>> obtenerTurnosDisponibles(
            @RequestParam(required = false) Long doctorId) {
        return ResponseEntity.ok(turnoService.obtenerDisponibles(doctorId));
    }
    @GetMapping
    @Operation(summary = "Buscar turnos por filtros",
            description = "Requiere autenticación y admite filtros opcionales con paginación.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<TurnoResponse>> buscarTurnos(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) EstadoTurno estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ResponseEntity.ok(turnoService.buscarTurnosConFiltros(
                pacienteId, doctorId, estado, fechaDesde, fechaHasta, pageable));
    }
}
