# Little Smile --- Instrucciones de mejora del agente WhatsApp

## Objetivo

Corregir la pérdida de contexto y convertir el bot actual en un
asistente conversacional que interprete números, texto parcial y
lenguaje natural sin reiniciar el flujo.

## 1. Errores actuales

### Menú

El bot muestra `4. Consultar disponibilidad`, pero al recibir `4`
ejecuta nuevamente el clasificador general. Debe resolver el número
contra el menú activo:

`MENU_PRINCIPAL + 4 -> CONSULTAR_DISPONIBILIDAD`.

### Selección de odontólogo

Si el paso activo es `SELECCIONAR_ODONTOLOGO` y el usuario responde `2`,
debe guardar el odontólogo asociado a la opción 2 y continuar con la
fecha. Nunca debe volver a identificación.

## 2. Regla principal

Antes de `clasificarIntencion(mensaje)`:

``` java
if (contexto.tieneFlujoActivo()) {
    return flowEngine.procesar(contexto, mensaje);
}
return intentClassifier.clasificar(mensaje);
```

Prioridad: 1. comandos globales; 2. flujo activo; 3. menú/opciones; 4.
sí/no; 5. dato esperado; 6. intención general; 7. fallback.

## 3. Contexto JSONB

``` json
{
  "flujo": "CONSULTAR_DISPONIBILIDAD",
  "paso": "SELECCIONAR_ODONTOLOGO",
  "pacienteId": null,
  "odontologoId": null,
  "servicioId": null,
  "fecha": null,
  "franja": null,
  "slotId": null,
  "menuActual": "ODONTOLOGOS",
  "opciones": [],
  "requiereIdentificacion": false,
  "ultimaPregunta": "¿Con qué odontólogo te gustaría revisar?",
  "intentosFallidos": 0
}
```

## 4. Opciones contextuales

Cada menú enviado debe persistir su mapa:

``` json
{
  "menuActual": "ODONTOLOGOS",
  "opciones": [
    {"numero":1,"id":15,"tipo":"ODONTOLOGO","label":"Dra. Ana Paz"},
    {"numero":2,"id":21,"tipo":"ODONTOLOGO","label":"Dr. Anderson Moreno"}
  ]
}
```

`2` se resuelve contra `contexto.opciones`; no se envía a IA como
mensaje aislado.

## 5. Menú principal

``` text
¡Hola! 👋 Soy el asistente virtual de Little Smile 🦷.
¿En qué puedo ayudarte?

1. 📅 Agendar una cita
2. 🦷 Ver servicios
3. 📋 Ver mis citas
4. 🕐 Consultar disponibilidad
5. ✅ Confirmar o cancelar una cita
6. 👤 Ver mi información
7. 💬 Hablar con recepción

Puedes responder con el número o escribir lo que necesitas.
```

Mapa: `1 AGENDAR_CITA`, `2 VER_SERVICIOS`, `3 VER_CITAS`,
`4 CONSULTAR_DISPONIBILIDAD`, `5 GESTIONAR_CITA`, `6 VER_PERFIL`,
`7 TRANSFERIR_HUMANO`.

## 6. Lenguaje natural

Deben ser equivalentes: - `4` - `consultar disponibilidad` -
`consultar disponi` - `disponibilidad` - `qué horarios tienen` -
`hay espacio mañana` - `tienen citas el lunes`

Normalizar lowercase, trim, espacios y tildes para matching auxiliar.
Fuzzy matching puede apoyar, no decidir operaciones.

## 7. Identificación

No pedir cédula para información pública: - servicios; - precios
públicos; - odontólogos; - disponibilidad.

Sí identificar para: - reservar; - ver citas; -
confirmar/cancelar/reprogramar; - perfil.

Primero buscar por teléfono de WhatsApp. Si hay un adulto:

``` text
¡Hola, Alexis! Ya encontré tu información.
¿La cita es para ti?
1. Sí
2. Para otra persona
```

Si es teléfono de un tutor:

``` text
Encontré estos pacientes asociados:
1. Alexis Ortiz
2. Dulce Ortiz Guerrero
3. Otro paciente
¿Para quién será?
```

Si debe pedir cédula en mitad del agendamiento, conservar servicio,
odontólogo, fecha y slot. Tras identificar, continuar el flujo; no
volver al menú.

## 8. Flujo de disponibilidad

``` text
CONSULTAR_DISPONIBILIDAD
→ SELECCIONAR_SERVICIO
→ SELECCIONAR_ODONTOLOGO
→ SELECCIONAR_FECHA
→ SELECCIONAR_FRANJA
→ CONSULTAR_SLOTS
→ MOSTRAR_DISPONIBILIDAD
```

### Servicio

``` text
¿Qué atención necesitas?
1. Valoración
2. Limpieza
3. Endodoncia
4. Otro
5. No estoy seguro
```

### Odontólogo

Siempre ofrecer:

``` text
1. Cualquier odontólogo disponible
2. Dra. Ana Paz — Odontología General
3. Dr. Anderson Moreno — Endodoncia
```

### Fecha

Aceptar `hoy`, `mañana`, `lunes`, `este viernes`, `20 de octubre`.

### Franja

Antes de listar muchas horas:

``` text
1. 🌅 Mañana
2. ☀️ Mediodía
3. 🌤️ Tarde
4. Ver todos
```

### Slots

Mostrar únicamente slots reales del backend, nunca una lista teórica de
horas.

## 9. Conversación corregida

``` text
USUARIO: Hola

BOT:
¡Hola! 👋 Soy el asistente virtual de Little Smile 🦷.
1. Agendar una cita
2. Ver servicios
3. Ver mis citas
4. Consultar disponibilidad
5. Confirmar/cancelar
6. Ver mi información
7. Hablar con recepción

USUARIO: 4

BOT:
Claro 😊 ¿Qué atención necesitas?
1. Valoración
2. Limpieza
3. Endodoncia
4. Otro
5. No estoy seguro

USUARIO: Endodoncia

BOT:
¿Tienes preferencia?
1. Cualquier odontólogo disponible
2. Dr. Anderson Moreno — Endodoncia

USUARIO: 2

BOT:
Perfecto, revisaremos con el Dr. Anderson Moreno.
¿Para qué día quieres consultar?

USUARIO: lunes

BOT:
¿Qué horario prefieres?
1. Mañana
2. Mediodía
3. Tarde
4. Ver todos

USUARIO: mañana

BOT:
Tengo disponibles:
1. 08:30
2. 09:30
3. 11:00
¿Quieres reservar alguno?

USUARIO: 9:30

BOT:
¡Excelente! Las 09:30 están disponibles.
Antes de reservar necesito identificar al paciente.
Envíame su cédula o escribe "soy nuevo".

USUARIO: 0400000000

BOT:
Encontré a Alexis Ortiz.
Confirma:
🦷 Endodoncia
👨‍⚕️ Dr. Anderson Moreno
📅 Lunes
🕤 09:30

1. Confirmar
2. Cambiar horario
3. Cambiar día
4. Cancelar proceso

USUARIO: 1

BOT:
¡Listo! 🎉 Tu cita quedó agendada.
```

## 10. Agendamiento libre

Si llega:
`Quiero una limpieza para Dulce el lunes por la mañana con Ana`

extraer de una vez:

``` json
{
 "intencion":"AGENDAR_CITA",
 "paciente":"Dulce",
 "servicio":"LIMPIEZA",
 "fecha":"fecha_resuelta",
 "franja":"MANANA",
 "odontologo":"Ana"
}
```

Preguntar sólo datos faltantes.

## 11. Confirmaciones

Aceptar `sí`, `si`, `s`, `ok`, `dale`, `claro`, `correcto`, `confirmo`,
`de acuerdo`. Los números sólo tienen significado según el menú activo.

## 12. Correcciones

Aceptar: - `mejor mañana`; - `cambia al doctor`; -
`prefiero por la tarde`; - `no, para Dulce`; -
`quiero limpieza, no valoración`.

Modificar sólo el dato afectado y limpiar dependencias. Ejemplo: cambiar
fecha invalida el slot, pero conserva paciente, servicio y odontólogo.

## 13. Comandos globales

Reconocer siempre: `menu`, `inicio`, `cancelar`, `salir`, `reiniciar`,
`ayuda`, `humano`, `asesor`, `recepción`.

`humano` transfiere y conserva contexto para recepción.

## 14. Fallback contextual

No repetir siempre "No logré entenderte".

Si espera odontólogo, repetir odontólogos. Si espera fecha, mostrar
ejemplos de fecha. Si espera hora, indicar que debe responder una opción
disponible.

Tras tres errores, ofrecer recepción.

## 15. Reanudación

Guardar `updatedAt` y `expiresAt`. Tras una pausa:

``` text
Hola nuevamente 👋. Estábamos revisando disponibilidad para una endodoncia con el Dr. Anderson.
1. Continuar
2. Empezar de nuevo
```

## 16. Responsabilidad de IA vs backend

IA: - intención; - entidades; - lenguaje natural; - correcciones; -
fechas.

Backend: - existencia del paciente; - disponibilidad; - horarios del
odontólogo; - precios; - crear/reprogramar/cancelar cita; - confirmación
de operaciones.

Nunca permitir que la IA invente estos datos.

## 17. Arquitectura

``` text
WhatsApp
→ MessageController
→ ConversationEngine
   ├─ ContextManager
   ├─ MenuResolver
   ├─ ConfirmationResolver
   ├─ EntityExtractor
   ├─ IntentClassifier
   └─ FlowEngine
      → PatientTool
      → ServiceTool
      → DentistTool
      → AvailabilityTool
      → AppointmentTool
```

Flujos: `RegisterPatientFlow`, `ScheduleAppointmentFlow`,
`AvailabilityFlow`, `ViewAppointmentsFlow`, `ManageAppointmentFlow`,
`ServicesFlow`, `ProfileFlow`, `HumanTransferFlow`.

## 18. Resultado estructurado de pasos

``` json
{
 "message":"¿Para qué día quieres revisar disponibilidad?",
 "flow":"CONSULTAR_DISPONIBILIDAD",
 "step":"SELECCIONAR_FECHA",
 "expectedInput":"DATE",
 "options":[],
 "completed":false
}
```

## 19. Idempotencia y concurrencia

Operaciones críticas deben usar idempotencia para que webhooks
duplicados no creen dos citas.

Un slot mostrado no significa reservado. Antes de confirmar,
revalidarlo. Si se ocupó:

``` text
Ese horario acaba de ocuparse 😕.
1. 10:00
2. 10:30
3. 11:00
```

## 20. Tests obligatorios

-   `4 → CONSULTAR_DISPONIBILIDAD`.
-   `consultar disponi → CONSULTAR_DISPONIBILIDAD`.
-   `SELECCIONAR_ODONTOLOGO + 2 → odontólogo correcto`.
-   disponibilidad no solicita cédula.
-   `CONFIRMAR_RESERVA + si/1/ok → crear`.
-   cambio de fecha invalida slot y conserva resto.
-   webhook duplicado → una cita.
-   slot ocupado antes de confirmar → alternativas.

## 21. Prioridad

**P0:** ContextManager, menú/opciones, resolución numérica, paso y
evitar clasificación general durante flujo activo.

**P1:** servicio → odontólogo → fecha → franja → slots reales.

**P2:** identificación por WhatsApp, tutor/dependientes y cédula sólo
cuando haga falta.

**P3:** extracción múltiple, confirmación, revalidación, creación e
idempotencia.

**P4:** fuzzy matching, correcciones, reanudación, fallback y
transferencia humana.

## 22. Regla final

**Primero entender dónde está el usuario dentro de la conversación;
después interpretar lo que escribió.**

``` text
mensaje
→ contexto actual
→ flujo/paso
→ interpretación contextual
→ validación backend
→ acción
→ actualizar contexto
→ respuesta
```

Esto evita exactamente los errores observados: que `4` no funcione
después del menú y que `2` reinicie la conversación después de
seleccionar odontólogo.
