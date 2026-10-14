# PLANIFICACIÓN DE AMPLIACIÓN --- CRM DENTAL AT (ALANTEK)

## Facturación Electrónica + Historia Clínica Odontológica

**Fecha:** 2026-10-01

## 1. Objetivo

Ampliar CRM Dental AT sin crear sistemas aislados, reutilizando
pacientes, odontólogos, servicios, citas, usuarios, permisos, auditoría
y multi-tenant.

## 2. Arquitectura funcional

``` text
Paciente
├─ Cita
│  └─ Atención clínica
│     ├─ Historia clínica
│     ├─ Odontograma
│     ├─ Periodontograma
│     ├─ Diagnósticos
│     ├─ Evolución
│     └─ Plan de tratamiento
│        └─ Presupuesto
│           └─ Cuenta / Pago
│              └─ Factura → XML → Firma → SRI → RIDE
└─ Timeline longitudinal
```

**Principio:** clínica y facturación estarán integradas pero
desacopladas. Un error tributario nunca bloqueará el registro clínico y
una anulación tributaria nunca borrará información clínica.

# 3. MÓDULO DE FACTURACIÓN Y COMPROBANTES ELECTRÓNICOS

## 3.1 Configuración tributaria

-   RUC, razón social, nombre comercial y direcciones.
-   Establecimiento y punto de emisión.
-   Ambiente SRI PRUEBAS/PRODUCCIÓN.
-   Secuenciales.
-   Logo y datos para RIDE.
-   Firma electrónica/certificado cifrado.
-   Fecha de expiración y alertas.
-   Correo y WhatsApp para entrega.
-   Configuración independiente por tenant.

## 3.2 Catálogo facturable

Ampliar `servicios` con código, precio, impuestos, tarifa, descuentos,
categoría y estado. Crear catálogo de productos si el consultorio
comercializa artículos.

## 3.3 Cuenta del paciente

Crear una capa financiera previa al comprobante: - cargos clínicos y
manuales; - descuentos; - anticipos; - abonos; - devoluciones; -
saldo; - formas de pago.

Estados: `ABIERTA`, `PARCIAL`, `PAGADA`, `CERRADA`, `ANULADA`.

> **Implementado en la Fase F:** migración `V37__cuenta_paciente.sql` con
> `cuentas_paciente` (una por paciente, `UNIQUE tenant_id + paciente_id`,
> totales denormalizados y estados derivados del saldo más `CERRADA`/`ANULADA`
> manuales) y `cuenta_detalles` como **libro mayor** (`CARGO`, `DESCUENTO`,
> `ABONO`, `ANTICIPO`, `DEVOLUCION`, con `forma_pago` obligatoria en los que
> mueven dinero, `odontologo_id`/`servicio_id` para los reportes, `total >= 0`
> con el signo según el tipo e índice único parcial por `referencia_id` para
> no duplicar cargos). `CuentaPacienteService` + `CuentaPacienteController`
> con 6 endpoints bajo `/api/v1/facturacion/cuentas` más
> `GET /facturacion/pagos`, permisos `FACTURACION_READ/WRITE` de `V27` y
> página **`/facturacion/cuentas`** (listado con filtros, detalle con el
> libro, alta de movimientos, anulación y cerrar/reabrir).

## 3.4 Caja

-   Apertura/cierre.
-   Ingresos.
-   Cobros.
-   Anticipos.
-   Devoluciones.
-   Efectivo, transferencia, tarjeta y otros.
-   Diferencias y conciliación.
-   Usuario responsable.
-   Reporte de caja.

> **Implementado en la Fase F:** migración `V38__caja.sql` con `cajas`
> (**una sesión `ABIERTA` por tenant** garantizada por índice único parcial,
> fondo inicial, responsable, declarado y diferencia) y `movimientos_caja`
> (`INGRESO`/`EGRESO` con `cuenta_detalle_id` de enlace). `CajaService` +
> `CajaController` (7 endpoints: actual, apertura, cierre con conciliación
> `esperado = apertura + neto efectivo`, historial, detalle, movimientos y
> anulación) con `CAJA_READ/WRITE/CERRAR`; cada abono, anticipo o devolución
> de una cuenta genera su movimiento en la sesión abierta
> (`registrarDesdeCuenta`). Página **`/facturacion/caja`** con apertura,
> totales por forma de pago, egreso manual, cierre con efectivo declarado e
> historial.

## 3.5 Factura electrónica

``` text
Cuenta → Validación → Secuencial → Clave acceso → XML
→ XSD → Firma → Envío SRI → Respuesta
→ XML autorizado → RIDE → Entrega paciente
```

Persistir estados de generación y estados SRI por separado.

## 3.6 Motor electrónico

``` text
ComprobanteElectronicoService
├─ FacturaBuilder
├─ NotaCreditoBuilder
├─ NotaDebitoBuilder
├─ RetencionBuilder
├─ LiquidacionBuilder [opcional]
└─ GuiaRemisionBuilder [opcional]

XmlGenerator
XmlValidator
ElectronicSignatureService
SriGateway
AuthorizationService
RideGenerator
DocumentDeliveryService
```

## 3.7 Bandeja de comprobantes

Filtros por fechas, paciente, identificación, tipo, número, clave de
acceso, estado, establecimiento y usuario.

Acciones: - ver; - XML; - RIDE; - reenviar; - consultar SRI; - revisar
errores; - nota de crédito; - anulación/control; - auditoría.

## 3.8 Estados

Generación: `BORRADOR`, `GENERADO`, `FIRMADO`, `ENVIADO`, `ERROR`.

Tributario: `PENDIENTE`, `AUTORIZADO`, `DEVUELTO`, `NO_AUTORIZADO`,
`ANULADO`.

Nunca eliminar físicamente comprobantes emitidos.

## 3.9 Documentos

### Nota de crédito

Referencia obligatoria al documento original, motivo, devolución
total/parcial, líneas, impuestos, XML, firma, autorización y RIDE.

### Nota de débito

Documento relacionado, motivo, valor, impuestos y flujo electrónico.

### Retenciones

Submódulo habilitable: sujeto, documento sustento, bases, porcentajes,
impuestos, XML, firma y RIDE.

### Liquidaciones y guías

Dejarlas previstas en arquitectura y activarlas sólo si CRM Dental AT
realmente las requiere.

## 3.10 Anulación

Registrar solicitud, motivo, fecha, usuario, evidencia, estado,
resultado y auditoría. Nunca implementar "eliminar factura".

## 3.11 RIDE y entrega

Generar PDF con datos fiscales, cliente, detalle, impuestos, pagos,
autorización y clave de acceso. Entrega por correo, WhatsApp y descarga.

## 3.12 Dashboard financiero

-   facturado/cobrado hoy y mes;
-   cuentas por cobrar;
-   anticipos;
-   ticket promedio;
-   errores SRI;
-   notas de crédito;
-   ingresos por odontólogo;
-   ingresos por servicio;
-   ingresos por forma de pago.

> **Implementado en la Fase F (sin SRI):** `GET /api/v1/facturacion/dashboard`
> con `FACTURACION_READ` devuelve facturado/cobrado del rango, de hoy y del
> mes, cuentas por cobrar con su cantidad, anticipos, número de cobros y
> ticket promedio, más los desgloses por forma de pago, odontólogo y
> servicio. Página **`/facturacion`** (entrada del menú **Finanzas**): rango
> editable, seis tarjetas, bloque de hoy/mes y las tres tablas de desglose.
> Los errores de SRI y las notas de crédito llegan con las Fases G y H.

## 3.13 Reportes

-   ventas por rango;
-   servicios;
-   producción por odontólogo;
-   pacientes/saldos;
-   caja;
-   comprobantes;
-   autorizados/devueltos/anulados;
-   notas de crédito;
-   impuestos;
-   secuenciales;
-   PDF/CSV y posteriormente XLSX.

> **Implementado en la Fase F:** cuatro endpoints con
> `REPORTES_FINANCIEROS` — `GET /facturacion/reportes/ventas` (totales, filas
> por día, cobros por forma de pago y top de servicios), `.../produccion`
> (cargos y facturado por odontólogo), `.../saldos` (total por cobrar con las
> cuentas) y `.../caja` (sesiones con apertura, declarado y diferencia) — con
> `400 RANGO_INVALIDO`. Página **`/facturacion/reportes`** con pestañas
> Ventas / Producción / Saldos / Caja. Quedan para la Fase G/H los
> comprobantes, autorizados/devueltos/anulados, notas de crédito, impuestos,
> secuenciales y la exportación.

## 3.14 Tablas sugeridas

> **Implementado en la Fase F:** `cuentas_paciente`, `cuenta_detalles`,
> `cajas` y `movimientos_caja` (`V37`/`V38`). No se creó `pagos`: el abono o
> anticipo es un movimiento del libro con `forma_pago`; `productos_facturables`
> y `configuracion_facturacion` quedan para la Fase G.

``` text
configuracion_facturacion
establecimientos
puntos_emision
secuenciales
productos_facturables
cuentas_paciente
cuenta_detalles
pagos
cajas
movimientos_caja
comprobantes
comprobante_detalles
comprobante_impuestos
comprobante_pagos
comprobante_mensajes_sri
comprobante_archivos
notas_credito_relaciones
retenciones
retencion_detalles
entregas_comprobante
solicitudes_anulacion
```

## 3.15 API

> **Implementado en la Fase F:** `/cuentas`, `/pagos`, `/cajas`, `/dashboard`
> y `/reportes` (18 endpoints bajo `/api/v1/facturacion`). El resto —
> configuración, comprobantes, facturas, notas y SRI — pertenece a las fases
> G y H.

``` text
/api/v1/facturacion/configuracion
/api/v1/facturacion/cuentas
/api/v1/facturacion/pagos
/api/v1/facturacion/cajas
/api/v1/facturacion/comprobantes
/api/v1/facturacion/facturas
/api/v1/facturacion/notas-credito
/api/v1/facturacion/notas-debito
/api/v1/facturacion/retenciones
/api/v1/facturacion/reportes
/api/v1/facturacion/sri
```

## 3.16 Permisos

> Todos venían sembrados en `V27`; desde la Fase F se usan
> `FACTURACION_READ/WRITE`, `CAJA_READ/WRITE/CERRAR` y `REPORTES_FINANCIEROS`
> (`FACTURACION_EMITIR/ANULAR` y `CONFIGURACION_FACTURACION` quedan para las
> fases G y H).

`FACTURACION_READ`, `FACTURACION_WRITE`, `FACTURACION_EMITIR`,
`FACTURACION_ANULAR`, `CAJA_READ`, `CAJA_WRITE`, `CAJA_CERRAR`,
`REPORTES_FINANCIEROS`, `CONFIGURACION_FACTURACION`.

# 4. MÓDULO DE HISTORIA CLÍNICA ODONTOLÓGICA

## 4.1 Navegación

`Pacientes > Paciente > Historia clínica`

Tabs: - Resumen - Antecedentes - Examen - Odontograma - Periodoncia -
Diagnósticos - Plan de tratamiento - Evoluciones - Recetas -
Imágenes/archivos - Consentimientos - Presupuestos - Facturación -
Auditoría clínica

## 4.2 Resumen clínico

Paciente, edad, odontólogo, última/próxima cita, alergias, alertas,
enfermedades, medicación, diagnósticos activos, tratamientos pendientes
y última evolución.

## 4.3 Antecedentes

Estructurar enfermedades, cirugías, hospitalizaciones, alergias,
medicación, antecedentes odontológicos y hábitos. Evitar guardar todo
como un solo texto libre.

## 4.4 Examen

Secciones general, extraoral e intraoral con ATM, simetría, ganglios,
apertura, mucosas, lengua, paladar, encías, higiene, oclusión y
observaciones.

## 4.5 Odontograma interactivo

-   Dentición permanente, temporal y mixta.
-   Numeración FDI.
-   Diente y superficies.
-   Condiciones configurables.
-   Condición existente, diagnóstico, planificado, proceso, completado y
    referido.
-   Colores + leyenda.
-   Historial por fecha.
-   Comparación temporal.

**No guardar una imagen como fuente de verdad.** Guardar datos
estructurados y generar la representación visual desde ellos.

> **Implementado en la Fase C:** `odontogramas` (versiones),
> `odontograma_dientes` (diente FDI + superficie + condición + estado) y
> `odontograma_eventos`; 8 endpoints en `/api/v1/historia-clinica` con
> `ODONTOGRAMA_READ/WRITE` y pestaña **Odontograma** (SVG, tabla, comparación
> y log). Falta lo puramente visual de esta sección: impresión/PDF del
> odontograma.

## 4.6 Procedimientos

Relacionar paciente, odontólogo, fecha, diente, superficies, servicio,
diagnóstico, prioridad, pronóstico, costo, cita y plan.

Estados: `PROPUESTO`, `ACEPTADO`, `PROGRAMADO`, `EN_PROCESO`,
`COMPLETADO`, `CANCELADO`, `REFERIDO`.

Al completar: actualizar odontograma, evolución y opcionalmente crear
cargo financiero.

## 4.7 Diagnósticos

Catálogo configurable, región/diente, fecha, odontólogo, severidad y
estado: sospecha, confirmado, resuelto o descartado. Conservar
historial.

## 4.8 Plan de tratamiento

Estados: `BORRADOR`, `PRESENTADO`, `ACEPTADO`, `PARCIALMENTE_ACEPTADO`,
`RECHAZADO`, `EN_EJECUCION`, `COMPLETADO`, `CANCELADO`.

Líneas con procedimiento, diente, superficie, diagnóstico, prioridad,
sesiones, precio, descuento y estado.

Permitir fases, alternativas, PDF, aceptación/firma, conversión a citas
y comparación planificado/ejecutado.

## 4.9 Presupuesto

Separado del plan clínico:
`Plan → Presupuesto → Aceptación → Procedimientos → Cuenta → Factura`.

> **Implementado en la Fase D:** `planes_tratamiento`, `plan_lineas`,
> `presupuestos` y `presupuesto_lineas`; 15 endpoints en
> `/api/v1/planes-tratamiento` con `PLAN_TRATAMIENTO_READ/WRITE`, matriz de
> estados validada en backend, líneas con fases/diente/superficie y recálculo de
> totales, presupuestos con **snapshot inmutable** de las líneas, aceptación con
> firma `SHA-256` y marca `vigente`, PDF con OpenPDF y pestaña **Planes** dentro
> de `/pacientes/:id/historia`.

## 4.10 Evoluciones

Fecha, odontólogo, cita, motivo, hallazgos, diagnóstico, procedimiento,
anestesia, materiales, indicaciones y conducta.

Soportar SOAP opcional.

Una nota cerrada/firmada no se modifica libremente: cualquier corrección
posterior será una adenda auditada.

## 4.11 Periodontograma

Por diente/sitio: - profundidad de sondaje; - margen gingival; - nivel
de inserción; - sangrado; - placa; - supuración; - movilidad; -
furcación.

Permitir comparación entre exámenes y reporte periodontal.

## 4.12 Recetas

Medicamento, concentración, presentación, dosis, vía, frecuencia,
duración, instrucciones, profesional, PDF e historial.

## 4.13 Consentimientos

Plantillas para tratamiento general, extracción, endodoncia, implante,
cirugía, ortodoncia, imágenes, etc.

Guardar versión, fecha, paciente/tutor, profesional, firma, hash y PDF
inmutable.

## 4.14 Menores

Reutilizar tutores existentes para consentimientos, autorizaciones,
contacto y facturación al representante cuando corresponda.

## 4.15 Imágenes/archivos

Radiografías, fotografías, panorámicas, tomografías, resultados y
documentos.

Guardar metadata en PostgreSQL y archivos mediante `FileStorageProvider`
(local/S3). Evitar BLOB grandes en la BD.

## 4.16 Timeline

Una línea temporal única para consultas, odontogramas, diagnósticos,
planes, procedimientos, evoluciones, recetas, archivos y controles.

> **Implementado en la Fase E:** `GET /historia-clinica/pacientes/{id}/timeline`
> pasa a `TimelineService` (sin migración: todo se compone de datos ya
> existentes) y devuelve **once tipos** — `CITA`, `EXAMEN`, `DIAGNOSTICO`,
> `EVOLUCION`, `ODONTOGRAMA`, `PERIODONCIA` (los controles), `PLAN`,
> `PROCEDIMIENTO` (sólo líneas `EN_EJECUCION`/`COMPLETADO`), `RECETA`,
> `ARCHIVO` y `CONSENTIMIENTO` —, con 25 eventos por fuente y 100 en total,
> ordenados por fecha descendente; los eventos de paciente (citas, planes,
> recetas, archivos, consentimientos) se muestran aunque la historia clínica
> todavía no exista. El endpoint exige `HISTORIA_CLINICA_READ` y todos los
> roles tienen también el permiso de lectura de cada fuente que aparece.
> En la pestaña **Línea de tiempo** hay chips con el conteo por tipo y el
> badge lleva etiqueta legible (nunca el código suelto).

## 4.17 Reportes clínicos

Historia completa, resumen, odontograma, evolución, tratamientos,
recetas, periodoncia, consentimientos, diagnósticos frecuentes,
tratamientos pendientes, continuidad y producción clínica.

> **Implementado en la Fase E:** dos endpoints bajo `/api/v1/historia-clinica`
> con `REPORTES_CLINICOS` (sembrado en `V27`, sin migración nueva):
> `GET /reportes?desde&hasta&limite` devuelve los totales del período, los
> diagnósticos frecuentes, los tratamientos pendientes, la continuidad de la
> atención y la producción clínica, y
> `GET /pacientes/{id}/reportes/historia-completa.pdf` descarga la historia
> completa en PDF. En la app: la página `/reportes-clinicos` (entrada del menú
> **Gestión**) y el botón **Historia completa (PDF)** de la cabecera de
> `/pacientes/:id/historia`.

## 4.18 Tablas sugeridas

``` text
historias_clinicas
antecedentes_medicos
alergias
medicaciones
habitos
examenes_clinicos
odontogramas
odontograma_eventos
odontograma_dientes
diagnosticos_clinicos
procedimientos_clinicos
planes_tratamiento
plan_tratamiento_detalles
presupuestos
presupuesto_detalles
evoluciones
evolucion_adendas
periodontogramas
periodonto_mediciones
recetas
receta_detalles
consentimientos
consentimiento_firmas
archivos_clinicos
```

## 4.19 API

``` text
/api/v1/historia-clinica/pacientes/{id}
/api/v1/historia-clinica/pacientes/{id}/timeline
/api/v1/historia-clinica/antecedentes
/api/v1/historia-clinica/examenes
/api/v1/historia-clinica/odontogramas
/api/v1/historia-clinica/diagnosticos
/api/v1/historia-clinica/procedimientos
/api/v1/historia-clinica/planes
/api/v1/historia-clinica/evoluciones
/api/v1/historia-clinica/periodoncia
/api/v1/historia-clinica/recetas
/api/v1/historia-clinica/consentimientos
/api/v1/historia-clinica/archivos
/api/v1/historia-clinica/reportes
```

> **Implementado en la Fase B:** `GET /pacientes/{id}`, `GET /pacientes/{id}/timeline`,
> los CRUD de `/antecedentes`, `/examenes`, `/diagnosticos` y `/evoluciones`
> (incluidos `POST /evoluciones/{id}/cerrar` y `POST /evoluciones/{id}/adendas`).
> Odontogramas, procedimientos, planes, periodoncia, recetas, consentimientos,
> archivos y reportes clínicos quedan para las Fases C–E.
>
> **Implementados en la Fase E:** periodontograma (§4.11),
> `/historia-clinica/recetas` (§4.12: historial, detalle, alta, edición,
> anulación y PDF) y `/historia-clinica/consentimientos` (§4.13: plantillas,
> historial, detalle, emisión en borrador, edición, firma y PDF).

## 4.20 Permisos

`HISTORIA_CLINICA_READ/WRITE/CLOSE`, `ODONTOGRAMA_READ/WRITE`,
`PERIODONCIA_READ/WRITE`, `PLAN_TRATAMIENTO_READ/WRITE`,
`RECETAS_READ/WRITE`, `CONSENTIMIENTOS_READ/WRITE`,
`ARCHIVOS_CLINICOS_READ/WRITE`, `REPORTES_CLINICOS`.

# 5. Integración

## Procedimiento → cuenta

`Procedimiento COMPLETADO → ¿facturable? → cargo en CuentaPaciente`

> **Implementado en la Fase F:** `PlanTratamientoService.cambiarEstadoLinea`
> llama a `CuentaPacienteService.integrarCuenta()` — al completar crea el
> cargo (`origen PROCEDIMIENTO`, `referenciaId` = id de la línea, idempotente
> y con reactivación si el cargo estaba anulado) y al revertir lo anula.
> Nunca bloquea el flujo clínico: una cuenta `CERRADA` recibe el cargo y una
> `ANULADA` no recibe ninguno.

## Cita → clínica

Al marcar `ATENDIDA`, ofrecer abrir evolución y procedimientos
planificados.

## Facturación

Relacionar factura con paciente, cuenta y pagos. No incluir datos
clínicos sensibles innecesarios en comprobantes.

# 6. Menú futuro

``` text
Dashboard
Agenda
Pacientes
Clínica
├─ Odontograma
├─ Planes de tratamiento
├─ Evoluciones
├─ Periodoncia
├─ Recetas
└─ Consentimientos
Facturación
├─ Dashboard
├─ Cuentas
├─ Caja
├─ Comprobantes
├─ Facturas
├─ Notas de crédito
├─ Retenciones
└─ Reportes
Comunicación
Reportes
Administración
```

# 7. Roadmap

## Fase A --- Fundamentos

Permisos, menú dinámico, archivos, PDF, firma/hash, auditoría y tests.

> **Estado: completada (2026-10-01).** Migración `V27` (25 permisos nuevos →
> 49), `permisos[]` en `GET /auth/login`, menú dinámico en el sidebar
> (`AuthService.puede()`), `FileStorageProvider`/`LocalFileStorageProvider`
> con volumen `storage_data`, `HashUtil.sha256Hex()`, y gates en verde:
> backend **201/201** · unit **34/34** · lint ✔ · e2e **36/36** · build prod ✔.
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.5.

## Fase B --- Historia clínica base

Antecedentes, alergias, medicación, examen, diagnósticos, evoluciones y
timeline.

> **Estado: completada (2026-10-01).** Migración `V28__historia_clinica_base.sql`
> (6 tablas), `HistoriaClinicaService` + `HistoriaClinicaController` (17 endpoints
> bajo `/api/v1/historia-clinica` con `HISTORIA_CLINICA_READ/WRITE/CLOSE`, auditoría
> y multi-tenant), módulo Angular `/pacientes/:id/historia` (6 pestañas + botón
> "Historia" en el listado de pacientes), reglas de edición/cierre/adenda con 409
> y gates en verde: backend **217/217** · unit **42/42** · lint ✔ · e2e **36/36** ·
> build prod ✔. Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.6.

## Fase C --- Odontograma

FDI, SVG interactivo, superficies, condiciones, versionado, comparación
e impresión.

> **Estado: completada (2026-10-02).** Migración `V29__odontograma.sql`
> (3 tablas: `odontogramas` versionados, `odontograma_dientes` por
> diente/superficie, `odontograma_eventos`), `OdontogramaService` +
> `OdontogramaController` (8 endpoints bajo `/api/v1/historia-clinica` con
> `ODONTOGRAMA_READ/WRITE`, validación FDI, copia de versión y auditoría),
> pestaña **Odontograma** de `/pacientes/:id/historia` (SVG con 5 regiones por
> pieza, tabla editable, comparación y log de cambios) y gates en verde:
> backend **232/232** · unit **57/57** · lint ✔ · e2e **38/38** · build prod ✔.
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.7.

## Fase D --- Planes

Planes, fases, prioridades, aceptación, presupuesto, PDF e integración
con citas.

> **Estado: completada (2026-10-03).** Migración `V30__planes_tratamiento.sql`
> (4 tablas), `PlanTratamientoService` + `PlanTratamientoController`
> (15 endpoints bajo `/api/v1/planes-tratamiento` con
> `PLAN_TRATAMIENTO_READ/WRITE`: CRUD de planes, líneas, estados, presupuestos
> y PDF), `PlanPdfService` (OpenPDF con cabecera del consultorio), pestaña
> **Planes** de `/pacientes/:id/historia` (lista, detalle, líneas, presupuestos
> con snapshot y aceptación firmada) y gates en verde: backend **251/251** ·
> unit **68/68** · lint ✔ · e2e **41/41** · build prod ✔.
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.8.

## Fase E --- Clínica avanzada

Periodoncia, recetas, consentimientos, imágenes y reportes.

> **§4.11 Periodontograma: completada (2026-10-05).** Migraciones
> `V31__periodontograma.sql` (3 tablas: `examenes_periodontales` versionados,
> `periodontograma_sitios` con UNIQUE `examen + diente + sitio` y rangos,
> `periodontograma_dientes` con movilidad/furcación 0–3) y
> `V32__periodoncia_recepcion.sql` (`PERIODONCIA_READ` para `RECEPCION`),
> `PeriodontogramaService` + `PeriodontogramaController` (9 endpoints bajo
> `/api/v1/historia-clinica` con `PERIODONCIA_READ/WRITE`: vigente, historial,
> detalle, comparación de versiones, nueva versión con copia, edición de
> cabecera, mediciones upsert por diente/sitio, eliminar pieza y PDF),
> `PeriodontogramaPdfService` (OpenPDF: matriz diente × sitio + resumen
> clínico), pestaña **Periodoncia** de `/pacientes/:id/historia` (9.ª pestaña:
> historial, resumen, matriz con alertas, modal de mediciones, comparación y
> descarga) y gates en verde: backend **270/270** · unit **80/80** · lint ✔ ·
> e2e **43/43** · build prod ✔.
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.9.
>
> **§4.12 Recetas: completada (2026-10-05).** Migración
> `V33__recetas.sql` (tablas `recetas` con estado `VIGENTE/ANULADA` y motivo
> de anulación, y `receta_lineas` con UNIQUE `receta + número`; además
> `RECETAS_READ` concedido a `RECEPCION`, 24 permisos en total), dominio
> `Receta`/`RecetaLinea` con `ViaAdministracion`, `RecetaService` +
> `RecetaController` (6 endpoints bajo `/api/v1/historia-clinica` con
> `RECETAS_READ/WRITE`: historial, detalle, alta con pauta completa,
> edición que reemplaza las líneas, anulación motivada y PDF),
> `RecetaPdfService` (OpenPDF: cabecera del consultorio, tabla de
> medicamentos, instrucciones por línea y sello de anulación), pestaña
> **Recetas** de `/pacientes/:id/historia` (10.ª pestaña: historial,
> detalle con la pauta, alta/edición con editor de líneas, anulación con
> motivo y descarga) y gates en verde: backend **286/286** · unit **91/91** ·
> lint ✔ · e2e **45/45** · build prod ✔.
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.10.
>
> **§4.13 Consentimientos: completada (2026-10-05).** Migración
> `V34__consentimientos.sql` (tablas `consentimiento_plantillas` con
> `UNIQUE (tenant_id, codigo)` y `consentimientos` con snapshot del texto,
> `plantilla_codigo/version`, estado `BORRADOR/FIRMADO`, firma manuscrita,
> `firma_hash` SHA-256 y `firmado_en`; 8 plantillas sembradas por tenant;
> además `CONSENTIMIENTOS_READ` concedido a `RECEPCION`, 25 permisos),
> dominio `Consentimiento`/`ConsentimientoPlantilla`, `ConsentimientoService`
> + `ConsentimientoController` (9 endpoints bajo
> `/api/v1/historia-clinica` con `CONSENTIMIENTOS_READ/WRITE`: catálogo y
> edición de plantillas con `version+1`, historial, detalle, emisión en
> borrador con copia de la plantilla, edición/borrado sólo en borrador,
> firma con huella SHA-256 y PDF), `ConsentimientoPdfService` (OpenPDF:
> texto congelado, bloque de firma con imagen opcional y marca `BORRADOR`
> cuando no está firmado), pestaña **Consentimientos** de
> `/pacientes/:id/historia` (11.ª pestaña: historial, lectura del texto,
> alta/edición, modal de firma con lienzo, catálogo de plantillas y
> descarga) y gates en verde: backend **305/305** · unit **103/103** ·
> lint ✔ · e2e **47/47** · build prod ✔.
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.11.
>
> **§4.14 Menores: completada (2026-10-05).** Migración
> `V35__tutores_consentimientos.sql` (columnas `tutor_id`,
> `tutor_parentesco` y `tutor_telefono` en `consentimientos`, como
> referencia débil —sin FK— porque los tutores se reescriben al editar el
> paciente, más `idx_consentimientos_tutor`), reglas en
> `ConsentimientoService`: emitir de un menor sin tutor →
> `400 TUTOR_REQUERIDO`, tutor que no pertenece al paciente →
> `404 TUTOR_NO_ENCONTRADO` y firma de un menor con un firmante distinto
> del tutor → `400 FIRMANTE_DEBE_SER_TUTOR` (comparación insensible a
> mayúsculas y a espacios, guardando luego el nombre canónico del tutor);
> snapshot de tutor (`id`, `nombre`, `parentesco`, `teléfono`) en DTOs,
> PDF (línea `Tutor / representante:`) y respuestas; formulario con
> selector `#con-tutor-sel` de los tutores registrados en el paciente
> (con texto libre de respaldo), aviso en la pestaña **Consentimientos**
> cuando el paciente es menor y validación en cliente antes de firmar, y
> gates en verde: backend **312/312** · unit **108/108** · lint ✔ ·
> e2e **48/48** · build prod ✔ · Flyway V35 aplicada en el contenedor.
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.12.
>
> **§4.15 Imágenes/archivos: completada (2026-10-05).** Migración
> `V36__archivos_clinicos.sql` (tabla `archivos_clinicos` con categoría
> `RADIOGRAFIA/FOTOGRAFIA/PANORAMICA/TOMOGRAFIA/RESULTADO/DOCUMENTO`,
> nombre original, clave de almacenamiento, `mime`, `tamano`, `sha256`,
> fecha del estudio y observaciones; **sólo metadata en PostgreSQL**).
> `ArchivoClinicoService` + `ArchivoClinicoController` (4 endpoints bajo
> `/api/v1/historia-clinica` con los permisos `ARCHIVOS_CLINICOS_READ/WRITE`
> ya sembrados en `V27`): alta multipart con validación de categoría,
> extensión (JPG/PNG/WEBP/TIFF/PDF/DCM) y 20 MB, listado, descarga del
> contenido (`Content-Disposition: inline` + `mime` real) y borrado que
> elimina también el archivo físico; los bytes viven en
> `FileStorageProvider` (primera implementación real: `LocalFileStorageProvider`
> con la clave `tenant-*/paciente-*/archivos/uuid-nombre`, volumen
> `storage_data`), el límite de multipart sube a 25 MB y
> `MaxUploadSizeExceededException` responde `400 ARCHIVO_DEMASIADO_GRANDE`;
> pestaña **Archivos** de `/pacientes/:id/historia` (12.ª pestaña: alta con
> selector de categoría, validación en cliente, tabla con tamaño y
> acciones, vista previa de imágenes en diálogo y descarga con el nombre
> original), gates en verde: backend **326/326** · unit **120/120** ·
> lint ✔ · e2e **50/50** · build prod ✔ · Flyway V36 aplicada en el contenedor.
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.13.
>
> **§4.16 Timeline ampliada: completada (2026-10-06).** Nuevo
> `TimelineService` (12 repositorios como fuentes, sin migración) que
> compone once tipos de evento — consultas atendidas, exámenes,
> diagnósticos, evoluciones, odontogramas versionados, controles
> periodontales, planes con su estado y totales, procedimientos en ejecución
> o completados, recetas con el nº de medicamentos, archivos con categoría y
> tamaño, y consentimientos con su firmante — limitando a 25 por fuente y
> 100 en total, ordenados por fecha descendente; los eventos de paciente se
> devuelven aunque todavía no exista historia clínica y el `404` sólo llega
> cuando el paciente no existe. La pestaña **Línea de tiempo** gana chips de
> filtro con conteo por tipo, badges con etiqueta legible (`TipoTimeline`,
> `ETIQUETAS_TIMELINE`/`TONES_TIMELINE`) y color por punto, y gates en
> verde: backend **333/333** · unit **124/124** · lint ✔ · e2e **52/52** ·
> build prod ✔ · smoke `/tmp/opencode/smoke_timeline.sh`.
>
> **§4.17 Reportes clínicos: completada (2026-10-06).** `ReporteClinicoService`
> + `ReporteClinicoController` (`GET /historia-clinica/reportes` con rango por
> defecto de un año, `limite` acotado a 1–50 y `400 RANGO_INVALIDO` cuando el
> rango se invierte) y `HistoriaCompletaService` + `HistoriaCompletaPdfService`
> (PDF OpenPDF con cabecera del consultorio y las 11 secciones de la historia;
> el paciente inexistente responde `404 NO_ENCONTRADO`). Todo con el permiso
> `REPORTES_CLINICOS` de `V27` (SUPER_ADMIN/ADMIN/ODONTOLOGO; la recepción
> recibe `403`), sin migración: sólo consultas sobre tablas existentes.
> Página **`/reportes-clinicos`** con seis tarjetas de totales, diagnósticos
> frecuentes, producción clínica, tratamientos pendientes (con badge de
> prioridad), continuidad de la atención y la lista de pacientes sin actividad
> en 90 días, más el botón **Historia completa (PDF)** en la ficha del
> paciente; gates en verde: backend **351/351** · unit **130/130** · lint ✔ ·
> e2e **55/55** · build prod ✔ · smoke `/tmp/opencode/smoke_reportes.sh`
> (7 pasos).
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.15.

## Fase F --- Finanzas internas

Cuenta, cargos, descuentos, anticipos, pagos, caja y reportes. Debe
funcionar sin SRI.

> **§3.3/§3.4/§3.12/§3.13 completadas (2026-10-07), sin SRI.** Migraciones
> `V37__cuenta_paciente.sql` (`cuentas_paciente` + `cuenta_detalles` como
> libro mayor con índice parcial de idempotencia) y `V38__caja.sql` (`cajas`
> con una sesión abierta por tenant + `movimientos_caja`), 4 entidades de
> dominio con sus repositorios, `CuentaPacienteService`, `CajaService` y
> `ReporteFinancieroService` con **18 endpoints** bajo `/api/v1/facturacion`
> (cuentas y movimientos, pagos, caja con conciliación, dashboard y los
> cuatro reportes), integración §5 en `PlanTratamientoService` (el
> `COMPLETADO` genera el cargo y la reversión lo anula, idempotente y sin
> bloquear lo clínico) y cobros que se reflejan solos en caja. Sin permisos
> nuevos: se reutilizan `FACTURACION_READ/WRITE`, `CAJA_READ/WRITE/CERRAR` y
> `REPORTES_FINANCIEROS` de `V27`. En la app: sección de menú **Finanzas**
> con `/facturacion` (dashboard), `/facturacion/cuentas`,
> `/facturacion/caja` y `/facturacion/reportes`. Gates en verde: backend
> **386/386** · unit **155/155** · lint ✔ · e2e **61/61** (27 rutas en
> `qa`/`axe`/`foco`/`hover`) · build prod **717.63 kB / 146.90 kB gzip** ·
> imágenes Docker reconstruidas · smoke `/tmp/opencode/smoke_finanzas.sh`
> (16 pasos).
> Detalle: [`ESTADO_DEL_SISTEMA.md`](ESTADO_DEL_SISTEMA.md) §2.16.

## Fase G --- SRI

Configuración, secuenciales, XML, XSD, firma, pruebas, autorización,
persistencia, RIDE y envío.

## Fase H --- Complementarios

Notas de crédito/débito, retenciones, anulación y documentos opcionales.

## Fase I --- Cierre

Dashboards, exportaciones, auditoría cruzada, QA, E2E, performance,
backup/restore y smoke productivo.

# 8. Migraciones sugeridas

``` text
V27 permisos
V28 historia base
V29 odontograma
V30 planes
V31 periodoncia (exámenes, mediciones, PDF)
V32 PERIODONCIA_READ para recepción
V33 recetas (+ RECETAS_READ para recepción) [hecha]
V34 consentimientos (+ CONSENTIMIENTOS_READ para recepción) [hecha]
V35 tutores de consentimientos (§4.14) [hecha]
V36 archivos clínicos (+ ARCHIVOS_CLINICOS_READ/WRITE ya en V27) [hecha]
V37 cuentas/pagos
V38 caja
V39 configuración tributaria
V40 comprobantes
V41 notas/retenciones [notas de crédito y anulación hechas en V43]
V42 índices/reportes
```

Ajustar números a las migraciones existentes al iniciar desarrollo.

# 9. QA obligatorio

Backend: tenant, permisos, versionado, cierre de evolución, planes,
pagos, caja, concurrencia de secuenciales, XML, firma, SRI, reintentos y
auditoría.

E2E clínico:
`Paciente → Cita → Atención → Odontograma → Diagnóstico → Plan → Procedimiento → Evolución`.

E2E financiero:
`Procedimiento → Cargo → Pago → Factura → Firma → SRI pruebas → Autorización → RIDE`.

# 10. Decisiones técnicas

-   Odontograma: componente Angular SVG responsive.
-   Archivos: `FileStorageProvider` con local y S3.
-   SRI: `TaxAuthorityGateway` / `SriGateway`.
-   Firma: `ElectronicSignatureProvider`.
-   No mezclar lógica clínica con lógica tributaria.
-   No borrar registros clínicos firmados ni comprobantes emitidos.
-   Todos los nuevos dominios deben conservar `tenant_id` y auditoría.

# 11. Prioridad

**P0:** producción/backups/permisos.\
**P1:** historia + odontograma + evolución + planes.\
**P2:** cuenta + pagos + caja.\
**P3:** facturación electrónica SRI.\
**P4:** periodoncia + recetas + consentimientos + archivos.\
**P5:** analítica y exportaciones.

# 12. Resultado

CRM Dental AT (ALANTEK) evolucionará a:

`CRM + Agenda + Historia clínica + Odontograma + Planes + Finanzas + Facturación electrónica + WhatsApp + IA + Reportes`

manteniendo tres dominios separados pero integrados: **clínico**,
**financiero/tributario** y **CRM/operativo**.
