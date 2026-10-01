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
| `META_APP_ID` / `META_APP_SECRET` | — | opcional (panel o `.env`) |
| `META_REDIRECT_URI` | `https://crm.miclina.com/api/v1/social/callback/meta` | OAuth Meta |
| `TIKTOK_CLIENT_KEY` / `TIKTOK_CLIENT_SECRET` | — | opcional (panel o `.env`) |
| `TIKTOK_REDIRECT_URI` | `https://crm.miclina.com/api/v1/social/callback/tiktok` | OAuth TikTok |
| `CLINICA_NOMBRE` / `CLINICA_DIRECCION` / `CLINICA_TELEFONO` | — | variables de plantillas |
| `APP_INTEGRACIONES_ENCRYPTION_KEY` | `openssl rand -base64 48` | recomendada para secretos del panel |
| `APP_FIRMA_DIGITAL_ENCRYPTION_KEY` | `openssl rand -base64 48` | recomendada para firma `.p12` |

## Credenciales por módulo

Cada módulo externo tiene sus propias variables en `.env`. Ningún secreto se hardcodea.

OpenWA, SMTP y OAuth de Google también pueden editarse desde el panel de administración; esos cambios
se guardan por consultorio y se aplican inmediatamente. Las claves API, contraseñas y secretos OAuth
se cifran con AES-GCM y nunca se devuelven al navegador. Se recomienda definir claves de cifrado
estables de al menos 32 caracteres y mantener una copia segura: cambiarlas sin migrar los datos
cifrados impediría recuperar las credenciales almacenadas.

Base de datos, puertos, JWT, CORS y claves maestras permanecen en `.env` y requieren configuración
de despliegue. El botón **Usar valores de .env** elimina el override del consultorio, no modifica el
archivo `.env`.

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
| `GOOGLE_CLIENT_SECRET` | `your-secret` | idem |
| `GOOGLE_REDIRECT_URI` | `http://localhost:8080/api/v1/google/callback` | sí |

> **Nota:** Las credenciales también se pueden gestionar desde la interfaz admin (ruta `/google`, sección "Credenciales OAuth"). Esto permite cambiarlas sin reiniciar el backend.

## Scripts

- `./scripts/secrets.sh` — genera los secretos locales faltantes (BD, JWT, OpenWA). **No genera** las credenciales de Google (requieren Google Cloud Console).
- `./scripts/deploy.sh` — si `.env` no existe, lo crea desde `.env.example`, solicita el usuario y contraseña de PostgreSQL (Enter genera una contraseña segura) y completa los secretos faltantes. Si `.env` ya existe, no lo modifica y valida que contenga los valores obligatorios. `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` son opcionales si se configurarán desde el panel de administración.

El despliegue usa `OPENWA_MODE=auto` por defecto: detecta un OpenWA sano en `localhost:2785` y lo reutiliza, o levanta el incluido si no hay uno. Usa `OPENWA_MODE=external` para forzar uno externo (por defecto accesible desde el backend como `http://host.docker.internal:2785`) o `OPENWA_MODE=bundled` para levantar siempre el contenedor del proyecto. En modo externo, `OPENWA_API_KEY` debe coincidir con la clave del gateway existente.

Para servidores con Nginx nativo, `deploy.sh` compila Angular y copia los assets a `FRONTEND_ROOT` (por defecto `/var/www/dentalcrm`); el contenedor frontend queda opcional en el perfil `docker-frontend`. El backend sigue en Docker y se publica solo en loopback `127.0.0.1:18082` (`BACKEND_PORT`), no directamente a Internet. El bloque del sitio `alan-tek.com` está agregado en el archivo `control-servidor` que acompaña al proyecto. Las rutas configuradas son:

```nginx
location = /dental_crm { return 301 /dental_crm/; }
location /dental_crm/api/ {
    rewrite ^/dental_crm/api/(.*)$ /api/$1 break;
    proxy_pass http://127.0.0.1:18082;
}
location /dental_crm/ {
    alias /var/www/dentalcrm/;
    index index.html;
    try_files $uri $uri/ /dental_crm/index.html;
}
```

El build se guarda en `/var/www/dentalcrm/assets/config.json` con API `/dental_crm/api/v1`. Asegúrate de que Nginx pueda leer el directorio. Después de copiar el bloque a `/etc/nginx/sites-available/control-servidor`, ejecuta `sudo nginx -t && sudo systemctl reload nginx`. En `.env`, usa `FRONTEND_PUBLIC_URL=https://alan-tek.com/dental_crm`, `GOOGLE_REDIRECT_URI=https://alan-tek.com/dental_crm/api/v1/google/callback`, `META_REDIRECT_URI=https://alan-tek.com/dental_crm/api/v1/social/callback/meta`, `TIKTOK_REDIRECT_URI=https://alan-tek.com/dental_crm/api/v1/social/callback/tiktok` y `OPENWA_WEBHOOK_URL=https://alan-tek.com/dental_crm/api/v1/webhooks/whatsapp` cuando deban ser accesibles desde fuera.

Copia `.env.example` → `.env` y edita. Nunca comitear `.env`.

## Docker Compose (opcional)

```bash
cp .env.example .env
# edita .env: JWT_SECRET, DATABASE_PASSWORD, dominios, credenciales de Google
docker compose up --build -d backend postgres redis
# Para servir frontend con Nginx en Docker en vez del Nginx nativo:
docker compose --profile docker-frontend up --build -d
```

Servicios:

- **postgres** (`postgres:17-alpine`): volumen `postgres_data`, healthcheck.
- **backend** (build `./backend`): Java 21, Flyway aplica migraciones al arrancar.
- **frontend** (perfil `docker-frontend`): opción alternativa que sirve los estáticos y
  hace proxy `/api` → `backend:8080`.
- **redis** (`redis:7-alpine`): reservado (sesiones/cola; aún sin uso en la app).
- **openwa** (`rmyndharis/openwa:0.23.4`, puerto 2785): gateway WhatsApp
  ([open-wa.org](https://www.open-wa.org), MIT). SQLite en `openwa_data`, engine `baileys`.
  Recibe mensajes y los entrega vía webhook `OPENWA_WEBHOOK_URL` al backend.

Verificación:

```bash
curl http://127.0.0.1:18082/actuator/health  # {"status":"UP"}
curl https://alan-tek.com/dental_crm/api/v1/ping # vía Nginx nativo
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

## Checklist de despliegue inicial

Antes de abrir el sistema a usuarios:

- [ ] `.env` existe en la máquina objetivo y contiene los valores obligatorios.
- [ ] `DATABASE_PASSWORD`, `JWT_SECRET`, `OPENWA_API_KEY` y `OPENWA_WEBHOOK_SECRET` fueron generados con `openssl` o `scripts/secrets.sh`.
- [ ] `SPRING_PROFILES_ACTIVE` coincide con el ambiente real (`dev`, `test`, `prod`).
- [ ] `FRONTEND_PUBLIC_URL`, `GOOGLE_REDIRECT_URI`, `META_REDIRECT_URI`, `TIKTOK_REDIRECT_URI` y `OPENWA_WEBHOOK_URL` usan el dominio público correcto.
- [ ] La base de datos PostgreSQL está levantada y accesible desde el backend.
- [ ] Nginx o el balanceador externo apunta al backend correcto y permite el subpath configurado (por ejemplo `/dental_crm`).
- [ ] El certificado TLS está presente y la configuración de nginx pasa `nginx -t`.
- [ ] El backend responde en `/actuator/health` y los webhooks de WhatsApp/Google cargan sin errores.
- [ ] Se prueba al menos un flujo crítico: login, creación de cita, envío de recordatorio y sincronización con Google Calendar o redes sociales si están habilitadas.

## Verificación post-despliegue

```bash
# backend
curl -fsS http://127.0.0.1:18082/actuator/health
curl -fsS http://127.0.0.1:18082/api/v1/ping

# frontend
curl -I https://alan-tek.com/dental_crm/

# OpenWA
curl -fsS http://localhost:2785/api/health
```

Revisa la salida y confirma que:

- `{"status":"UP"}` aparece en el healthcheck del backend.
- El frontend sirve la SPA sin errores 4xx/5xx.
- OpenWA responde con el estado correcto y no hay conexiones rechazadas.
- Los logs de backend no muestran errores de conexión a BD, JWT, OAuth ni webhooks.

## Solución rápida de problemas

### Backend no inicia

- Revisa si `DATABASE_PASSWORD`, `JWT_SECRET` y `DATABASE_URL` están cargados.
- Comprueba que el contenedor `postgres` está saludable y la BD acepta conexiones.
- Verifica que el puerto `SERVER_PORT` o el proxy del servicio no esté ocupado.

### Frontend 404 o rutas rotas

- Confirma que `FRONTEND_PUBLIC_URL` coincide con el dominio real.
- Verifica `try_files` y `alias` en nginx para el subpath.
- Rebuilda Angular y vuelve a publicar `dist` en el directorio web correcto.

### WhatsApp no recibe mensajes

- Comprueba `OPENWA_MODE` y que el gateway correcto está escuchando en `localhost:2785` o `host.docker.internal:2785`.
- Asegúrate de que `OPENWA_API_KEY` y `OPENWA_WEBHOOK_SECRET` coinciden con el valor esperado por OpenWA y el backend.
- Revisa el webhook `OPENWA_WEBHOOK_URL` y confirma que el backend lo recibe sin bloqueo por firewall o Nginx.

### OAuth de Google / Meta / TikTok falla

- Verifica `*_REDIRECT_URI` y que el dominio esté exactamente registrado en la consola del proveedor.
- Confirma que el `client_id`/`client_secret` corresponden al proyecto correcto.
- Revisa que el navegador no esté bloqueando cookies o redirecciones relativas.

## Rollback

Si hay un problema crítico tras el despliegue:

1. Detener la versión nueva `docker compose down` o revertir el servicio del host.
2. Restaurar la última imagen o release estable.
3. Volver a aplicar la configuración de `.env` validada.
4. Restaurar la BD desde un backup verificado si hubo un problema con migraciones o datos.
5. Repetir las verificaciones del healthcheck y del flujo principal antes de reabrir el sistema.

El despliegue debe considerarse exitoso solo cuando la aplicación responde sanamente, los flujos críticos funcionan y el backup más reciente ha sido restaurado al menos una vez en un entorno de prueba.