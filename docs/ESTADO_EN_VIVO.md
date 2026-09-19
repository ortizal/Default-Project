# Estado en vivo — 2026-09-19 23:55 UTC

> Snapshot automático generado por `scripts/estado-en-vivo.sh` (lo invoca `deploy.sh`).
> El documento curado y por módulos es `docs/ESTADO.md`.

## Contenedores

| Contenedor | Estado | Imagen |
|---|---|---|
| dentalcrm-frontend | Up About a minute | dentalcrm-frontend |
| dentalcrm-backend | Up 10 hours | dentalcrm-backend |
| dentalcrm-openwa | Up 11 hours (healthy) | rmyndharis/openwa:0.23.4 |
| dentalcrm-postgres | Up 11 hours (healthy) | postgres:17-alpine |
| dentalcrm-redis | Up 11 hours | redis:7-alpine |

## Salud

| Servicio | Estado |
|---|---|
| backend  (http://localhost:8080/actuator/health) | ok |
| openwa   (http://localhost:2785/api/health) | ok |
| frontend (http://localhost/api/v1/ping) | no |

## Enlaces

- CRM: http://localhost
- API: http://localhost:8080
- Swagger: http://localhost:8080/api/v1/swagger-ui.html
- Dashboard OpenWA (QR): http://localhost:2785
- Webhook WhatsApp: http://localhost:8080/api/v1/webhooks/whatsapp
