package com.grupo1.turnera.controller;

import com.grupo1.turnera.dto.doctor.DoctorSummaryResponse;
import com.grupo1.turnera.service.DoctorService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctores")
@RequiredArgsConstructor
@Validated
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public List<DoctorSummaryResponse> buscar(
            @RequestParam(name = "especialidadId", required = false)
            @Positive(message = "La especialidad debe ser mayor que cero")
            Long especialidadId,

            @RequestParam(name = "nombre", required = false)
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
            @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
            String nombre
    ) {
        return doctorService.buscar(especialidadId, nombre);
    }
}
