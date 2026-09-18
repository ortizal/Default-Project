# WhatsApp / OpenWA

## Arquitectura

El backend integra **OpenWA** ([open-wa.org](https://www.open-wa.org), MIT, self-hosted) como puente
de WhatsApp. OpenWA corre como un contenedor independiente (`rmyndharis/openwa:0.23.4`, puerto 2785)
que gestiona sesiones y entrega los eventos al backend.

```
teléfono ⇄ WhatsApp ⇄ OpenWA (gateway) ── webhook ──► backend /api/v1/webhooks/whatsapp (HMAC)
                          ▲
backend ── X-API-Key ─────┘  (sesiones, QR, send-text)
```

## Flujo de mensajes entrantes

OpenWA envía `POST {event, sessionId, idempotencyKey, data{from, body, fromMe, ...}}` al webhook
`POST /api/v1/webhooks/whatsapp`:

1. **Firma**: si `OPENWA_WEBHOOK_SECRET` está definida, se verifica `X-OpenWA-Signature`
   (HMAC-SHA256 del cuerpo crudo). Firmas inválidas → `401`.
2. **Dedupe**: se ignora un evento ya procesado (`X-OpenWA-Idempotency-Key`).
3. Se ignora lo propio (`fromMe`) y los mensajes de grupo (`isGroup`).
4. El `sessionId` (UUID de OpenWA) se resuelve al `sesionId` interno del CRM.
5. Se registra o actualiza la **conversación** (clave: sesión + teléfono) y se guarda el mensaje.
6. Si el agente IA está activo en la conversación, le delega la respuesta; si no, queda para el bot
   de citas o atención humana.

## Sesiones

- `POST /whatsapp/sesiones` registra una sesión en el CRM (no crea nada en OpenWA todavía).
- `POST /whatsapp/sesiones/{id}/conectar` crea la sesión en OpenWA, arranca el engine, registra el
  webhook y devuelve el **QR** (base64 PNG) para vincular el teléfono.
- `POST /whatsapp/sesiones/{id}/desconectar` detiene el engine (`stop`), conservando las credenciales.
- Estados en el CRM: `DESCONECTADA`, `CONECTANDO`, `CONECTADA`, `ERROR`.

## Envío

- `POST /conversaciones/{id}/mensajes` `{texto}` → OpenWA `POST /api/sessions/{id}/messages/send-text`
  con `chatId: <teléfono>@c.us`. Si la sesión no está conectada falla de forma controlada.

## Vista del frontend

- **Sesiones**: listado con estado, QR y errores; crear/conectar/desconectar.
- **Inbox**: conversaciones; al abrir una se cargan los mensajes, se puede responder y cambiar el
  estado. Polling cada ~6 s.

## Configuración

| Variable | Uso |
|---|---|
| `OPENWA_URL` | base del gateway (`http://openwa:2785`) |
| `OPENWA_API_KEY` | API key admin de OpenWA (es la misma pasada a `API_MASTER_KEY`) |
| `OPENWA_WEBHOOK_URL` | url del webhook interno (`http://backend:8080/api/v1/webhooks/whatsapp`) |
| `OPENWA_WEBHOOK_SECRET` | secreto HMAC para firmar los webhooks |

El envío falla con error controlado (no rompe el CRM) cuando OpenWA no está configurado o caído.

## Pruebas

- Unitarias (87): usan un `MessagingProvider` mock; no dependen de servicios externos.
- Smoke E2E con un **mock local** (`.smoke/mock_openwa.py` en `127.0.0.1:19091`) para desarrollo;
  en el despliegue real se prueba contra el contenedor OpenWA.