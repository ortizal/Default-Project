# Automatizaciones y notificaciones

## Conceptos

- **Plantilla**: mensaje reutilizable con variables (`{{paciente}}`, `{{servicio}}`, …).
- **Automatización**: regla `evento + plantilla + (minutosAntes | condicion)`. Se dispara con eventos de dominio.
- **Notificación**: instancia pagada de una automatización contra un paciente/cita concreta,
  con estado (`PENDIENTE`, `ENVIADA`, `ERROR`), intentos y error.

## Eventos disponibles

| Evento | Cuándo | Notas |
|---|---|---|
| `CITA_CREADA` | al crear una cita | confirmación inicial |
| `CITA_PROXIMA` | recordatorio programado | usa `minutosAntes` (scheduler) |
| `CITA_CONFIRMADA` | al confirmar | |
| `CITA_ATENDIDA` | al atender | |
| `CITA_CANCELADA` | al cancelar | |
| `NO_ASISTIO` | al marcar no-asistió | |

Un código de evento inválido se rechaza con `400 EVENTO_INVALIDO` (la lista anterior es la única válida).

## Motor (scheduler)

Cada minuto, un `@Scheduled`:

1. Busca `Notificacion` en `PENDIENTE` cuya hora `programada_at` ya pasó.
2. Compone el mensaje con las variables de la plantilla (clínica, paciente, cita).
3. Envía por OpenWA (sesión del tenant).
4. Marca `ENVIADA` (con éxito) o `ERROR` + `intentos+1` (reintentos).
5. Regenera la próxima `CITA_PROXIMA` de las citas futuras dentro de la ventana.

## Variables de plantilla

`{{clinica}}`, `{{direccion}}`, `{{telefono}}`, `{{paciente}}`, `{{odontologo}}`,
`{{servicio}}`, `{{fecha}}`, `{{hora}}`.

## Reglas de negocio

- Las notificaciones se disparan solo para citas del propio tenant.
- Mismas claves de plantilla: `CITA_PROXIMA` requiere `minutosAntes > 0`.
- Si la sesión de WhatsApp está desconectada, la notificación queda en `ERROR` sin romper el flujo.

## Frontend

- **Plantillas**: CRUD del texto con variables.
- **Automatizaciones**: CRUD de reglas (evento, plantilla, minutos antes, activa/desactivar).
- **Notificaciones**: cola generada con estado, teléfono y mensaje.