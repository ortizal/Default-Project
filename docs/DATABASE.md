# Base de datos

## Motor y migraciones

PostgreSQL 17, `ddl-auto=validate` + Flyway. El esquema se define **solo** con migraciones
versionadas en `backend/src/main/resources/db/migration/`. Nunca editar una migración aplicada:
crear `V{n+1}__descripcion.sql`.

| Migración | Contenido |
|---|---|
| `V1__schema_base.sql` | `tenants`, `roles`, `permisos`, `rol_permisos`, `usuarios`, `usuario_roles`, `auditoria` (catálogo de permisos + rol SUPER_ADMIN/ADMIN) |
| `V2__crm.sql` | `pacientes`, `odontologos`, `servicios` (Fase 2) |
| `V3__agenda.sql` | `horarios_odontologos`, `bloqueos_agenda`, `citas` + reglas de estado (Fase 3) |
| `V4__google.sql` | `google_accounts`, `google_calendars`, columnas de sincronización en `citas` (Fase 4) |
| `V5__whatsapp.sql` | `whatsapp_sesiones`, `conversaciones`, `mensajes` (Fase 5) |
| `V6__automatizaciones.sql` | `plantillas_mensajes`, `automatizaciones`, `notificaciones` (Fase 6) |
| `V7__reportes.sql` | vistas/índices para reportes (Fase 7) |
| `V8__agente.sql` | contexto del agente IA en `conversaciones` (Fase 8) |
| `V9__multitenant.sql` | `tenant_id` en las 16 tablas de negocio + seed del tenant `principal` (Fase 9) |

## Modelo (resumen)

- **Catálogos globales**: `tenants`, `roles`, `permisos`, `rol_permisos`, `usuario_roles`.
- **Negocio (con `tenant_id`)**: `usuarios`, `pacientes`, `odontologos`, `servicios`,
  `horarios_odontologos`, `bloqueos_agenda`, `citas`, `auditoria`, `google_accounts`,
  `google_calendars`, `whatsapp_sesiones`, `conversaciones`, `mensajes`, `plantillas_mensajes`,
  `automatizaciones`, `notificaciones`.

### Tablas clave

- `citas`: `estado` (`PENDIENTE/CONFIRMADA/REALIZADA/NO_ASISTIO/CANCELADA`), `version` (bloqueo
  optimista para mover), `confirmada_at`, `confirmation_source`, `cancelada_at`,
  `cancelada_motivo`, y columnas Google (`no_sync/pending/synced/error` con `sync_*`).
- `auditoria`: `datos_anteriores` y `datos_nuevos` en JSONB, `usuario_id`, `accion`, `modulo`,
  `entidad`, `ip`, `created_at`.
- `conversaciones`: `intencion` y `contexto_agente` (JSONB) del agente IA; `agente_activo`.
- `automatizaciones`: `evento` + `minutos_antes` + `plantilla_id` + `activa`.
- `notificaciones`: cola del scheduler (`programada_at`, `enviada_at`, `intentos`, `error`).

### Unicidades

- Globales (cruzan tenant): `usuarios.username`, `usuarios.email`, `pacientes.cedula`.
- Verificadas en aplicación con consultas nativas cuando el índice global aplica.

## Consultas de reportes

Son nativas (agrupaciones SQL) aisladas por tenant manualmente: reciben `tenant_id` desde
`TenantContext`. Evitan cargar entidades completas para KPIs.

## Backups

Ver `docker/backups/` (`backup.sh` con rotación, `restore.sh`). Backups diarios + semanales;
una restauración probada periódicamente es la única verificación válida.