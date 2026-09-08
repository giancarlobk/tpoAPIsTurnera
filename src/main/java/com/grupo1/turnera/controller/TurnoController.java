package com.grupo1.turnera.controller;


import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas;
import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.TurnoRequest;
import com.grupo1.turnera.dto.turno.TurnoReservaRequest;
import com.grupo1.turnera.dto.turno.TurnoResponse;
import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.service.TurnoService;
import jakarta.validation.Valid;
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

    @PostMapping("/reservar")
    @Operation(summary = "Reservar turno")
    @ApiResponse(responseCode = "201", description = "Turno reservado",
            content = @Content(schema = @Schema(implementation = TurnoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Fecha inválida o fuera del horario de atención")
    @ApiResponse(responseCode = "404", description = "Paciente o médico inexistente")
    @ApiResponse(responseCode = "409", description = "El horario ya está ocupado")
    public ResponseEntity<TurnoResponse> reservarTurno(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = TurnoReservaRequest.class)))
            @Valid @RequestBody TurnoReservaRequest request) {
        TurnoResponse resultado = turnoService.reservarTurno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PostMapping("/sobreturno")
    @Operation(summary = "Crear sobreturno")
    @ApiResponse(responseCode = "201", description = "Sobreturno creado",
            content = @Content(schema = @Schema(implementation = TurneraOpenApiSchemas.TurnoResponse.class)))
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
