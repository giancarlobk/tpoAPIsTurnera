package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.doctor.DoctorCreateRequest;
import com.grupo1.turnera.dto.doctor.DoctorSummaryResponse;
import com.grupo1.turnera.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.grupo1.turnera.exception.ApiErrorResponse; 

import java.util.List;

@RestController
@RequestMapping("/api/doctores")
@RequiredArgsConstructor
@Validated
@Tag(name = "Médicos", description = "Consulta de profesionales activos")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @Operation(summary = "Registrar médico", description = "Crea un médico activo asociado a una especialidad.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Médico registrado", content = @Content(schema = @Schema(implementation =DoctorSummaryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content( schema = @Schema(implementation =ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Especialidad inexistente", content = @Content(schema = @Schema(implementation =ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "DNI, email o matrícula duplicados",content = @Content(schema = @Schema(implementation =ApiErrorResponse.class)))
})
    public ResponseEntity<DoctorSummaryResponse> registrar(@Valid @RequestBody DoctorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.registrar(request));
    }

    @GetMapping
@Operation(summary = "Buscar médicos", description = "Lista médicos activos y permite filtrar por especialidad y nombre.")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultado de la búsqueda",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = DoctorSummaryResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Filtros inválidos", content = @Content)
})
public ResponseEntity<List<DoctorSummaryResponse>> buscar(
        @RequestParam(name = "especialidadId", required = false)
        @Positive(message = "La especialidad debe ser mayor que cero")
        Long especialidadId,

        @RequestParam(name = "nombre", required = false)
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
        String nombre
) {
    return ResponseEntity.ok(doctorService.buscar(especialidadId, nombre));
}
     
}
