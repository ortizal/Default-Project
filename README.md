# CRM Odontológico

CRM odontológico multi-tenant con gestión de pacientes, agenda, citas, WhatsApp (OpenWA), Google Calendar, reportes y agente de IA conversacional.

## Stack

- Backend: Spring Boot 3 (Java 21), Spring Security + JWT, Spring Data JPA, Flyway
- Base de datos: PostgreSQL 17
- Frontend: Angular 22 (standalone, listo)
- Infraestructura: Docker Compose

## Estado actual (Fase 10 - Frontend Angular)

- [x] **Fase 1 - Arquitectura**: Spring Boot, JWT + RBAC, Flyway, errores globales, Swagger, Docker Compose
- [x] **Fase 2 - CRM**: pacientes, odontólogos, servicios, usuarios, auditoría (JSONB), RBAC verificado
- [x] **Fase 3 - Agenda**: horarios por odontólogo (Lun–Dom, mañana/tarde, intervalo), bloqueos, disponibilidad por servicio
- [x] Citas: crear (sin doble reserva), confirmar/atender/no-asistió/cancelar, reagendar, mover (PUT) con bloqueo optimista
- [x] Reglas de negocio: fecha/hora no pasadas, dentro del horario activo, fuera de bloqueos, duración = duración del servicio
- [x] Smoke test E2E verde contra PostgreSQL (12 pasos) + 42 tests unitarios

### Fase 4 - Google Calendar

- [x] OAuth 2.0 con Google (`GET /api/v1/google/connect` y `callback`), una cuenta conectada a la vez; tokens en BD
- [x] Calendarios del usuario (`GET /api/v1/google/calendars`), selección de calendario destino (`POST /calendars/{id}/select`)
- [x] Sincronización automática por eventos de dominio: crear cita → crea evento, mover/reagendar → actualiza, cancelar → elimina
- [x] Sincronización masiva manual (`POST /api/v1/google/sync`) con contadores por estado
- [x] Estado por cita: `NO_SYNC`, `PENDING`, `SYNCED`, `ERROR` (con `sync_error`, `sync_attempts` y `last_sync_at`)
- [x] Robustez: si Google falla la cita se guarda igualmente, se registra el error y queda `ERROR` para reintentar
- [x] Desconexión (`DELETE /api/v1/google/disconnect`): limpia tokens y datos de Google en las citas
- [x] Acceso restringido a usuarios con permiso `INTEGRACIONES` (SUPER_ADMIN y ADMIN)
- [x] Smoke test E2E (12 pasos) + servidor mock de Google

### Fase 5 - WhatsApp / OpenWA

- [x] Sesiones de WhatsApp (`GET/POST /api/v1/whatsapp/sesiones`, `conectar`, `desconectar`) con QR y estados `DESCONECTADA|CONECTANDO|CONECTADA|ERROR`
- [x] Abstracción de proveedor `MessagingProvider` + adaptador `OpenWAProvider` (URL y API key configurables); la lógica de negocio no depende de OpenWA
- [x] Webhook público `POST /api/v1/webhooks/whatsapp`: valida evento, identifica sesión y número, busca paciente por teléfono, crea/actualiza conversación y guarda el mensaje
- [x] Inbox: listar conversaciones (`GET /conversaciones`), detalle con historial (`GET /conversaciones/{id}`), estados `BOT|ATENCION_HUMANA|ATENDIDA|CERRADA` y cambio de estado
- [x] Envío de mensajes manuales (`POST /conversaciones/{id}/mensajes`): si OpenWA falla el mensaje queda `ERROR` (no se pierde) y permite reintento
- [x] Idempotencia: dos mensajes del mismo número sin duplicar la conversación
- [x] Permisos `WHATSAPP_READ`/`WHATSAPP_WRITE` (SUPER_ADMIN, ADMIN y RECEPCION); ODONTOLOGO no accede
- [x] Smoke test E2E (11 pasos) + servidor mock de OpenWA (42 tests unitarios en verde)

Pendiente: notificaciones/plantillas/automatizaciones (Fase 6).

### Fase 6 - Automatizaciones

- [x] Plantillas de mensajes con variables (`{{nombre}}`, `{{fecha}}`, `{{doctor}}`, `{{servicio}}`, `{{clinica}}`, etc.) y CRUD (`GET/POST/PUT/DELETE /api/v1/plantillas`)
- [x] Automatizaciones por evento (`CITA_CREADA|CITA_PROXIMA|CITA_CONFIRMADA|CITA_CANCELADA|CITA_ATENDIDA|NO_ASISTIO`) con `minutos_antes`; CRUD, activar/desactivar (`/api/v1/automatizaciones`)
- [x] Motor de notificaciones: al crear/confirmar/cancelar/atender/no-asistir una cita genera notificaciones (listeners de eventos de dominio) con reintentos (máx 3) y errores trazables
- [x] Scheduler cada 60s: envía notificaciones pendientes por WhatsApp, repara las colgadas (`ENVIANDO` > 10 min vuelven a `PENDIENTE`)
- [x] envío con sesión de WhatsApp conectada; si no hay sesión o el proveedor falla, reintenta y luego pasa a `ERROR`
- [x] Variables de clínica configurables (`app.clinica.nombre/direccion/telefono` en `.env`)
- [x] Bot simple: respuesta `1` confirma la cita por WhatsApp (`confirmation_source=WHATSAPP`), respuesta `3` la cancela
- [x] Permiso `AUTOMATIZACIONES` (SUPER_ADMIN y ADMIN); ODONTOLOGO recibe 403
- [x] Smoke test E2E (8 pasos) + 68 tests unitarios en verde

### Fase 7 - Reportes

- [x] Dashboard (`GET /api/v1/reportes/dashboard`): citas de hoy, pendientes/confirmadas/canceladas de hoy, no asistieron hoy, pacientes nuevos del mes, conversaciones abiertas, mensajes enviados/con error, notificaciones enviadas/con error
- [x] Estadísticas por rango (`GET /api/v1/reportes/estadisticas?desde&hasta&limite`, rango por defecto últimos 30 días): citas por día, citas por odontólogo, servicios más solicitados, conteo por estado, confirmaciones (serie diaria y por fuente PANEL/WHATSAPP), cancelaciones y no asistencia (serie diaria)
- [x] Permiso `REPORTES` otorgado también a RECEPCION; ODONTOLOGO recibe 403
- [x] Índices de apoyo para las consultas agregadas y rango inválido devuelve 400
- [x] Smoke test E2E (8 pasos) + 73 tests unitarios en verde

### Fase 8 - IA (Agente conversacional)

- [x] `Intencion` clasificada por el mensaje (SALUDO, AGENDAR_CITA, CONFIRMAR_CITA, CANCELAR_CITA, PREGUNTAR_DISPONIBILIDAD, LISTAR_SERVICIOS, REGISTRAR_PACIENTE, TRANSFERIR_HUMANO, DESCONOCIDO)
- [x] Agendamiento natural en español: parser de fechas relativas y explícitas (hoy, mañana, pasado mañana, día de la semana, dd/mm), horas (a las 14:30, 9:45, 5 pm), servicios y odontólogos por nombre
- [x] Flujo conversacional de varios turnos: pide el dato que falta (servicio → odontólogo → fecha → hora), valida el cupo con la herramienta de agenda y propone la cita para confirmación
- [x] Tools del agente: agenda (disponibilidad por doctor/fecha/servicio), citas (crear/confirmar/cancelar con `source=WHATSAPP`) y servicios; respeta las reglas de negocio de citas
- [x] Registro del paciente por cédula desde el chat; si no existe, transfiere a una persona
- [x] Transferencia a humano: por petición del paciente o desde el panel; la conversación pasa a `ATENCION_HUMANA` y el agente queda en silencio
- [x] Estado conversacional persistido por conversación: `intencion`, `contexto_agente` (JSONB) y `agente_activo`; el contexto se limpia al completar/abortar el agendamiento
- [x] Supervisión (`GET /api/v1/agente/conversaciones`), transferir (`POST /api/v1/agente/conversaciones/{id}/transferir`) y activar/desactivar el agente (`POST /api/v1/agente/conversaciones/{id}/agente`); permiso `AGENTE_IA` para SUPER_ADMIN/ADMIN/RECEPCION
- [x] Integrado al webhook de WhatsApp tras el bot simple de la Fase 6; el agente responde automáticamente por la sesión conectada
- [x] Smoke test E2E (10 pasos) + 85 tests unitarios en verde

### Fase 9 - Multi-tenant

- [x] Tabla `tenants` (catálogo de clínicas) sembrada con la fila `principal`; columna `tenant_id` en las 16 tablas de negocio (`usuarios`, `pacientes`, `odontologos`, `servicios`, `horarios_odontologos`, `bloqueos_agenda`, `citas`, `auditoria`, `google_accounts`, `google_calendars`, `whatsapp_sesiones`, `conversaciones`, `mensajes`, `plantillas_mensajes`, `automatizaciones`, `notificaciones`) con FK a `tenants`
- [x] Roles, permisos y `usuario_roles` permanecen globales (catálogos compartidos); los usuarios pertenecen a un tenant: `rol_permisos` y `usuario_roles` vinculan por id
- [x] Aislamiento automático en JPA: entidades anotadas con `@TenantId` de Hibernate 6, `CurrentTenantIdentifierResolver` que lee el `TenantContext` (ThreadLocal) con fallback al tenant por defecto (1)
- [x] Contexto por petición: el filtro JWT fija el tenant desde el claim `tenantId` del token y lo limpia al terminar; login/refresh resuelven el usuario con lookups globales (nativas) y emiten el claim en `JwtService`
- [x] Webhook de WhatsApp: la sesión receptora determina el tenant del mensaje; conversaciones y mensajes entrantes quedan aislados al tenant de la sesión
- [x] Consultas nativas aisladas manualmente (reportes: `citasPorEstado`, `citasPorOdontologo`, `serviciosMasSolicitados`, `confirmacionesPorFuente`) reciben `tenant_id` del contexto
- [x] Unicidad global (cruza tenant) para username, email y cédula de paciente; listados (`usuarios`, `sesiones de WhatsApp`) filtrados por tenant; `sesionOError` valida la pertenencia
- [x] Regresión completa de Fases 2-8 verificada; smoke test E2E (6 pasos) con un segundo tenant sembrado en BD que confirma el aislamiento de pacientes, sesiones, usuarios y reportes, y 87 tests unitarios en verde

## Requisitos

- Java 21 (JDK completo, no solo JRE)
- Maven 3.9+
- Docker Compose (producción local)
- Node 20+ (frontend Angular; 24 recomendado)

## Ejecución con Docker Compose

```bash
cp .env.example .env
# edita .env y define JWT_SECRET
docker compose up --build -d
```

- Frontend (nginx): http://localhost → login con `admin` / `admin123`
- Backend: http://localhost:8080
  - Swagger: http://localhost:8080/api/v1/swagger-ui.html
  - Health: http://localhost:8080/actuator/health
- Ver `docs/DEPLOYMENT.md` y `docker/README.md` para ambientes, nginx/TLS y backups.

## Ejecución sin Docker (desarrollo)

```bash
# PostgreSQL local con base "dentalcrm"
export DATABASE_URL=jdbc:postgresql://localhost:5432/dentalcrm
export DATABASE_USERNAME=dentalcrm
export DATABASE_PASSWORD=dentalcrm
export JWT_SECRET=<secreto-de-al-menos-32-caracteres>
cd backend
mvn spring-boot:run
```

### Fase 10 - Frontend Angular

- [x] Cliente Angular 22 standalone (Node 24), compilación de producción OK, componentes lazy por módulo
- [x] Login JWT + interceptor con `Authorization: Bearer` + guard de autenticación; logout y manejo de 401 (redirige a login)
- [x] `ApiService` con URL base por entorno (`public/assets/config.json`, por defecto `/api/v1`); proxy de desarrollo a `localhost:8080`
- [x] Layout con sidebar agrupada (Operación / Comunicación / Automatización / Gestión) y usuario/rol activo
- [x] Módulos: pacientes, odontólogos, servicios, horarios (por odontólogo), agenda del día con pestañas Día/Semana/Mes (calendario semanal y mensual con citas por día, bloqueos y días sin horario marcados; clic en un día lleva a la vista del día), citas (filtros por rango/estado/doctor + confirmar/atender/cancelar/no asistió), WhatsApp (sesiones + inbox con envío y cambio de estado), plantillas, automatizaciones (eventos válidos del backend), notificaciones, reportes (dashboard + estadísticas), supervisión del agente IA (transferir/activar) y usuarios (CRUD con roles)
- [x] Google Calendar: estado de conexión, iniciar OAuth (`/google/connect`), selección de calendario, sincronización manual y desconexión (permiso `INTEGRACIONES`)
- [x] Auditoría: nuevo endpoint `GET /api/v1/auditoria` (`AuditoriaController`, permiso `AUDITORIA_READ`) con búsqueda por acción/módulo/entidad, página y datos antes/después; pantalla con expandible que muestra el diff JSON. El aislamiento por tenant aplica automáticamente (`@TenantId`)
- [x] Fix backend descubierto en el smoke de citas: `CitaRepository.buscar` (JPQL con `(:x IS NULL OR ...)`) fallaba en PostgreSQL con "could not determine data type of parameter" para los `LocalDate` (Hibernate 6.6 bindeaba nulls como `bytea`); se reemplazó por `JpaSpecificationExecutor<Cita>` + `Specification` tipada en `CitaService.listar`
- [x] Fix de tests frágiles: `AgenteConversacionalServiceTest` fijaba `hoy = LocalDate.of(2026,9,10)` y reventaba al avanzar la fecha real; ahora usa `LocalDate.now()` (87 tests unitarios en verde)

## Frontend (desarrollo)

```bash
cd frontend
npm install
npx ng serve --proxy-config proxy.conf.json   # proxy /api -> http://localhost:8080
# abrir http://localhost:4200 (usuario admin / admin123)
# compilación de producción:
npx ng build --configuration production
```

## Estructura

```
/backend        API REST Spring Boot (Java 21, Flyway)
/frontend       Angular 22 standalone + Dockerfile/nginx.conf
/docker         Configuración Docker adicional (nginx, backups)
/docs           Documentación técnica (arquitectura, API, seguridad, despliegue…)
/database       Respaldo del modelo y scripts (ver /backend/src/main/resources/db/migration)
```

## Documentación

| Doc | Contenido |
|---|---|
| `docs/ARCHITECTURE.md` | Arquitectura: backend, multi-tenant, frontend, flujo de una cita |
| `docs/DATABASE.md` | Migraciones Flyway y modelo de datos |
| `docs/API.md` | Referencia REST (endpoints, auth, formato de error) |
| `docs/DEPLOYMENT.md` | Ambientes, variables, Docker Compose, nginx, backups |
| `docs/SECURITY.md` | JWT, RBAC, tenencia, auditoría, endurecimiento |
| `docs/WHATSAPP.md` | OpenWA: sesiones, webhook, inbox |
| `docs/GOOGLE-CALENDAR.md` | OAuth, calendarios y sincronización |
| `docs/AUTOMATIONS.md` | Plantillas, eventos y motor de notificaciones |
| `docs/ADMIN.md` | Manual básico de administración |

## Entregables del plan (sección #41)

- [x] `README.md` · `ARCHITECTURE.md` · `DATABASE.md` · `API.md` · `DEPLOYMENT.md`
- [x] `WHATSAPP.md` · `GOOGLE-CALENDAR.md` · `AUTOMATIONS.md` · `SECURITY.md`
- [x] `.env.example` · Docker Compose (backend+frontend+postgres+redis) · Flyway · Tests (87)
- [x] Manual de administración (`docs/ADMIN.md`) · frontend Docker (nginx) · backups (`docker/backups/`)

## Scripts de verificación (Fase 1)

```bash
cd backend
mvn test                 # tests unitarios (JWT)
mvn package              # build completo
```

## Seguridad

- No subir `.env` a Git.
- `JWT_SECRET` con al menos 32 caracteres aleatorios (`openssl rand -base64 48`).
- Cambiar el password `admin123` del usuario inicial en cualquier entorno que no sea local.