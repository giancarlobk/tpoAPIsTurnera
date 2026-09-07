package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.especialidad.EspecialidadCreateRequest;
import com.grupo1.turnera.dto.especialidad.EspecialidadResponse;
import com.grupo1.turnera.exception.ApiErrorResponse;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.service.EspecialidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@Tag(name = "Especialidades", description = "Registro y consulta de especialidades medicas")
public class EspecialidadController {

    @Autowired
    private EspecialidadService especialidadService;

        @PostMapping
        @Operation(summary = "Registrar especialidad")
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Especialidad creada",
                content = @Content(schema = @Schema(implementation = EspecialidadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La especialidad ya existe",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        public ResponseEntity<EspecialidadResponse> crear(
            @Valid @RequestBody EspecialidadCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crear(request));
        }

    @GetMapping
    public ResponseEntity<List<Especialidad>> listarEspecialidades() {
        List<Especialidad> especialidades = especialidadService.obtenerTodas();
        return ResponseEntity.ok(especialidades);
    }
}
