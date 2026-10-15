# API REST

Base URL: `/api/v1` · Swagger UI: `GET /api/v1/swagger-ui.html` · OpenAPI JSON: `GET /api/v1/api-docs`.

Autenticación: `Authorization: Bearer <token>` (JWT). Públicos: `/auth/login`, `/auth/refresh`,
`/webhooks/**`, `/google/callback`, Swagger/OpenAPI y `/actuator/health|info`. El resto responde
`403` sin token válido.

## Formato de error

Todos los errores controlados usan el mismo formato (Global Exception Handler):

```json
{
  "timestamp": "2026-09-09T18:30:00",
  "status": 400,
  "code": "APPOINTMENT_NOT_AVAILABLE",
  "message": "El horario seleccionado ya no está disponible",
  "path": "/api/v1/citas"
}
```

Códigos típicos: `HORA_PASADA`, `HORARIO_NO_DISPONIBLE`, `DOBLE_RESERVA`, `BLOQUEADO`,
`CITA_NO_ENCONTRADA`, `ESTADO_INVALIDO`, `CITA_NO_CANCELABLE`, `CEDULA_DUPLICADA`,
`USERNAME_EN_USO`, `GOOGLE_NO_CONFIGURADA`, `EVENTO_INVALIDO`, `PARAMETRO_INVALIDO`, `NO_AUTORIZADO`,
`EVOLUCION_CERRADA`, `EVOLUCION_ABIERTA`, `EVOLUCION_YA_CERRADA`, `DIENTE_INVALIDO`.

## Auth

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` | `{username, password}` → `{token, tokenType, expiresInMs, username, roles}` |
| POST | `/auth/refresh` | Renueva el token con el refresh token vigente |

## Catálogos

### Configuración del consultorio

`GET/PUT /configuracion/consultorio` permite administrar nombre, razón social, RUC, correo,
dirección, teléfono, ubicación y horario. La firma se administra por separado con
`PUT /configuracion/consultorio/firma-digital` como `multipart/form-data` (`archivo`, `.p12`, hasta
5 MB) y se elimina con `DELETE /configuracion/consultorio/firma-digital`. La respuesta expone solo
el nombre del archivo y si existe una firma guardada; el contenido no se descarga por la API.
El contenido se cifra con AES-GCM en la base de datos. Configura `APP_FIRMA_DIGITAL_ENCRYPTION_KEY`
con al menos 32 caracteres y conserva esa clave para poder usar la firma posteriormente.

### Integraciones

`GET /configuracion/integraciones` muestra el estado y los valores no secretos de OpenWA y SMTP.
Meta/Facebook-Instagram y TikTok también se gestionan en esta pantalla. Se actualizan con `PUT`
en `/configuracion/integraciones/openwa`, `/correo`, `/meta` y `/tiktok`; `GET` en la ruta base
devuelve estado/valores públicos y `DELETE` en cada ruta elimina el override y vuelve a `.env`.
Los secretos se cifran con AES-GCM y nunca se devuelven; omitirlos al guardar conserva el valor
actual. Los cambios se aplican inmediatamente. Requiere `PERMISO_CONFIGURACION`.

Las credenciales OAuth de Google se editan en `/google`. Su `clientSecret` también se cifra y la API
solo informa si está configurado; deja el campo vacío para conservarlo.

Base de datos, puertos, JWT, CORS y claves maestras siguen siendo configuración de despliegue en
`.env`; no se modifican desde el panel.

### Pacientes

| Método | Ruta |
|---|---|
| GET | `/pacientes?q=&estado=&page=&size=` (paginado) |
| POST | `/pacientes` — `{cedula, nombres, apellidos, telefono, email, fechaNacimiento, direccion, observaciones}` |
| GET | `/pacientes/{id}` |
| PUT | `/pacientes/{id}` |
| DELETE | `/pacientes/{id}` (desactiva; la cédula es única nacional) |

### Odontólogos

`GET /odontologos` · `GET /odontologos/activos` · `POST /odontologos` · `GET/PUT /odontologos/{id}` ·
`DELETE /odontologos/{id}` (desactiva).

### Servicios

`GET /servicios` · `GET /servicios/activos` · `POST /servicios` (requiere `duracionMinutos`, `precio`) ·
`GET/PUT /servicios/{id}` · `DELETE /servicios/{id}`.

### Horarios

| Método | Ruta | Notas |
|---|---|---|
| GET | `/horarios?odontologoId=` | Filtrados por tenant y odontólogo |
| POST | `/horarios` | `{odontologoId, diaSemana(1=Lun…7=Dom), horaInicio, horaFin, intervaloMinutos}` |
| PUT | `/horarios/{id}` | Mover/editar horario |
| DELETE | `/horarios/{id}` | |

## Agenda

| Método | Ruta | Notas |
|---|---|---|
| GET | `/agenda?odontologoId=&fecha=` | Citas del día ordenadas por hora |
| GET | `/agenda/disponibilidad?odontologoId=&fecha=&servicioId=&duracion=` | Slots libres `[{horaInicio, horaFin}]` |
| GET | `/agenda/bloqueos?odontologoId=&fecha=` o `&desde=&hasta=` | Bloqueos por día o rango |
| POST | `/agenda/bloqueos` | `{odontologoId, fecha, horaInicio?, horaFin?, motivo?}` |
| DELETE | `/agenda/bloqueos/{id}` | |

## Citas

| Método | Ruta | Notas |
|---|---|---|
| GET | `/citas?estado=&doctorId=&pacienteId=&fecha=&desde=&hasta=&page=&size=` | Paginado; filtros combinables vía `Specification` |
| GET | `/citas/{id}` | |
| POST | `/citas` | `{pacienteId, doctorId, servicioId, fecha, horaInicio, observaciones?}` — valida doble reserva, horario y bloqueos |
| PUT | `/citas/{id}` | Reagendar/mover con bloqueo optimista (`@Version`) |
| POST | `/citas/{id}/confirmar` | |
| POST | `/citas/{id}/atender` | |
| POST | `/citas/{id}/cancelar` | `{motivo?}` |
| POST | `/citas/{id}/no-asistio` | |

Estados: `PENDIENTE` → `CONFIRMADA` → `REALIZADA` | `NO_ASISTIO` | `CANCELADA`.

## Historia clínica

Base: `/historia-clinica` · permisos `HISTORIA_CLINICA_READ` (GET) ·
`HISTORIA_CLINICA_WRITE` (POST/PUT) · `HISTORIA_CLINICA_CLOSE` (cierre de evolución).

| Método | Ruta | Notas |
|---|---|---|
| GET | `/historia-clinica/pacientes/{pacienteId}` | Resumen (antecedentes, examen, diagnósticos, evoluciones y última actividad); crea la historia vacía si no existe |
| GET | `/historia-clinica/pacientes/{pacienteId}/timeline` | Línea única con **11 tipos** de evento (ver abajo) ordenados por fecha |
| GET/POST | `/historia-clinica/pacientes/{pacienteId}/antecedentes` | `{tipo, nombre, detalle?, fecha?}` — tipos: `ENFERMEDAD`, `CIRUGIA`, `HOSPITALIZACION`, `ALERGIA`, `MEDICACION`, `ANTECEDENTE_ODONTOLOGICO`, `HABITO` |
| PUT/DELETE | `/historia-clinica/antecedentes/{id}` | |
| GET/POST | `/historia-clinica/pacientes/{pacienteId}/examenes` | `{tipo, hallazgos, fecha?}` |
| PUT | `/historia-clinica/examenes/{id}` | |
| GET/POST | `/historia-clinica/pacientes/{pacienteId}/diagnosticos` | `{nombre, severidad?, estado?}` — `LEVE`/`MODERADA`/`SEVERA` · `PRESUNTIVO`/`CONFIRMADO`/`RESUELTO` |
| PUT | `/historia-clinica/diagnosticos/{id}` | |
| GET/POST | `/historia-clinica/pacientes/{pacienteId}/evoluciones` | `{titulo, nota, citaId?, fecha?}`; la evolución nace `ABIERTA` |
| PUT | `/historia-clinica/evoluciones/{id}` | **Sólo si está `ABIERTA`** → si no, `409 EVOLUCION_CERRADA` |
| POST | `/historia-clinica/evoluciones/{id}/cerrar` | Requiere `HISTORIA_CLINICA_CLOSE` → `409 EVOLUCION_YA_CERRADA` si ya está cerrada |
| POST | `/historia-clinica/evoluciones/{id}/adendas` | `{texto}` — **sólo sobre evolución `CERRADA`** → `409 EVOLUCION_ABIERTA` |

### Línea temporal (`GET /pacientes/{id}/timeline`)

Responde `{tipo, fecha, titulo, detalle?, referenciaId}[]` con todos los
eventos del paciente en una sola lista (plan §4.16): máximo **25 por fuente**
y **100 en total**, siempre de más reciente a más antiguo.

| `tipo` | Fuente | `titulo` / `detalle` |
|---|---|---|
| `CITA` | citas `ATENDIDA` | Consulta atendida / servicio · odontólogo |
| `EXAMEN` | exámenes clínicos | Examen clínico / observaciones |
| `DIAGNOSTICO` | diagnósticos | nombre / región · severidad · estado |
| `EVOLUCION` | evoluciones | motivo / hallazgos |
| `ODONTOGRAMA` | versiones del odontograma | `Odontograma (vN)` / observaciones o dentición |
| `PERIODONCIA` | exámenes periodontales | `Control periodontal (vN)` / observaciones |
| `PLAN` | planes de tratamiento | título / estado · nº de líneas · total |
| `PROCEDIMIENTO` | líneas `EN_EJECUCION` o `COMPLETADO` | procedimiento / pieza · superficie · estado · plan |
| `RECETA` | recetas | diagnóstico / estado · nº de medicamentos |
| `ARCHIVO` | archivos clínicos | nombre / categoría · tamaño |
| `CONSENTIMIENTO` | consentimientos | título / estado · firmante |

Los eventos de paciente se devuelven aunque la historia clínica todavía no
exista; `404 NO_ENCONTRADO` sólo si el paciente no existe.

Errores típicos: `NO_ENCONTRADO` (404), `EVOLUCION_CERRADA`, `EVOLUCION_ABIERTA`,
`EVOLUCION_YA_CERRADA` (409), `CITA_DE_OTRO_PACIENTE` (400).

## Odontograma

Base: `/historia-clinica` · permisos `ODONTOGRAMA_READ` (GET) ·
`ODONTOGRAMA_WRITE` (POST/DELETE). Los dientes se identifican por número **FDI**
(11–18, 21–28, 31–38, 41–48; permanentes; 51–55, 61–65, 71–75, 81–85: temporales).

| Método | Ruta | Notas |
|---|---|---|
| GET | `/historia-clinica/pacientes/{pacienteId}/odontograma` | Versión vigente con sus dientes; crea una versión `PERMANENTE` vacía si no existe |
| GET | `/historia-clinica/pacientes/{pacienteId}/odontogramas` | Historial (resumen por versión) |
| GET | `/historia-clinica/pacientes/{pacienteId}/odontogramas/{version}` | Una versión concreta (sólo lectura) |
| GET | `/historia-clinica/pacientes/{pacienteId}/odontograma/comparar?desde=&hasta=` | `nuevos` / `modificados` / `eliminados` entre dos versiones |
| GET | `/historia-clinica/odontogramas/{id}/eventos` | Log de cambios de la versión (`CREADO`, `MODIFICADO`, `ELIMINADO`) |
| POST | `/historia-clinica/pacientes/{pacienteId}/odontogramas` | `{fecha?, denticion?, observaciones?, copiarUltima?}` — crea la siguiente versión (por defecto copia la anterior) → `201` |
| POST | `/historia-clinica/odontogramas/{id}/dientes` | `{diente, superficie?, condicion, estado, notas?}` — alta o modificación (upsert por diente+superficie); **sólo sobre la versión vigente** |
| DELETE | `/historia-clinica/odontogramas/{id}/dientes/{dienteId}` | Quita la condición de la versión vigente → `204` |

`denticion`: `PERMANENTE` · `TEMPORAL` · `MIXTA`.
`superficie`: `OCLUSAL` · `MESIAL` · `DISTAL` · `VESTIBULAR` · `LINGUAL` · `INCISAL` (opcional: pieza completa).
`estado`: `DIAGNOSTICO` · `PLANIFICADO` · `PROCESO` · `COMPLETADO` · `REFERIDO`.

Errores típicos: `DIENTE_INVALIDO` (400, número FDI fuera de la dentición),
`NO_ENCONTRADO` (404), `VALIDACION` (400).

## Planes de tratamiento

Base: `/planes-tratamiento` · permisos `PLAN_TRATAMIENTO_READ` (GET) ·
`PLAN_TRATAMIENTO_WRITE` (POST/PUT/DELETE). El plan sólo se edita en
`BORRADOR`; sus líneas también sólo en `BORRADOR`, y cambian de estado sólo
cuando el plan está `EN_EJECUCION`.

| Método | Ruta | Notas |
|---|---|---|
| GET | `/planes-tratamiento/pacientes/{pacienteId}` | Listado con el conteo de líneas y presupuestos de cada plan |
| POST | `/planes-tratamiento/pacientes/{pacienteId}` | `{titulo, odontologoId?, observaciones?, fecha?}` → `201`, nace en `BORRADOR` |
| GET | `/planes-tratamiento/{id}` | Detalle con líneas y presupuestos |
| PUT | `/planes-tratamiento/{id}` | Cabecera; sólo `BORRADOR`/`PRESENTADO`/`RECHAZADO` → `409 PLAN_NO_EDITABLE` |
| DELETE | `/planes-tratamiento/{id}` | Sólo `BORRADOR` → `409 PLAN_NO_BORRADOR`; en otro estado → `204` |
| PUT | `/planes-tratamiento/{id}/estado` | `{estado, comentario?}`; sólo los destinos de la matriz → `409 TRANSICION_INVALIDA` / `ESTADO_ACTUAL` |
| POST | `/planes-tratamiento/{id}/lineas` | `{fase?, diente?, superficie?, diagnostico?, procedimiento, servicioId?, prioridad?, sesiones, precioUnitario, descuentoPct?, notas?, numero?}` → `201` y recalcula los totales del plan |
| PUT/DELETE | `/planes-tratamiento/{id}/lineas/{lineaId}` | Editar / eliminar una línea (sólo `BORRADOR`) |
| PUT | `/planes-tratamiento/{id}/lineas/{lineaId}/estado` | `{estado}` — sólo con el plan en `EN_EJECUCION` → `409 PLAN_NO_EN_EJECUCION` |
| GET | `/planes-tratamiento/{id}/presupuestos` | Historial (el más reciente primero) |
| POST | `/planes-tratamiento/{id}/presupuestos` | `{observaciones?}` → `201`; congela un **snapshot** de las líneas, deja el presupuesto en `PRESENTADO` y presenta el plan (`BORRADOR→PRESENTADO`); sin líneas → `409 PLAN_SIN_LINEAS` |
| GET | `/planes-tratamiento/{id}/presupuestos/{presupuestoId}` | Detalle con las líneas congeladas |
| PUT | `/planes-tratamiento/{id}/presupuestos/{presupuestoId}/estado` | `{estado, firmante?, comentario?}`; al `ACEPTADO` exige `firmante` (`400 FIRMANTE_REQUERIDO`), guarda `firmaHash` (SHA-256), marca `vigente` y sincroniza el estado del plan |
| GET | `/planes-tratamiento/{id}/pdf` | PDF (OpenPDF) descargable con `Content-Disposition: attachment` |

`estado` del plan: `BORRADOR` · `PRESENTADO` · `ACEPTADO` · `PARCIALMENTE_ACEPTADO` · `RECHAZADO` · `EN_EJECUCION` · `COMPLETADO` · `CANCELADO`.
`estado` de línea: `PENDIENTE` · `EN_EJECUCION` · `COMPLETADO` · `CANCELADO` · `REFERIDO`.
`estado` de presupuesto: `BORRADOR` · `PRESENTADO` · `ACEPTADO` · `RECHAZADO` · `ANULADO`.
`superficie`: `OCLUSAL` · `MESIAL` · `DISTAL` · `VESTIBULAR` · `LINGUAL` · `INCISAL`.
`prioridad`: `BAJA` · `MEDIA` · `ALTA` · `URGENTE`.

Errores típicos: `NO_ENCONTRADO` (404), `DIENTE_INVALIDO` y `FIRMANTE_REQUERIDO`
(400), `PLAN_NO_BORRADOR`, `PLAN_NO_EDITABLE`, `PLAN_NO_EN_EJECUCION`,
`PLAN_SIN_LINEAS`, `TRANSICION_INVALIDA`, `ESTADO_ACTUAL` (409),
`VALIDACION` (400).

## Periodontograma

Base: `/historia-clinica` · permisos `PERIODONCIA_READ` (GET) ·
`PERIODONCIA_WRITE` (POST/PUT/DELETE). Cada examen es una **versión**
inmutable de mediciones; la vigente es la de mayor `version`.

| Método | Ruta | Notas |
|---|---|---|
| GET | `/historia-clinica/pacientes/{pacienteId}/periodontograma` | Examen vigente con sus mediciones; `null` si no existe |
| GET | `/historia-clinica/pacientes/{pacienteId}/periodontogramas` | Historial (resumen por versión con conteo de sitios y piezas) |
| GET | `/historia-clinica/periodontogramas/{id}` | Detalle de un examen concreto |
| GET | `/historia-clinica/pacientes/{pacienteId}/periodontograma/comparar?desde=&hasta=` | Cambios campo a campo entre dos versiones: `NUEVO` / `MODIFICADO` / `ELIMINADO` + `agregados` / `eliminados` |
| POST | `/historia-clinica/pacientes/{pacienteId}/periodontogramas` | `{fecha?, observaciones?, copiarUltima?}` → `201`; por defecto copia las mediciones de la anterior |
| PUT | `/historia-clinica/periodontogramas/{id}` | Cabecera: `{fecha?, observaciones?}` |
| PUT | `/historia-clinica/periodontogramas/{id}/mediciones` | `{sitios[], dientes[]}` — upsert masivo por `diente + sitio` (y por `diente` para movilidad/furcación/notas) |
| DELETE | `/historia-clinica/periodontogramas/{id}/dientes/{diente}` | Elimina todas las mediciones de una pieza → `204` |
| GET | `/historia-clinica/periodontogramas/{id}/pdf` | PDF (OpenPDF) con matriz diente × sitio y resumen; `Content-Disposition: attachment` |

`sitio`: `VESTIBULAR` · `MESIAL` · `DISTAL` · `LINGUAL`.
`sondajeMm` / `insercionMm`: 0–20 · `margenMm`: −5 a 10 · `movilidad` / `furcacion`: 0–3.
`diente` en notación **FDI** (11–18, 21–28, 31–38, 41–48; 51–55, 61–65, 71–75, 81–85).

Celda del PDF/UI: `sondaje/margen/inserción` más las letras `S` (sangrado),
`P` (placa) y `U` (supuración).

Errores típicos: `DIENTE_INVALIDO`, `MOVILIDAD_INVALIDA`,
`FURCACION_INVALIDA` (400), `VALIDACION` (400), `NO_ENCONTRADO` (404).

## Recetas

Base: `/historia-clinica` · permisos `RECETAS_READ` (GET) ·
`RECETAS_WRITE` (POST/PUT). Cada receta lleva fecha, profesional,
diagnóstico e historial de anulación; las líneas concentran la pauta
completa del medicamento.

| Método | Ruta | Notas |
|---|---|---|
| GET | `/historia-clinica/pacientes/{pacienteId}/recetas` | Historial (fecha, estado, profesional, diagnóstico y nº de medicamentos) |
| GET | `/historia-clinica/recetas/{id}` | Detalle con las líneas numeradas |
| POST | `/historia-clinica/pacientes/{pacienteId}/recetas` | `{fecha?, odontologoId?, diagnostico?, observaciones?, lineas[]}` → `201` |
| PUT | `/historia-clinica/recetas/{id}` | Editar cabecera y **reemplazar** las líneas (sólo si `VIGENTE`) |
| PUT | `/historia-clinica/recetas/{id}/estado` | `{estado: "ANULADA", motivo}` — la anulación exige motivo y es irreversible |
| GET | `/historia-clinica/recetas/{id}/pdf` | PDF (OpenPDF) con cabecera, tabla de medicamentos e instrucciones; `Content-Disposition: attachment` |

`via`: `ORAL` · `SUBLINGUAL` · `TOPICA` · `INTRAMUSCULAR` · `INTRAVENOSA` ·
`RECTAL` · `OFTALMICA` · `OTICA` · `INHALATORIA`.
`medicamento` y `dosis` son obligatorios en cada línea.

Errores típicos: `RECETA_SIN_LINEAS` y `LINEA_INVALIDA` (400),
`RECETA_ANULADA` y `RECETA_YA_ANULADA` (409), `MOTIVO_REQUERIDO` (400),
`ODONTOLOGO_NO_ENCONTRADO` y `NO_ENCONTRADO` (404).

## Consentimientos

Base: `/historia-clinica` · permisos `CONSENTIMIENTOS_READ` (GET) ·
`CONSENTIMIENTOS_WRITE` (POST/PUT/DELETE). Las plantillas son por tenant y
se sirven con su versión; al emitir, el consentimiento **copia** el texto y
la versión de la plantilla (cambios futuros de la plantilla no lo afectan).

| Método | Ruta | Notas |
|---|---|---|
| GET | `/historia-clinica/plantillas-consentimiento` | Catálogo (`codigo`, `nombre`, `categoria`, `contenido`, `version`) |
| PUT | `/historia-clinica/plantillas-consentimiento/{codigo}` | `{contenido, nombre?}` → guarda y **sube `version` en 1** |
| GET | `/historia-clinica/pacientes/{pacienteId}/consentimientos` | Historial (fecha, título, plantilla+versión, estado, firmante) |
| GET | `/historia-clinica/consentimientos/{id}` | Detalle con el texto congelado, tutor (`tutorId`, `tutorNombre`, `tutorParentesco`, `tutorTelefono`), profesional, firma y `firmaHash` |
| POST | `/historia-clinica/pacientes/{pacienteId}/consentimientos` | `{plantillaCodigo, fecha?, tutorNombre?, tutorId?, odontologoId?, observaciones?}` → `201` en `BORRADOR` |
| PUT | `/historia-clinica/consentimientos/{id}` | Editar cabecera y texto **sólo en `BORRADOR`** |
| DELETE | `/historia-clinica/consentimientos/{id}` | `204` **sólo en `BORRADOR`** |
| POST | `/historia-clinica/consentimientos/{id}/firmar` | `{firmante, firmaImagen?}` (data URL) → congela el consentimiento y calcula `firmaHash` (SHA-256) |
| GET | `/historia-clinica/consentimientos/{id}/pdf` | PDF con texto, bloque de firma (imagen opcional) y huella; `Content-Disposition: attachment` |

Estados: `BORRADOR` (editable/borrable) → `FIRMADO` (inmutable).
Plantillas sembradas: `TRATAMIENTO_GENERAL` · `EXTRACCION` · `ENDODONCIA` ·
`IMPLANTE` · `CIRUGIA_ORAL` · `ORTODONCIA` · `IMAGENES` · `BLANQUEAMIENTO`.

**Menores de edad (§4.14).** `tutorId` referencia un tutor registrado en
`GET /pacientes/{id}` (`tutores[]`) y al emitir se guarda un snapshot del
tutor (`tutorId`, `tutorNombre`, `tutorParentesco`, `tutorTelefono`). Si el
paciente es menor (menos de 18 años al emitir) el consentimiento **no se
crea** sin tutor; al firmar, `firmante` debe coincidir con el tutor
(insensible a mayúsculas y espacios) y se persiste el nombre canónico del
tutor. Con menor sin `tutorId` ni `tutorNombre` se permite el texto libre
si el paciente no tiene tutores registrados.

Errores típicos: `PLANTILLA_NO_ENCONTRADA` (404), `FIRMANTE_REQUERIDO` (400),
`TUTOR_REQUERIDO` (400, menor sin tutor al emitir o al editar),
`FIRMANTE_DEBE_SER_TUTOR` (400, la firma de un menor no corresponde a su
tutor), `CONSENTIMIENTO_FIRMADO` y `CONSENTIMIENTO_YA_FIRMADO` (409),
`TUTOR_NO_ENCONTRADO` (404, el tutor no pertenece al paciente),
`ODONTOLOGO_NO_ENCONTRADO` y `NO_ENCONTRADO` (404).

## Archivos clínicos

Base: `/historia-clinica` · permisos `ARCHIVOS_CLINICOS_READ` (GET) ·
`ARCHIVOS_CLINICOS_WRITE` (POST/DELETE). Sólo se persiste la **metadata** en
PostgreSQL; los bytes viven en el `FileStorageProvider` local
(`STORAGE_LOCAL_DIR`, volumen `storage_data`) y nunca se expone la ruta física.

| Método | Ruta | Notas |
|---|---|---|
| GET | `/historia-clinica/pacientes/{pacienteId}/archivos` | Listado (`categoria`, `nombre`, `mime`, `tamano`, `sha256`, `fecha`, `observaciones`, `subidoEn`) |
| POST | `/historia-clinica/pacientes/{pacienteId}/archivos` | multipart: `archivo`, `categoria`, `fecha?`, `observaciones?` → `201` |
| GET | `/historia-clinica/archivos/{id}/contenido` | Bytes con su `mime` real y `Content-Disposition: inline` |
| DELETE | `/historia-clinica/archivos/{id}` | `204`; borra la metadata **y** el archivo físico |

Categorías: `RADIOGRAFIA` · `FOTOGRAFIA` · `PANORAMICA` · `TOMOGRAFIA` ·
`RESULTADO` · `DOCUMENTO`. Formatos admitidos: JPG, JPEG, PNG, WEBP, TIF,
TIFF, PDF y DCM · máximo 20 MB por archivo (el multipart global está en 25 MB).

Errores típicos: `CATEGORIA_REQUERIDA` y `CATEGORIA_INVALIDA` (400),
`ARCHIVO_VACIO`, `ARCHIVO_DEMASIADO_GRANDE`, `ARCHIVO_FORMATO_INVALIDO`,
`ARCHIVO_NOMBRE_INVALIDO` y `ARCHIVO_INVALIDO` (400), `NO_ENCONTRADO` y
`ARCHIVO_FISICO_NO_ENCONTRADO` (404), `ACCESO_DENEGADO` (403) y
`VALIDACION` (400, parámetro `categoria` ausente).

## Reportes clínicos

Base: `/historia-clinica` · permiso `REPORTES_CLINICOS` (sembrado en `V27`:
SUPER_ADMIN, ADMIN y ODONTOLOGO; recepción recibe `403`). Sin migración: los
dos endpoints sólo leen datos que ya existen.

| Método | Ruta | Notas |
|---|---|---|
| GET | `/historia-clinica/reportes` | Query: `desde?`, `hasta?`, `limite?` (1–50, defecto 10) → totales, diagnósticos frecuentes, tratamientos pendientes, continuidad y producción |
| GET | `/historia-clinica/pacientes/{pacienteId}/reportes/historia-completa.pdf` | Historia completa en PDF con `Content-Disposition: attachment; filename="historia-clinica-completa-{id}.pdf"` |

Sin fechas el rango es **el último año** hasta hoy; `desde > hasta` responde
`400 RANGO_INVALIDO` y el paciente inexistente `404 NO_ENCONTRADO`.

``` json
{
  "desde": "2025-10-06",
  "hasta": "2026-10-06",
  "totales": { "evoluciones": 1, "examenes": 1, "diagnosticos": 1, "odontogramas": 3,
               "periodontos": 2, "recetas": 18, "consentimientos": 42, "archivos": 1,
               "planes": 3, "citasAtendidas": 0, "pacientesAtendidos": 1 },
  "diagnosticosFrecuentes": [ { "nombre": "Caries", "total": 4, "pacientes": 3 } ],
  "tratamientosPendientes": [ { "pacienteId": 1, "paciente": "Ana Pérez", "planId": 2,
                                "plan": "Rehabilitación", "lineaId": 5,
                                "procedimiento": "Empaste", "diente": 21,
                                "superficie": "OCLUSAL", "prioridad": "URGENTE",
                                "estado": "PENDIENTE", "precio": 40 } ],
  "continuidad": { "citasAtendidas": 0, "citasConfirmadas": 2, "citasCanceladas": 1,
                   "citasNoAsistio": 0, "pacientesActivos": 1, "pacientesSinProximaCita": 7,
                   "sinActividad90d": [ { "pacienteId": 3, "paciente": "Luis Gómez" } ] },
  "produccionClinica": [ { "procedimiento": "Extracción", "cantidad": 2, "total": 90 } ]
}
```

Los totales, la producción y los diagnósticos se calculan sobre el rango
`[desde, hasta]`; los tratamientos pendientes reflejan el estado **actual**
(líneas `PENDIENTE`/`EN_EJECUCION` de planes no cancelados, ordenadas por
prioridad) y `sinActividad90d` lista los pacientes sin evolución, examen,
odontograma ni receta en los 90 días previos a `hasta`.

El PDF se arma en `HistoriaCompletaService` (11 secciones: antecedentes,
exámenes, diagnósticos, evoluciones con sus adendas, odontograma vigente,
periodoncia, planes, recetas, consentimientos, archivos y citas atendidas) y
lo renderiza `HistoriaCompletaPdfService` con la cabecera del consultorio.

## Facturación (finanzas internas)

Base: `/facturacion` · permisos sembrados en `V27` (sin migración de permisos):
`FACTURACION_READ/WRITE/EMITIR/ANULAR`, `CAJA_READ/WRITE/CERRAR` y
`REPORTES_FINANCIEROS`. SUPER_ADMIN/ADMIN los tienen todos; RECEPCION tiene
`FACTURACION_READ/WRITE` y `CAJA_READ/WRITE` (no `CAJA_CERRAR` ni
`REPORTES_FINANCIEROS`); ODONTOLOGO ninguno. Migraciones `V37` (cuenta) · `V38` (caja) ·
`V39`–`V43` (configuración tributaria, comprobantes electrónicos y notas de
crédito, Fases G y H.1).

### Cuenta del paciente

| Método | Ruta | Notas |
|---|---|---|
| GET | `/facturacion/cuentas?estado=&buscar=` | Listado con saldo; `estado`: `ABIERTA/PARCIAL/PAGADA/CERRADA/ANULADA`, `buscar` por nombre o cédula |
| GET | `/facturacion/cuentas/{pacienteId}` | **Auto-crea** la cuenta si el paciente todavía no tiene una |
| POST | `/facturacion/cuentas/{pacienteId}/movimientos` | `{tipo, concepto, diente?, superficie?, cantidad?, precioUnitario?, descuentoPct?, total?, formaPago?, fecha?, observaciones?}` → devuelve la cuenta actualizada |
| POST | `/facturacion/cuentas/{pacienteId}/movimientos/{id}/anular` | `{motivo}` |
| PUT | `/facturacion/cuentas/{pacienteId}/estado` | `{estado, motivo?}`: cerrar, reabrir (`ABIERTA`) o anular |
| GET | `/facturacion/pagos?desde&hasta&formaPago&pacienteId` | Abonos, anticipos y devoluciones vigentes del rango |

`tipo` ∈ `CARGO`, `DESCUENTO`, `ABONO`, `ANTICIPO`, `DEVOLUCION`. El saldo es
`ΣCARGO − ΣDESCUENTO − ΣABONO − ΣANTICIPO + ΣDEVOLUCION`. Abonos, anticipos y
devoluciones exigen `formaPago` (`EFECTIVO/TRANSFERENCIA/TARJETA/OTRO`).

``` json
{
  "id": 1, "pacienteId": 1, "paciente": "Ana Pérez", "cedula": "1712345678",
  "estado": "PARCIAL", "saldo": 60, "totalCargos": 200, "totalDescuentos": 0,
  "totalAbonos": 140, "cantidadMovimientos": 3,
  "movimientos": [
    { "id": 11, "tipo": "CARGO", "origen": "PROCEDIMIENTO", "referenciaId": 5,
      "concepto": "Obturación", "diente": 21, "superficie": "OCLUSAL",
      "cantidad": 1, "precioUnitario": 200, "descuentoPct": 0, "total": 200,
      "formaPago": null, "fecha": "2026-10-07", "anulado": false }
  ]
}
```

Errores: `400 MONTO_INVALIDO` · `400 DESCUENTO_INVALIDO` ·
`409 DESCUENTO_EXCEDE` · `400 FORMA_PAGO_REQUERIDA` · `400 FORMA_PAGO_INVALIDA` ·
`409 SALDO_INSUFICIENTE` (devolución mayor que lo abonado) · `409 CUENTA_BLOQUEADA` ·
`409 CUENTA_CON_DINERO` · `409 ESTADO_ACTUAL` · `400 ESTADO_INVALIDO` ·
`409 REAPERTURA_INVALIDA` · `409 MOVIMIENTO_YA_ANULADO` · `400 RANGO_INVALIDO`.

### Caja

| Método | Ruta | Notas |
|---|---|---|
| GET | `/facturacion/cajas/actual` | Sesión `ABIERTA`; `404` si no hay ninguna |
| POST | `/facturacion/cajas` | `{nombre, montoApertura, observaciones?}` → `409 CAJA_ABIERTA` si ya hay una |
| PUT | `/facturacion/cajas/actual/cerrar` | `{montoDeclarado, observaciones?}` → `409 CAJA_NO_ABIERTA` (permiso `CAJA_CERRAR`) |
| GET | `/facturacion/cajas?desde&hasta` | Historial de sesiones |
| GET | `/facturacion/cajas/{id}` | Detalle con totales y movimientos |
| POST | `/facturacion/cajas/{id}/movimientos` | `{tipo, concepto, formaPago, monto, pacienteId?}` (`INGRESO`/`EGRESO`) |
| POST | `/facturacion/cajas/{id}/movimientos/{mid}/anular?motivo=` | Sólo en sesión `ABIERTA` |

Conciliación: `esperado = apertura + ingresos − egresos` (sólo efectivo) y
`diferencia = montoDeclarado − esperado`. Los cobros de una cuenta generan su
movimiento de caja automáticamente.

``` json
{
  "id": 7, "estado": "ABIERTA", "fecha": "2026-10-07", "montoApertura": 20,
  "montoDeclarado": null, "diferencia": null, "abiertoPor": "admin",
  "totales": { "apertura": 20, "ingresos": 144, "egresos": 5, "declarado": null,
               "esperadoEfectivo": 159, "diferencia": null,
               "porForma": [ { "formaPago": "EFECTIVO", "monto": 139 } ],
               "movimientos": 2 },
  "movimientos": []
}
```

Errores: `404 CAJA_NO_ENCONTRADA` · `409 CAJA_ABIERTA` · `409 CAJA_NO_ABIERTA` ·
`409 CAJA_CERRADA` · `404 MOVIMIENTO_NO_ENCONTRADO` · `409 MOVIMIENTO_YA_ANULADO` ·
`400 CONCEPTO_REQUERIDO` · `400 MONTO_INVALIDO`.

### Dashboard y reportes

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/facturacion/dashboard?desde&hasta` | `FACTURACION_READ` |
| GET | `/facturacion/reportes/ventas?desde&hasta` | `REPORTES_FINANCIEROS` |
| GET | `/facturacion/reportes/produccion?desde&hasta` | `REPORTES_FINANCIEROS` |
| GET | `/facturacion/reportes/saldos` | `REPORTES_FINANCIEROS` |
| GET | `/facturacion/reportes/caja?desde&hasta` | `REPORTES_FINANCIEROS` |

El dashboard devuelve facturado/cobrado del rango, lo de hoy y del mes, cuentas
por cobrar con su cantidad, anticipos, cobros y ticket promedio más los
desgloses por forma de pago, odontólogo y servicio. Los reportes devuelven
ventas por día y por forma de pago, producción por odontólogo, cuentas con
saldo y el resumen de sesiones de caja. `desde > hasta` → `400 RANGO_INVALIDO`.

### Comprobantes electrónicos (SRI)

El ambiente `PRUEBAS` con P12 válido llama los servicios de pruebas del SRI. Sin P12, el modo
`SIMULADA` es un **SRI local**: no hace llamadas de red y autoriza el comprobante en la misma
transacción (`estadoGeneracion=ENVIADO`, `estadoTributario=AUTORIZADO`, número de autorización =
clave de acceso, fecha actual, `xmlAutorizado` = XML firmado y `sriRespuesta` «Autorización
simulada (ambiente de pruebas)…»), de modo que RIDE, notas de crédito y anulación funcionan
end-to-end sin salir de la máquina. No equivale a recepción ni autorización del SRI real: el flag
`simulado` de la respuesta es el aviso único de esa distinción. En `PRODUCCION`, firma simulada →
`SRI_FIRMA_REAL_REQUERIDA`. `FacturaBuilder` actualmente presupone precio con IVA incluido y tarifa
codificada de 15%; no emitir en productivo hasta confirmar tratamiento fiscal por concepto con
la clínica/asesoría tributaria. Formas de pago: catálogo `01/16/19/20`.

| Método | Ruta | Permiso |
|---|---|---|
| POST | `/facturacion/comprobantes` | `FACTURACION_EMITIR` |
| GET | `/facturacion/comprobantes?q&desde&hasta&tipo&estado&generacion&pacienteId&establecimiento&usuarioId&page&size` | `FACTURACION_READ` |
| GET | `/facturacion/comprobantes/{id}` | `FACTURACION_READ` |
| GET | `/facturacion/comprobantes/{id}/xml` | `FACTURACION_READ` |
| GET | `/facturacion/comprobantes/{id}/ride` | `FACTURACION_READ` |
| POST | `/facturacion/comprobantes/{id}/reenviar` | `FACTURACION_EMITIR` |
| POST | `/facturacion/comprobantes/{id}/consultar` | `FACTURACION_EMITIR` |
| POST | `/facturacion/notas-credito` | `FACTURACION_ANULAR` |
| POST | `/facturacion/comprobantes/{id}/anular` | `FACTURACION_ANULAR` |

Emisión: `POST /facturacion/comprobantes` con
`{cuentaId, fechaEmision?, formasPago:[{codigo, total, plazo?, unidadTiempo?}]}`.
La cuenta se factura una sola vez (`409 CUENTA_YA_FACTURADA`); se recalculan los
totales, se genera la clave de 49 dígitos, el XML se valida contra el XSD y se firma. En firma
SIMULADA el «SRI local» autoriza sin red; con certificado el backend llama recepción y
autorización. Un timeout requiere consultar el mismo comprobante antes de considerar otro envío.

El RIDE requiere `estadoTributario=AUTORIZADO` y XML (firmado o autorizado); si no,
`409 RIDE_NO_AUTORIZADO`. Reenviar/consultar un simulado responde
`409 COMPROBANTE_SIMULADO_NO_REINTENTABLE` (el SRI no se consultó nunca).

Bandeja: paginada (`{content, page:{…}}`) y filtrable por texto libre (número,
clave o paciente), rango de fechas, `tipo` (factura/nota de crédito), `estado`
(`GENERADO/ENVIADO/AUTORIZADO/RECHAZADO`), `generación` y usuario. `tipo`
desconocido → `400 TIPO_COMPROBANTE_INVALIDO`.

Errores: `400 FORMA_PAGO_INVALIDA` · `400 FORMAS_PAGO_NO_COINCIDEN` ·
`400 TIPO_COMPROBANTE_INVALIDO` · `409 CUENTA_YA_FACTURADA` ·
`409 COMPROBANTE_YA_AUTORIZADO` · `409 COMPROBANTE_NO_REINTENTABLE` ·
`404 COMPROBANTE_NO_ENCONTRADA`.

**Nota de crédito** (plan §3.9): `POST /facturacion/notas-credito` con
`{comprobanteId, motivo, lineas?, fechaEmision?}`. La factura de referencia debe
estar `AUTORIZADO` y sin anular; `motivo` es obligatorio (5–300 caracteres, el
XSD del SRI lo exige). `lineas` vacío o ausente = **devolución total**; para una
**devolución parcial** se envía `lineas:[{linea, cantidad}]` (línea 1-based, con
`cantidad` ≤ la facturada). La nota arrastra el detalle y los impuestos del XML
original, usa el **secuencial propio del tipo 04** y devuelve como mínimo el
mismo importe que la factura: lo ya devuelto con notas vigentes más la nueva no
puede superarlo (`409 NOTA_CREDITO_EXCEDE`). Recorre el mismo flujo que la
factura (XSD, firma, envío y autorización) y se lista en la misma bandeja.

**Anulación** (plan §3.10): `POST /facturacion/comprobantes/{id}/anular` con
auditoría. Repetir la anulación → `409 COMPROBANTE_YA_ANULADO` · ya con un
trámite vigente → `409 ANULACION_YA_SOLICITADA` · sin autorizar →
`409 COMPROBANTE_NO_AUTORIZADO` · ya no se admiten más notas (`409 COMPROBANTE_ANULADO`).
`{motivo}` (5–500). En modo simulado la anulación la aplica el **SRI local** en la misma
transacción: el comprobante pasa a `ANULADO`, el trámite queda `APLICADA` con `aplicadoEn`, la
respuesta SRI es «Anulación simulada (ambiente de pruebas)…» y se audita `ANULAR_COMPROBANTE`.
La anulación real sigue sin estar integrada: un comprobante no simulado responde
`501 SRI_ANULACION_NO_DISPONIBLE` y no cambia el comprobante ni crea trámite. No se debe
interpretar una anulación local como anulación tributaria.

La respuesta de comprobante incluye ahora `comprobanteReferenciaId` (la factura
que modifica la nota) y `motivo`.

### Integración con el plan de tratamiento

Cambiar una línea a `COMPLETADO` crea el cargo correspondiente en la cuenta del
paciente (`origen: PROCEDIMIENTO`, `referenciaId` = id de la línea, idempotente)
y revertir el estado lo anula. Una cuenta `CERRADA` recibe el cargo pero no
acepta movimientos manuales; una `ANULADA` no recibe ninguno.

## WhatsApp / OpenWA

| Método | Ruta | Notas |
|---|---|---|
| POST | `/webhooks/whatsapp` | Público (lo llama OpenWA); mensaje + webhook automático del agente |
| GET | `/whatsapp/sesiones` | |
| POST | `/whatsapp/sesiones` | `{sesionId, nombre?}` |
| GET | `/whatsapp/sesiones/{id}` | |
| POST | `/whatsapp/sesiones/{id}/conectar` | Devuelve QR (o `PENDIENTE_QR`) |
| POST | `/whatsapp/sesiones/{id}/desconectar` | |
| DELETE | `/whatsapp/sesiones/{id}` | |
| GET | `/whatsapp/conversaciones?estado=&sesionId=` | Estado: `BOT`, `ATENCION_HUMANA` |
| GET | `/whatsapp/conversaciones/{id}` | Detalle con mensajes |
| GET | `/whatsapp/conversaciones/{id}/mensajes` | |
| POST | `/whatsapp/conversaciones/{id}/mensajes` | `{texto}` (envía por OpenWA) |
| POST | `/whatsapp/conversaciones/{id}/estado` | `{estado}` |

## Plantillas y automatizaciones

| Método | Ruta |
|---|---|
| GET | `/plantillas` · POST `/plantillas` · PUT/DELETE `/plantillas/{id}` |
| GET | `/automatizaciones` · POST `/automatizaciones` · PUT/DELETE `/automatizaciones/{id}` |
| POST | `/automatizaciones/{id}/activar` | POST `/automatizaciones/{id}/desactivar` |
| GET | `/automatizaciones/notificaciones?limite=` | Cola de notificaciones generadas |

Eventos válidos: `CITA_CREADA`, `CITA_PROXIMA`, `CITA_CONFIRMADA`, `CITA_ATENDIDA`,
`CITA_CANCELADA`, `CITA_MODIFICADA`, `NO_ASISTIO`, `COBRO_REALIZADO`. Variables de plantilla
(citas): `{{paciente}}`, `{{odontologo}}`, `{{servicio}}`, `{{fecha}}`, `{{hora}}`,
`{{clinica}}`, `{{direccion}}`, `{{telefono}}`. Variables de plantilla (cobro):
`{{nombre}}`, `{{apellido}}`, `{{nombre_completo}}`, `{{monto}}`, `{{forma_pago}}`,
`{{concepto}}`, `{{saldo}}`, `{{tipo_movimiento}}`, `{{fecha}}`, `{{codigo_movimiento}}`.
Las automatizaciones aceptan `destinatario: PACIENTE|ODONTOLOGO`; las reglas de `CITA_PROXIMA`
usan `minutosAntes` (por ejemplo, 1440 para 24 horas o 120 para 2 horas). Al confirmar una cita
se regeneran sus recordatorios pendientes. Los avisos para el odontólogo usan el teléfono del
odontólogo asignado. `COBRO_REALIZADO` se publica al registrar un abono o anticipo en la cuenta
del paciente (`CuentaPacienteService.agregarMovimiento`); sólo admite destinatario `PACIENTE` y
genera una notificación sin cita asociada (la plantilla sembrada es «Pago recibido», activa por
tenant desde `V45`). El envío sale por WhatsApp (`MessagingProvider`) y, si sale bien, por correo
secundario con asunto «Pago recibido» cuando el paciente tiene email.

## Agente IA

| Método | Ruta | Notas |
|---|---|---|
| GET | `/agente/conversaciones` | Supervisión (intención, contexto, agente activo) |
| POST | `/agente/conversaciones/{id}/transferir` | Pasa a `ATENCION_HUMANA` y silencia al agente |
| POST | `/agente/conversaciones/{id}/agente` | `{activo: bool}` — reactivar/desactivar el bot |

## Agenda pública (enlace temporal, sin JWT)

Endpoints públicos protegidos por un token HMAC-SHA256 firmado (§11–§13 del plan de
identificación/agendamiento). El token lo entrega el agente de WhatsApp cuando el paciente
elige "Ver calendario de disponibilidad"; caduca por defecto en 15 minutos
(`app.agenda-publica.ttl-minutes`). El token lleva un propósito: `BOOK_APPOINTMENT` si
el agente conoce al paciente (sólo ése puede reservar), o `VIEW_AVAILABILITY` cuando el
enlace es anónimo (sólo consulta; `/reservar` lo rechaza). La página Angular
`dental_crm/agenda-publica?t=` los consume en móvil.

| Método | Ruta | Notas |
|---|---|---|
| GET | `/public/agenda/context?t=` | `{odontologoId, odontologo, especialidad, paciente (primer nombre, vacío si anónimo), servicioId, expiraEnMinutos}` — nunca cédula ni teléfono |
| GET | `/public/agenda/dias?t=&year=&month=` | Días del mes con `estado` (`DISPONIBLE`, `POCA`, `SIN_DISPONIBILIDAD`, `NO_LABORABLE`, `PASADO`) y `cupos`; usa la misma fuente que el chat (`AgendaService.disponibilidad`); `NO_LABORABLE` = excepción `esCerrado()` o sin horario activo ese día de la semana |
| GET | `/public/agenda/slots?t=&fecha=` | Cupos libres del día en vivo; cada cupo trae además `slotToken`, un token firmado (propósito `SLOT`, mismo TTL) que fija odontólogo/fecha/hora exactos |
| POST | `/public/agenda/reservar` | `{token, fecha?, horaInicio?, odontologoId?, servicioId?, slotToken?, idempotencyKey?}` → `{estado: CREADA\|REPETIDA\|OCUPADA\|ERROR, cita?, alternativas?, mensaje}`; si llega `slotToken`, fecha/hora/odontólogo se resuelven del propio cupo (la página no los envía); exige token con propósito `BOOK_APPOINTMENT` y paciente; revalida el cupo antes de crear; con `idempotencyKey` la misma clave devuelve la cita ya creada (`REPETIDA`, UNIQUE por tenant+key) y sin clave hay dedupe natural por persona/hora; audita como `RESERVA_WEB` (módulo `AGENDA`) y notifica por WhatsApp vía `CitaCreadaEvent` |

El filtro `PublicRateLimitFilter` limita a 60 peticiones/min por IP en `/api/v1/public/` (§16).

## Google Calendar

| Método | Ruta |
|---|---|
| GET | `/google/status` |
| GET | `/google/connect` → `{authUrl}` |
| GET | `/google/callback?code=` \| `?error=` | Público (OAuth); intercambia el código y responde `302` a `{FRONTEND_PUBLIC_URL}/google?google=connected\|error` |
| GET | `/google/calendars` |
| POST | `/google/calendars/{id}/select` |
| POST | `/google/sync` → `{creados, actualizados, cancelados, errores, sincronizadas}` |
| DELETE | `/google/disconnect` |

## Reportes

| Método | Ruta | Notas |
|---|---|---|
| GET | `/reportes/dashboard` | KPIs del día/mes |
| GET | `/reportes/estadisticas` | Series por día + top odontólogos, servicios, estados y fuentes (`confirmaciones/cancelaciones/noAsistencia` son listas `{fecha,total}`) |

## Usuarios y auditoría

| Método | Ruta |
|---|---|
| GET | `/usuarios?q=&page=&size=` · POST `/usuarios` · GET/PUT `/usuarios/{id}` |
| GET | `/auditoria?q=&modulo=&page=&size=` |

`UsuarioUpdateRequest`: `{email, nombres, apellidos, telefono, estado, nuevaPassword?, roles[]}`.
Módulos de auditoría: `PACIENTES`, `ODONTOLOGOS`, `SERVICIOS`, `HORARIOS`, `AGENDA`, `CITAS`,
`USUARIOS`, `WHATSAPP`, `AUTOMATIZACIONES`, `AGENTE_IA`, `GOOGLE_CALENDAR`, `REDES_SOCIALES`.

## Redes sociales

| Método | Ruta |
|---|---|
| GET | `/social/status` · GET `/social/{provider}/connect` · DELETE `/social/{provider}/disconnect` |
| GET | `/social/callback/{provider}` (sin permiso, sólo OAuth) |
| POST | `/social/publish` (`multipart/form-data`: `cuentaId`, `texto`, `imagen?`) |

`POST /social/publish` acepta sólo **Facebook** con texto (1–2200 caracteres) e imagen opcional
(JPEG/PNG/WebP, ≤5 MB); Instagram y TikTok devuelven `SOCIAL_URL_PUBLICA_REQUERIDA` porque sólo
aceptan imágenes servidas desde una URL pública. Ver `docs/SOCIAL.md` para el detalle completo.