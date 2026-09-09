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
- [Seguridad y permisos](#seguridad-y-permisos)
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
| `POST` | `/api/auth/register` | Registra un paciente con BCrypt. | Datos de PacienteCreateRequest | `201`, `400`, `409` |
| `POST` | `/api/auth/login` | Autentica y emite un JWT para un usuario activo. | `email`, `password` | `200`, `400`, `401` |
| `GET` | `/api/doctores` | Lista médicos activos y permite combinar filtros. | Query opcional: `especialidadId`, `nombre` | `200`, `400` |
| `POST` | `/api/pacientes` | Registra un paciente con rol `PACIENTE`. | Datos personales, contacto y cobertura | `201`, `400`, `409` |
| `GET` | `/api/especialidades` | Consulta el catálogo público. | Sin cuerpo | `200` |
| `GET` | `/api/turnos/disponibles` | Consulta horarios sin datos de pacientes. | Query opcional: `doctorId` | `200` |
| `POST` | `/api/turnos/reservar` | Reserva para el paciente autenticado. | Doctor y horario | `201`, `400`, `401`, `403`, `404`, `409` |
| `POST` | `/api/turnos/sobreturno` | Crea un sobreturno en una agenda autorizada. | Paciente, doctor, horario y justificación | `201`, `400`, `401`, `403`, `404`, `409` |
| `PATCH` | `/api/turnos/{turnoId}/estado` | Cambia el estado con autorización e historial. | Estado destino y motivo opcional/obligatorio según transición | `200`, `400`, `401`, `403`, `404`, `409` |

### Contratos high level

#### Autenticación

```json
{
  "email": "usuario@turnera.com",
  "password": "ClaveSegura123"
}
```

Una autenticación exitosa conserva los datos del usuario y agrega `token` (JWT), `tokenType: Bearer` y `expiresIn` (segundos). El JWT contiene `sub=email`, `userId`, `roles` con prefijo `ROLE_`, `iat` y `exp`; se firma con HS256.

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

`telefono`, `obraSocial` y `numeroAfiliado` son opcionales; el resto de los campos son obligatorios y se validan con Bean Validation. La contraseña admite entre 8 caracteres y 72 bytes UTF-8, límite de BCrypt. El email se normaliza a minúsculas y se verifica sin distinguir mayúsculas entre las tres tablas de usuarios.

Request (`POST /api/auth/register`; `POST /api/pacientes` se conserva como alias público):

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

Las reservas usan `ReservaTurnoRequest`: doctor y horario; el paciente se obtiene del principal autenticado. Los sobreturnos usan `SobreturnoRequest`: paciente destinatario, horario y justificación obligatoria; el médico trabaja sobre su propia agenda y ADMIN debe indicar el doctor.

Un sobreturno requiere médico y paciente activos, inicio y fin futuros, fin posterior al inicio y una justificación de hasta 2000 caracteres no vacía. Puede ubicarse fuera del horario regular y no se crea para pacientes inactivos. Cada alta asigna `estado=RESERVADO`, `esSobreturned=true` y un registro en el historial con actor, rol, fecha y motivo. Las respuestas son DTOs sin entidades completas ni información clínica.

Ejemplo (`POST /api/turnos/sobreturno`, JWT de `MEDICO` o `ADMIN`):

```json
{
  "doctor": {"id": 1},
  "paciente": {"id": 2},
  "fechaHoraInicio": "2030-01-01T10:00:00",
  "fechaHoraFin": "2030-01-01T10:30:00",
  "justificacionSobreturned": "Control adicional indicado por el profesional"
}
```

Responde `201` con `TurnoResponse`. Devuelve `400` si faltan o son inválidos la justificación o las fechas, `401` si falta un JWT válido, `403` si el rol o la agenda no están autorizados y `404` si el médico o paciente no existen o están inactivos. Los ejemplos de autorización están en [Probar el flujo Bearer](#probar-el-flujo-bearer).

### Cambio de estado de un turno

Esta operación cumple el requisito de incluir al menos un endpoint `PUT`, `PATCH` o `DELETE`. Se usa `PATCH` porque modifica únicamente el estado y el motivo de un turno existente. Las operaciones de reserva y sobreturno continúan como `POST` porque crean nuevos recursos.

Endpoint: `PATCH /api/turnos/{turnoId}/estado`. El request usa `CambioEstadoTurnoRequest`:

```json
{
  "estadoDestino": "CONFIRMADO",
  "motivo": "Paciente confirmó la asistencia"
}
```

La matriz permitida es:

| Estado actual | Estados destino permitidos |
| --- | --- |
| `RESERVADO` | `CONFIRMADO`, `CANCELADO_PACIENTE`, `CANCELADO_MEDICO` |
| `CONFIRMADO` | `ATENDIDO`, `AUSENTE`, `CANCELADO_PACIENTE`, `CANCELADO_MEDICO` |
| `DISPONIBLE`, `ATENDIDO`, `AUSENTE`, `CANCELADO_PACIENTE`, `CANCELADO_MEDICO` | Ninguno |

`PACIENTE` solo puede cancelar sus propios turnos como `CANCELADO_PACIENTE`; `MEDICO` puede modificar turnos de su agenda y `ADMIN` cualquier turno. El motivo es obligatorio para `AUSENTE` y cualquier cancelación. Todo cambio responde `200` con `TurnoResponse` y agrega al historial el estado anterior, estado nuevo, fecha, actor, rol y motivo. Devuelve `400` por request inválido, `401` sin JWT, `403` sin permisos, `404` si el turno no existe y `409` si la transición no está en la matriz.

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

3. Revisar las credenciales de desarrollo y completar `JWT_SECRET` en `.env` según [Seguridad y permisos](#seguridad-y-permisos). Luego levantar los servicios:

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

## Seguridad y permisos

Se sigue el flujo de Clase 05: `AuthController → AuthenticationService → AuthenticationManager → UserDetailsService → UsuarioRepository`. El registro delega la construcción, validación y persistencia en `PacienteService` dentro de una transacción. `BaseUsuario` implementa `UserDetails` para pacientes, médicos y administradores, sin cambiar la estructura de las tablas. `UsuarioRepository` reúne las consultas de las tres tablas y rechaza identidades ambiguas.

`JwtUtil` emite y valida; `JwtFilter` corre antes de `UsernamePasswordAuthenticationFilter`. Cada request verifica firma, vencimiento, identidad y rol contra la cuenta actual. Una cuenta desactivada o un rol modificado invalidan el uso del token anterior. CSRF se deshabilita porque la API usa únicamente Bearer enviado explícitamente en el header. No usa sesiones, form login, autenticación Basic ni cookies para autenticar.

### Configuración

- `jwt.secret` / `JWT_SECRET`: obligatorio, Base64 de al menos 32 bytes aleatorios. No tiene valor por defecto.
- `jwt.expiration` / `JWT_EXPIRATION`: duración en milisegundos; por defecto `3600000`, mínimo `1000`.
- `.env` está ignorado por Git y Docker Compose lo lee. Maven no carga `.env`: usar variables del proceso o propiedades externas.

Para ejecutar desde PowerShell, generar una clave en la sesión y mantenerla mientras se reinicia el backend:

```powershell
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:JWT_EXPIRATION = "3600000"
.\mvnw.cmd spring-boot:run
```

Para Docker, guardar una clave generada de la misma forma como `JWT_SECRET=<valor Base64>` en el archivo local `.env`. No publicar ese archivo. Si cambia la clave, los JWT anteriores dejan de validar. La clave versionada en `src/test/resources/application-test.properties` es pública y exclusiva de pruebas.

### Matriz de acceso

| Método y ruta | Acceso |
| --- | --- |
| `/api/auth/**` | Público: registro y login |
| `POST /api/pacientes` | Público: alias del registro, siempre crea PACIENTE |
| `GET /api/doctores`, `GET /api/especialidades` | Público: catálogo para elegir profesional |
| `POST /api/doctores`, `POST /api/especialidades` | ADMIN: altas administrativas |
| `GET /api/turnos/disponibles` | Público: horarios e id del médico, sin datos de pacientes |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Público: documentación de la API |
| `POST /api/turnos/reservar` | PACIENTE: reserva exclusivamente para sí mismo |
| `POST /api/turnos/sobreturno` | MEDICO: su propia agenda; ADMIN: cualquier médico activo |
| `/api/admin/**` | ADMIN; prefijo reservado para futuras operaciones administrativas |
| Cualquier otra ruta | Requiere autenticación |

Sin token en una operación protegida, o con token inválido/vencido, se devuelve `401`. Un usuario autenticado sin permiso recibe `403`. Ambos usan `ApiErrorResponse`; los errores del filtro no pasan por el controller. Un header Authorization inválido también se rechaza en rutas públicas; para usarlas anónimamente, omitir el header.

### Probar el flujo Bearer

1. Registrar con `POST /api/auth/register` usando el JSON de registro anterior.
2. Llamar a `POST /api/auth/login` con email y contraseña.
3. Copiar `token`. En Swagger UI, pulsar **Authorize** y pegar solo el JWT.
4. En curl/Postman, enviar `Authorization: Bearer <token>`.

Reserva (`POST /api/turnos/reservar`, JWT de PACIENTE):

```json
{
  "doctor": {"id": 1},
  "fechaHoraInicio": "2030-01-01T10:00:00"
}
```

Sobreturno (`POST /api/turnos/sobreturno`, JWT de MEDICO o ADMIN):

```json
{
  "doctor": {"id": 1},
  "paciente": {"id": 2},
  "fechaHoraInicio": "2030-01-01T10:00:00",
  "fechaHoraFin": "2030-01-01T10:30:00",
  "justificacionSobreturned": "Control adicional"
}
```

El médico puede omitir `doctor`; el administrador debe indicarlo. `paciente` es el destinatario del sobreturno, no la identidad del actor. Ninguna operación confía en `usuarioId` o `rol` enviados por el cliente. Las reservas no aceptan identidad del paciente ni estado: el servidor asigna el principal y `RESERVADO`. Las respuestas son DTOs sin contraseñas ni relaciones clínicas.

Estos contratos reemplazan la recepción de entidades completas de turnos; los clientes deben usar los JSON anteriores. La duración se calcula desde la agenda del médico y las reservas superpuestas se rechazan.

### Material de referencia

Base: `Clase05 - Material/02-PasoAPaso-Autenticacion-Clase05 (1).txt`, `03-ResumenPasosSpringSecurity (1).txt` y el ejemplo `e-commerce-auth` (SecurityConfig, Usuario, AuthenticationService, JwtUtil y JwtFilter).

Se adapta ese flujo a Spring Boot 4.1 / Spring Security 7, usando la API vigente de [JJWT 0.13.0](https://github.com/jwtk/jjwt#installation) y [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html). Se agregan las exigencias de la [tarjeta SECURITY](https://trello.com/c/TQvx0pnh): stateless explícito, secreto externo, identidad desde el principal y errores JSON.

## Pruebas

Windows:

```powershell
.\mvnw.cmd test
```

Linux o macOS:

```bash
./mvnw test
```

La suite incluye pruebas unitarias, `spring-security-test`, MockMvc y pruebas HTTP reales con `HttpClient` contra Tomcat en un puerto aleatorio. H2 usa una base aislada por contexto. Se verifican BCrypt y persistencia, JWT firmado, credenciales incorrectas, firma alterada/ajena, expiración, permisos 401/403, cuentas desactivadas, cambios de rol, suplantación de paciente/médico, transiciones de estado con historial, ausencia de cookies de sesión y el contrato OpenAPI, incluido el endpoint `PATCH`. Las pruebas no requieren configurar un secreto real ni conectarse a MySQL.

## Arquitectura y tecnologías

| Área | Tecnología |
| --- | --- |
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.1, Spring MVC |
| Persistencia | Spring Data JPA, Hibernate |
| Base de datos | MySQL 8.4; H2 para pruebas |
| Contratos API | OpenAPI 3.1, Springdoc, Swagger UI |
| Validación | Jakarta Validation |
| Seguridad | Spring Security, BCrypt, JJWT 0.13.0 |
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

- consulta y cancelación de turnos;
- configuración y consulta de disponibilidad;
- administración completa de estados;
- prevención de reservas superpuestas;
- integración con el frontend.

## Documentación del producto

La definición funcional, el alcance, la arquitectura propuesta y la estimación de esfuerzo se encuentran en la [Propuesta de Producto](docs/Propuesta%20de%20Producto.docx).

Proyecto integrador de **Aplicaciones Interactivas**, desarrollado por un equipo de cinco integrantes.
