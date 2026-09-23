package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.admin.DoctorCreateRequest;
import com.grupo1.turnera.dto.admin.EspecialidadCreateRequest;
import com.grupo1.turnera.dto.admin.HorarioRequest;
import com.grupo1.turnera.dto.admin.HorarioResponse;
import com.grupo1.turnera.dto.doctor.DoctorSummaryResponse;
import com.grupo1.turnera.model.Especialidad;
import com.grupo1.turnera.service.AdministracionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Alta de médicos y configuración de agendas")
public class AdministracionController {

    private final AdministracionService administracionService;

    @PostMapping("/especialidades")
    @Operation(summary = "Crear especialidad", description = "Requiere autenticación con rol ADMIN.")
    public ResponseEntity<Especialidad> crearEspecialidad(@Valid @RequestBody EspecialidadCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administracionService.crearEspecialidad(request));
    }

    @PostMapping("/doctores")
    @Operation(summary = "Crear médico", description = "Requiere autenticación con rol ADMIN.")
    public ResponseEntity<DoctorSummaryResponse> crearDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administracionService.crearDoctor(request));
    }

    @PutMapping("/doctores/{doctorId}/horarios")
    @Operation(summary = "Reemplazar agenda del médico", description = "Reemplaza de forma idempotente todas las franjas del médico.")
    public List<HorarioResponse> reemplazarHorarios(
            @PathVariable Long doctorId,
            @RequestBody @NotNull List<@Valid HorarioRequest> request
    ) {
        return administracionService.reemplazarHorarios(doctorId, request);
    }
}