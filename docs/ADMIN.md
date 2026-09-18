# Manual básico de administración

## Usuarios iniciales

| Usuario | Contraseña | Rol | Notas |
|---|---|---|---|
| `admin` | `admin123` | SUPER_ADMIN | **cambiar/desactivar al desplegar**. Se crea por seed de migración (sembrado solo en dev). |
| `administrador` | `admin123` | ADMIN | etc. — se crean desde el panel Usuarios |

## Roles y permisos

| Rol | Permisos relevantes |
|---|---|
| SUPER_ADMIN | todos (incluye `INTEGRACIONES`, `AUDITORIA_READ`, `CONFIGURACION`) |
| ADMIN | operativos + `USUARIOS_READ`, `AUDITORIA_READ` |
| ODONTOLOGO | agenda, pacientes, citas (lectura/escritura de lo suyo), horarios |
| RECEPCION | pacientes, agenda, citas, WhatsApp, agente IA |

Los permisos se asignan por rol en `roles → permisos` (catálogo global). El frontend filtra el
menú por autenticación; el backend valida cada endpoint por permiso.

## Calendario operativo

1. **Horarios**: asignar la semana por odontólogo (día, hora inicio/fin, intervalo).
2. **Servicios**: nombre + duración (define el slot de la cita) + precio.
3. **Agenda**: ver el día/semana/mes, bloquear horas y crear citas desde los slots libres.
4. **Citas**: filtrar, confirmar, atender, cancelar o marcar no-asistió.
5. **WhatsApp**: escanear el QR de la sesión una vez; el inbox atiende los chats del agente.
6. **Automatizaciones**: plantillas + reglas por evento; el scheduler envía los recordatorios.

## Google Calendar

Panel Google Calendar → "Conectar con Google" (una cuenta/token a la vez), elegir calendario,
"Sincronizar citas". Cualquier cita creada/movida/cancelada se refleja; si una cita muestra error
de sync, reintentar con sincronización manual.

## Agente IA

- En el inbox, el bot responde automáticamente cuando `agenteActivo=true` (flujo: quién → servicio
  → doctor → fecha → hora; registra al paciente por cédula o transfiere a humano).
- "Transferir" pasa la conversación a atención humana (se silencia el bot).
- Supervisión en el panel Agente: intención, contexto y estado de cada conversación.

## Auditoría

El registro (quién, qué, antes/después, IP, cuándo) se consulta en el panel Auditoría; filtrar por
búsqueda o módulo. Solo SUPER_ADMIN/ADMIN lo ven.

## Mantenimiento

- **Backups**: `docker/backups/backup.sh` (diario, rotación por `RETENTION_DAYS`); probar
  `restore.sh` periódicamente.
- **Salud**: `GET /actuator/health` en el backend (o `http://<host>/api/v1/../..` vía nginx).
- **Cambios de esquema**: agregar una migración Flyway nueva; nunca editar una aplicada.
- **Secreto**: rotate `JWT_SECRET` con cuidado (invalida sesiones activas) y rota el token bearer del webhook.