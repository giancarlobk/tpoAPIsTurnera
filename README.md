# 🩺 Turnero Médico

> Aplicación web para administrar profesionales, disponibilidad y turnos médicos desde una única fuente de información.

Proyecto integrador de **Aplicaciones Interactivas**, desarrollado por un equipo de cinco integrantes.

## 🎯 Objetivo

Digitalizar el circuito principal de turnos de un consultorio y ofrecer una experiencia simple para pacientes, profesionales y administradores.

## ✨ Alcance del MVP

- 👤 Registro e inicio de sesión de usuarios.
- 🧑‍⚕️ Gestión de especialidades y profesionales.
- 📅 Configuración y consulta de disponibilidad.
- ✅ Reserva, consulta y cancelación de turnos.
- 🔄 Administración de estados de los turnos.
- 🔒 Prevención de reservas superpuestas.

## 🧰 Tecnologías previstas

| Capa | Tecnología |
| --- | --- |
| 🖥️ Frontend | React con JavaScript; Vite es opcional |
| ⚙️ Backend | Java 17, Spring Boot, API REST y Spring Data JPA |
| 🗄️ Base de datos | MySQL; H2 para pruebas o desarrollo local |
| 🔗 Comunicación | HTTP/HTTPS y JSON |

## 🚧 Estado del proyecto

El proyecto se encuentra **en desarrollo**.

Todavía están pendientes:

- El contrato definitivo de la API.
- La configuración completa de los entornos.
- El paso a paso de instalación y ejecución.
- La integración del frontend con el backend.

## 📚 Documentación

La definición del producto, el alcance, la arquitectura propuesta y la estimación de esfuerzo se encuentran en la [Propuesta de Producto](docs/Propuesta%20de%20Producto.docx).

## 🧑‍⚕️ Consulta de médicos

### `GET /api/doctores`

Devuelve médicos activos ordenados por apellido, nombre e identificador. Los filtros son opcionales y se pueden combinar.

| Parámetro | Tipo | Validación | Descripción |
| --- | --- | --- | --- |
| `especialidadId` | `Long` | Mayor que cero | Filtra por el identificador de la especialidad. |
| `nombre` | `String` | No vacío; máximo 100 caracteres | Busca una coincidencia parcial sin distinguir mayúsculas. |

**Ejemplos**

```http
GET /api/doctores
GET /api/doctores?especialidadId=1
GET /api/doctores?nombre=ana
GET /api/doctores?especialidadId=1&nombre=ana
```

**Response `200 OK`**

```json
[
  {
    "id": 10,
    "nombre": "Ana",
    "apellido": "Alvarez",
    "matriculaNacional": "MN-100",
    "especialidadId": 1,
    "especialidadNombre": "Cardiología"
  }
]
```

Si no existen coincidencias, responde `200 OK` con `[]`. Los filtros inválidos responden `400 Bad Request`. El DTO no expone contraseñas, horarios completos ni relaciones JPA.

### Pruebas de médicos

```bash
./mvnw test
```

La suite incluye pruebas unitarias de filtros y mapeo, además de integración con Spring Boot, MockMvc, JPA y H2.
