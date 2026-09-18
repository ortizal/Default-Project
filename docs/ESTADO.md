# Estado del sistema — Dental CRM

> Fecha de corte: 2026-09-13. Este documento describe qué se ha construido, cómo funciona el
> stack y el alcance de cada módulo. Todo lo que figura como "funcionando" está verificado por
> tests (87/87) o por smoke real contra el despliegue Docker.

---

## 1. Resumen ejecutivo

- **Aplicación**: CRM odontológico multi-tenant (Java 21, Spring Boot 3, Angular/nginx, PostgreSQL).
- **Integraciones**:
  - **WhatsApp** vía **OpenWA** (gateway FOSS, `rmyndharis/openwa:0.23.4`, puerto 2785) — activo.
  - **Google Calendar / OAuth** — implementado, en `dev` con credenciales de prueba.
  - **Agente de IA conversacional** (IA Generativa para el flujo de citas) — funcional con `enable: false` por defecto.
- **Alcance operativo**: el CRM gestiona pacientes, agenda/citas, odontólogos, servicios, reportes,
  auditoría, automatizaciones, WhatsApp y un agente IA para agendar citas por chat.
- **Estado**: 4 servicios corriendo en Docker + OpenWA, con healthchecks y scripts de operación.

---

## 2. Estado del despliegue

### 2.1 Stack (Docker Compose, red `dentalcrm`)

| Servicio | Imagen/Build | Puerto host | Descripción |
|---|---|---|---|
| `postgres` | `postgres:17-alpine` | 5432 | BD principal (volumen `postgres_data`) |
| `redis` | `redis:7-alpine` | 6379 | Cache/sesiones (volumen `redis_data`) |
| `backend` | build `./backend` (Java 21) | 8080 | API REST `/api/v1`, Swagger, actuator |
| `frontend` | build `./frontend` (node 24 → nginx) | 80 | SPA + proxy `/api` → backend |
| `openwa` | `rmyndharis/openwa:0.23.4` | 2785 | Gateway WhatsApp (dashboard + API) |

Volúmenes: `postgres_data`, `redis_data`, `openwa_data` (SQLite de OpenWA en `/app/data`).

### 2.2 Rutas principales

```
http://localhost/                      → Frontend (SPA)
http://localhost:8080                  → Backend API
http://localhost:8080/api/v1/swagger-ui.html → Swagger UI
http://localhost:8080/api/v1/webhooks/whatsapp → Webhook WhatsApp (firmado HMAC)
http://localhost:2785                  → Dashboard OpenWA (QR, sesiones)
http://localhost:2785/api              → API del gateway OpenWA
```

### 2.3 Salud

```bash
./scripts/health.sh        # resumen de salud de todo el stack y sesiones WhatsApp
./scripts/deploy.sh        # build + up + espera de salud
./scripts/secrets.sh       # genera secretos faltantes (.env)
./scripts/whatsapp.sh      # utilidades de WhatsApp (QR, envío, webhook test)
```

---

## 3. Arquitectura

```
[Cliente Web]  → nginx (frontend) → /api → backend:8080 (Spring Boot)
                                            ├─ PostgreSQL (JPA/Flyway V1..V9)
                                            └─ Redis (reservado)
[WhatsApp]  ⇄  openwa:2785 (gateway) ──── webhook firmado (HMAC) ──→ backend /api/v1/webhooks/whatsapp
                                                          ←── sesiones/QR/mensajes (API + X-API-Key)
[Google]   ←── OAuth2 / Calendar ── (credenciales por tenant)
[IA]       ←── AgenteConversacionalService (IA Gen.) ── agenda natural de citas
```

Principios transversales:
- **Multi-tenant**: tablas con `tenant_id`; `TenantContext` aísla consultas por cliente (Flyway V9).
- **Abstracción de proveedores**: `WhatsAppProvider` (interfaz) → implementación `OpenWAProvider`;
  la lógica de negocio no depende del proveedor concreto.
- **Seguridad**: JWT (firma HMAC), roles `ADMIN`/`ODONTOLOGO`/`RECEPCIONISTA`/`ASISTENTE`,
  webhooks con firma HMAC-SHA256 (`WebhookSignatureFilter`), auditoría de operaciones.
- **Eventos/citas**: transición de estados, solapamientos de horarios y disponibilidad validada.

---

## 4. Módulos y alcance

### 4.1 Autenticación y usuarios (`/api/v1/auth`, `/api/v1/usuarios`)
- **Estado**: implementado.
- Funcionalidad: login/refresh/logout (JWT), CRUD de usuarios, gestión de roles.
- Pendiente: reinicio/revocación masiva de tokens (solo logout por usuario).

### 4.2 Pacientes (`/api/v1/pacientes`)
- **Estado**: implementado.
- Funcionalidad: CRUD, búsqueda, teléfonos (formato nacional/internacional), historial.
- Integración WhatsApp: resolución paciente por teléfono en el webhook.

### 4.3 Odontólogos y servicios (`/api/v1/odontologos`, `/api/v1/servicios`)
- **Estado**: implementado (catálogos para agenda, citas y reportes).

### 4.4 Agenda y citas (`/api/v1/agenda`, `/api/v1/citas`, `/api/v1/horarios`)
- **Estado**: implementado.
- Funcionalidad: agendar/consultar/mover/cancelar citas, validación de conflictos de horario,
  disponibilidad por odontólogo, recordatorios (base para WhatsApp).
- Integración Google Calendar y WhatsApp.

### 4.5 WhatsApp / OpenWA (`/api/v1/whatsapp`, `/api/v1/webhooks`)
- **Estado**: implementado end-to-end contra la API real de OpenWA.
- Funcionalidades:
  - Sesiones: crear, conectar (QR), estado, desconectar (dashboards y QR).
  - Entrada: webhook `message.received` → conversación + mensaje (dedupe por `idempotencyKey`).
  - Salida: `enviarMensaje` (send-text) con `chatId@c.us`.
  - Seguridad: firma HMAC (`X-OpenWA-Signature`), filtro `WebhookSignatureFilter`.
- Doc: `docs/WHATSAPP.md`.
- Pasos QA verificados: crear sesión, QR, webhook con/sin firma, send-text, conversaciones.

### 4.6 Agente IA conversacional (`/api/v1/agente`)
- **Estado**: implementado (backend) y desactivado por defecto en el stack.
- Funcionalidad: flujo conversacional para agendar citas (fase 9/10 del alcance WhatsApp),
  parser de lenguaje natural.
- Pendiente: habilitar con credenciales de IA en producción.

### 4.7 Google Calendar (`/api/v1/google`)
- **Estado**: implementado (OAuth2 + Calendar básico), en `dev` con credenciales de prueba.
- Pendiente: credenciales de producción por tenant.

### 4.8 Automatizaciones (`/api/v1/automatizaciones`) y plantillas (`/api/v1/plantillas`)
- **Estado**: implementado (CRUD de automatizaciones y plantillas, agenda/recordatorios).

### 4.9 Reportes (`/api/v1/reportes`) y auditoría (`/api/v1/auditoria`)
- **Estado**: implementado (reportes de cierre, ingresos/actividad; auditoría de acciones).

### 4.10 Operación del sistema (`/api/v1` — SystemController, actuator)
- **Estado**: health/info, ping, métricas (Spring Actuator), Swagger.

---

## 5. Seguridad

- **Autenticación**: JWT firmado (HMAC-SHA256, `JWT_SECRET`), expiración configurable.
- **Autorización**: roles y protección por tenant.
- **Webhooks**: HMAC del body (`X-OpenWA-Signature`) verificado con tiempo constante; sin firma → 401.
- **Secretos**: `.env` (fuera de git), `JWT_SECRET`, `OPENWA_API_KEY`, `OPENWA_WEBHOOK_SECRET`,
  credenciales BD/Google.
- **Práctica**: nunca comitear `.env`; usar `scripts/secrets.sh` para generarlos.

---

## 6. Configuración y variables de entorno

Ver `.env.example` y `docs/DEPLOYMENT.md`. Las obligatorias son `DATABASE_PASSWORD`,
`JWT_SECRET`, `OPENWA_API_KEY`, `OPENWA_WEBHOOK_SECRET`; las de Google/IA son opcionales (dev).

---

## 7. Pruebas

- **Unitarias/Integración**: 87/87 verdes (`mvn test`).
- **Smoke de despliegue**: verificado contra el stack: salud, login, creación de sesión WhatsApp,
  QR, webhook firmado (200), webhook con firma inválida (401), consulta de conversaciones,
  envío con mock del agente. Ver `docs/WHATSAPP.md` (sección "Pruebas").

---

## 8. Próximos pasos sugeridos

1. Escanear el QR de la sesión WhatsApp para dejar `SES_PRINCIPAL` en `CONECTADA` y probar un
   envío real (script `whatsapp.sh enviar`).
2. Habilitar Google Calendar con credenciales de producción.
3. Activar el agente IA con credenciales reales.
4. Poner TLS en producción (nginx/certbot o balanceador) y endurecer CORS.
5. Rellenar/Woof de datos de demo (pacientes, citas, etc.).

Para operaciones al desplegar: `./scripts/deploy.sh`, `./scripts/health.sh`,
`./scripts/whatsapp.sh`.
