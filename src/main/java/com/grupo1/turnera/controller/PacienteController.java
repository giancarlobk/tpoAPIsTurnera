package com.grupo1.turnera.controller;

import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.PacienteRegistrationRequest;
import com.grupo1.turnera.config.openapi.TurneraOpenApiSchemas.PacienteResponse;
import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.service.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController // recibe request HTTP y devuelve JSON
@RequestMapping("/api/pacientes") // define el path base para los metodos de esta clase
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Registro de pacientes")

public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping // Este metodo recibe POST /api/pacientes
    @Operation(summary = "Registrar paciente", description = "Crea un paciente con rol PACIENTE y estado activo.")
    @ApiResponse(responseCode = "201", description = "Paciente registrado",
            content = @Content(schema = @Schema(implementation = PacienteResponse.class)))
    public ResponseEntity<Paciente> registrarPaciente(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = PacienteRegistrationRequest.class)))
            @RequestBody Paciente paciente){
        Paciente nuevoPaciente = pacienteService.registraPaciente(paciente);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPaciente);
    }//                                                   ^  
    //  convierte automaticamente la respuesta en JSON    |
}
