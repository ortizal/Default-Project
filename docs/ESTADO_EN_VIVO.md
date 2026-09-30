# Estado en vivo — 2026-09-30 20:53 UTC

> Snapshot automático generado por `scripts/estado-en-vivo.sh` (lo invoca `deploy.sh`).
> El documento curado y por módulos es `docs/ESTADO.md`.

## Contenedores

| Contenedor | Estado | Imagen |
|---|---|---|
| dentalcrm-backend | Up 31 seconds | dentalcrm-backend |
| dentalcrm-postgres | Up About an hour (healthy) | postgres:17-alpine |
| dentalcrm-openwa | Up About an hour (healthy) | rmyndharis/openwa:0.23.4 |
| dentalcrm-redis | Up About an hour | redis:7-alpine |

## Salud

| Servicio | Estado |
|---|---|
| backend  (http://127.0.0.1:18082/api/v1/ping) | ok |
| openwa   (http://localhost:2785/api/health) | ok |
| frontend (http://localhost/dental_crm/api/v1/ping) | no |

## Enlaces

- CRM: http://localhost/dental_crm
- API: http://127.0.0.1:18082/api/v1
- Swagger: http://127.0.0.1:18082/api/v1/swagger-ui.html
- Dashboard OpenWA (QR): http://localhost:2785
- Webhook WhatsApp: http://127.0.0.1:18082/api/v1/webhooks/whatsapp
