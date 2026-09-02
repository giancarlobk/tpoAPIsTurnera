package com.grupo1.turnera.controller;


import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.TurnoRequest;
import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.TurnoResponse;
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

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Reserva de turnos regulares y sobreturnos")
public class TurnoController {

    private final TurnoService turnoService;

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
}
