package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.auth.LoginRequest;
import com.grupo1.turnera.dto.paciente.PacienteCreateRequest;
import com.grupo1.turnera.dto.paciente.PacienteResponse;
import org.springframework.http.HttpStatus;
import com.grupo1.turnera.dto.auth.LoginResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión de usuarios")
public class AuthController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registro público de pacientes con BCrypt; rol PACIENTE asignado por el servidor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente registrado",
                    content = @Content(schema = @Schema(implementation = PacienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Datos ya registrados",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PacienteResponse> register(@Valid @RequestBody PacienteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida mediante AuthenticationManager y devuelve un JWT firmado. Usar Authorization: Bearer <token>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
