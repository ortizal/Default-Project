# Docker - Configuración adicional

## Estructura

```
docker/
  nginx/         # ejemplos TLS/proxy reverso para producción
  backups/       # scripts de backup/restore PostgreSQL
```

## Servicios (docker-compose.yml)

| Servicio | Imagen/Build | Puertos |
|---|---|---|
| `postgres` | postgres:17-alpine | 5432 |
| `redis` | redis:7-alpine | 6379 |
| `backend` | build `./backend` (Java 21) | 8080 |
| `frontend` | build `./frontend` (node 24 → nginx) | 80 (`FRONTEND_PORT`) |
| `openwa` | `rmyndharis/openwa:0.23.4` | 2785 |

Uso:

```bash
cp .env.example .env   # editar JWT_SECRET, DB, dominios, Google
docker compose up --build -d
```

- Frontend → `http://localhost` (nginx sirve estáticos y proxya `/api` → `backend:8080`).
- Swagger → `http://localhost:8080/api/v1/swagger-ui.html`.
- Dashboard OpenWA → `http://localhost:2785` (inicia sesión con `OPENWA_API_KEY`).
- Ver `docs/DEPLOYMENT.md` para variables de entorno y ambientes.

## nginx (producción)

`frontend/nginx.conf` es la config usada por el servicio `frontend` (SPA + proxy `/api`).
En producción con TLS, terminar HTTPS en el balanceador (ALB/Caddy) o usar esta plantilla:

```nginx
server {
    listen 443 ssl http2;
    server_name crm.miclina.com;
    ssl_certificate     /etc/letsencrypt/live/crm.miclina.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/crm.miclina.com/privkey.pem;
    # (resto igual que frontend/nginx.conf)
}
server { listen 80; server_name crm.miclina.com; return 301 https://$host$request_uri; }
```

## Backups

```bash
# Diario (cron): guarda en /backups y rota > RETENTION_DAYS
PG_HOST=localhost PG_USER=dentalcrm PG_PASSWORD=x DATABASE_NAME=dentalcrm \
  BACKUP_DIR=/backups RETENTION_DAYS=14 docker/backups/backup.sh

# Restauración (VERIFICAR periódicamente)
PG_HOST=localhost PG_USER=dentalcrm PG_PASSWORD=x DATABASE_NAME=dentalcrm \
  docker/backups/restore.sh /backups/dentalcrm_20260912_000000.sql.gz
```

Un backup solo se considera verificado cuando se ha restaurado con éxito al menos una vez.