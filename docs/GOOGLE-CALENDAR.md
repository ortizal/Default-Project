# Google Calendar

## OAuth 2.0

1. Crear credenciales OAuth en Google Cloud Console (tipo *Web application*).
2. Configurar `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` y `GOOGLE_REDIRECT_URI`
   (p. ej. `https://crm.miclina.com/api/v1/google/callback`). Añadir ese URI a los *Authorized redirect URIs* de la consola.
3. Una sola cuenta conectada a la vez (las credenciales se guardan en `google_accounts`).

Unico usuario (el que inició sesión) inicia la conexión desde el panel Google Calendar:

- `GET /Calendarapi/v1/google/connect` → `{authUrl}` (URL de autorización).
- El navegador autoriza y Google vuelve a `/api/v1/google/callback?code=…`, que intercambia el código por tokens y guarda la cuenta.
- `DELETE /api/v1/google/disconnect` limpia tokens y datos de sincronización.

## Calendarios

- `GET /api/v1/google/calendars` lista los calendarios de la cuenta.
- `POST /api/v1/google/calendars/{id}/select` fija el destino de los eventos (`seleccionado=true`).

## Sincronización

Eventos de dominio (`CitaCreadaEvent`, cita movida, cita cancelada) sincronizan automáticamente.
También `POST /api/v1/google/sync` sincroniza en masa por si algo quedó pendiente:

```json
{ "creados": 3, "actualizados": 1, "cancelados": 1, "errores": 0, "sincronizadas": 5 }
```

Estado por cita: `NO_SYNC` → `PENDING` → `SYNCED` | `ERROR` (con `sync_error`, `sync_attempts`,
`last_sync_at`). Si Google falla, la cita se guarda igual y queda `ERROR` para reintentar.

## Configuración en el frontend

Panel Google Calendar: estado de conexión (email, citas sincronizadas/pendientes/error), botón
“Conectar con Google” (abre la URL en pestaña aparte), selección de calendario, “Sincronizar citas”
manual y “Desconectar”. Si el backend no está configurado (`configurada=false`) muestra un aviso.

## Permisos

Solo quienes tengan `PERMISO_INTEGRACIONES` (SUPER_ADMIN, ADMIN) gestionan Google Calendar.