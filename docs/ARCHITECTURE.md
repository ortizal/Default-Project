# Arquitectura

## Visión general

CRM odontológico multi-tenant con agenda, citas, WhatsApp (OpenWA), Google Calendar,
automatizaciones y agente de IA conversacional.

```
                ┌────────────────────────────┐
                │  Frontend Angular 22       │
                │  (standalone, lazy modules)│
                └─────────────┬──────────────┘
                              │  /api/v1 (JWT Bearer)
                              ▼
                ┌────────────────────────────┐
                │  Spring Boot 3 (Java 21)   │
                │  spring-web + security     │
                └──────┬─────────────┬───────┘
                       │             │
              ┌────────▼─────┐  ┌────▼─────────────┐
              │ PostgreSQL 17│  │    Integraciones │
              │ Flyway +     │  │ • OpenWA (WS)    │
              │ multi-tenant │  │ • Google Calendar│
              └──────────────┘  └──────────────────┘
```

## Backend (`/backend`)

Spring Boot 3.3 / Java 21, organizado por dominio (`org.dentalcrm.domain.*`, `org.dentalcrm.web.*`).
`ddl-auto=validate` + Flyway: el esquema existe solo por migraciones versionadas.

Capas por dominio:

- **Controllers** (`web/…/*Controller.java`): solo enrutado, DTOs y `@PreAuthorize`.
- **Services** (`web/…/*Service.java`): reglas de negocio, transacciones, auditoría, eventos de dominio.
- **Domain** (`domain/*`): entidades JPA. Las 16 tablas de negocio usan `@TenantId`.
- **DTOs** (`web/**/dto/*.java`): `record` con respuestas `from(Entity)`.

### Módulos

| Módulo | Responsabilidad |
|---|---|
| `paciente` | CRUD, cédula única global (consulta nativa), desactivación |
| `odontologo` | CRUD, activos para selects |
| `servicio` | CRUD, duración y precio; la duración define la cita |
| `horario` | Horarios semanales por odontólogo (1=Lun … 7=Dom), intervalo |
| `agenda` | Citas del día, disponibilidad (slots libres), bloqueos |
| `cita` | CRUD + transiciones (`confirmar/atender/no-asistio/cancelar`), filtros con `Specification` |
| `automatizacion` | Plantillas, automatizaciones por evento, notificaciones programadas |
| `whatsapp` | Sesiones OpenWA, webhook, conversaciones y mensajes |
| `agente` | Agente conversacional (intención + tools), supervisión/transferencia |
| `google` | OAuth 2.0, calendarios, sincronización de eventos |
| `reporte` | Dashboard y estadísticas (consultas nativas aisladas por tenant) |
| `usuario` | CRUD con roles; username/email únicos globales |
| `auditoria` | Registro de acciones con antes/después (JSONB) |

Paquetes transversales:

- `web.security` — filtro JWT, `SecurityConfig` (stateless), `@PreAuthorize` por permiso.
- `multitenant` — `TenantContext` (ThreadLocal), `CurrentTenantIdentifierResolver`, `@TenantId`.
- `service.AuditService` — auditoría central en JSONB.
- `service.EventPublisher` — eventos de dominio (`CitaCreadaEvent`, etc.) para sincronizar Google y disparar automatizaciones.
- `web.cita.CitaService.listar` — usa `JpaSpecificationExecutor<Cita>` (Criteria API) para evitar el bug de PostgreSQL con `(:param IS NULL OR …)` en JPQL con `LocalDate` (Hibernate 6.6 enlazaba nulls como `bytea`).

## Multi-tenancy

- Una fila por clínica en `tenants` (por ahora `principal`, id 1).
- Las tablas de negocio tienen `tenant_id` + `@TenantId`; Hibernate añade el filtro en cada consulta automáticamente.
- El claim `tenantId` del JWT fija el `TenantContext` por petición (se limpia en un `finally`).
- El webhook de WhatsApp resuelve el tenant por la sesión receptora.
- Unicidades que cruzan tenant (username, email, cédula) se validan con consultas nativas sin filtro de tenant.

## Autenticación y autorización

- `POST /api/v1/auth/login` → JWT en respuesta `{token, tokenType, expiresInMs, username, roles}`.
- Cada petición protegida requiere `Authorization: Bearer <token>`.
- RBAC: 4 roles (SUPER_ADMIN, ADMIN, ODONTOLOGO, RECEPCION) y 21 permisos; los roles se resuelven en el login y se embeben en el token. Los endpoints autorizan por permiso (`PERMISO_*`).

## Frontend (`/frontend`)

Angular 22 standalone, TypeScript estricto, build de producción con lazy loading por módulo
(sin prefijo `-component`; `selectors` en minúscula).

- `src/app/app.routes.ts` — rutas lazy.
- `src/app/layout/layout.ts` — shell con sidebar y topbar; los ítems se agrupan por sección.
- `src/app/core/` — `Api` (URL base desde `public/assets/config.json`, defecto `/api/v1`),
  `AuthService` (token/roles/logout), `auth.interceptor` (añade Bearer, redirige en 401), `guards`.
- Módulos: login, dashboard, pacientes, odontólogos, servicios, horarios, agenda
  (Día/Semana/Mes), citas, whatsapp (sesiones + inbox), plantillas, automatizaciones,
  notificaciones, google calendar, reportes, agente, usuarios, auditoría.
- Dev: `npx ng serve --proxy-config proxy.conf.json` (proxy `/api` → `localhost:8080`).
- Prod: build estático servido por nginx (ver `frontend/Dockerfile`) con `/api` proxy al backend.

## Flujo de una cita

1. **Agenda**: el agente WhatsApp consulta disponibilidad por servicio/doctor/fecha/hora.
2. **Creación**: `POST /api/v1/citas` valida doble reserva, horario activo y bloqueos; publica `CitaCreadaEvent`.
3. **Sincronización Google**: el event listener crea/actualiza/elimina el evento del calendario seleccionado (estado por cita: `NO_SYNC/PENDING/SYNCED/ERROR`).
4. **Confirmación**: `POST /{id}/confirmar` publica `CitaConfirmadaEvent` → automatización envía WhatsApp de confirmación.
5. **Recordatorio**: automatización `CITA_PROXIMA` con `minutosAntes` genera `Notificacion` pagada por el scheduler.
6. **Cierre**: `atender` → `REALIZADA`; o `no-asistio` → `NO_ASISTIO` (dispara `NO_ASISTIO`).

## Scheduler

`@Scheduled` (Spring) ejecuta el motor de automatizaciones cada minuto: busca `Notificacion` en
estado `PENDIENTE` cuya hora ya llegó, la envía por OpenWA y registra resultado (`ENVIADA`/`ERROR`).

## Observabilidad

- `/actuator/health`, `/actuator/info`, `/actuator/metrics` (expuestos).
- Swagger UI en `/api/v1/swagger-ui.html` (OpenAPI 3, `/api/v1/api-docs`).
- `server.error.include-message=never` e `include-stacktrace=never`: los errores no exponen trazas;
  el `GlobalExceptionHandler` devuelve el formato plano del plan (timestamp, status, code, message, path).