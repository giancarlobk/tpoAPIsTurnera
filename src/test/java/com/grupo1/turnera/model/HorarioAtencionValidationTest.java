package com.grupo1.turnera.model;

import com.grupo1.turnera.model.enums.DiaSemana;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HorarioAtencionValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {

        validator =
                Validation
                        .buildDefaultValidatorFactory()
                        .getValidator();
    }

    @Test
    void deberiaRechazarDuracionCero() {

        HorarioAtencion horario =
                HorarioAtencion.builder()
                        .diaSemana(DiaSemana.LUNES)
                        .horaInicio(
                                LocalTime.of(9, 0)
                        )
                        .horaFin(
                                LocalTime.of(12, 0)
                        )
                        .duracionTurnoMinutos(0)
                        .build();

        Set<ConstraintViolation<HorarioAtencion>>
                errores =
                validator.validate(horario);

        assertThat(errores)
                .anyMatch(error ->
                        error.getPropertyPath()
                                .toString()
                                .equals(
                                        "duracionTurnoMinutos"
                                )
                );
    }

    @Test
    void deberiaRechazarDuracionNegativa() {

        HorarioAtencion horario =
                HorarioAtencion.builder()
                        .diaSemana(DiaSemana.LUNES)
                        .horaInicio(
                                LocalTime.of(9, 0)
                        )
                        .horaFin(
                                LocalTime.of(12, 0)
                        )
                        .duracionTurnoMinutos(-30)
                        .build();

        Set<ConstraintViolation<HorarioAtencion>>
                errores =
                validator.validate(horario);

        assertThat(errores)
                .isNotEmpty();
    }
  @Test
  void deberiaRechazarFranjaQueCruzaMedianoche() {

    HorarioAtencion horario =
            HorarioAtencion.builder()
                    .diaSemana(DiaSemana.LUNES)
                    .horaInicio(
                            LocalTime.of(23, 45)
                    )
                    .horaFin(
                            LocalTime.of(0, 15)
                    )
                    .duracionTurnoMinutos(30)
                    .build();

    Set<ConstraintViolation<HorarioAtencion>>
            errores =
            validator.validate(horario);

    assertThat(errores)
            .anyMatch(error ->
                    error.getPropertyPath()
                            .toString()
                            .equals(
                                    "franjaCoherente"
                            )
            );
}
}
