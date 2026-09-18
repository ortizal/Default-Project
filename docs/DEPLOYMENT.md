# Despliegue

## Ambientes

Se usan perfiles Spring (`SPRING_PROFILES_ACTIVE`) y variables de entorno. Sin código propio del
cliente en el repositorio: las 3 plantillas se definen solo por `SPRING_PROFILES_ACTIVE` y las
variables del `.env` correspondiente.

| Ambiente | Var | Notas |
|---|---|---|
| development | `SPRING_PROFILES_ACTIVE=dev` | `localhost:5432`, CORS `http://localhost:4200`, Swagger activo |
| testing | `SPRING_PROFILES_ACTIVE=test` | Base de pruebas, datos sembrados |
| production | `SPRING_PROFILES_ACTIVE=prod` | HTTPS (TLS en nginx), secretos por secreto de la nube |

## Variables de entorno

| Variable | Ejemplo | Obligatoria |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://postgres:5432/dentalcrm` | sí |
| `DATABASE_USERNAME` | `dentalcrm` | sí |
| `DATABASE_PASSWORD` | — | sí |
| `JWT_SECRET` | `openssl rand -base64 48` | sí (mín. 32 bytes) |
| `JWT_EXPIRATION_MS` | `86400000` | no |
| `SERVER_PORT` | `8080` | no |
| `TIMEZONE` | `America/Guayaquil` | no |
| `CORS_ALLOWED_ORIGINS` | `https://crm.miclina.com` | producción |
| `OPENWA_URL` | `http://openwa:2785` | para WhatsApp |
| `OPENWA_API_KEY` | `openssl rand -hex 24` | para WhatsApp (≥32 caracteres) |
| `OPENWA_WEBHOOK_URL` | `http://backend:8080/api/v1/webhooks/whatsapp` | url del webhook interno |
| `OPENWA_WEBHOOK_SECRET` | `openssl rand -hex 16` | firma HMAC del webhook |
| `GOOGLE_CLIENT_ID` | — | para Google Calendar |
| `GOOGLE_CLIENT_SECRET` | — | idem |
| `GOOGLE_REDIRECT_URI` | `https://crm.miclina.com/api/v1/google/callback` | idem |
| `CLINICA_NOMBRE` / `CLINICA_DIRECCION` / `CLINICA_TELEFONO` | — | variables de plantillas |

## Credenciales por módulo

Cada módulo externo tiene sus propias variables en `.env`. Ningún secreto se hardcodea.

### Base de datos + Seguridad
| Variable | Generar con | Obligatoria |
|---|---|---|
| `DATABASE_PASSWORD` | `openssl rand -hex 18` | sí |
| `JWT_SECRET` | `openssl rand -base64 48` | sí (mín. 32 bytes) |

### OpenWA (WhatsApp)
| Variable | Generar con | Obligatoria |
|---|---|---|
| `OPENWA_API_KEY` | `openssl rand -hex 24` | sí (≥32 caracteres) |
| `OPENWA_WEBHOOK_SECRET` | `openssl rand -hex 16` | sí (firma HMAC) |

### Google Calendar (OAuth 2.0)
Las credenciales NO se generan automáticamente. Se crean en [Google Cloud Console](https://console.cloud.google.com/apis/credentials):
1. **APIs & Services → Credentials → Create Credentials → OAuth 2.0 Client ID**
2. Tipo: **Web application**
3. Agregar URI autorizado: `{GOOGLE_REDIRECT_URI}`
4. Copiar `client_id` y `client_secret` a `.env` **o** configurarlas desde el panel de administración en **Google Calendar → Credenciales OAuth**

| Variable | Ejemplo | Obligatoria |
|---|---|---|
| `GOOGLE_CLIENT_ID` | `xxx.apps.googleusercontent.com` | para la app (se puede configurar desde el admin) |
| `GOOGLE_CLIENT_SECRET` | `GOCSPX-...` | idem |
| `GOOGLE_REDIRECT_URI` | `http://localhost:8080/api/v1/google/callback` | sí |

> **Nota:** Las credenciales también se pueden gestionar desde la interfaz admin (ruta `/google`, sección "Credenciales OAuth"). Esto permite cambiarlas sin reiniciar el backend.

## Scripts

- `./scripts/secrets.sh` — genera los secretos locales faltantes (BD, JWT, OpenWA). **No genera** las credenciales de Google (requieren Google Cloud Console).
- `./scripts/deploy.sh` — valida las variables obligatorias. `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` son opcionales si se configurarán desde el panel de administración.

Copia `.env.example` → `.env` y edita. Nunca comitear `.env`.

## Docker Compose (producción local)

```bash
cp .env.example .env
# edita .env: JWT_SECRET, DATABASE_PASSWORD, dominios, credenciales de Google
docker compose up --build -d
```

Servicios:

- **postgres** (`postgres:17-alpine`): volumen `postgres_data`, healthcheck.
- **backend** (build `./backend`): Java 21, Flyway aplica migraciones al arrancar.
- **frontend** (build `./frontend`): build de producción → nginx que sirve los estáticos y
  hace proxy `/api` → `backend:8080`.
- **redis** (`redis:7-alpine`): reservado (sesiones/cola; aún sin uso en la app).
- **openwa** (`rmyndharis/openwa:0.23.4`, puerto 2785): gateway WhatsApp
  ([open-wa.org](https://www.open-wa.org), MIT). SQLite en `openwa_data`, engine `baileys`.
  Recibe mensajes y los entrega vía webhook `OPENWA_WEBHOOK_URL` al backend.

Verificación:

```bash
curl http://localhost:8080/actuator/health   # {"status":"UP"}
curl http://localhost/api/v1/ping            # vía nginx/frontend
curl http://localhost:2785/api/health        # {"status":"ok",...}
```

## nginx (producción, TLS)

`docker/nginx/` trae la config del proxy reverso:

- Sirve `dist/dental-crm-frontend` (SPA con `try_files … /index.html`).
- `location /api/` → `proxy_pass http://backend:8080;` (los JWT pasan tal cual).
- TLS con Let's Encrypt: mueve el certbot el `server_name`, emite el certificado y descargar el
  archivo `docker/nginx/snippets/tls-ssl.conf` de ejemplo (adaptar rutas de los certificados).
  En nube (Caddy/ELB/ALB) el TLS puede hacerse en el balanceador y nginx solo sirve estático + proxy.

## Salud y observabilidad

- `GET /actuator/health` — readiness.
- `GET /actuator/info` — info de la app.
- `GET /actuator/metrics` — métricas JMX/Micrometer.
- Swagger: `GET /api/v1/swagger-ui.html`.

## Backups (PostgreSQL)

Ver `docker/backups/`:

- `backup.sh` — `pg_dump` diario con rotación (`RETENTION_DAYS`).
- `restore.sh` — restaura un `.sql.gz` verificando la integridad antes de aplicar.

Regla de oro: un backup solo está verificado cuando se ha restaurado al menos una vez.

## Actualización

1. `git pull`
2. `docker compose up --build -d`
3. Flyway valida migraciones nuevas; si hubo breaking changes de BD, ejecutar primero la migración correspondiente.