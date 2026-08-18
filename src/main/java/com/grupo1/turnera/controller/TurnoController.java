package com.grupo1.turnera.controller;


import com.grupo1.turnera.model.Turno;
import com.grupo1.turnera.service.TurnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping("/reservar")
    public ResponseEntity<Turno> reservarTurno(@RequestBody Turno turno) {
        Turno resultado = turnoService.reservarTurno(turno);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/sobreturno")
    public ResponseEntity<Turno> crearSobreturno(@RequestBody Turno sobreturno) {
        Turno resultado = turnoService.crearSobreturno(sobreturno);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
