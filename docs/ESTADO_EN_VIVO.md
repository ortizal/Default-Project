# Estado en vivo — 2026-10-14 21:15 UTC

> Snapshot automático generado por `scripts/estado-en-vivo.sh` (lo invoca `deploy.sh`).
> El documento curado y por módulos es `docs/ESTADO.md`.

## Contenedores

| Contenedor | Estado | Imagen |
|---|---|---|
| dentalcrm-backend | Up 28 seconds | dentalcrm-backend |
| dentalcrm-postgres | Up 7 hours (healthy) | postgres:17-alpine |
| dentalcrm-openwa | Up 7 hours (healthy) | rmyndharis/openwa:0.23.4 |
| dentalcrm-redis | Up 7 hours | redis:7-alpine |

## Salud

| Servicio | Estado |
|---|---|
| backend  (http://127.0.0.1:18082/api/v1/ping) | ok |
| openwa   (http://localhost:2785/api/health) | ok |
| frontend (http://localhost/dental_crm/api/v1/ping) | ok |

## Enlaces

- CRM: http://localhost/dental_crm
- API: http://127.0.0.1:18082/api/v1
- Swagger: http://127.0.0.1:18082/api/v1/swagger-ui.html
- Dashboard OpenWA (QR): http://localhost:2785
- Webhook WhatsApp: http://127.0.0.1:18082/api/v1/webhooks/whatsapp
