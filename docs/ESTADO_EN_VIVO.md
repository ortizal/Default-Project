# Estado en vivo — 2026-09-30 19:35 UTC

> Snapshot automático generado por `scripts/estado-en-vivo.sh` (lo invoca `deploy.sh`).
> El documento curado y por módulos es `docs/ESTADO.md`.

## Contenedores

| Contenedor | Estado | Imagen |
|---|---|---|
| dentalcrm-frontend | Up 25 seconds | dentalcrm-frontend |
| dentalcrm-backend | Up 25 seconds | dentalcrm-backend |
| dentalcrm-postgres | Up 36 seconds (healthy) | postgres:17-alpine |
| dentalcrm-openwa | Up 36 seconds (healthy) | rmyndharis/openwa:0.23.4 |
| dentalcrm-redis | Up 36 seconds | redis:7-alpine |

## Salud

| Servicio | Estado |
|---|---|
| backend  (http://localhost:8080/actuator/health) | ok |
| openwa   (http://localhost:2785/api/health) | ok |
| frontend (http://localhost/api/v1/ping) | ok |

## Enlaces

- CRM: http://localhost
- API: http://localhost:8080
- Swagger: http://localhost:8080/api/v1/swagger-ui.html
- Dashboard OpenWA (QR): http://localhost:2785
- Webhook WhatsApp: http://localhost:8080/api/v1/webhooks/whatsapp
