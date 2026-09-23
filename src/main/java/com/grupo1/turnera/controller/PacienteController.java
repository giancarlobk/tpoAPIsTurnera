package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteResponse;
import com.grupo1.turnera.exception.ApiErrorResponse;
import com.grupo1.turnera.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // recibe request HTTP y devuelve JSON
@RequestMapping("/api/pacientes") // define el path base para los metodos de esta clase
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Registro de pacientes")
public class PacienteController {

    private final AuthenticationService authenticationService;

    @PostMapping // Este metodo recibe POST /api/pacientes
    @Operation(summary = "Registrar paciente", description = "Crea un paciente con rol PACIENTE y estado activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente registrado",
                    content = @Content(schema = @Schema(implementation = PacienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "DNI, email, teléfono o número de afiliado ya registrados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PacienteResponse> registrarPaciente(@Valid @RequestBody PacienteCreateRequest request) {
        PacienteResponse pacienteCreado = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteCreado);
    }
}
