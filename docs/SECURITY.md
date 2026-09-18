# Seguridad

## Principios

- Mínimo privilegio: cada endpoint pide un permiso concreto (`@PreAuthorize`), no el rol.
- RBAC por capas: JWT trae `username` y `roles`; el permiso se evalúa por autoridad.
- Aislamiento de datos por tenant obligatorio (ver `ARCHITECTURE.md`).
- No se almacenan datos clínicos; el CRM guarda solo agenda/datos de contacto. Si el alcance
  clínico crece, la historia clínica debe separarse en otro módulo.

## Autenticación (JWT)

- `POST /api/v1/auth/login` emite un token firmado con `JWT_SECRET` (HS256). Cambiar `JWT_SECRET`
  invalida todos los tokens (rotación por cambio de secreto o por cambio de `JWT_EXPIRATION_MS`).
- El token contiene `username`, `roles` y `tenantId`. El filtro JWT valida firma, expiración y
  estado del usuario y fija el `TenantContext` de la petición (limpia en `finally`).
- `Bearer` header obligatorio en todos los endpoints protegidos.
- El frontend guarda el token en `localStorage` y lo adjunta vía interceptor; en 401 redirige al login.

## Tenencia (multi-tenant)

- Todas las entidades de negocio con `@TenantId`; Hibernate agrega el filtro por `TenantContext`.
- El `tenantId` nunca se recibe del cliente: se toma del token. El webhook de WhatsApp lo deriva
  de la sesión receptora.
- Unicidades tipo username/email/cédula se chequean a nivel global (consultas nativas sin tenant).

## Endpoints y validaciones

- Entradas validadas con Bean Validation (`@Valid`) en los DTOs.
- Errores controlados no exponen trazas (`include-stacktrace=never`) ni mensajes internos.
- Límites: listados paginados (`size` acotado a 100), `q` normalizada/trimmed en las consultas.
- SQL: JPA/Criteria (no concatenación); consultas nativas usan `@Param` ligado.

## Secretos y datos personales

- `.env` fuera del repositorio (`gitignore`); en producción, secretos del proveedor de nube.
- En repositorio solo plantillas (`application.yml` usa `${VAR:default}`).
- `CORS_ALLOWED_ORIGINS` restringido en producción a tu propio dominio.
- HTTPS obligatorio en producción (TLS en nginx/balanceador). El JWT viaja siempre por HTTPS.

## Auditoría

- `AuditService.registrar(accion, modulo, entidad, entidadId, datosAnteriores, datosNuevos)`
  escribe JSONB en `auditoria` (quién, qué, cuándo, ip). Consultable por `GET /api/v1/auditoria`
  (solo `AUDITORIA_READ`).

## Retención y backups

- Política recomendada: 6 meses de auditoría en línea, luego archivo.
- Backups cifrados en repositorio de respaldo; restauración probada periódicamente (`docker/backups/`).

## Checklist de endurecimiento antes de producción

1. `JWT_SECRET` largo y único por ambiente.
2. `CORS_ALLOWED_ORIGINS` = solo el dominio (sin `*`).
3. TLS terminado (nginx/ALB) y HTTP redirigido a HTTPS.
4. Desactivar perfiles `dev`/`test`; usar `SPRING_PROFILES_ACTIVE=prod`.
5. Credenciales de OpenWA/Google por secret manager.
6. Auditoría y backups activos con retención y prueba de restauración.