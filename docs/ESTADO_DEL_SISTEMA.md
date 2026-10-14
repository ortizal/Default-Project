# Estado del sistema — CRM Dental AT

> **Documento consolidado**: estado total actual, funcionalidad, módulos, accesos, operaciones y proyección.
> **Fecha de corte:** 2026-10-09 · verificado con los gates del día (backend, frontend, e2e, Docker).
> **Complementos:** [`README.md`](../README.md) (plan por fases) · [`docs/ESTADO.md`](ESTADO.md) (corte histórico 2026-09-13) · [`docs/ESTADO_EN_VIVO.md`](ESTADO_EN_VIVO.md) (snapshot de contenedores, script `scripts/estado-en-vivo.sh`).

---

## 1. Resumen ejecutivo

| Aspecto | Situación |
|---|---|
| Qué es | **CRM Dental AT** (marca **ALANTEK**): CRM multi-clínica para consultorios dentales con agenda/citas, pacientes, historia clínica, WhatsApp, automatizaciones, reportes, redes sociales y **facturación electrónica SRI en pruebas** |
| Madurez | **Desarrollo completo** (Fases 1–10 del plan) + ciclo de calidad (lint/unit/e2e/a11y) cerrado |
| Backend | Spring Boot 3 · Java 21 · PostgreSQL · Flyway **V43** · JWT + RBAC · **33 controladores / 205 endpoints** |
| Frontend | Angular 22 (standalone, lazy) · **28 rutas** + login · tema claro/oscuro · kit UI propio · SCSS + Bootstrap 5 |
| Calidad | **backend 500/500** · unit **170/170** · lint ✔ · e2e **63/63** (axe 0 violaciones en 28 rutas × 2 temas, contraste WCAG AA y menú de acciones abierto) |
| Despliegue | Docker Compose (backend, postgres, redis, openwa[, frontend]) + nginx nativo en `/dental_crm` |
| Producción | **Pendiente de verificar** (bloqueo 403 en login: infraestructura ya endurecida, falta smoke en el servidor) |

---

## 2. Estado actual (verificado el 2026-10-09)

### 2.1 Gates de calidad

| Gate | Comando | Resultado |
|---|---|---|
| Backend unit/integración | `cd backend && mvn -o test` | **531/531** (0 fallos) |
| Lint frontend | `cd frontend && npx ng lint` | ✔ todos los archivos |
| Unit frontend (Vitest) | `npx ng test --no-watch` | **186/186** (21 specs) |
| Build producción | `npx ng build --configuration production --output-path /tmp/opencode/dist-ng` | **720.52 kB** inicial / 147.39 kB gzip, 0 warnings |
| E2E (Playwright) | `npx playwright test` | **63/63** en ~12,5 min (21 specs, `workers: 1`, exit 0, sin flaky) |
| Accesibilidad | `axe.spec.ts` · `contraste.spec.ts` | **0 violaciones** WCAG A/AA en 28 rutas × claro/oscuro, con el menú de acciones abierto y **contraste AA en los 4 escenarios** (sistema/elección manual) |
| Imágenes Docker | `docker compose --profile docker-frontend build` | `dentalcrm-backend` y `dentalcrm-frontend` reconstruidas hoy (incluye las Fases A–G) · `dentalcrm-backend` reiniciado y `200` en `POST /auth/login` |
| Compose | `docker compose config --quiet` | válido |

> Reproducción completa: §10.

### 2.2 Infraestructura local (contenedores activos)

| Contenedor | Imagen | Puerto | Notas |
|---|---|---|---|
| `dentalcrm-backend` | `dentalcrm-backend` (build local) | `127.0.0.1:18082 → 8080` | Sólo loopback; `SERVER_FORWARD_HEADERS_STRATEGY=framework`, `FRONTEND_PUBLIC_URL` expuestos |
| `dentalcrm-postgres` | postgres | `5433 → 5433` (interno también 5433) | healthy · Flyway **V43**, 43 migraciones OK, **57 tablas** |
| `dentalcrm-openwa` | openwa | `0.0.0.0:2785` | healthy · QR de sesiones WhatsApp |
| `dentalcrm-redis` | redis:7-alpine | `6379` | presente en el stack, **sin uso en el código del backend actual** |
| `dentalcrm-frontend` (imagen) | nginx | perfil `docker-frontend` (8081) | **No usar en el servidor** (colisiona con conteo-electoral); en producción manda `deploy.sh` + nginx nativo |

### 2.3 Estado del repositorio

- Último commit: `ac57d6e UPDATE` (proxy `18082`, callback de Google, docs de puertos).
- **Cambios sin commitear** correspondientes a las Fases A–H.1 del plan de ampliación (§2.5–§2.19): `V27`–`V43`, permisos en login, menú dinámico, `FileStorageProvider`/`HashUtil`, historia clínica (backend + módulo Angular), odontograma interactivo, planes de tratamiento con PDF, periodontograma, recetas y consentimientos informados (backend + pestañas), línea temporal unificada
(§2.14), reportes clínicos (§2.15), **finanzas internas** (cuenta, caja, dashboard y reportes financieros, §2.16), **facturación electrónica SRI en pruebas** (§2.18), **notas de crédito y anulación de comprobantes** (`V43`, §2.19), **renombre de marca a CRM Dental AT / ALANTEK**, specs y esta documentación.
- `.env` local configurado (seeds de QA: `admin/admin123`).

### 2.4 Datos locales (entorno de pruebas)

1 tenant (`principal`) · 9 usuarios · 4 roles · 49 permisos · 13 pacientes · 9 citas · historia clínica, odontograma y periodontograma de prueba del paciente 1 (antecedente, examen, diagnóstico, evolución cerrada con adenda, 3 versiones de odontograma y 2 exámenes periodontales) · 18 recetas (vigentes y anuladas) · **42 consentimientos** (33 firmados y 9 borradores, con huella SHA-256) · 8 plantillas de consentimiento · 2 tutores registrados (pacientes 3 y 11, los dos menores) · 1 archivo clínico de prueba (el resto se crea y se borra en los smoke/e2e) · línea temporal del paciente 1 con **47 eventos** de 9 tipos · **cuenta del paciente 1** (Lucía Martínez, `1700000001`) con cargos/abonos de prueba y **3 sesiones de caja** cerradas con su diferencia · **6 comprobantes electrónicos** (4 facturas `001-001-000000001..4` y 2 **notas de crédito** tipo `04`, todos autorizados en **modo simulado**) con sus eventos de auditoría (GENERAR/ENVIAR/AUTORIZAR, más GENERAR_NOTA_CREDITO y ANULAR_COMPROBANTE en la Fase H) · **1 anulación aplicada** (la factura 4) · secuenciales: factura y nota de crédito en `000000001` cada uno · 13 pacientes de prueba de facturación (Pacientes 12 y 13 con RUC) → **datos de prueba, no producción**.

### 2.5 Plan de ampliación — Fase A completada (2026-10-01)

De [`docs/PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md`](PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md) §7:

| Elemento de la Fase A | Estado |
|---|---|
| Permisos | ✅ `V27__permisos_historia_facturacion.sql`: **25 códigos nuevos** (historia clínica, odontograma, periodoncia, planes, recetas, consentimientos, archivos clínicos, facturación, caja, reportes) → **49 en total**; concesiones por rol verificadas en BD (SUPER_ADMIN 49 · ADMIN 48 · **RECEPCION 24** desde `V33` · ODONTOLOGO 21) |
| Menú dinámico | ✅ `GET /auth/login` devuelve `permisos[]`; `AuthService.puede()` + `menu.ts` (`permiso` por ítem) + `SidebarComponent` filtra secciones. Sesión antigua sin el campo: no se oculta nada |
| Archivos | ✅ `FileStorageProvider` + `LocalFileStorageProvider` (raíz `STORAGE_LOCAL_DIR`, volumen `storage_data`, anti-traversal), listo para S3 y para radiografías/RIDE (Fases E/G) |
| PDF | ✅ ya existente: OpenPDF (`HorarioPdfService`) reutilizable para presupuestos y RIDE |
| Firma/hash | ✅ `HashUtil.sha256Hex()` (SHA-256 hex) para integridad de archivos |
| Auditoría | ✅ ya existente: `AuditService` |
| Tests | ✅ backend **201/201** (+15: `AuthServiceTest`, `LocalFileStorageProviderTest`, `HashUtilTest`) · unit **34/34** (+9: `auth.service.spec.ts`, `menu.spec.ts`) · e2e **36/36** (+2: `permisos.spec.ts`, menú filtrado como ODONTOLOGO) |

### 2.6 Plan de ampliación — Fase B completada (2026-10-01)

De [`docs/PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md`](PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md) §7:

| Elemento de la Fase B | Estado |
|---|---|
| Esquema | ✅ `V28__historia_clinica_base.sql`: 6 tablas (`historias_clinicas` 1:1 con el paciente, `antecedentes_medicos`, `examenes_clinicos`, `diagnosticos_clinicos`, `evoluciones`, `evolucion_adendas`) |
| Backend | ✅ `HistoriaClinicaService` + `HistoriaClinicaController`: 17 endpoints bajo `/api/v1/historia-clinica`, permisos `HISTORIA_CLINICA_READ/WRITE/CLOSE`, auditoría con `AuditService` y aislamiento multi-tenant (`@TenantId`) |
| Reglas | ✅ la evolución sólo es editable en `ABIERTA` (409 `EVOLUCION_CERRADA`), las adendas sólo se añaden sobre `CERRADA` (409 `EVOLUCION_ABIERTA`) y el cierre exige `HISTORIA_CLINICA_CLOSE` |
| Frontend | ✅ ruta `/pacientes/:id/historia` con 6 pestañas (Resumen, Antecedentes, Examen, Diagnósticos, Evoluciones, Línea temporal), ficha editable + modales, botón **Historia** en el listado de pacientes y visibilidad por `AuthService.puede()` |
| Tests | ✅ backend **217/217** (+16: `HistoriaClinicaServiceTest`) · unit **42/42** (+8: `historia-clinica.spec.ts`) · e2e **36/36** (ruta nueva auditada en `qa`, `axe` y `hover`) |


### 2.7 Plan de ampliación — Fase C completada (2026-10-02)

De [`docs/PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md`](PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md) §7:

| Elemento de la Fase C | Estado |
|---|---|
| Esquema | ✅ `V29__odontograma.sql`: 3 tablas (`odontogramas` versionados con UNIQUE `historia + versión`, `odontograma_dientes` por diente FDI + superficie + condición + estado, `odontograma_eventos` con el log de cambios) |
| Backend | ✅ `OdontogramaService` + `OdontogramaController`: 8 endpoints bajo `/api/v1/historia-clinica` con `ODONTOGRAMA_READ/WRITE`, validación FDI (`400 DIENTE_INVALIDO`), nueva versión por copia, comparación entre versiones, eventos y auditoría con `AuditService` |
| Frontend | ✅ pestaña **Odontograma** en `/pacientes/:id/historia`: SVG con 32 piezas × 5 regiones (160 polígonos) en dentición permanente/temporal/mixta, tabla de condiciones editable desde teclado, modal de condición/estado, comparación y log de cambios; visibilidad por `AuthService.puede()` |
| Tests | ✅ backend **232/232** (+15: `OdontogramaServiceTest`) · unit **57/57** (+13: `odontograma.spec.ts`) · e2e **38/38** (+2: `e2e/odontograma.spec.ts`, dibujo + permisos de sólo lectura) |

### 2.8 Plan de ampliación — Fase D completada (2026-10-03)

De [`docs/PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md`](PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md) §4.8-§4.9 y §7:

| Elemento de la Fase D | Estado |
|---|---|
| Esquema | ✅ `V30__planes_tratamiento.sql`: 4 tablas (`planes_tratamiento` con los 8 estados, `plan_lineas` con fase/diente/superficie/prioridad/sesiones/precio/descuento/subtotal y `UNIQUE plan+numero`, `presupuestos` con totales y `vigente`, `presupuesto_lineas` como **snapshot** con `plan_linea_id ON DELETE SET NULL`) |
| Backend | ✅ `PlanTratamientoService` + `PlanTratamientoController`: **15 endpoints** bajo `/api/v1/planes-tratamiento` con `PLAN_TRATAMIENTO_READ/WRITE` (permisos que ya otorgaba `V27`); matriz de transiciones y estados editables validados en servidor (409 `TRANSICION_INVALIDA` / `ESTADO_ACTUAL` / `PLAN_NO_BORRADOR` / `PLAN_NO_EN_EJECUCION` / `PLAN_SIN_LINEAS` / `FIRMANTE_REQUERIDO`), recálculo de totales, FDI validado (`400 DIENTE_INVALIDO`), presupuestos congelados desde las líneas, aceptación firmada con `HashUtil.sha256Hex` + marca `vigente` que sincroniza el estado del plan (emitir presenta el plan), auditoría con `AuditService` y **PDF** con OpenPDF (`PlanPdfService`, `Content-Disposition: attachment`) |
| Frontend | ✅ pestaña **Planes** en `/pacientes/:id/historia` (8.ª pestaña, visible sólo con `PLAN_TRATAMIENTO_READ`): listado, detalle con totales, CRUD de líneas, cambio de estado por matriz, emisión y detalle de presupuestos, aceptación con firmante y descarga del PDF; modelos nuevos en `core/models.ts` y `Api.getBlob()` para descargas autenticadas |
| Tests | ✅ backend **251/251** (+19: `PlanTratamientoServiceTest`) · unit **68/68** (+11: `planes.spec.ts`) · e2e **41/41** (+3: `e2e/planes.spec.ts` — línea + presupuesto por UI, aceptación firmada, y recepción con `PLAN_TRATAMIENTO_READ` sin acciones de escritura) |
| Smoke | ✅ crear plan → línea (`subtotal 144.00`) → emitir presupuesto (plan pasa a `PRESENTADO`) → aceptar con firmante (plan `ACEPTADO`, `vigente`, firma de 64 hex) → PDF 1.9 kB · FDI `49` → `400 DIENTE_INVALIDO` · transición inválida → `409 TRANSICION_INVALIDA` |

### 2.9 Plan de ampliación — Fase E, periodontograma completada (2026-10-05)

> Primer entregable de la **Fase E — Clínica avanzada** (§4.11). Quedan pendientes de la
> Fase E: menores/tutores (§4.14), imágenes y archivos (§4.15) y reportes
> clínicos (§4.17). Ver §2.10 (recetas), §2.11 (consentimientos) y
> §2.12 (menores/tutores).

| Elemento | Estado |
|---|---|
| Esquema | ✅ `V31__periodontograma.sql`: 3 tablas (`examenes_periodontales` versionados con UNIQUE `historia + versión`, `periodontograma_sitios` con UNIQUE `examen + diente + sitio` y checks de rangos, `periodontograma_dientes` con UNIQUE `examen + diente` y movilidad/furcación 0–3) · `V32__periodoncia_recepcion.sql`: `PERIODONCIA_READ` concedido a `RECEPCION` (lectura igual que odontograma y planes) |
| Backend | ✅ `PeriodontogramaService` + `PeriodontogramaController`: **9 endpoints** bajo `/api/v1/historia-clinica` con `PERIODONCIA_READ/WRITE` (otorgados por `V27`): vigente, historial, detalle, comparar dos versiones, crear versión (copia la anterior), editar cabecera, guardar mediciones (upsert por diente/sitio), eliminar pieza y **PDF** (`PeriodontogramaPdfService`, OpenPDF con cabecera del consultorio y matriz diente × sitio). Validación FDI (`400 DIENTE_INVALIDO`), grados 0–3 (`MOVILIDAD_INVALIDA`/`FURCACION_INVALIDA`), rangos por Bean Validation (sondaje 0–20, margen −5 a 10, inserción 0–20) y auditoría con `AuditService` |
| Frontend | ✅ pestaña **Periodoncia** en `/pacientes/:id/historia` (9.ª pestaña, visible sólo con `PERIODONCIA_READ`): historial de exámenes, resumen clínico (sitios medidos, promedio de sondaje, sitios con sangrado/supuración, piezas con movilidad/furcación), matriz diente × sitio con celda `s/m/i` + `S P U` (en rojo si sangra o supura), modal de mediciones por pieza (4 sitios + movilidad/furcación/notas), comparación de dos versiones y descarga del PDF; modelos nuevos en `core/models.ts` y helpers exportados (`formatoMedicion`, `filasDe`, `promedioSondaje`, `piezasConMovilidad`) |
| Tests | ✅ backend **270/270** (+19: `PeriodontogramaServiceTest`) · unit **80/80** (+12: `periodoncia.spec.ts`, y `historia-clinica.spec.ts` pasa a 9 pestañas) · e2e **43/43** (+2: `e2e/periodoncia.spec.ts` — mediciones por UI + comparación + PDF por API, y recepción con `PERIODONCIA_READ` sin acciones de escritura) |
| Smoke | ✅ crear examen → mediciones de la 46 MESIAL `4.5` con movilidad 2 → FDI `49` → `400 DIENTE_INVALIDO` → movilidad 5 → `400 VALIDACION` → comparación de versiones → PDF `%PDF` 1.9 kB con `Content-Disposition: attachment` → examen inexistente → `404 NO_ENCONTRADO` |

### 2.10 Plan de ampliación — Fase E, recetas completada (2026-10-05)

> Segundo entregable de la **Fase E — Clínica avanzada** (§4.12). Quedan
> pendientes de la Fase E: menores/tutores (§4.14), imágenes y archivos
> (§4.15), timeline ampliada (§4.16) y reportes clínicos (§4.17).

| Elemento | Estado |
|---|---|
| Esquema | ✅ `V33__recetas.sql`: 2 tablas (`recetas` con paciente, odontólogo opcional, fecha, diagnóstico, observaciones, estado `VIGENTE/ANULADA` y motivo de anulación, con índice `paciente + fecha`; `receta_lineas` con número, medicamento, concentración, presentación, dosis, vía con CHECK de catálogo, frecuencia, duración, instrucciones y `UNIQUE receta + número`) y `RECETAS_READ` concedido a `RECEPCION` (**24 permisos**) |
| Backend | ✅ `RecetaService` + `RecetaController`: **6 endpoints** bajo `/api/v1/historia-clinica` con `RECETAS_READ/WRITE` (otorgados por `V27`): historial, detalle, alta con la pauta completa, edición (reemplaza las líneas con `flush` intermedio para respetar la unicidad `receta + número`), anulación motivada y **PDF** (`RecetaPdfService`, OpenPDF con cabecera del consultorio, tabla de medicamentos, instrucciones por línea y sello de anulación). Validación de pauta (`400 RECETA_SIN_LINEAS` / `LINEA_INVALIDA`), `409 RECETA_ANULADA` / `RECETA_YA_ANULADA`, `400 MOTIVO_REQUERIDO` y auditoría `CREAR/MODIFICAR/ANULAR_RECETA` |
| Frontend | ✅ pestaña **Recetas** en `/pacientes/:id/historia` (10.ª pestaña, visible sólo con `RECETAS_READ`): historial con estado y nº de medicamentos, detalle con la pauta completa, alta/edición con editor de líneas (catálogo de 9 vías), anulación con motivo obligatorio y descarga del PDF; modelos nuevos en `core/models.ts` y helpers exportados (`etiquetaVia`, `etiquetaEstado`, `toneEstado`, `lineaVacia`, `lineaValida`, `pautaTexto`) |
| Tests | ✅ backend **286/286** (+16: `RecetaServiceTest`) · unit **91/91** (+11: `recetas.spec.ts`, y `historia-clinica.spec.ts` pasa a 10 pestañas) · e2e **45/45** (+2: `e2e/recetas.spec.ts` — alta y anulación por UI + PDF por API, y recepción con `RECETAS_READ` sin acciones de escritura) |
| Smoke | ✅ crear con 2 líneas (números 1–2) → línea sin dosis `400 VALIDACION` → pauta vacía `400 RECETA_SIN_LINEAS` → editar (2 líneas → 1) → anular sin motivo `400` → anular (motivo registrado) → editar anulada `409 RECETA_ANULADA` → historial del paciente → PDF `%PDF` 1.8 kB con `attachment` → inexistente `404 NO_ENCONTRADO` → recepción con `RECETAS_READ` |

### 2.11 Plan de ampliación — Fase E, consentimientos completada (2026-10-05)

> Tercer entregable de la **Fase E — Clínica avanzada** (§4.13). Quedan
> pendientes de la Fase E: imágenes y archivos (§4.15), timeline ampliada
> (§4.16) y reportes clínicos (§4.17).

| Elemento | Estado |
|---|---|
| Esquema | ✅ `V34__consentimientos.sql`: 2 tablas (`consentimiento_plantillas` con `UNIQUE tenant_id + codigo` y `version`; `consentimientos` con snapshot del texto, `plantilla_codigo`/`plantilla_version`, fecha, tutor, odontólogo, estado `BORRADOR/FIRMADO`, firmante, `firma_imagen` (data URL), `firma_hash` SHA-256, `firmado_en` y `created_by`, con índices por paciente/fecha/estado) · 8 plantillas sembradas por tenant (`TRATAMIENTO_GENERAL`, `EXTRACCION`, `ENDODONCIA`, `IMPLANTE`, `CIRUGIA_ORAL`, `ORTODONCIA`, `IMAGENES`, `BLANQUEAMIENTO`) · `CONSENTIMIENTOS_READ` concedido a `RECEPCION` (**25 permisos**) |
| Backend | ✅ `ConsentimientoService` + `ConsentimientoController`: **9 endpoints** bajo `/api/v1/historia-clinica` con `CONSENTIMIENTOS_READ/WRITE` (otorgados por `V27`): catálogo y edición de plantillas (sube `version` en 1), historial, detalle, emisión en `BORRADOR` **copiando el texto de la plantilla**, edición y borrado **sólo en borrador**, firma (calcula `firmaHash` con `HashUtil.sha256Hex` y congela el documento) y **PDF** (`ConsentimientoPdfService`, OpenPDF con cabecera del consultorio, párrafos del texto, bloque de firma con imagen opcional y marca `BORRADOR SIN FIRMAR`). Validación `PLANTILLA_NO_ENCONTRADA` (404), `FIRMANTE_REQUERIDO` (400), `CONSENTIMIENTO_FIRMADO` y `CONSENTIMIENTO_YA_FIRMADO` (409) y auditoría `CREAR/MODIFICAR/ELIMINAR/FIRMAR_CONSENTIMIENTO` + `MODIFICAR_PLANTILLA_CONSENTIMIENTO` |
| Frontend | ✅ pestaña **Consentimientos** en `/pacientes/:id/historia` (11.ª pestaña, visible sólo con `CONSENTIMIENTOS_READ`): historial con estado y firmante, lectura del texto congelado y bloque de firma con la huella, alta (selector de plantilla) y edición en borrador, **modal de firma con lienzo** (`pointerdown/pointermove`, `toDataURL` opcional), modal de plantillas con selector y textarea, borrado sólo en borrador y descarga del PDF; modelos nuevos en `core/models.ts` y helpers exportados (`etiquetaEstadoConsentimiento`, `toneEstadoConsentimiento`, `parrafos`) |
| Tests | ✅ backend **305/305** (+19: `ConsentimientoServiceTest`) · unit **103/103** (+12: `consentimientos.spec.ts`, y `historia-clinica.spec.ts` pasa a 11 pestañas) · e2e **47/47** (+2: `e2e/consentimientos.spec.ts` — emisión, firma con dibujo en el lienzo y PDF por API + rechazo de edición/borrado de un firmado, y recepción con `CONSENTIMIENTOS_READ` sin acciones de escritura) |
| Smoke | ✅ 8 plantillas en el catálogo → editar plantilla (`version` 1→2) → plantilla inexistente `404 PLANTILLA_NO_ENCONTRADA` → emitir borrador (copia el texto y la versión) → plantilla desconocida `404` → editar borrador → firmar sin firmante `400 FIRMANTE_REQUERIDO` → firmar (huella SHA-256 de 64 hex + imagen) → firmar 2 veces `409 CONSENTIMIENTO_YA_FIRMADO` → editar/borrar firmado `409 CONSENTIMIENTO_FIRMADO` → historial → PDF `%PDF` 2.2 kB con `attachment` → inexistente `404 NO_ENCONTRADO` → recepción `200` |
| Higiene e2e | ✅ `recetas.spec.ts` y `planes.spec.ts` seleccionan la fila con `.first()` (evita el fallo por duplicados entre corridas) |

### 2.12 Plan de ampliación — Fase E, menores completada (2026-10-05)

> Cuarto entregable de la **Fase E — Clínica avanzada** (§4.14). Los
> tutores de `paciente_tutores` (`V12`) se reutilizan en los
> consentimientos de menores. Quedan pendientes de la Fase E: timeline
> ampliada (§4.16) y reportes clínicos (§4.17).

| Elemento | Estado |
|---|---|
| Esquema | ✅ `V35__tutores_consentimientos.sql`: `tutor_id`, `tutor_parentesco` y `tutor_telefono` en `consentimientos` (**referencia débil, sin FK**, porque los tutores se reescriben con `clear()` al editar el paciente) + índice `idx_consentimientos_tutor` |
| Backend | ✅ `ConsentimientoService` con el snapshot del tutor (`aplicarTutor`) y tres reglas: emitir de un menor sin tutor → **400 `TUTOR_REQUERIDO`**, tutor que no pertenece al paciente → **404 `TUTOR_NO_ENCONTRADO`**, firmar un consentimiento de menor con un firmante distinto del tutor → **400 `FIRMANTE_DEBE_SER_TUTOR`** (comparación sin mayúsculas ni espacios dobles); al firmar se guarda el nombre canónico del tutor. `tutorId` aceptado en `NuevoConsentimientoRequest` y `EditarConsentimientoRequest`; `tutorId/tutorParentesco/tutorTelefono` devueltos en historial y detalle. El PDF muestra la línea `Tutor / representante: Nombre (PARENTESCO) · teléfono` |
| Frontend | ✅ selector `#con-tutor-sel` con los tutores del paciente (opción de texto libre `#con-tutor` si no hay), etiqueta `etiquetaTutor`, aviso de menor en la pestaña y en el modal de firma, y validación en cliente antes de enviar la firma; modelos `Consentimiento`/`ConsentimientoResumen` con los tres campos nuevos |
| Datos | ✅ 2 menores en BD: paciente **11** (Dulce Ortiz Guerrero, n. 2024-12-14, tutor **4** MADRE Annie Charlotte Guerrero Almeida) y paciente **3** (Alis Ortiz, n. 2021-02-06, tutor **3** PADRE Alexis Ortiz) |
| Tests | ✅ backend **312/312** (+7: `ConsentimientoServiceTest` — snapshot, tutor ajeno, menor sin tutor al emitir y al editar, firma con nombre distinto, firma con el tutor en otro formato y control adulto) · unit **108/108** (+5: `etiquetaTutor`, `esMenor`, selector de tutor, validación de menor y bloqueo de firma) · e2e **48/48** (+1: `e2e/consentimientos.spec.ts` — bloqueos por API, selector de tutor, rechazo y firma del tutor, PDF) |
| Smoke | ✅ `/tmp/opencode/smoke_consentimientos.sh`: paciente menor con tutor → `400 TUTOR_REQUERIDO` → `404 TUTOR_NO_ENCONTRADO` → emitir con `tutorId` (guarda nombre/parentesco) → `400 FIRMANTE_DEBE_SER_TUTOR` → firmar con el tutor (nombre canónico + huella) → PDF con la línea de tutor → adulto firma con su nombre |

### 2.13 Plan de ampliación — Fase E, archivos clínicos completada (2026-10-05)

> Quinto entregable de la **Fase E — Clínica avanzada** (§4.15). Primera
> implementación real de `FileStorageProvider` (sembrado en la Fase A).
> Quedan pendientes de la Fase E: timeline ampliada (§4.16) y reportes
> clínicos (§4.17).

| Elemento | Estado |
|---|---|
| Esquema | ✅ `V36__archivos_clinicos.sql`: tabla `archivos_clinicos` con `categoria` (CHECK `RADIOGRAFIA/FOTOGRAFIA/PANORAMICA/TOMOGRAFIA/RESULTADO/DOCUMENTO`), nombre original, `clave` de almacenamiento, `mime`, `tamano > 0`, `sha256`, fecha del estudio, observaciones e índices por paciente/fecha; **sólo metadata en PostgreSQL, sin BLOB** |
| Backend | ✅ `ArchivoClinicoService` + `ArchivoClinicoController` (4 endpoints en `/api/v1/historia-clinica` con `ARCHIVOS_CLINICOS_READ/WRITE`, ya sembrados en `V27`): alta multipart (categoría, extensión JPG/PNG/WEBP/TIFF/PDF/DCM y máximo 20 MB), listado, descarga del contenido con el `mime` real y `Content-Disposition: inline`, y borrado que elimina también el archivo físico; errores `CATEGORIA_REQUERIDA`/`CATEGORIA_INVALIDA`, `ARCHIVO_VACIO`, `ARCHIVO_DEMASIADO_GRANDE`, `ARCHIVO_FORMATO_INVALIDO`, `ARCHIVO_NOMBRE_INVALIDO`, `ARCHIVO_FISICO_NO_ENCONTRADO` y auditoría `SUBIR_ARCHIVO`/`ELIMINAR_ARCHIVO`. Multipart global subido a 25 MB y `MaxUploadSizeExceededException` → `400 ARCHIVO_DEMASIADO_GRANDE` |
| Almacenamiento | ✅ `LocalFileStorageProvider` con claves `tenant-1/paciente-<id>/archivos/<uuid>-<nombre>` dentro del volumen `storage_data` (`/app/data/archivos`), anti-traversal y sin exponer rutas físicas; si la metadata no llega a guardarse se borra el archivo recién escrito |
| Frontend | ✅ pestaña **Archivos** en `/pacientes/:id/historia` (12.ª pestaña, visible sólo con `ARCHIVOS_CLINICOS_READ`): alta con `#arc-archivo`/`#arc-categoria`/`#arc-fecha`/`#arc-obs` y validación en cliente (formato, 20 MB, categoría), tabla con categoría, fecha, tamaño y acciones, **vista previa de imágenes en diálogo** (blob + `URL.createObjectURL`), descarga con el nombre original y borrado con confirmación; sin `ARCHIVOS_CLINICOS_WRITE` no hay formulario ni botón Eliminar |
| Tests | ✅ backend **326/326** (+14: `ArchivoClinicoServiceTest`) · unit **120/120** (+12: `archivos.spec.ts`, y `historia-clinica.spec.ts` pasa a 12 pestañas) · e2e **50/50** (+2: `e2e/archivos.spec.ts` — subida, validación de formato, vista previa, descarga con nombre original, borrado con confirmación y contenido 404 tras borrar + recepción que sólo lee y descarga) |
| Smoke | ✅ `/tmp/opencode/smoke_archivos.sh` (14 pasos): listado → `400 CATEGORIA_INVALIDA` → `400 ARCHIVO_FORMATO_INVALIDO` → `400 ARCHIVO_DEMASIADO_GRANDE` (21 MB) → subida 201 con SHA-256 → archivo en `/app/data/archivos/tenant-1/...` → descarga `image/png` → `404` inexistente/paciente → borrado 204 con archivo físico eliminado → recepción `200`/`403` → odontólogo `201` → limpieza |

---

### 2.14 Plan de ampliación — Fase E, línea temporal ampliada completada (2026-10-06)

> Sexto entregable de la **Fase E — Clínica avanzada** (§4.16). Sin
> migración: todo se compone de datos que ya existen. Con §2.15 (reportes
> clínicos) la Fase E queda cerrada.

| Elemento | Estado |
|---|---|
| Backend | ✅ `TimelineService` nuevo (los 12 repositorios como fuentes) que sustituye al método `timeline()` de `HistoriaClinicaService`; `GET /api/v1/historia-clinica/pacientes/{id}/timeline` sigue exigiendo `HISTORIA_CLINICA_READ` y ahora devuelve **11 tipos**: `CITA` (atendidas), `EXAMEN`, `DIAGNOSTICO`, `EVOLUCION`, `ODONTOGRAMA` (cada versión), `PERIODONCIA` (los controles), `PLAN` (estado + nº de líneas + total), `PROCEDIMIENTO` (sólo líneas `EN_EJECUCION`/`COMPLETADO`, con pieza/superficie), `RECETA` (estado + nº de medicamentos), `ARCHIVO` (categoría + tamaño) y `CONSENTIMIENTO` (estado + firmante) |
| Comportamiento | ✅ 25 eventos por fuente y **100 en total**, orden por fecha descendente con desempate por tipo e id; los eventos de paciente (citas, planes, recetas, archivos, consentimientos) se devuelven **aunque todavía no exista historia clínica**; `404 NO_ENCONTRADO` sólo si el paciente no existe; sin token → `403` (igual que el resto de la API) |
| Permisos | ✅ Sin permisos nuevos: `HISTORIA_CLINICA_READ` basta porque los cuatro roles tienen además `ODONTOGRAMA_READ`, `PLAN_TRATAMIENTO_READ`, `PERIODONCIA_READ`, `RECETAS_READ`, `ARCHIVOS_CLINICOS_READ` y `CONSENTIMIENTOS_READ` |
| Frontend | ✅ `TipoTimeline` en `models.ts`; pestaña **Línea de tiempo** con chips de filtro por tipo (con conteo), lista filtrada con su propio estado vacío, `ETIQUETAS_TIMELINE`/`TONES_TIMELINE` para que el badge muestre siempre etiqueta legible (Consulta, Evolución, Procedimiento…) y un color por punto en el eje |
| Tests | ✅ backend **333/333** (+9: `TimelineServiceTest` — fuentes completas, orden, procedimientos sólo en ejecución/completados, citas no atendidas fuera, límite por fuente y global; −2 tests de timeline movidos de `HistoriaClinicaServiceTest`) · unit **124/124** (+4 en `historia-clinica.spec.ts`) · e2e **52/52** (+2: `e2e/timeline.spec.ts` — agregación de fuentes y filtro por tipo con recuento, y recepción que sólo consulta) |
| Smoke | ✅ `/tmp/opencode/smoke_timeline.sh` (4 pasos): 200 con **47 eventos de 9 tipos** ordenados desc, título y detalle en los seis tipos nuevos, `404` de paciente inexistente y `403` sin token |

---

### 2.15 Plan de ampliación — Fase E, reportes clínicos completada (2026-10-06)

> Séptimo y último entregable de la **Fase E — Clínica avanzada** (§4.17).
> Sin migración: `REPORTES_CLINICOS` ya venía sembrado en `V27` y ambos
> endpoints sólo leen tablas existentes.

| Elemento | Estado |
|---|---|
| Backend | ✅ `ReporteClinicoService` + `ReporteClinicoController`: `GET /api/v1/historia-clinica/reportes` (`desde`/`hasta`/`limite`, defecto = último año con `limite` 10 acotado a 1–50, `400 RANGO_INVALIDO` si el rango se invierte) con totales del período, **diagnósticos frecuentes** (`GROUP BY nombre` con pacientes distintos), **tratamientos pendientes** (líneas `PENDIENTE`/`EN_EJECUCION` de planes no cancelados, ordenadas por prioridad y fecha), **continuidad** (citas por estado, pacientes activos, sin próxima cita y los que no tuvieron actividad en 90 días) y **producción clínica** (líneas `COMPLETADO` agrupadas por procedimiento con su suma) |
| PDF | ✅ `HistoriaCompletaService` arma las secciones —antecedentes, exámenes, diagnósticos, evoluciones con adendas, odontograma vigente, periodoncia, planes, recetas, consentimientos, archivos y citas atendidas— y `HistoriaCompletaPdfService` las renderiza en PDF (OpenPDF con la cabecera del consultorio, `Content-Disposition: attachment`); `GET /pacientes/{id}/reportes/historia-completa.pdf`, `404 NO_ENCONTRADO` si el paciente no existe |
| Permisos | ✅ Sin permisos nuevos: ambos endpoints exigen `REPORTES_CLINICOS` (V27 → SUPER_ADMIN, ADMIN y ODONTOLOGO); la recepción no lo tiene y recibe `403`, igual que sin token |
| Frontend | ✅ Página **`/reportes-clinicos`** (`ReportesClinicosComponent`, entrada del menú **Gestión** con `permiso: REPORTES_CLINICOS`): seis tarjetas de totales, fechas y límite editables, tablas de diagnósticos frecuentes, producción clínica, tratamientos pendientes (badge de prioridad), continuidad y pacientes sin actividad en 90 días, con estado vacío propio y mensaje específico en `403`; botón **Historia completa (PDF)** en la cabecera de `/pacientes/:id/historia`, visible sólo con `REPORTES_CLINICOS` y descarga vía blob |
| Tests | ✅ backend **351/351** (+18: `ReporteClinicoServiceTest` — rango por defecto y acotación del límite, totales, mapeo de frecuentes/pendientes/producción, continuidad y actividad por paciente; `HistoriaCompletaServiceTest` — cabecera sin historia, secciones, adendas, odontograma vigente, medicamentos de la receta y firmante del consentimiento) · unit **130/130** (+6: `reportes-clinicos.spec.ts`) · e2e **55/55** (+3: `e2e/reportes-clinicos.spec.ts` — menú y página con los seis totales, descarga real del PDF `%PDF` con nombre correcto y recepción sin menú/botón con `403`; `permisos.spec.ts` pasa a esperar la sección **Gestión** del odontólogo, que ahora sí tiene `REPORTES_CLINICOS`) |
| Smoke | ✅ `/tmp/opencode/smoke_reportes.sh` (7 pasos): `200` por defecto y con rango/límite, `400 RANGO_INVALIDO`, PDF con `Content-Disposition` y `%PDF` de 5 966 bytes, `404` de paciente inexistente, `403` de recepción en los dos endpoints y `403` sin token |

### 2.16 Plan de ampliación — Fase F, finanzas internas completada (2026-10-07)

> Primer entregable de la **Fase F — Finanzas internas** (§3.3 cuenta, §3.4
> caja, §3.12 dashboard y §3.13 reportes), **sin SRI**: sólo la capa financiera
> interna, que sigue funcionando cuando en la Fase G llegue el comprobante
> electrónico.

| Elemento | Estado |
|---|---|
| Esquema | ✅ `V37__cuenta_paciente.sql` (2 tablas: `cuentas_paciente` con `UNIQUE tenant_id + paciente_id`, estados `ABIERTA/PARCIAL/PAGADA` derivados más `CERRADA/ANULADA` manuales y totales denormalizados; `cuenta_detalles` como libro mayor con `CARGO/DESCUENTO/ABONO/ANTICIPO/DEVOLUCION`, `total >= 0` con signo según tipo, `forma_pago` obligatoria en abonos/anticipos/devoluciones, `odontologo_id`/`servicio_id` para reportes e índice único parcial `(cuenta_id, origen, referencia_id) WHERE referencia_id IS NOT NULL` para idempotencia) y `V38__caja.sql` (2 tablas: `cajas` con **una sesión `ABIERTA` por tenant** vía índice único parcial y `movimientos_caja` con `INGRESO/EGRESO` y `cuenta_detalle_id` de enlace) → **54 tablas**, Flyway `V38` aplicada en el contenedor |
| Dominio | ✅ Enum `EstadoCuenta`, `TipoMovimientoCuenta`, `OrigenMovimientoCuenta`, `FormaPago`, `EstadoCaja` y `TipoMovimientoCaja` + `CuentaPaciente`, `CuentaDetalle`, `Caja` y `MovimientoCaja` con sus cuatro repositorios |
| Backend | ✅ `CuentaPacienteService` (con el record `CargoProcedimiento`), `CajaService` y `ReporteFinancieroService` con **18 endpoints** en `CuentaPacienteController`, `CajaController` y `ReporteFinancieroController` bajo `/api/v1/facturacion`: cuentas (listado con `estado`/`buscar`, detalle con auto-creación, movimientos, anulación, `PUT /estado` y `GET /pagos?desde&hasta&formaPago&pacienteId`), caja (`GET /cajas/actual`, apertura, cierre con conciliación, historial, detalle, movimientos y anulación) y finanzas (`/dashboard` + `/reportes/{ventas,produccion,saldos,caja}`) |
| Reglas | ✅ `saldo = ΣCARGO − ΣDESCUENTO − ΣABONO − ΣANTICIPO + ΣDEVOLUCION`; conciliación `esperado = apertura + neto efectivo` y `diferencia = declarado − esperado`; errores `MONTO_INVALIDO`, `DESCUENTO_INVALIDO`, `DESCUENTO_EXCEDE`, `FORMA_PAGO_REQUERIDA`, `FORMA_PAGO_INVALIDA`, `SALDO_INSUFICIENTE`, `CUENTA_BLOQUEADA`, `CUENTA_CON_DINERO`, `ESTADO_ACTUAL`, `ESTADO_INVALIDO`, `REAPERTURA_INVALIDA`, `CONCEPTO_REQUERIDO`, `CANTIDAD_INVALIDA`, `MOVIMIENTO_NO_ENCONTRADO/YA_ANULADO`, `CAJA_ABIERTA`, `CAJA_NO_ABIERTA`, `CAJA_NO_ENCONTRADA`, `CAJA_CERRADA` y `RANGO_INVALIDO`; auditoría con `AuditService` (`MODULO=FACTURACION`) |
| Integración §5 | ✅ `PlanTratamientoService.cambiarEstadoLinea` llama a `integrarCuenta()`: al pasar una línea a `COMPLETADO` crea el cargo (`origen PROCEDIMIENTO`, idempotente por `referencia_id`, reactiva el cargo anulado) y al revertirlo lo anula. Nunca bloquea el flujo clínico (una cuenta `CERRADA` recibe el cargo; una `ANULADA` no) y los cobros se reflejan solos en caja (`CajaService.registrarDesdeCuenta`/`anularPorDetalle`) |
| Permisos | ✅ Sin permisos nuevos: `FACTURACION_READ/WRITE/EMITIR/ANULAR`, `CAJA_READ/WRITE/CERRAR` y `REPORTES_FINANCIEROS` ya venían sembrados en `V27`; SUPER_ADMIN/ADMIN los tienen todos, RECEPCION `FACTURACION_READ/WRITE` + `CAJA_READ/WRITE` (sin `CAJA_CERRAR` ni `REPORTES_FINANCIEROS`) y ODONTOLOGO ninguno, por lo que el menú **Finanzas** no se pinta para él |
| Frontend | ✅ Módulo `frontend/src/app/facturacion/` con cuatro páginas y la sección de menú **Finanzas**: `/facturacion` (dashboard con rango, 6 tarjetas, facturado/cobrado de hoy y del mes, y desgloses por forma de pago, odontólogo y servicio), `/facturacion/cuentas` (listado con filtros, detalle con el libro de movimientos, alta de cargo/abono/descuento/anticipo/devolución, anulación y cerrar/reabrir la cuenta), `/facturacion/caja` (apertura con fondo inicial, totales y conciliación, ingresos/egresos manuales, cierre con efectivo declarado e historial de sesiones) y `/facturacion/reportes` (pestanas ventas/producción/saldos/caja con rango); `data-testid` propios, `403` con mensaje específico y estado vacío en cada tabla |
| Tests | ✅ backend **386/386** (+35: `CuentaPacienteServiceTest` 18 — altas, anulación, estados y bloqueos; `CajaServiceTest` 10 — una sesión abierta, conciliación y anulación; `ReporteFinancieroServiceTest` 7 — dashboard, cuatro reportes y rango invertido; `PlanTratamientoServiceTest` adaptado a la nueva dependencia) · unit **155/155** (+25 en 4 specs: `dashboard`, `cuentas`, `caja` y `reportes-financieros.spec.ts`) · e2e **61/61** (+5 de Fase F: `e2e/facturacion.spec.ts` — menú y dashboard, cargo con detalle, caja con egreso, pestañas de reportes y recepción sin `REPORTES_FINANCIEROS`; +1 por el menú de acciones: `axe.spec.ts` audita también el menú abierto); las cuatro rutas nuevas entran en `qa`/`axe`/`foco`/`hover` (**23 → 27**) |
| Smoke | ✅ `/tmp/opencode/smoke_finanzas.sh` (16 pasos): cuenta auto-creada, cargo y abono, `400 FORMA_PAGO_REQUERIDA`, `409 SALDO_INSUFICIENTE`, pagos del mes, apertura→egreso→cierre con `diferencia -5`, dashboard, los 4 reportes, `400 RANGO_INVALIDO`, `403` de recepción sin `REPORTES_FINANCIEROS` ni `CAJA_CERRAR` y `403` sin token; la integración §5 se confirma en BD con `cuenta_detalles.origen = PROCEDIMIENTO` |

---

### 2.17 Menú de acciones en las tablas (2026-10-07)

Refactor de UI: la columna de acciones deja de apilar botones y pasa **todas**
sus opciones a un menú desplegable, con un único disparador por fila (icono de
menú + flecha hacia abajo y `aria-label` con el nombre de la fila).

| Elemento | Detalle |
|---|---|
| Patrón | `mat-menu` de Angular Material con disparador `.btn.small.menu-trigger`; ítem destructivo con la clase `peligro` (color `--ls-danger`) |
| Alcance | **24 celdas en 20 plantillas**: agenda, agente IA, automatizaciones, plantillas, citas, Google Calendar, horarios (×2), odontólogos, pacientes, servicios, usuarios, sesiones WhatsApp, archivos, historia clínica (×2), odontograma, periodoncia, planes (×2), recetas y facturación (caja ×1, cuentas ×2). Siempre menú, aunque la fila sólo tenga una opción |
| Fuera de alcance | Selectores de fila (nombre, fecha o `+`/`−` de expansión) y controles de estado en su propia columna (On/Off del agente, "Activa" de automatizaciones, `select` de estado de línea en planes) |
| Accesibilidad | Disparador con nombre accesible ("Acciones de …") y `aria-haspopup`/`aria-expanded` del propio `matMenuTriggerFor`, ítems `role="menuitem"`, Escape devuelve el foco; el contenedor de overlays lleva `role="region"` + etiqueta desde `core/overlay-landmark.ts` para que la regla `region` de axe no lo marque como contenido fuera de landmarks (sin depender de CDK: +47 kB en el bundle inicial) |
| Estilos | `src/styles/_menu.scss`: tipografía de la app heredada, ítems de 40 px y color de `.peligro` |
| Tests | Helpers `e2e/actions.ts` (`abrirAcciones`, `accionFila`, `abrirDesactivar`) usados por `archivos`, `confirm`, `qa`, `axe`, `facturacion`, `odontograma`, `periodoncia` y `planes`; `axe.spec.ts` audita el menú abierto · gates en verde: lint ✔ · unit **155/155** · e2e **61/61** · build **717.63 kB / 146.90 kB gzip** |

### 2.18 Plan de ampliación — Fase G, SRI en pruebas completada (2026-10-09)

> **Fase G — SRI** (§3.5, §3.6, §3.7, §3.8, §3.11): configuración tributaria,
> secuenciales, XML con XSD, firma **simulada**, autorización en el SRI de
> pruebas, RIDE en PDF y **bandeja de comprobantes**. Queda **G.6** como
> gate final de cierre (docs, smoke productivo y checklist de §9).

| Elemento | Estado |
|---|---|
| Esquema | ✅ `V41__comprobantes_electronicos.sql` + `V42__comprobante_totales.sql` (**Flyway V42**, 56 tablas): `comprobantes_electronicos` con clave de 49 dígitos, secuencial por establecimiento+punto de emisión, totales denormalizados, XML almacenado y estado SRI; índices para la bandeja |
| Motor | ✅ `ComprobanteElectronicoService`: genera XML (`SriXmlBuilder`) con XSD oficial validado, clave `1` de pruebas, `codigoPorcentaje 4` (IVA 13%), formas de pago del catálogo `01/16/19/20` y **firma simulada** (`simulado=true`; si el ambiente es `PRODUCCION` y sigue simulada → `SRI_FIRMA_REAL_REQUERIDA`); envío al SRI con reintentos (`COMPROBANTE_NO_REINTENTABLE`), autorización persistida y rollback del secuencial si falla la generación |
| Endpoints | ✅ `ComprobanteElectronicoController` bajo `/api/v1/facturacion`: `GET /comprobantes` (bandeja paginada con `q`, `desde`, `hasta`, `tipo`, `estado`, `estadoGeneracion`, `pacienteId`, `establecimiento`, `puntoEmision`), `GET /comprobantes/{id}`, `GET /comprobantes/{id}/xml` (`Content-Disposition`, `PERMISO_FACTURACION_READ`), `POST /comprobantes/{id}/reenviar` y `POST /comprobantes/{id}/consultar` (`PERMISO_FACTURACION_EMITIR`) |
| Emisión | ✅ Botón **Emitir factura** en `/facturacion/cuentas` (sobre los cargos de la cuenta, `CUENTA_YA_FACTURADA` en 409) y `POST /api/v1/facturacion/comprobantes` (body `{cuentaId, fechaEmision?, formasPago[]}`); el comprobante emitido se muestra en la misma página con enlace a la bandeja |
| Reglas | ✅ Errores `FORMA_PAGO_INVALIDA`, `FORMAS_PAGO_NO_COINCIDEN`, `CUENTA_YA_FACTURADA`, `COMPROBANTE_YA_AUTORIZADO`, `COMPROBANTE_NO_REINTENTABLE`, `COMPROBANTE_SIN_XML`, `COMPROBANTE_NO_ENCONTRADA`, `TIPO_COMPROBANTE_INVALIDO`, `ESTADO_INVALIDO`, `CUENTA_ANULADA`; el filtro de fechas usa sentinelas (`1900-01-01`/`9999-12-31`) para evitar el `could not determine data type of parameter` de Postgres con `LocalDate` nulo |
| Auditoría | ✅ `AuditoriaRepository` ampliado con `entidad` + `entidadId` (JPQL seguro, sin `jsonb` en la consulta: la columna `datos_previos`/`datos_nuevos` es `jsonb` y no es searchable); la página `/auditoria` lee esos parámetros de la URL y muestra el chip **“Historial de COMPROBANTE_ELECTRONICO #id”** |
| RIDE | ✅ `RideDatos` + `RideXmlParser` + `RideGenerator` (OpenPDF con cabecera, QR simulado y bloque de firma) |
| Frontend | ✅ Página **`/facturacion/comprobantes`** (`FacturacionComprobantesComponent`, ítem **Comprobantes** del menú Finanzas): filtros (texto, fechas, tipo, estado SRI, generación), tabla paginada con helpers `paginasDe`/`totalDe`, panel de detalle con XML/RIDE/reenviar/consultar y su historial de auditoría |
| Marca | ✅ **Renombrado a CRM Dental AT (ALANTEK)**: título del navegador, login, sidebar, pie de app y de login, mensajes del asistente IA y Swagger; wordmark tipográfico `.alantek` (distintivo con el primario + texto uppercase), oculto con el sidebar colapsado; el plan de ampliación pasó a llamarse `PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md` |
| Tests | ✅ backend **472/472** (+86 en total de la Fase G) · unit **170/170** (+15, incluye `comprobantes.spec.ts` y las dos pruebas de emisión en `cuentas.spec.ts`) · e2e **63/63** (+2 en `e2e/comprobantes.spec.ts`) · build **720.52 kB / 147.39 kB gzip** |
| Smoke | ✅ `/tmp/opencode/smoke_sri.sh` (8 pasos, contra el backend reconstruido): bandeja paginada (4 comprobantes), filtros `desde`/`hasta`/`tipo`/`estado`/`q`, `400 TIPO_COMPROBANTE_INVALIDO`, detalle + XML (`Content-Disposition`) + RIDE `%PDF`, `409` al reenviar y `200` al consultar, auditoría enlazada por `entidad`/`entidadId`, **emisión completa** (cargo → `POST /comprobantes` → `AUTORIZADO` simulado → RIDE), `409 CUENTA_YA_FACTURADA`, `400 FORMA_PAGO_INVALIDA` y `400 FORMAS_PAGO_NO_COINCIDEN`; Swagger vivo con el título **CRM Dental AT API** |

### 2.19 Plan de ampliación — Fase H.1, notas de crédito y anulación (2026-10-09)

> **Fase H.1** (§3.9 *Nota de crédito*, §3.10 *Anulación*): documentos que
> modifican una factura ya autorizada, con la misma cadena que la factura
> (clave → XML → XSD → firma → envío → autorización → RIDE). Quedan pendientes
> de la Fase H las **notas de débito** y las **retenciones**, y **H.2** es la
> interfaz de estas acciones.

| Elemento | Estado |
|---|---|
| Esquema | ✅ `V43__notas_de_credito.sql` (**Flyway V43**, 57 tablas): secuencial por **tipo** de comprobante (`uq_comp_secuencial` con `tipo_comprobante`), `comprobante_referencia_id` + `motivo` en los comprobantes y la tabla `anulaciones_comprobante` (motivo, estado `PENDIENTE/APLICADA/RECHAZADA`, resultado, mensaje, usuario, fechas) |
| Esquema XSD | ✅ `sri/NotaCredito_V1.1.0.xsd` (SHA-256 `379e9f27…`) junto a `factura_V1.1.0.xsd`; `SriXmlValidator` es **multi-esquema**: elige el XSD por el elemento raíz (`factura`, `notaCredito`) y tolera la declaración `<?xml?>` al detectarlo |
| Motor | ✅ `NotaCreditoBuilder` lee el **XML de la factura original como fuente de verdad** (comprador, establecimiento, líneas e impuestos) y devuelve total —todas las líneas— o parcial —`{linea, cantidad}` con prorrateo del descuento y recálculo del impuesto—; `SriXmlGenerator.generarNotaCredito` emite `<notaCredito>` 1.1.0 con `codigoInterno`, `codDocModificado`/`numDocModificado`/`fechaEmisionDocSustento`, `valorModificacion` y `<motivo>` (sin `direccionComprador`, sin `pagos` y sin `tarifa` en `totalImpuesto`, como exige el esquema). Clave de acceso con `codDoc` **`04`** y **secuencial propio** de notas de crédito |
| Reglas | ✅ Sólo sobre una factura `AUTORIZADO` (`409 COMPROBANTE_NO_AUTORIZADO`) y no anulada (`409 COMPROBANTE_ANULADO`); `motivo` obligatorio (5–300); cantidad por línea ≤ la facturada (`CANTIDAD_EXCEDE`), líneas duplicadas/rango (`LINEA_DUPLICADA`/`LINEA_INVALIDA`); **tope global** `409 NOTA_CREDITO_EXCEDE` (lo ya devuelto con notas vigentes + la nueva ≤ total de la factura); la factura original se carga con bloqueo pesimista, así que dos notas simultáneas no escapan del tope |
| Anulación | ✅ `POST /comprobantes/{id}/anular` con `FACTURACION_ANULAR`: sólo facturas y notas `AUTORIZADO`, una vez (`409 COMPROBANTE_YA_ANULADO`, `409 ANULACION_YA_SOLICITADA`); **nunca se borra la fila** — pasa a `ANULADO` y queda el trámite con motivo, usuario, fechas y resultado. La cuenta vuelve a ser facturable (el comprobante `ANULADO` ya no la bloquea) |
| Endpoints | ✅ `POST /api/v1/facturacion/notas-credito` y `POST /api/v1/facturacion/comprobantes/{id}/anular`, ambos con `PERMISO_FACTURACION_ANULAR` (V27); `ComprobanteElectronicoResponse` y `ComprobanteResumenResponse` añaden `comprobanteReferenciaId` y `motivo` |
| RIDE | ✅ `RideXmlParser` acepta `infoFactura` **e** `infoNotaCredito` (importe total desde `valorModificacion`, código de línea desde `codigoInterno`, tarifa deducida cuando el XSD no la trae) y `RideGenerator` pinta «NOTA DE CRÉDITO ELECTRÓNICA» |
| Auditoría | ✅ `GENERAR_NOTA_CREDITO_SRI` sobre la nota nueva y `SOLICITAR_ANULACION_COMPROBANTE` → `ANULAR_COMPROBANTE_SRI` → `APLICAR_ANULACION_COMPROBANTE` sobre la factura, todos con `entidad=COMPROBANTE_ELECTRONICO` (enlazables desde la bandeja) |
| Tests | ✅ backend **500/500** (+28: `NotaCreditoBuilderTest` 12, casos de nota en `SriXmlGeneratorTest` +5, y 11 pruebas nuevas de `ComprobanteElectronicoServiceTest` para nota/anulación) · lint ✔ · unit **170/170** · build **720.52 kB / 147.39 kB gzip** · e2e **63/63** |
| Smoke | ✅ `/tmp/opencode/smoke_h1.sh` (9 pasos, **31/31** contra el backend reconstruido con `V43`): nota total (tipo `04`, referencia, clave con `04` en las posiciones 9–10, XML `<notaCredito>` y RIDE `%PDF`), tope `409 NOTA_CREDITO_EXCEDE`, devolución parcial con `valorModificacion`, `400` por motivo corto y `404` por comprobante inexistente, anulación a `ANULADO` + `409` al repetir + `409 COMPROBANTE_ANULADO` al facturar de nuevo, y los 4 eventos de auditoría |
| Pendiente | ⛔ **H.2** (interfaz: acciones *Nota de crédito* y *Anular* en la bandeja de comprobantes) · notas de débito y retenciones · UI de `anulaciones_comprobante` |

### 2.20 Plan de identificación, agendamiento y calendario (2026-10-12)

| Área | Estado |
|---|---|
| Identificación por cédula reutilizable (§2, §9) | ✅ `PatientIdentificationFlow` centraliza "pide cédula → verifica → saluda"; los flujos privados (`Agendar`, `Mis citas`, `Gestionar cita`, `Mi información`) lo invocan antes de operar; sesión de identidad **15 min** (`ContextManager.VIDA_IDENTIDAD_MS`) reutilizada entre flujos |
| Guard privado (§16) | ✅ `ConversationEngine.protegido()` exige identidad válida (`paciente` o `identidadVerificada` reciente) para los 4 flujos protegidos; sin cédula no se listan citas ni datos |
| Menú de 6 ítems (§1) | ✅ 1 Agendar · 2 Mis citas · 3 Disponibilidad · 4 Gestionar cita · 5 Mi información · 6 Recepción; **sin "Ver servicios"** (`ServicesFlow` sigue registrado) |
| Cambio de intención (§5) | ✅ `ConversationEngine.cambioDeIntencion()` redirige durante un flujo por palabras clave ("disponibilidad", "mis citas", "agendar", "cancelar/confirmar una cita", "mi información") |
| Enmascarado (§7) | ✅ Mi información muestra cédula `171*****11` y teléfono enmascarado; el saludo de cédula suelta nunca volcar datos |
| Modalidad chat/calendario (§2, §10) | ✅ Tras elegir odontólogo, `AgendaPasos.pedirModalidad()` ofrece "por este chat" o "calendario web"; la opción 2 genera un **enlace temporal firmado** (`AgendaPublicaTokenService`, HMAC-SHA256, 15 min por defecto) y termina el paso en WhatsApp; si el calendario no está disponible cae al **fallback de 4 opciones** (ver horarios por chat · reintentar · recepción · menú) sin pedir "escribe humano" |
| API pública (§13) | ✅ `GET /public/agenda/{context,dias,slots}` + `POST /public/agenda/reservar` con revalidación en vivo e idempotencia; token con **propósito**: `BOOK_APPOINTMENT` (con paciente, el único que reserva) o `VIEW_AVAILABILITY` (enlace anónimo de sólo consulta); `slotToken` firmado por cupo fija odontólogo/fecha/hora al reservar; `PublicRateLimitFilter` 60/min por IP; notificación WhatsApp automática vía `CitaCreadaEvent` |
| Calendario Angular (§12) | ✅ Ruta pública `dental_crm/agenda-publica?t=` (sin `authGuard`, el interceptor no cierra sesión en `/public/`); mes actual + siguiente, estados con color, cupos en vivo y confirmación; móvil primero |
| Odontólogo primero (§3/§4) | ✅ Orden del plan: identificación → odontólogo (doctores primero, "cualquier" al final) → modalidad → servicio → fecha → franja → cupos → "Confirma:" |
| Tests | ✅ backend **552/552** (+10 nuevos: saludos con typos, estado terminal tras cancelar, conservación del contexto de transferencia con motivo/odontólogo, fallback del calendario con agenda nula, propósito `VIEW_AVAILABILITY`, `slotToken` y su resolución/rechazo en `/reservar`) · lint ✔ · unit **194/194** · e2e ✔ (`agenda-publica.spec.ts`: marca Little Smile, reintentar, enlace anónimo sin saludo; axe verde con `tbl-wrap[tabindex]` restaurado) |
| Reserva web (P4) | ✅ idempotencia explícita por `idempotencyKey` (`V44__reserva_web_idempotencia`, UNIQUE por tenant+key, se responde `REPETIDA` sin crear otra) + auditoría `RESERVA_WEB` módulo `AGENDA` + dedupe natural por persona/hora · estado `NO_LABORABLE` (excepción `esCerrado()` o sin horario activo ese día) en `GET /public/agenda/dias`, con etiqueta "No atiende" en el calendario Angular |

---

## 3. Stack tecnológico

| Capa | Elección |
|---|---|
| API | Java 21, Spring Boot 3, Spring Security (JWT stateless), JPA/Hibernate 6 (`@TenantId`), Flyway, springdoc/OpenAPI |
| UI | Angular 22 standalone + signals/ChangeDetector, lazy `loadComponent`, Angular Material 22 + Bootstrap 5 (import selectivo) |
| Estilo | SCSS con tokens `--ls-*` (`src/styles/_tokens.scss`), paleta **#A1CB35** (primario) / **#F86401** (secundario) / blanco; tema oscuro por clase `html.theme-dark` (persistido en `localStorage['ls-theme']`) |
| Mensajería | OpenWA (WhatsApp) vía adaptador `MessagingProvider`; webhook con firma HMAC-SHA256 |
| Correo | SMTP opcional (`MAIL_*`); si no hay host, los avisos salen sólo por WhatsApp |
| Datos | PostgreSQL 16, esquema multi-tenant por `tenant_id`, respaldos en `docker/backups/` |
| QA | JUnit 5 + Mockito, Vitest, Playwright (axe-core, contraste, foco, hover, tema, flujos) |
| Despliegue | Docker Compose + `scripts/deploy.sh` (build estático a `/var/www/dentalcrm`) + nginx nativo |

---

## 4. Arquitectura y estructura

```
Default Project/
├── backend/        Spring Boot (org.dentalcrm) · domain/ · web/ · config/ · security/ · storage/ · db/migration (V1–V44)
├── frontend/       Angular 22 · src/app/{layout,core,ui,<módulos>} · src/styles · e2e/ (Playwright)
├── database/       datos auxiliares
├── docker/         nginx de contenedor, README, backups/{backup.sh,restore.sh}
├── docs/           14 documentos (API, SECURITY, DEPLOYMENT, …)
├── scripts/        deploy.sh · health.sh · estado-en-vivo.sh · secrets.sh · whatsapp.sh
└── docker-compose.yml · .env(.example) · README.md
```

- **Dev:** `ng serve` (puerto 4200, base `/dental_crm/`) con proxy `/api → http://localhost:18082` (backend Docker).
- **Prod:** build estático en `/var/www/dentalcrm`, `assets/config.json = {"apiUrl": "/dental_crm/api/v1"}`, nginx reescribe `/dental_crm/api/* → 127.0.0.1:18082`.

---

## 5. Módulos

### 5.1 Tabla maestra

| # | Módulo | Backend (base `/api/v1`) | Frontend | Permiso | Estado |
|---|---|---|---|---|---|
| 1 | Autenticación | `/auth` (login, refresh, logout), `GET /ping` | `/login` | — (público) | ✅ |
| 2 | Dashboard | `/reportes/dashboard` | `/dashboard` | `REPORTES` | ✅ |
| 3 | Pacientes | `/pacientes` (CRUD + búsqueda) | `/pacientes` | `PACIENTES_*` | ✅ |
| 4 | Odontólogos | `/odontologos` (+ `/activos`, etiquetas, `google_calendar_id`) | `/odontologos` | `ODONTOLOGOS_*` | ✅ |
| 5 | Servicios | `/servicios` (+ `/activos`) | `/servicios` | `SERVICIOS_*` | ✅ |
| 6 | Horarios | `/horarios` (semanal por doctor) | `/horarios` | `AGENDA_*` | ✅ |
| 7 | Agenda y bloqueos | `/agenda` (día, `/disponibilidad`, `/bloqueos`, `/excepciones`) | `/agenda` | `AGENDA_*` | ✅ |
| 8 | Citas | `/citas` (CRUD + confirmar/cancelar/atender/no-asistió) | `/citas` | `CITAS_*` | ✅ |
| 9 | WhatsApp sesiones | `/whatsapp/sesiones` (conectar/desconectar/QR) | `/whatsapp/sesiones` | `WHATSAPP_*` | ✅ |
| 10 | WhatsApp inbox | `/whatsapp/conversaciones` (+mensajes, estados) | `/whatsapp/inbox` | `WHATSAPP_READ/WRITE` | ✅ |
| 11 | Campañas | `POST /whatsapp/campanas` (multipart) | `/whatsapp/campana` | `WHATSAPP_WRITE` | ✅ |
| 12 | Webhook OpenWA | `POST /webhooks/whatsapp` (HMAC) | — (server-side) | público + firma | ✅ |
| 13 | Redes sociales | `/social` (status, publish, connect/disconnect, callback) | `/redes-sociales` | `REDES_SOCIALES` | ✅ (falta pulido de mensajes, §12) |
| 14 | Plantillas | `/plantillas` (CRUD, variables `{{…}}`) | `/plantillas` | `AUTOMATIZACIONES` | ✅ |
| 15 | Automatizaciones | `/automatizaciones` (CRUD, activar/desactivar, `/notificaciones`) | `/automatizaciones` | `AUTOMATIZACIONES` | ✅ |
| 16 | Notificaciones | motor + scheduler 60s (derivado de eventos de cita) | `/notificaciones` | `AUTOMATIZACIONES` | ✅ |
| 17 | Agente IA | `/agente` (start, mensaje, supervisión, transferencia) | `/agente` | `AGENTE_IA` | ✅ |
| 18 | Google Calendar | `/google` (connect, **callback**, calendars, sync, credentials) | `/google` | `INTEGRACIONES` | ✅ funcional (falta smoke real, §12) |
| 19 | Reportes | `/reportes/dashboard`, `/reportes/estadisticas` | `/reportes` | `REPORTES` | ✅ |
| 20 | Auditoría | `/auditoria` (búsqueda por acción/módulo/entidad) | `/auditoria` | `AUDITORIA_READ` | ✅ |
| 21 | Usuarios y roles | `/usuarios` (CRUD + roles + estado) | `/usuarios` | `USUARIOS_*` | ✅ |
| 22 | Consultorio | `/configuracion/consultorio` (+ firma digital) | `/configuracion/consultorio` | `CONFIGURACION` | ✅ |
| 23 | Integraciones | `/configuracion/integraciones` (OpenWA, correo, Meta, TikTok) | `/configuracion/integraciones` | `CONFIGURACION` | ✅ |
| 24 | Historia clínica | `/historia-clinica` (resumen, antecedentes, exámenes, diagnósticos, evoluciones, timeline unificado) | `/pacientes/:id/historia` | `HISTORIA_CLINICA_READ/WRITE/CLOSE` | ✅ Fase B (§2.6) · timeline (§2.14) |
| 25 | Odontograma | `/historia-clinica/pacientes/{id}/odontograma(s)` (vigente, versiones, dientes, comparación, eventos) | `/pacientes/:id/historia` (pestaña Odontograma) | `ODONTOGRAMA_READ/WRITE` | ✅ Fase C (§2.7) |
| 26 | Planes de tratamiento | `/planes-tratamiento/pacientes/{id}` + `/{id}` (líneas, estados, presupuestos, `/pdf`) | `/pacientes/:id/historia` (pestaña Planes) | `PLAN_TRATAMIENTO_READ/WRITE` | ✅ Fase D (§2.8) |
| 27 | Periodontograma | `/historia-clinica/pacientes/{id}/periodontograma(s)` (vigente, versiones, mediciones, comparación, `/pdf`) | `/pacientes/:id/historia` (pestaña Periodoncia) | `PERIODONCIA_READ/WRITE` | ✅ Fase E (§2.9) |
| 28 | Recetas | `/historia-clinica/pacientes/{id}/recetas` + `/recetas/{id}` (detalle, edición, anulación, `/pdf`) | `/pacientes/:id/historia` (pestaña Recetas) | `RECETAS_READ/WRITE` | ✅ Fase E (§2.10) |
| 29 | Consentimientos | `/historia-clinica/plantillas-consentimiento` + `/consentimientos/{id}` (detalle, edición, `/firmar`, `/pdf`) | `/pacientes/:id/historia` (pestaña Consentimientos) | `CONSENTIMIENTOS_READ/WRITE` | ✅ Fase E (§2.11) · menores (§2.12) |
| 30 | Archivos clínicos | `/historia-clinica/pacientes/{id}/archivos` (lista y alta multipart) + `/archivos/{id}` (`/contenido`, borrado) | `/pacientes/:id/historia` (pestaña Archivos) | `ARCHIVOS_CLINICOS_READ/WRITE` | ✅ Fase E (§2.13) |
| 31 | Reportes clínicos | `/historia-clinica/reportes` (rango + límite) + `/pacientes/{id}/reportes/historia-completa.pdf` | `/reportes-clinicos` y botón PDF en `/pacientes/:id/historia` | `REPORTES_CLINICOS` | ✅ Fase E (§2.15) |
| 32 | Cuenta del paciente | `/facturacion/cuentas` (listado, detalle con auto-creación, movimientos, anular, `PUT /estado`) + `/facturacion/pagos` | `/facturacion/cuentas` | `FACTURACION_READ/WRITE` | ✅ Fase F (§2.16) |
| 33 | Caja | `/facturacion/cajas` (actual, abrir, cerrar, historial, detalle, movimientos y anulación) | `/facturacion/caja` | `CAJA_READ/WRITE/CERRAR` | ✅ Fase F (§2.16) |
| 34 | Finanzas (dashboard y reportes) | `/facturacion/dashboard` + `/facturacion/reportes/{ventas,produccion,saldos,caja}` | `/facturacion` y `/facturacion/reportes` | `FACTURACION_READ` / `REPORTES_FINANCIEROS` | ✅ Fase F (§2.16) |
| 35 | Comprobantes electrónicos (SRI) | `/facturacion/comprobantes` (bandeja paginada, detalle, `/xml`, `/ride`, `/reenviar`, `/consultar`) + `/facturacion/notas-credito` + `/facturacion/comprobantes/{id}/anular` + emisión desde `/facturacion/cuentas/{id}/comprobante` | `/facturacion/comprobantes` y botón **Emitir factura** en `/facturacion/cuentas` | `FACTURACION_READ` / `FACTURACION_EMITIR` / `FACTURACION_ANULAR` | ✅ Fase G (§2.18) + **H.1 notas de crédito y anulación** (§2.19), **SRI de pruebas** |

### 5.2 Detalle operativo

- **Citas:** reglas activas — fecha/hora no pasadas, dentro de horario activo, sin doble reserva, fuera de bloqueos, duración = duración del servicio; estados `PENDIENTE→CONFIRMADA→ATENDIDA`, `CANCELADA`, `NO_ASISTIO`; bloqueo optimista al mover.
- **Eventos de dominio** (publicados por `Cita*Event`): los escuchan la sincronización con Google, el motor de notificaciones y la auditoría.
- **Automatizaciones (7 eventos):** `CITA_CREADA`, `CITA_PROXIMA`, `CITA_CONFIRMADA`, `CITA_CANCELADA`, `CITA_ATENDIDA`, `NO_ASISTIO`, `CITA_MODIFICADA`; destinatario `PACIENTE`/`ODONTOLOGO`; reintentos (máx 3) y scheduler cada 60 s (repara `ENVIANDO` colgadas).
- **Agente IA:** clasifica intención (saludo, agendar, confirmar, cancelar, disponibilidad, servicios, registrar paciente, transferir), parser de fechas/horas en español, flujo multi-turno con tools de agenda/citas, contexto en `contexto_agente` (JSONB).
- **Historia clínica (Fase B):** se crea bajo demanda al guardar el primer antecedente/examen/diagnóstico/evolución (UNIQUE `tenant_id + paciente_id`); la evolución sólo es editable mientras está `ABIERTA` (409 `EVOLUCION_CERRADA`), se cierra con permiso `CLOSE` y las **adendas** sólo se añaden sobre evoluciones `CERRADA` (409 `EVOLUCION_ABIERTA`); la línea temporal agrega antecedentes, exámenes, diagnósticos y evoluciones ordenados por fecha.
- **Odontograma (Fase C):** versiones inmutables (una vigente, `Nueva versión` copia la última), condiciones por diente o superficie con estados `DIAGNOSTICO/PLANIFICADO/PROCESO/COMPLETADO/REFERIDO`, FDI validado en backend (`400 DIENTE_INVALIDO`), log de cambios por versión y comparación entre dos versiones; sólo se edita la versión vigente (`puedeEditar = ODONTOGRAMA_WRITE + vigente`).
- **Planes de tratamiento (Fase D):** estados `BORRADOR→PRESENTADO→ACEPTADO/PARCIALMENTE_ACEPTADO/RECHAZADO→EN_EJECUCION→COMPLETADO` (más `CANCELADO`) con matriz de transiciones validada en backend; las líneas sólo se editan en `BORRADOR` y sólo cambian de estado con el plan `EN_EJECUCION`; emitir un presupuesto **congela un snapshot** de las líneas y presenta el plan; la aceptación exige `firmante`, guarda `firma_hash` (SHA-256), marca el presupuesto `vigente` (desmarca los anteriores) y pasa el plan a `ACEPTADO`; descarga en PDF con cabecera del consultorio.
- **Periodontograma (Fase E):** exámenes versionados (una versión vigente; `Nuevo examen` copia la anterior por defecto) con mediciones por **diente + sitio** (`VESTIBULAR/MESIAL/DISTAL/LINGUAL`: sondaje, margen, inserción y signos `sangrado/placa/supuración`) y datos por pieza (movilidad 0–3, furcación 0–3, notas); upsert por clave única, FDI y rangos validados en servidor, comparación de dos versiones (nuevos/modificados/eliminados por campo) y PDF con matriz diente × sitio.
- **Recetas (Fase E):** recetas por paciente con estado `VIGENTE/ANULADA`; cada línea guarda medicamento, concentración, presentación, dosis, vía, frecuencia, duración e instrucciones; editar reemplaza la pauta completa, anular exige motivo y queda auditado (`CREAR/MODIFICAR/ANULAR_RECETA`); el PDF lleva cabecera del consultorio, tabla de medicamentos, instrucciones por línea y sello de anulación
- **Consentimientos (Fase E):** plantillas por tenant con versión incrementada al editar; al emitir el consentimiento **copia el texto y la versión** de la plantilla (los cambios posteriores no lo afectan); sólo se edita o borra en `BORRADOR` (`409 CONSENTIMIENTO_FIRMADO`), la firma exige `firmante`, guarda la imagen del lienzo en data URL, calcula `firmaHash` (SHA-256) y lo congela (`409 CONSENTIMIENTO_YA_FIRMADO`); el PDF lleva el texto, el bloque de firma y la huella, o la marca `BORRADOR SIN FIRMAR`
- **Comprobantes electrónicos (Fase G):** la emisión arranca desde la cuenta del paciente (un comprobante por cuenta), calcula totales con IVA 13% incluido en los precios, arma el XML con la clave de 49 dígitos (`1` = pruebas), lo firma en **modo simulado** y lo envía al SRI de pruebas; el estado pasa `GENERADO → ENVIADO → AUTORIZADO/RECHAZADO` con reintentos manuales (`/reenviar`, `/consultar`) y la autorización se persiste con su RIDE y XML descargables. **Fase H.1**: sobre una factura `AUTORIZADO` se emite una **nota de crédito** (devolución total o parcial por líneas, con motivo obligatorio, secuencial propio del tipo `04`, tope de «no devolver más de lo facturado» y RIDE) y un comprobante se **anula** sin borrarse jamás (`ANULADO` + trámite en `anulaciones_comprobante`); al anular, la cuenta vuelve a ser facturable. **Pendiente**: firma real (`SRI_FIRMA_REAL_REQUERIDA`), notas de débito y retenciones (resto de la Fase H).
- **Multi-tenant:** `tenant_id` en 16+ tablas, aislamiento automático vía `@TenantId` + `TenantContext` (fijado por el claim JWT); roles/permisos globales.
- **Reportes:** KPIs del día/mes + estadísticas por rango (citas por día/odontólogo, servicios, estados, confirmaciones por fuente, cancelaciones/inasistencia).
- **Finanzas (Fase F):** el libro mayor `cuenta_detalles` alimenta el saldo del paciente y, a la vez, la caja (cada abono/anticipo/devolución genera su movimiento en la sesión abierta); una sola sesión de caja `ABIERTA` por tenant, conciliación por efectivo esperado vs. declarado y estados de cuenta derivados del saldo; el dashboard y los cuatro reportes financieros se calculan en servidor sobre el mismo rango (`desde`/`hasta` con `400 RANGO_INVALIDO`).

---

## 6. Accesos y seguridad

### 6.1 Autenticación

| Elemento | Detalle |
|---|---|
| Mecanismo | JWT Bearer (`Authorization: …`), estado **stateless** (`SessionCreationPolicy.STATELESS`), CSRF deshabilitado |
| Contraseñas | BCrypt (`$2b$12$…`) |
| Duración | `JWT_EXPIRATION_MS` = 24 h por defecto; `POST /auth/refresh` renueva |
| Claims | `sub` (username) + `tenantId` → fija el tenant por petición (`TenantContext`) |
| Frontend | `AuthService` en `localStorage['dentalcrm.auth']`; interceptor agrega el Bearer y hace `logout()` automático ante **401** |
| Errores | 401 `TOKEN_EXPIRADO`/`TOKEN_INVALIDO` · 403 = permiso/CORS |

### 6.2 Roles y permisos (49 códigos → authority `PERMISO_<código>`)

| Permiso | SUPER_ADMIN | ADMIN | RECEPCION | ODONTOLOGO |
|---|:-:|:-:|:-:|:-:|
| `PACIENTES_READ / WRITE / DELETE` | ✔✔✔ | ✔✔✔ | ✔✔ | ✔ |
| `CITAS_READ / WRITE / CANCEL` | ✔✔✔ | ✔✔✔ | ✔✔✔ | ✔ |
| `AGENDA_READ / WRITE` | ✔✔ | ✔✔ | ✔✔ | ✔ |
| `ODONTOLOGOS_READ / WRITE` | ✔✔ | ✔✔ | ✔ | ✔ |
| `SERVICIOS_READ / WRITE` | ✔✔ | ✔✔ | ✔ | ✔ |
| `WHATSAPP_READ / WRITE / DELETE` | ✔✔✔ | ✔✔✔ | ✔✔ | — |
| `AUTOMATIZACIONES` | ✔ | ✔ | — | — |
| `REPORTES` | ✔ | ✔ | ✔ | — |
| `USUARIOS_READ / WRITE` | ✔✔ | ✔ | — | — |
| `INTEGRACIONES` | ✔ | ✔ | — | — |
| `AUDITORIA_READ` | ✔ | ✔ | — | — |
| `CONFIGURACION` | ✔ | ✔ | — | — |
| `AGENTE_IA` | ✔ | ✔ | ✔ | — |
| `REDES_SOCIALES` | ✔ | ✔ | — | — |
| `HISTORIA_CLINICA_READ / WRITE / CLOSE` | ✔✔✔ | ✔✔✔ | ✔✔ | ✔✔✔ |
| `ODONTOGRAMA_READ / WRITE` | ✔✔ | ✔✔ | ✔ | ✔✔ |
| `PLAN_TRATAMIENTO_READ / WRITE` | ✔✔ | ✔✔ | ✔ | ✔✔ |
| `PERIODONCIA_READ / WRITE` | ✔✔ | ✔✔ | ✔ | ✔✔ |
| `RECETAS_READ / WRITE` | ✔✔ | ✔✔ | ✔ | ✔✔ |
| `CONSENTIMIENTOS_READ / WRITE` | ✔✔ | ✔✔ | ✔ | ✔✔ |
| `ARCHIVOS_CLINICOS_READ / WRITE` | ✔✔ | ✔✔ | ✔ | ✔✔ |
| `REPORTES_CLINICOS` | ✔ | ✔ | — | ✔ |
| `FACTURACION_READ / WRITE / EMITIR / ANULAR` | ✔✔✔✔ | ✔✔✔✔ | ✔✔ | — |
| `CAJA_READ / WRITE / CERRAR` | ✔✔✔ | ✔✔✔ | ✔✔ | — |
| `REPORTES_FINANCIEROS` | ✔ | ✔ | — | — |
| `CONFIGURACION_FACTURACION` | ✔ | ✔ | — | — |

*(`USUARIOS_WRITE` sólo SUPER_ADMIN — se gestiona desde la pantalla de Usuarios.
Filas 1–14: código previo · filas 15–26: añadidas por `V27`, Fase A del plan de ampliación ·
`ODONTOGRAMA_READ`, `PERIODONCIA_READ` y `RECETAS_READ` de RECEPCION concedidos por `V32`/`V33`, y `CONSENTIMIENTOS_READ` por `V34` (Fase E). `ARCHIVOS_CLINICOS_READ/WRITE` vienen de `V27` y los usa la pestaña Archivos (§2.13): RECEPCION sólo lee/descarga. La línea temporal unificada (§2.14) exige `HISTORIA_CLINICA_READ` y ninguna fuente queda fuera: los cuatro roles leen todos los tipos que aparecen. `REPORTES_CLINICOS` (§2.15) es el único que abre la página `/reportes-clinicos` y el PDF de historia completa: SUPER_ADMIN, ADMIN y ODONTOLOGO; RECEPCION no lo tiene y recibe `403`.)*

### 6.3 Rutas públicas (sin token)

```
POST /api/v1/auth/login · POST /api/v1/auth/refresh
     /api/v1/webhooks/**            (firma HMAC X-OpenWA-Signature, se desactiva si no hay secret)
GET  /api/v1/google/callback        (callback OAuth)
GET  /api/v1/social/callback/**     (callback OAuth Meta/TikTok)
GET  /api/v1/ping · /actuator/health|info · /api/v1/api-docs/** · /api/v1/swagger-ui**
GET/POST /api/v1/public/agenda/**    (agenda con enlace temporal HMAC; rate limit 60/min por IP)
```

Todo lo demás: `anyRequest().authenticated()`.

### 6.4 Control de acceso: backend vs frontend

| Nivel | Comportamiento |
|---|---|
| Backend | **Fino y real**: `@PreAuthorize("hasAuthority('PERMISO_…')")` en casi todos los métodos (`@EnableMethodSecurity`) |
| Frontend | **Sesión + menú filtrado**: `authGuard` comprueba login; el login entrega `permisos[]` y el sidebar sólo pinta los ítems que `AuthService.puede()` aprueba (sesión antigua sin `permiso`: visible). Las rutas no están bloqueadas por permiso: el backend sigue decidiendo con **403** (4 componentes lo traducen a "Sin permisos…") |
| CORS | Lista explícita `CORS_ALLOWED_ORIGINS` + `SERVER_FORWARD_HEADERS_STRATEGY=framework` (mismo origen detrás de nginx) |
| Webhooks | Filtro `WebhookSignatureFilter` con HMAC-SHA256 |

### 6.5 Usuarios existentes (BD local)

| Usuario | Rol |
|---|---|
| `admin` | SUPER_ADMIN (sembrado por `V1`, clave `admin123`) |
| `ortizal` | SUPER_ADMIN |
| `jh.perez`, `prueba.rec`, `p.rec.0acff9`, `r.7e1bb2` | RECEPCION (creados durante pruebas) |
| `qa.odonto` | ODONTOLOGO (creado por `e2e/permisos.spec.ts`, clave `qaMenu123`) |

> ⚠️ Cambiar `admin123` en cualquier entorno que no sea local (`docs/SECURITY.md`).

### 6.6 Superficie de configuración (`.env` / `docker-compose.yml`)

Grupos principales: `DATABASE_*` · `JWT_SECRET`/`JWT_EXPIRATION_MS` · `CORS_ALLOWED_ORIGINS` · `SERVER_FORWARD_HEADERS_STRATEGY` · `FRONTEND_PUBLIC_URL` · `TIMEZONE` · `CLINICA_*` · `GOOGLE_*` · `META_*`/`TIKTOK_*` · `SOCIAL_FRONTEND_URL` · `OPENWA_*` · `MAIL_*` · llaves de cifrado (`APP_*_ENCRYPTION_KEY`). Plantilla completa: `.env.example`.

---

## 7. Operaciones del sistema

| # | Operación | Cómo ocurre | Control |
|---|---|---|---|
| 1 | **Login / sesión** | `POST /auth/login` → JWT → SPA lo guarda y lo adjunta en cada request | BCrypt, expiración 24 h, 401 → logout automático |
| 2 | **Agendar cita** | Panel → `POST /citas` con validación de horario/bloqueo/doble reserva → evento `CitaCreadaEvent` → notificación + evento Google | `CITAS_WRITE` |
| 3 | **Ciclo de vida de la cita** | confirmar / cancelar / atender / no-asistió / mover (`PUT`) → regenera notificaciones y sincroniza Google | `CITAS_*` |
| 4 | **WhatsApp: conexión** | `POST /whatsapp/sesiones/{id}/conectar` → QR (OpenWA) → estados `DESCONECTADA/CONECTANDO/CONECTADA/ERROR` | `WHATSAPP_WRITE` |
| 5 | **WhatsApp: recepción** | OpenWA → `POST /webhooks/whatsapp` (HMAC) → identifica sesión/número → paciente → conversación/mensaje → **bot simple** (`1` confirma, `3` cancela) o **agente IA** | público con firma |
| 6 | **WhatsApp: envío** | Inbox manual, campañas masivas y notificaciones programadas salen por `MessagingProvider`; fallo → estado `ERROR` con reintento | `WHATSAPP_WRITE` |
| 7 | **Automatizaciones** | Evento de cita → regla por evento/`minutos_antes`/destinatario → plantilla con variables → cola `PENDIENTE` → scheduler 60 s → enviado | `AUTOMATIZACIONES` |
| 8 | **Publicar en redes** | `POST /social/publish` (texto + imagen opcional) → OAuth de Meta/TikTok → `GET /social/callback/{provider}` | `REDES_SOCIALES` |
| 9 | **Google Calendar** | `GET /google/connect` → consentimiento → `GET /google/callback` (intercambia código) → `302 {FRONTEND_PUBLIC_URL}/google?google=…` → sync automática por eventos + `POST /google/sync` (masiva) e `/sync/inbound` (importa bloqueos) | `INTEGRACIONES` |
| 10 | **Reportes** | `GET /reportes/dashboard` (KPIs) y `/estadisticas?desde&hasta` | `REPORTES` |
| 11 | **Auditoría** | Escritura en `auditoria` (JSONB) desde los servicios; consulta por acción/módulo/entidad | `AUDITORIA_READ` |
| 12 | **Configuración** | Consultorio (+ firma digital cifrada) e integraciones (OpenWA/correo/Meta/TikTok) con cifrado en BD | `CONFIGURACION` |
| 13 | **Respaldos** | `docker/backups/backup.sh` / `restore.sh` (PostgreSQL) | ejecución manual/programada en el servidor |
| 14 | **Salud** | `GET /actuator/health`, `scripts/health.sh`, `scripts/estado-en-vivo.sh` | — |

---

## 8. Datos y migraciones

- **Flyway**: 43 migraciones `V1…V43` aplicadas y validadas (`ddl-auto: validate`), última: `V43__notas_de_credito`.
- **Hitos de esquema:** `V1` base (tenants/roles/permisos/usuarios) · `V2` CRM · `V3` agenda · `V4` Google · `V5` WhatsApp · `V6` automatizaciones · `V7` reportes · `V8` agente IA · `V9` multi-tenant · `V12` tutores de menores · `V14` credenciales Google · `V18` excepciones de horario · `V21` consultorio · `V22` redes · `V23` destinatarios de notificación · `V24` datos legales + firma · `V25` integraciones (OpenWA/correo) · `V26` credenciales Meta/TikTok · `V27` permisos de historia clínica y facturación · `V28` historia clínica base (antecedentes, exámenes, diagnósticos, evoluciones y adendas) · `V29` odontograma (versiones, dientes/superficies, eventos) · `V30` planes de tratamiento (plan, líneas, presupuestos con snapshot) · `V31` periodontograma (exámenes versionados, mediciones por sitio y pieza) · `V32` `PERIODONCIA_READ` para recepción · `V33` recetas (recetas, líneas de pauta y `RECETAS_READ` para recepción) · `V34` consentimientos (plantillas por tenant, consentimientos con snapshot/firma, 8 plantillas sembradas y `CONSENTIMIENTOS_READ` para recepción) · `V35` tutores de consentimientos (§4.14: `tutor_id`/`tutor_parentesco`/`tutor_telefono` en `consentimientos`) · `V36` archivos clínicos (§4.15: metadata de radiografías, fotos y documentos, sin BLOB) · `V37` cuenta del paciente (§3.3: `cuentas_paciente` + `cuenta_detalles` como libro mayor) · `V38` caja (§3.4: `cajas` con una sesión abierta por tenant + `movimientos_caja`) · `V39` configuración de facturación · `V40` tipo de emisión SRI · `V41` comprobantes electrónicos · `V42` totales denormalizados del comprobante · `V43` notas de crédito y anulación (§3.9/§3.10: secuencial por tipo, referencia + motivo, tabla `anulaciones_comprobante`).
- **57 tablas** públicas · 1 tenant sembrado (`principal`).

---

## 9. Despliegue y entornos

| Entorno | Comando | Resultado |
|---|---|---|
| Dev backend | `docker compose up -d` | API en `127.0.0.1:18082` (Swagger `/api/v1/swagger-ui.html`) |
| Dev frontend | `cd frontend && npx ng serve --proxy-config proxy.conf.json` | `http://localhost:4200/dental_crm/` → proxy a `18082` |
| QA/E2E | `npx playwright test` | levanta `ng serve` automáticamente (`webServer` de Playwright) |
| Producción | `./scripts/deploy.sh` | build Angular → `/var/www/dentalcrm` + `assets/config.json` + `docker compose up --build -d` + health checks |
| Nginx prod | bloque del sitio en `control-servidor` (validado con `nginx -t`) | `/dental_crm/` → SPA · `/dental_crm/api/` → `127.0.0.1:18082` · assets con hash cacheados |

**Checklist de producción (pendiente, §12.1):** subir `docker-compose.yml` nuevo · `CORS_ALLOWED_ORIGINS=https://alan-tek.com,…` · `FRONTEND_PUBLIC_URL=https://alan-tek.com/dental_crm` · `GOOGLE_REDIRECT_URI/META/TIKTOK` de dominio · `OPENWA_URL=http://openwa:2785` y `OPENWA_WEBHOOK_URL=http://backend:8080/…` (red interna) · `docker compose up -d backend`.

---

## 10. Calidad (cómo reproducir los gates)

```bash
# backend
cd backend && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -o test      # 386/386 (40 clases)

# frontend
cd frontend
npx ng lint                          # ✔
npx ng test --no-watch               # 155/155 (18 specs)
npx ng build --configuration production   # 717.63 kB / 146.90 kB gzip, 0 warnings (usar --output-path /tmp/opencode/dist-ng: frontend/dist da EACCES)
npx playwright test                  # 61/61 (~11 min, 20 specs, sin flaky)

# infra
docker compose config --quiet && docker compose ps
curl -s http://127.0.0.1:18082/actuator/health
```

**Suites e2e:** `qa` (27 rutas, flujo login) · `axe` (a11y) · `contraste` (WCAG AA, 4 modos) · `foco` (anillo de teclado) · `hover` (27 rutas) · `theme` (claro/oscuro) · `confirm` (diálogos destructivos) · `calendario` (Google) · `redes` (publicación) · `odontologos` · `permisos` (menú dinámico: ODONTOLOGO ve "Operación" y "Gestión") · `odontograma` (dibujo FDI + edición + permisos de sólo lectura) · `planes` (línea + presupuesto por UI, aceptación firmada y permisos de sólo lectura) · `periodoncia` (mediciones por pieza, comparación de versiones, PDF y permisos de sólo lectura) · `recetas` (alta y anulación por UI, PDF y permisos de sólo lectura) · `consentimientos` (emisión, firma con lienzo, PDF y permisos de sólo lectura) · `archivos` (subida, vista previa, descarga, borrado y permisos de sólo lectura) · `timeline` (línea unificada con 11 tipos, chips de filtro y recepción que sólo consulta) · `facturacion` (menú **Finanzas**, dashboard, cargo con detalle de cuenta, caja con egreso, pestañas de reportes y recepción sin `REPORTES_FINANCIEROS`).

---

## 11. Documentación disponible

| Archivo | Contenido |
|---|---|
| `README.md` | plan por fases (1–10), ejecución, estructura, seguridad |
| `docs/API.md` | Referencia REST: endpoints por módulo, auth y formato de error |
| `docs/ARCHITECTURE.md` | capas, eventos, decisiones |
| `docs/DATABASE.md` | modelo de datos |
| `docs/SECURITY.md` | JWT/RBAC/CORS/webhooks |
| `docs/DEPLOYMENT.md` | variables, nginx, checklist, **solución rápida de problemas** |
| `docs/WHATSAPP.md` · `SOCIAL.md` · `GOOGLE-CALENDAR.md` · `AUTOMATIONS.md` | módulos de integración |
| `docs/ADMIN.md` | manual de administración |
| `docs/Entorno de desarrollo.md` | `.env` de desarrollo |
| `docs/ESTADO.md` · `ESTADO_EN_VIVO.md` | estado histórico / snapshot en vivo |
| `docs/PLANIFICACION_MODULOS_FACTURACION_HISTORIA_CLINICA_CRM_DENTAL_AT.md` | plan de ampliación: facturación SRI + historia clínica (Fases A–I); **Fases A–F y Fase G (SRI en pruebas) completadas, queda G.6** |
| **`docs/ESTADO_DEL_SISTEMA.md`** | **este documento** |

---

## 12. Conocimiento: incidencias abiertas y deudas

### 12.1 Producción (prioridad alta)

1. **403 en `POST /api/v1/auth/login` en `alan-tek.com`** — pendiente de aislamiento en el servidor. Infraestructura ya endurecida en el repo: `SERVER_FORWARD_HEADERS_STRATEGY=framework`, `FRONTEND_PUBLIC_URL`, `CORS_ALLOWED_ORIGINS`, nginx item 8 corregido (`nginx -t` OK). Falta correr `deploy.sh` allí y la secuencia de 3 `curl` (directo → nginx → dominio) porque desde esta máquina el tráfico está filtrado por FortiGuard.
2. **Google OAuth sin smoke real**: el callback se implementó hoy y está cubierto por tests, pero requiere credenciales del proyecto Google + que la fila `google_credentials` (sembrada con `localhost:8080` por `V14`) se corrija **desde el panel `/google`** (tiene prioridad sobre `GOOGLE_REDIRECT_URI`).

### 12.2 Funcionales (prioridad media)

3. **Retorno de Meta/TikTok sin mensaje**: `GET /social/callback/{provider}` redirige a `…/redes-sociales?social=connected|error` pero la SPA **no lee ese parámetro** (sólo Google lo lee) → no se muestra confirmación.
4. **`SOCIAL_FRONTEND_URL` por defecto `http://localhost:4200`** (sin `/dental_crm`) → el retorno del callback social puede caer fuera de la base href en dev.
5. ~~**Menú sin autorización visual**~~ → **resuelto 2026-10-01** (Fase A): `GET /auth/login` entrega `permisos[]` (`V27`) y el sidebar sólo pinta los ítems aprobados por `AuthService.puede()`. Las rutas siguen protegidas por sesión + **403** del backend.
6. **Sin página 404**: el wildcard redirige a `dashboard`.
7. **Redis provisionado sin uso** en el código (candidato a caché de disponibilidad o colas de notificación).

- **Flaky de e2e:** `axe.spec.ts` «sin violaciones WCAG A/AA en las páginas principales» falla de vez en cuando por contraste del botón **↻ Refrescar** en `/whatsapp/sesiones` (`#888e92` sobre `#fff` → 3.31:1, `btn sm` en estado deshabilitado); pasa al reintento. Revisar el estilo de `.btn.sm:disabled`.

### 12.3 Operativas / higiene (prioridad baja)

8. **Credenciales de QA en BD local** (`admin/admin123`, usuarios `prueba.*`) → limpiar antes de cualquier export.
9. **Archivos de `root` en `frontend/dist`** (producidos por builds con `sudo`) → ya limpiado y documentado en `DEPLOYMENT.md` (§EACCES).
10. **Cifras del README desactualizadas** (menciona 42/87 tests de fases tempranas; hoy son 186 backend).
11. **Pulido de UI en automatización** (fechas ISO / códigos de estado en crudo y botones rojos en filas) — asignado a la sesión paralela que trabaja esos archivos.
12. **Paginación sólo resuelta en facturación**: la API pagina con `{content, page:{size,number,totalElements,totalPages}}` y las páginas de pacientes/usuarios/citas siguen leyendo `r.totalPages` (inexistente ⇒ muestran sólo la primera página). Helpers `paginasDe(r)`/`totalDe(r)` en `core/models.ts`; falta migrar el resto de listados.
13. **`contribuyenteEspecial` / `agenteRetencion` no se emiten en el XML** (quedan en la configuración del consultorio); documentado como pendiente hasta confirmar el XSD vigente del SRI.
14. **Propiedades de `root` en `frontend/dist`, `frontend/node_modules` y `.git`** por builds con `sudo`: `sudo chown -R "$USER:$USER" frontend/node_modules frontend/dist .git` (el `sudo` no está disponible en esta sesión).
15. **La anulación se aplica de inmediato en pruebas** (`anulaciones_comprobante` pasa a `APLICADA` en la misma transacción, con el mensaje «Anulación aplicada en ambiente de pruebas»); con el SRI real haría falta un trámite con respuesta diferida (estado `PENDIENTE → APLICADA/RECHAZADA`).
16. **La interfaz todavía no ofrece nota de crédito ni anulación** (Fase H.2): las acciones existen en la API (`FACTURACION_ANULAR`) pero sólo son invocables desde `curl`/Swagger.

---

## 13. Proyección

### Inmediato (esta semana) — cierre de la Fase G y salida a producción

- [x] **G.6**: smoke productivo del flujo `Cuenta → Emitir factura → SRI pruebas → RIDE` y checklist de §9 → **hecho 2026-10-09** (§2.18).
- [ ] Fase H: notas de crédito/débito, retenciones y anulación de comprobantes → **H.1 hecho** (§2.19): notas de crédito y anulación; faltan **notas de débito**, **retenciones** y **H.2** (interfaz).
- [ ] Migrar el resto de listados a `paginasDe`/`totalDe` (§12.2, #12).
- [ ] Ejecutar `./scripts/deploy.sh` en el servidor y completar el checklist de §9.
- [ ] Aislar el 403 del login con la secuencia de `curl` (si persiste: revisar WAF de Cloudflare / FortiGate).
- [ ] Cargar credenciales reales: Google, Meta, TikTok y OpenWA; verificar callbacks con el dominio público.
- [ ] Cambiar `admin123`, revisar `CORS_ALLOWED_ORIGINS` y dejar `FRONTEND_PUBLIC_URL=https://alan-tek.com/dental_crm`.
- [ ] Programar respaldos (`docker/backups/backup.sh`) y verificar restauración.

### Corto plazo (1–2 semanas) — cierre de deudas

- [ ] Mostrar mensajes de retorno social (`?social=`) igual que Google (#3) y corregir `SOCIAL_FRONTEND_URL` (#4).
- [x] Ocultar rutas/menú sin permiso en el frontend (#5) usando la información de roles del login → **hecho**: menú dinámico por `permisos` (Fase A, 2026-10-01).
- [ ] Página 404 real (#6) y smoke end-to-end de Google Calendar con credenciales (#2).
- [ ] Actualizar cifras y comandos del `README.md` (#10).

### Medio plazo (1–2 meses) — evolución

- [ ] Redis: caché de disponibilidad y/o cola de notificaciones (#7).
- [ ] Multi-clínica real: alta de tenants desde la UI (hoy sólo `principal`, aislamiento ya probado).
- [ ] Correo SMTP como canal alterno de notificaciones (config ya soportada, sin UI de envío).
- [ ] Exportación de reportes (PDF/CSV) y firma digital en reportes/consentimientos (ya almacenada).
- [ ] Endurecimiento continuo: rotación de secretos, rate limiting en `/auth/login`, auditoría de accesos.

---

*Documento generado a partir del código y de los gates ejecutados el 2026-10-09. Si cambia el código, revalidar §2 y §10.*
