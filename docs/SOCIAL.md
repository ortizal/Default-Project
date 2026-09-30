# Redes sociales

Conexión OAuth de Facebook/Instagram (Meta) y TikTok, y publicación de contenido desde el CRM.

## Arquitectura

- **Dominio**: `domain/social` (`SocialAccount`, `SocialOAuthRequest`, `SocialPlatform`, `SocialProvider`).
- **Persistencia**: migración `V22__social_connections.sql` (tabla `social_accounts`, permiso
  `REDES_SOCIALES` asignado al rol Admin).
- **Tokens**: se guardan cifrados con AES-GCM (`service/SocialTokenCipher`, clave
  `APP_SOCIAL_TOKEN_ENCRYPTION_KEY`, mínimo 32 caracteres) y se descifran al publicar.
- **Multi-tenant**: `@TenantId` en la entidad; cada clínica sólo ve sus cuentas.
- **UI**: módulo `/redes-sociales` (estado de cuentas, conectar/desconectar y crear publicación).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/social/status` | Estado de cada proveedor y cuentas conectadas |
| GET | `/social/{provider}/connect` | Inicia el flujo OAuth (`meta` · `tiktok`) |
| DELETE | `/social/{provider}/disconnect` | Elimina las cuentas del proveedor |
| GET | `/social/callback/{provider}` | Callback OAuth (redirige al frontend) |
| POST | `/social/publish` | Publica contenido (multipart) |

Todos requieren el permiso `PERMISO_REDES_SOCIALES` salvo el callback.

### `POST /social/publish`

`multipart/form-data`:

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `cuentaId` | número | sí | Cuenta conectada que recibe la publicación |
| `texto` | texto | sí | 1–2200 caracteres (recortado) |
| `imagen` | archivo | no | JPEG/PNG/WebP, hasta 5 MB |

Respuesta `200`: `{cuentaId, platform, publicacionId, publicadoEn}`.

Errores: `SOCIAL_ACCOUNT_MISSING`, `SOCIAL_TEXT_REQUIRED`, `SOCIAL_TEXT_TOO_LONG`,
`SOCIAL_IMAGE_INVALID`, `SOCIAL_IMAGE_TOO_LARGE`, `SOCIAL_URL_PUBLICA_REQUERIDA`,
`SOCIAL_TOKEN_ERROR`, `SOCIAL_PUBLISH_FAILED` (el proveedor rechazó la publicación,
incluye su mensaje), `SOCIAL_PROVIDER_UNAVAILABLE` (503).

Cada publicación queda registrada en auditoría como `PUBLICAR_EN_REDES` (módulo `REDES_SOCIALES`).

## Qué permite publicar cada red

| Red | Estado | Motivo |
|---|---|---|
| **Facebook** | ✅ publicación con texto e imagen | Graph API acepta la subida binaria (`POST /{page}/photos` con `source`) o sólo texto (`POST /{page}/feed`) |
| **Instagram** | ❌ `SOCIAL_URL_PUBLICA_REQUERIDA` | Sólo acepta fotos servidas desde una **URL pública** (`image_url`); el CRM no aloja archivos todavía |
| **TikTok** | ❌ `SOCIAL_URL_PUBLICA_REQUERIDA` | La Content Posting API de fotos (`/v2/post/publish/content/init/`) sólo admite `source: PULL_FROM_URL` con imágenes públicas |

La UI sólo ofrece cuentas de Facebook como destino y explica el límite de las otras dos.

## Configuración

| Variable | Descripción |
|---|---|
| `META_APP_ID` · `META_APP_SECRET` | Aplicación de Meta (Facebook Developers) |
| `META_REDIRECT_URI` | `https://<host>/api/v1/social/callback/meta` |
| `TIKTOK_CLIENT_KEY` · `TIKTOK_CLIENT_SECRET` | Aplicación de TikTok for Developers |
| `TIKTOK_REDIRECT_URI` | `https://<host>/api/v1/social/callback/tiktok` |
| `APP_SOCIAL_TOKEN_ENCRYPTION_KEY` | Clave AES-GCM de los tokens (≥32 caracteres) |
| `SOCIAL_FRONTEND_URL` | URL base del frontend a la que vuelve el callback |

Los scopes pedidos al conectar:

- Meta: `pages_show_list`, `pages_read_engagement`, **`pages_manage_posts`**, `instagram_basic`.
- TikTok: `user.info.basic`, `user.info.profile`, `video.publish`.

> `pages_manage_posts` es el permiso que permite publicar en la página. Las cuentas conectadas
> antes de añadirlo conservan el token antiguo: **reconecta Meta** después de desplegar para que
> la publicación funcione.

## Pruebas

```bash
cd backend && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -o test   # 175 tests (Social*: 18)
cd frontend && npx playwright test redes        # validación del formulario de publicación
cd frontend && npx playwright test axe qa foco hover   # la ruta /redes-sociales está incluida
```

Los tests de backend cubren el contrato de `POST /publish` (binding multipart con y sin imagen),
las validaciones y el manejo de errores de Graph API con WebClient simulado; el flujo real con
credenciales de Meta/TikTok queda para la verificación manual.

## Próximos pasos

1. **Alojar imágenes** (CDN/S3 o un endpoint público del CRM) para poder habilitar
   Instagram (`image_url`) y TikTok (`PULL_FROM_URL`).
2. Verificación manual con credenciales reales: los clientes de TikTok sin auditar publican
   en modo privado hasta que TikTok apruebe la integración.
