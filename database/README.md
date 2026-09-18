# Base de datos

## Migraciones

Las migraciones Flyway viven en el backend:

```
backend/src/main/resources/db/migration/
```

Cada fase agrega una migración versionada:

- `V1__schema_base.sql` - usuarios, roles, permisos, auditoría (Fase 1)
- Fase 2 - pacientes, odontólogos, servicios
- Fase 3 - horarios, bloqueos, citas
- Fase 5 - conversaciones, mensajes, sesiones WhatsApp
- Fase 6 - plantillas, automatizaciones, notificaciones
- Fase 4 - google_accounts, google_calendars

## Reglas

- Nunca modificar una migración ya aplicada: crear una nueva (`V{n+1}__...`).
- Flyway valida el esquema con `spring.jpa.hibernate.ddl-auto=validate`.

## Backups

Programar respaldo diario/semanal de PostgreSQL con retención configurable.
Un backup solo se considera verificado cuando se ha probado una restauración.