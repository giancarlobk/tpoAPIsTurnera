package com.grupo1.turnera.config.openapi;

import com.grupo1.turnera.model.enums.EstadoTurno;
import com.grupo1.turnera.model.enums.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.grupo1.turnera.model.enums.DiaSemana;
import java.time.LocalTime;

public final class TurneraOpenApiSchemas {

    private TurneraOpenApiSchemas() {
    }

    @Schema(name = "PacienteRegistrationRequest", description = "Datos requeridos para registrar un paciente")
    public record PacienteRegistrationRequest(
            @Schema(example = "30111222") String dni,
            @Schema(example = "Ana") String nombre,
            @Schema(example = "Pérez") String apellido,
            @Schema(example = "ana.perez@example.com") String email,
            @Schema(example = "ClaveSegura123", format = "password") String password,
            @Schema(example = "1122334455") String telefono,
            @Schema(example = "1995-04-18") LocalDate fechaNacimiento,
            @Schema(example = "OSDE") String obraSocial,
            @Schema(example = "123456789") String numeroAfiliado
    ) {
    }

    @Schema(name = "PacienteResponse", description = "Paciente registrado, sin contraseña ni relaciones JPA")
    public record PacienteResponse(
            Long id,
            String dni,
            String nombre,
            String apellido,
            String email,
            String telefono,
            Rol rol,
            Boolean activo,
            LocalDate fechaNacimiento,
            String obraSocial,
            String numeroAfiliado
    ) {
    }

    @Schema(name = "EntityReference", description = "Referencia por identificador a una entidad existente")
    public record EntityReference(
            @Schema(example = "1") Long id
    ) {
    }

    @Schema(name = "TurnoRequest", description = "Datos de un turno; doctor y paciente se referencian por id")
    public record TurnoRequest(
            EntityReference doctor,
            EntityReference paciente,
            @Schema(example = "2026-09-10T10:00:00") LocalDateTime fechaHoraInicio,
            @Schema(example = "2026-09-10T10:30:00") LocalDateTime fechaHoraFin,
            EstadoTurno estado,
            @Schema(example = "false") Boolean esSobreturned,
            String justificacionSobreturned
    ) {
    }

    @Schema(name = "TurnoResponse", description = "Turno persistido, sin historial ni historia clínica")
    public record TurnoResponse(
            Long id,
            EntityReference doctor,
            EntityReference paciente,
            LocalDateTime fechaHoraInicio,
            LocalDateTime fechaHoraFin,
            EstadoTurno estado,
            Boolean esSobreturned,
            String justificacionSobreturned
    ) {
    }
      // DTO utilizado para recibir los datos de un nuevo horario de atención
    @Schema(
            name = "HorarioCreateRequest",
            description = "Datos necesarios para registrar un bloque semanal de atención de un doctor"
    )
    public record HorarioCreateRequest(

            @Schema(example = "LUNES")
            DiaSemana diaSemana,

            @Schema(example = "09:00")
            LocalTime horaInicio,

            @Schema(example = "13:00")
            LocalTime horaFin,

            @Schema(example = "15")
            Integer duracionTurnoMinutos
    ) {
    }

    // DTO utilizado para devolver el horario creado
    @Schema(
            name = "HorarioResponse",
            description = "Bloque semanal de atención registrado para un doctor"
    )
    public record HorarioResponse(

            @Schema(example = "1")
            Long id,

            @Schema(example = "1")
            Long doctorId,

            @Schema(example = "LUNES")
            DiaSemana diaSemana,

            @Schema(example = "09:00")
            LocalTime horaInicio,

            @Schema(example = "13:00")
            LocalTime horaFin,

            @Schema(example = "15")
            Integer duracionTurnoMinutos
    ) {
    }
}
}
