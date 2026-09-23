-- ==========================================================
-- Permitir reutilizar horarios de turnos cancelados
-- manteniendo el turno histórico.
-- ==========================================================

-- 1. Agregamos la marca de ocupación.
ALTER TABLE turnos
    ADD COLUMN ocupacion_activa BOOLEAN NULL;


-- 2. Los turnos existentes que ya están cancelados
-- no deben ocupar el horario.
UPDATE turnos
SET ocupacion_activa = NULL
WHERE estado IN (
    'CANCELADO_PACIENTE',
    'CANCELADO_MEDICO'
);


-- 3. Todos los demás turnos existentes continúan activos.
UPDATE turnos
SET ocupacion_activa = TRUE
WHERE estado NOT IN (
    'CANCELADO_PACIENTE',
    'CANCELADO_MEDICO'
);


-- 4. Quitamos la restricción anterior.
ALTER TABLE turnos
    DROP INDEX uk_turno_doctor_inicio;


-- 5. Creamos la nueva protección.
ALTER TABLE turnos
    ADD CONSTRAINT uk_turno_doctor_inicio_activo
    UNIQUE (
        doctor_id,
        fecha_hora_inicio,
        es_sobreturned,
        ocupacion_activa
    );
