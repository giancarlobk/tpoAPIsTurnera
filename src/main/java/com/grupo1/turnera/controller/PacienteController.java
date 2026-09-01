package com.grupo1.turnera.controller;

import com.grupo1.turnera.model.Paciente;
import com.grupo1.turnera.service.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController // recibe request HTTP y devuelve JSON
@RequestMapping("/api/pacientes") // define el path base para los metodos de esta clase
@RequiredArgsConstructor

public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping // Este metodo recibe POST /api/pacientes
    public ResponseEntity<Paciente> registrarPaciente(@RequestBody Paciente paciente){
        Paciente nuevoPaciente = pacienteService.registraPaciente(paciente);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPaciente);
    }//                                                   ^  
    //  convierte automaticamente la respuesta en JSON    |
}
