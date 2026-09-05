package com.grupo1.turnera.controller;

import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.HorarioCreateRequest;
import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.HorarioResponse;
import com.grupo1.turnera.service.HorarioAtencionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctores")
@RequiredArgsConstructor
@Tag(
        name = "Horarios de atención",
        description = "Administración de bloques semanales de atención de los doctores"
)
public class HorarioAtencionController {

    private final HorarioAtencionService horarioAtencionService;

    @PostMapping("/{doctorId}/horarios")
    @Operation(
            summary = "Registrar horario de atención",
            description = "Registra un bloque semanal de atención para un doctor"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Horario creado correctamente",
                    content = @Content(
                            schema = @Schema(
                                    implementation = HorarioResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos del horario inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "El doctor no existe",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El horario se superpone con otro existente",
                    content = @Content
            )
    })
    public ResponseEntity<HorarioResponse> crearHorario(
            @PathVariable Long doctorId,
            @RequestBody HorarioCreateRequest request
    ) {

        HorarioResponse response =
                horarioAtencionService.crearHorario(
                        doctorId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
