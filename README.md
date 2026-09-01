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

## 🔐 Autenticación

### `POST /api/auth/login`

Valida las credenciales de pacientes, médicos y administradores activos. Las contraseñas almacenadas deben estar codificadas con **BCrypt**; nunca se devuelven en la respuesta.

**Request**

```json
{
  "email": "usuario@turnera.com",
  "password": "ClaveSegura123"
}
```

**Response `200 OK`**

```json
{
  "id": 1,
  "nombre": "Pablo",
  "apellido": "Paciente",
  "email": "usuario@turnera.com",
  "rol": "PACIENTE"
}
```

| Código | Motivo |
| --- | --- |
| `200 OK` | Credenciales válidas. |
| `400 Bad Request` | Email con formato inválido o campos obligatorios vacíos. |
| `401 Unauthorized` | Email o contraseña incorrectos, cuenta inactiva o email ambiguo entre tipos de usuario. |

Este endpoint verifica identidad y rol. La emisión de tokens o la autorización de los demás endpoints queda fuera de esta entrega.

### Pruebas del login

```bash
./mvnw test
```

Las pruebas incluyen casos unitarios, una prueba web del controller y un flujo integrado con `@SpringBootTest`: inicia Spring, usa MockMvc, persiste un paciente con BCrypt mediante JPA en una base H2 aislada y ejecuta el login completo sin mocks.
