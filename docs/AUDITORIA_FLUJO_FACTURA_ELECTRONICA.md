# Auditoría del flujo de facturación electrónica

Estado: auditoría del código terminada; primera corrección de seguridad planificada. No se han
realizado llamadas reales al SRI, SMTP ni WhatsApp durante esta revisión.

> **Resolución (2026-10-15, §2.22 de `ESTADO_DEL_SISTEMA.md`):** la recomendación de esta
> auditoría de *dejar la simulación en `PENDIENTE` sin autorizar* **no se adoptó**. Por decisión
> de diseño explícita, el modo simulado es ahora un «SRI local» que **autoriza y anula
> localmente** (número de autorización = clave de acceso, XML autorizado = XML firmado, trámite
> `APLICADA`), para que RIDE, notas de crédito y anulación funcionen end-to-end en pruebas. El
> aviso único de que nada de eso vale tributariamente es el flag `simulado` de la respuesta (y su
> badge en la UI). Los puntos que esta auditoría marcaba como resueltos por la corrección antigua
> quedan así: RIDE exige `AUTORIZADO` sí, la anulación real sigue bloqueada (`501`), y
> reenviar/consultar al SRI siguen prohibidos para simulados. Las demás deudas del documento
> (P12, idempotencia outbox, entrega SMTP/WhatsApp) siguen abiertas.

## Diagnóstico

Existe un flujo de cuenta → comprobante → XML/XSD → firma → SOAP de recepción/autorización y RIDE.
La base distingue estado de generación y tributario, y guarda `simulado`. La implementación tiene
un modo local cuando falta el certificado, pero hoy transforma esa simulación en una autorización
con número/fecha/XML autorizado. No hay entrega de factura a correo o WhatsApp ni estados de entrega.
Por lo tanto, el sistema sirve para validar generación/XML y algunas llamadas SRI con P12, pero no
está listo para asegurar un ciclo tributario y entrega extremo a extremo de forma fiable.

| Etapa | Implementación inspeccionada | Estado y riesgo |
|---|---|---|
| Cuenta y cobro | `CuentaPaciente`, `CuentaDetalle`, `CuentaPacienteService`; cargos y pagos quedan en el libro financiero | Operativo, pero no hay libro aislado de pruebas. Las pruebas que agregan cargos contaminan saldos/reportes oficiales |
| Emisión | `POST /api/v1/facturacion/comprobantes`; toma bloqueo de cuenta, valida cuenta, incrementa secuencial bloqueado y guarda comprobante | Idempotencia por cuenta y secuencial existente; reintento tras timeout puede volver a enviar una clave cuya recepción remota sea desconocida |
| Datos e impuestos | `FacturaBuilder` forma líneas desde `CuentaDetalle`; `Servicio` no tiene tarifa/código tributario | La tarifa IVA 15% y el supuesto “precio ya incluye IVA” están codificados; no se obtiene clasificación fiscal del concepto. No cambiar/importar una tarifa sin política aprobada |
| Numeración | `ConfiguracionFacturacion`, `buscarParaSecuencial`, clave de acceso y secuencial por tipo | PRUEBAS/PRODUCCION son configurables en backend; no existe pantalla Angular de configuración SRI |
| P12 | Carga multipart en `/configuracion/consultorio/firma-digital`; `PrivateFileCipher` cifra AES-GCM; la contraseña se cifra en `ConfiguracionFacturacion` | La carga valida extensión, tamaño y primer byte ASN.1, no contraseña/llave/certificado/validez. La contraseña se descubre al firmar. No existe UI de password/estado de vencimiento |
| XML/XSD | `SriXmlGenerator` v1.1.0; `SriXmlValidator` usa XSD en `resources/sri` | XSD local y parseo protegido. Sin clasificación fiscal por línea; el verificador XSD no verifica criptográficamente la firma |
| Firma | `ElectronicSignatureService`: XMLDSig enveloped, RSA-SHA1/SHA1 con `KeyInfo` X509; sin P12, deja XML sin firma | No se verifican vigencia/identidad/uso del certificado ni se prueba el perfil de firma contra SRI real. Requiere validación contra la ficha técnica vigente antes de usar en producción |
| Recepción SRI | `SriHttpGateway` implementa SOAP HTTP a URLs PRUEBAS/PRODUCCION, timeout 20 s | Interfaz y parser disponibles; pruebas automatizadas usan gateway simulado. No se verificó conectividad/aceptación real |
| Autorización SRI | `AuthorizationService.autorizar` procesa AUT/NAT/PPR | PPR conserva PENDIENTE. Modo simulado guarda `AUTORIZADO` con autorización y XML autorizado sin respuesta remota: era «hallazgo crítico» de esta auditoría; hoy es **decisión de diseño** (SRI local, §2.22) con `simulado` como única salvedad |
| Reintentos | Bandeja permite `reenviar` o `consultar`; se bloquea reenvío si figura ENVIADO o definitivo | El timeout tras recepción remota puede quedar ERROR y permitir reenvío sin consulta previa; requiere estado de intento y estrategia de recuperación |
| PDF/RIDE | `RideGenerator` obtiene XML autorizado si existe; si no, usa XML firmado | `ride()` exige `AUTORIZADO` + XML (§2.22): un pendiente responde `409 RIDE_NO_AUTORIZADO`; los simulados autorizados sí generan PDF |
| Anulación | `ComprobanteElectronicoService.aplicarAnulacion` | En simulado aplica localmente (`ANULADO` + trámite `APLICADA`, §2.22); los comprobantes reales quedan bloqueados con `501 SRI_ANULACION_NO_DISPONIBLE` hasta implementar `anular` en `SriGateway` |
| Email | `CorreoService.enviar` usa `SimpleMailMessage` | SMTP configurable y secreto cifrado, pero solo texto; no adjunta PDF/XML y no está conectado a emisión |
| WhatsApp | `OpenWAProvider` envía documentos a través de sesión `@c.us` | Es el gateway OpenWA/Baileys existente; no es WhatsApp Business Platform. Por la restricción solicitada no se usará para facturas |
| Entrega/auditoría | Auditoría guarda acciones tributarias; `ComprobanteElectronico` no tiene estado/fecha/destino de entrega | No existe estado de correo/WhatsApp, idempotencia de entrega ni enlace seguro. La factura y cada canal requieren estados separados |

## Rutas y control de acceso

| Acción | Ruta | Permiso |
|---|---|---|
| Emitir factura desde cuenta | `POST /facturacion/comprobantes` | `FACTURACION_EMITIR` |
| Consultar bandeja/detalle/XML/RIDE | `GET /facturacion/comprobantes/**` | `FACTURACION_READ` |
| Reenviar o consultar SRI | `POST /facturacion/comprobantes/{id}/{reenviar,consultar}` | `FACTURACION_EMITIR` |
| Administrar ambiente/secundarios/password P12 | `/facturacion/configuracion/**` | `CONFIGURACION_FACTURACION` |
| Subir/eliminar P12 | `/configuracion/consultorio/firma-digital` | `CONFIGURACION` |
| SMTP y OpenWA existentes | `/configuracion/integraciones/**` | `CONFIGURACION` |

El backend valida permisos y las entidades tienen `@TenantId`. XML/RIDE usan descargas autenticadas;
no existe enlace público. La UI carga P12 desde Datos del consultorio; no existe módulo de
configuración tributaria en Angular.

## Pruebas existentes y límites

- `ElectronicSignatureServiceTest` usa `backend/src/test/resources/firma-test.p12`, un artefacto de
  pruebas con contraseña de fixture en el test. No es la firma de la clínica; nunca se debe copiar
  una firma real o contraseña real a este directorio.
- `AuthorizationServiceTest` afirma que el modo simulado queda `AUTORIZADO`: esta expectativa es
  hoy la **especificación vigente** (SRI local, §2.22), no una deuda. Los tests SRI son mocks; no
  equivalen a autorización real.
- `ComprobanteElectronicoServiceTest` valida secuencial, XML/XSD, notas y estados usando firma/SRI
  simulados.
- No se encontró test de envío de factura SMTP ni proveedor oficial WhatsApp Business.
- No se dispone en esta sesión de P12 autorizado, su contraseña, RUC/ambiente de pruebas aprobado,
  destinatario de prueba ni credenciales SMTP/WhatsApp Business. Ninguno debe pegarse al chat.

## Primera corrección segura

> **Superado (2026-10-15):** este plan de dejar la simulación en `GENERADO`/`PENDIENTE` **no se
> aplicó** por decisión de diseño contraria (§2.22): el SRI local autoriza y anula localmente.
> Sí se mantienen del plan: no consultar/reenviar al SRI desde un comprobante simulado y exigir
> `AUTORIZADO` para el RIDE. El resto del documento conserva su análisis original.

Archivos previstos: `AuthorizationService`, `ComprobanteElectronicoService`, `RideGenerator` o su
fachada, componentes de cuenta/comprobantes y pruebas unitarias/E2E correspondientes. La simulación
quedará como `GENERADO` + estado tributario `PENDIENTE` + `simulado=true`, sin número/fecha/XML
autorizado; no se consultará ni reenviará SRI desde un comprobante local simulado; RIDE solo estará
disponible tras autorización real. La anulación local se bloqueará hasta tener respuesta remota.
Esto cabe en estados/columnas de V41 y no requiere migración.

Después se requiere trabajo separado para validar PKCS#12 al guardar contraseña, establecer reglas
fiscales por concepto, idempotencia/outbox de SRI, entrega SMTP con adjuntos y proveedor oficial de
WhatsApp Business. Los endpoints de email/WhatsApp no se deben habilitar hasta disponer de
configuración/canales aprobados y estados persistentes.