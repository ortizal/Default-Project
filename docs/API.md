# API REST

Base URL: `/api/v1` · Swagger UI: `GET /api/v1/swagger-ui.html` · OpenAPI JSON: `GET /api/v1/api-docs`.

Autenticación: `Authorization: Bearer <token>` (JWT). Públicos: `/auth/login`, `/auth/refresh`,
`/webhooks/**`, `/google/callback`, Swagger/OpenAPI y `/actuator/health|info`. El resto responde
`403` sin token válido.

## Formato de error

Todos los errores controlados usan el mismo formato (Global Exception Handler):

```json
{
  "timestamp": "2026-09-09T18:30:00",
  "status": 400,
  "code": "APPOINTMENT_NOT_AVAILABLE",
  "message": "El horario seleccionado ya no está disponible",
  "path": "/api/v1/citas"
}
```

Códigos típicos: `HORA_PASADA`, `HORARIO_NO_DISPONIBLE`, `DOBLE_RESERVA`, `BLOQUEADO`,
`CITA_NO_ENCONTRADA`, `ESTADO_INVALIDO`, `CITA_NO_CANCELABLE`, `CEDULA_DUPLICADA`,
`USERNAME_EN_USO`, `GOOGLE_NO_CONFIGURADA`, `EVENTO_INVALIDO`, `PARAMETRO_INVALIDO`, `NO_AUTORIZADO`.

## Auth

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` | `{username, password}` → `{token, tokenType, expiresInMs, username, roles}` |
| POST | `/auth/refresh` | Renueva el token con el refresh token vigente |

## Catálogos

### Pacientes

| Método | Ruta |
|---|---|
| GET | `/pacientes?q=&estado=&page=&size=` (paginado) |
| POST | `/pacientes` — `{cedula, nombres, apellidos, telefono, email, fechaNacimiento, direccion, observaciones}` |
| GET | `/pacientes/{id}` |
| PUT | `/pacientes/{id}` |
| DELETE | `/pacientes/{id}` (desactiva; la cédula es única nacional) |

### Odontólogos

`GET /odontologos` · `GET /odontologos/activos` · `POST /odontologos` · `GET/PUT /odontologos/{id}` ·
`DELETE /odontologos/{id}` (desactiva).

### Servicios

`GET /servicios` · `GET /servicios/activos` · `POST /servicios` (requiere `duracionMinutos`, `precio`) ·
`GET/PUT /servicios/{id}` · `DELETE /servicios/{id}`.

### Horarios

| Método | Ruta | Notas |
|---|---|---|
| GET | `/horarios?odontologoId=` | Filtrados por tenant y odontólogo |
| POST | `/horarios` | `{odontologoId, diaSemana(1=Lun…7=Dom), horaInicio, horaFin, intervaloMinutos}` |
| PUT | `/horarios/{id}` | Mover/editar horario |
| DELETE | `/horarios/{id}` | |

## Agenda

| Método | Ruta | Notas |
|---|---|---|
| GET | `/agenda?odontologoId=&fecha=` | Citas del día ordenadas por hora |
| GET | `/agenda/disponibilidad?odontologoId=&fecha=&servicioId=&duracion=` | Slots libres `[{horaInicio, horaFin}]` |
| GET | `/agenda/bloqueos?odontologoId=&fecha=` o `&desde=&hasta=` | Bloqueos por día o rango |
| POST | `/agenda/bloqueos` | `{odontologoId, fecha, horaInicio?, horaFin?, motivo?}` |
| DELETE | `/agenda/bloqueos/{id}` | |

## Citas

| Método | Ruta | Notas |
|---|---|---|
| GET | `/citas?estado=&doctorId=&pacienteId=&fecha=&desde=&hasta=&page=&size=` | Paginado; filtros combinables vía `Specification` |
| GET | `/citas/{id}` | |
| POST | `/citas` | `{pacienteId, doctorId, servicioId, fecha, horaInicio, observaciones?}` — valida doble reserva, horario y bloqueos |
| PUT | `/citas/{id}` | Reagendar/mover con bloqueo optimista (`@Version`) |
| POST | `/citas/{id}/confirmar` | |
| POST | `/citas/{id}/atender` | |
| POST | `/citas/{id}/cancelar` | `{motivo?}` |
| POST | `/citas/{id}/no-asistio` | |

Estados: `PENDIENTE` → `CONFIRMADA` → `REALIZADA` | `NO_ASISTIO` | `CANCELADA`.

## WhatsApp / OpenWA

| Método | Ruta | Notas |
|---|---|---|
| POST | `/webhooks/whatsapp` | Público (lo llama OpenWA); mensaje + webhook automático del agente |
| GET | `/whatsapp/sesiones` | |
| POST | `/whatsapp/sesiones` | `{sesionId, nombre?}` |
| GET | `/whatsapp/sesiones/{id}` | |
| POST | `/whatsapp/sesiones/{id}/conectar` | Devuelve QR (o `PENDIENTE_QR`) |
| POST | `/whatsapp/sesiones/{id}/desconectar` | |
| DELETE | `/whatsapp/sesiones/{id}` | |
| GET | `/whatsapp/conversaciones?estado=&sesionId=` | Estado: `BOT`, `ATENCION_HUMANA` |
| GET | `/whatsapp/conversaciones/{id}` | Detalle con mensajes |
| GET | `/whatsapp/conversaciones/{id}/mensajes` | |
| POST | `/whatsapp/conversaciones/{id}/mensajes` | `{texto}` (envía por OpenWA) |
| POST | `/whatsapp/conversaciones/{id}/estado` | `{estado}` |

## Plantillas y automatizaciones

| Método | Ruta |
|---|---|
| GET | `/plantillas` · POST `/plantillas` · PUT/DELETE `/plantillas/{id}` |
| GET | `/automatizaciones` · POST `/automatizaciones` · PUT/DELETE `/automatizaciones/{id}` |
| POST | `/automatizaciones/{id}/activar` | POST `/automatizaciones/{id}/desactivar` |
| GET | `/automatizaciones/notificaciones?limite=` | Cola de notificaciones generadas |

Eventos válidos: `CITA_CREADA`, `CITA_PROXIMA`, `CITA_CONFIRMADA`, `CITA_ATENDIDA`,
`CITA_CANCELADA`, `NO_ASISTIO`. Variables de plantilla: `{{paciente}}`, `{{odontologo}}`,
`{{servicio}}`, `{{fecha}}`, `{{hora}}`, `{{clinica}}`, `{{direccion}}`, `{{telefono}}`.

## Agente IA

| Método | Ruta | Notas |
|---|---|---|
| GET | `/agente/conversaciones` | Supervisión (intención, contexto, agente activo) |
| POST | `/agente/conversaciones/{id}/transferir` | Pasa a `ATENCION_HUMANA` y silencia al agente |
| POST | `/agente/conversaciones/{id}/agente` | `{activo: bool}` — reactivar/desactivar el bot |

## Google Calendar

| Método | Ruta |
|---|---|
| GET | `/google/status` |
| GET | `/google/connect` → `{authUrl}` |
| GET | `/google/callback?code=` | Público (OAuth) |
| GET | `/google/calendars` |
| POST | `/google/calendars/{id}/select` |
| POST | `/google/sync` → `{creados, actualizados, cancelados, errores, sincronizadas}` |
| DELETE | `/google/disconnect` |

## Reportes

| Método | Ruta | Notas |
|---|---|---|
| GET | `/reportes/dashboard` | KPIs del día/mes |
| GET | `/reportes/estadisticas` | Series por día + top odontólogos, servicios, estados y fuentes (`confirmaciones/cancelaciones/noAsistencia` son listas `{fecha,total}`) |

## Usuarios y auditoría

| Método | Ruta |
|---|---|
| GET | `/usuarios?q=&page=&size=` · POST `/usuarios` · GET/PUT `/usuarios/{id}` |
| GET | `/auditoria?q=&modulo=&page=&size=` |

`UsuarioUpdateRequest`: `{email, nombres, apellidos, telefono, estado, nuevaPassword?, roles[]}`.
Módulos de auditoría: `PACIENTES`, `ODONTOLOGOS`, `SERVICIOS`, `HORARIOS`, `AGENDA`, `CITAS`,
`USUARIOS`, `WHATSAPP`, `AUTOMATIZACIONES`, `AGENTE_IA`, `GOOGLE_CALENDAR`.