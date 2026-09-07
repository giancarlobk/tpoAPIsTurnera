# Turnero Médico

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.1-6BA539?logo=openapiinitiative&logoColor=white)](http://localhost:8080/v3/api-docs)

API REST para administrar autenticación, profesionales, pacientes y turnos médicos desde una única fuente de información.

> **Estado:** proyecto en desarrollo activo. Los contratos documentados a continuación corresponden a los endpoints disponibles actualmente.

## Contenido

- [Visión general](#visión-general)
- [API disponible](#api-disponible)
- [Inicio rápido](#inicio-rápido)
- [Documentación interactiva](#documentación-interactiva)
- [Pruebas](#pruebas)
- [Arquitectura y tecnologías](#arquitectura-y-tecnologías)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Roadmap](#roadmap)

## Visión general

Turnero Médico digitaliza el circuito principal de un consultorio y centraliza la información utilizada por pacientes, profesionales y administradores.

El backend está construido con Spring Boot y expone contratos HTTP/JSON. La persistencia utiliza MySQL en ejecución normal y H2 para las pruebas de integración.

```text
Cliente HTTP
    ↓
Controllers (Spring MVC)
    ↓
Services
    ↓
Repositories (Spring Data JPA)
    ↓
MySQL / H2
```

## API disponible

URL base local: `http://localhost:8080`

| Método | Endpoint | Descripción | Entrada principal | Respuestas |
| --- | --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Autentica un paciente, médico o administrador activo. | `email`, `password` | `200`, `400`, `401` |
| `GET` | `/api/doctores` | Lista médicos activos y permite combinar filtros. | Query opcional: `especialidadId`, `nombre` | `200`, `400` |
| `POST` | `/api/pacientes` | Registra un paciente con rol `PACIENTE`. | Datos personales, contacto y cobertura | `201`, `400`, `409` |
| `POST` | `/api/turnos/reservar` | Persiste la reserva de un turno regular. | Doctor, paciente, horario y estado | `200` |
| `POST` | `/api/turnos/sobreturno` | Persiste un sobreturno con su justificación. | Datos del turno y justificación | `201` |
| `PATCH` | `/api/turnos/{id}/cancelacion` | Cancela un turno y registra la transición. | `usuarioId`, `rol`, `motivo` | `200`, `400`, `403`, `404`, `409` |

### Contratos high level

#### Autenticación

```json
{
  "email": "usuario@turnera.com",
  "password": "ClaveSegura123"
}
```

Una autenticación exitosa devuelve el identificador, nombre, apellido, email y rol del usuario. La emisión de tokens todavía no forma parte del contrato actual.

#### Búsqueda de médicos

Los filtros son opcionales y se pueden combinar:

```http
GET /api/doctores
GET /api/doctores?especialidadId=1
GET /api/doctores?nombre=ana
GET /api/doctores?especialidadId=1&nombre=ana
```

La respuesta contiene datos resumidos del profesional y su especialidad. No expone contraseñas, horarios completos ni relaciones JPA.

#### Registro de pacientes

El alta usa un contrato REST (`PacienteCreateRequest` / `PacienteResponse`) desacoplado de la entidad JPA: la entidad nunca se serializa ni se recibe directamente.

`telefono`, `obraSocial` y `numeroAfiliado` son opcionales; el resto de los campos son obligatorios y se validan con Bean Validation.

Request (`POST /api/pacientes`):

```json
{
  "dni": "30111222",
  "nombre": "Ana",
  "apellido": "Pérez",
  "email": "ana.perez@example.com",
  "password": "ClaveSegura123",
  "telefono": "1122334455",
  "fechaNacimiento": "1995-04-18",
  "obraSocial": "OSDE",
  "numeroAfiliado": "123456789"
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "dni": "30111222",
  "nombre": "Ana",
  "apellido": "Pérez",
  "email": "ana.perez@example.com",
  "telefono": "1122334455",
  "rol": "PACIENTE",
  "activo": true,
  "fechaNacimiento": "1995-04-18",
  "obraSocial": "OSDE",
  "numeroAfiliado": "123456789"
}
```

El servidor asigna el rol `PACIENTE`, activa la cuenta y hashea la contraseña con BCrypt antes de persistir; la contraseña nunca se devuelve.

Códigos de respuesta:

| Código | Motivo |
| --- | --- |
| `201` | Paciente creado y persistido |
| `400` | DNI, nombre, apellido, email, contraseña o fecha de nacimiento inválidos/faltantes |
| `409` | DNI, email, teléfono o número de afiliado ya registrados |

#### Turnos y sobreturnos

Los contratos de turnos referencian al doctor y al paciente por identificador e incluyen:

```json
{
  "doctor": { "id": 1 },
  "paciente": { "id": 2 },
  "fechaHoraInicio": "2026-09-10T10:00:00",
  "fechaHoraFin": "2026-09-10T10:30:00",
  "estado": "RESERVADO",
  "esSobreturned": false,
  "justificacionSobreturned": null
}
```

Para un sobreturno, `esSobreturned` debe representar esa condición y `justificacionSobreturned` describe el motivo.

#### Cancelación de turnos

`PATCH /api/turnos/{id}/cancelacion` recibe `CancelacionTurnoRequest` y devuelve `TurnoResponse`.
El identificador del turno y `usuarioId` deben ser positivos. `rol` es obligatorio y el motivo debe
contener entre 1 y 1000 caracteres, con al menos un carácter distinto de espacio. Se guarda sin espacios externos.

```http
PATCH /api/turnos/1/cancelacion
Content-Type: application/json

{
  "usuarioId": 2,
  "rol": "PACIENTE",
  "motivo": "No puedo asistir"
}
```

Solo el paciente asignado o el médico del turno, activos y con el rol correspondiente en la base,
pueden cancelar. `ADMIN` no está habilitado para esta operación. Los identificadores de pacientes y
médicos pertenecen a tablas distintas, por lo que se verifica siempre la combinación de id y rol.

| Estado actual | Actor | Estado resultante |
| --- | --- | --- |
| `RESERVADO` o `CONFIRMADO` | `PACIENTE` | `CANCELADO_PACIENTE` |
| `RESERVADO` o `CONFIRMADO` | `MEDICO` | `CANCELADO_MEDICO` |
| `DISPONIBLE`, `ATENDIDO`, `AUSENTE` o cualquier cancelado | Paciente o médico autorizado | `409`, sin cambios |

Respuesta `200 OK`:

```json
{
  "id": 1,
  "doctor": { "id": 1 },
  "paciente": { "id": 2 },
  "fechaHoraInicio": "2026-09-10T10:00:00",
  "fechaHoraFin": "2026-09-10T10:30:00",
  "estado": "CANCELADO_PACIENTE",
  "esSobreturned": false,
  "justificacionSobreturned": null
}
```

Se conservan el horario, las referencias de médico y paciente, los datos del sobreturno y el historial
anterior. La cancelación no elimina el turno ni lo vuelve a marcar como disponible. Una única transacción
actualiza el estado y agrega `HistorialEstadoTurno` con estado anterior/nuevo, fecha, usuario, rol y motivo.
Un bloqueo de escritura por turno serializa las cancelaciones concurrentes: la segunda recibe `409`.
Si falla la escritura del historial, también se revierte el cambio de estado.

| Código | Motivo |
| --- | --- |
| `200` | Cancelación e historial persistidos |
| `400` | JSON, id, rol desconocido, campos obligatorios o motivo inválidos |
| `403` | Actor inexistente, inactivo, ajeno al turno, rol inconsistente o `ADMIN` |
| `404` | Turno inexistente |
| `409` | Transición inválida, incluida una cancelación repetida |

Los errores usan `ApiErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`, `fieldErrors`).
Primero se valida la entrada, luego la existencia del turno, el actor y finalmente la transición.

**Límite actual de autenticación:** el login aún no emite tokens ni establece sesión. `usuarioId` y `rol`
son datos declarados por el cliente; comprobarlos en la base no acredita la identidad del solicitante.
Antes de exponer este contrato a usuarios no confiables, el actor debe obtenerse de una sesión o token
verificado. Esta tarea no incorpora Spring Security ni cambia el contrato de login.

## Inicio rápido

### Requisitos

- Docker Desktop con Docker Compose, o
- Java 17 y una instancia MySQL disponible.

### Ejecución con Docker Compose

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/giancarlobk/tpoAPIsTurnera.git
   cd tpoAPIsTurnera
   ```

2. Crear el archivo local de variables:

   ```powershell
   Copy-Item .env.example .env
   ```

   En Linux o macOS:

   ```bash
   cp .env.example .env
   ```

3. Revisar las credenciales de desarrollo en `.env` y levantar los servicios:

   ```bash
   docker compose up --build
   ```

4. Detener los contenedores conservando los datos:

   ```bash
   docker compose down
   ```

Servicios expuestos por defecto:

| Servicio | Puerto | Propósito |
| --- | --- | --- |
| Backend | `8080` | API REST y documentación OpenAPI |
| MySQL | `3306` | Persistencia del entorno local |

> Las credenciales de `.env.example` son únicamente de desarrollo y deben reemplazarse fuera del entorno local.

## Documentación interactiva

Con la aplicación en ejecución:

- [Swagger UI](http://localhost:8080/swagger-ui.html): exploración y prueba interactiva de los endpoints.
- [OpenAPI JSON](http://localhost:8080/v3/api-docs): especificación consumible por herramientas y clientes.

La especificación cubre todos los endpoints actuales y utiliza esquemas públicos que omiten contraseñas y relaciones internas de persistencia.

## Pruebas

Windows:

```powershell
.\mvnw.cmd test
```

Linux o macOS:

```bash
./mvnw test
```

La suite incluye pruebas unitarias, web e integración con Spring Boot, MockMvc, JPA y H2. También verifica que `/v3/api-docs` y Swagger UI estén disponibles y que la especificación incluya los contratos actuales.

Las pruebas de cancelación usan HTTP real en un puerto aleatorio y comprueban persistencia tras el
commit, conservación del historial, rollback ante fallos y solicitudes concurrentes. Se pueden ejecutar con:

```powershell
.\mvnw.cmd "-Dtest=TurnoCancelacionServiceTest,TurnoCancelacionIntegrationTest" test
```

## Arquitectura y tecnologías

| Área | Tecnología |
| --- | --- |
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.1, Spring MVC |
| Persistencia | Spring Data JPA, Hibernate |
| Base de datos | MySQL 8.4; H2 para pruebas |
| Contratos API | OpenAPI 3.1, Springdoc, Swagger UI |
| Validación | Jakarta Validation |
| Seguridad de contraseñas | BCrypt |
| Build y pruebas | Maven Wrapper, JUnit, MockMvc |
| Contenedores | Docker, Docker Compose |

## Estructura del proyecto

```text
.
├── docs/                         # Documentación funcional del producto
├── src/main/java/.../config/     # Configuración general y OpenAPI
├── src/main/java/.../controller/ # Endpoints HTTP
├── src/main/java/.../dto/        # Contratos de entrada y salida
├── src/main/java/.../model/      # Entidades JPA y enumeraciones
├── src/main/java/.../repository/ # Acceso a datos
├── src/main/java/.../service/    # Lógica de aplicación
├── src/test/                     # Pruebas unitarias e integración
├── compose.yaml                  # Backend y MySQL para desarrollo
├── Dockerfile                    # Imagen del backend
└── pom.xml                       # Dependencias y build Maven
```

## Roadmap

El MVP prevé incorporar progresivamente:

- consulta de turnos con filtros;
- configuración y consulta de disponibilidad;
- administración completa de estados;
- prevención de reservas superpuestas;
- autenticación basada en tokens y autorización por roles;
- integración con el frontend.

## Documentación del producto

La definición funcional, el alcance, la arquitectura propuesta y la estimación de esfuerzo se encuentran en la [Propuesta de Producto](docs/Propuesta%20de%20Producto.docx).

Proyecto integrador de **Aplicaciones Interactivas**, desarrollado por un equipo de cinco integrantes.
