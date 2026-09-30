#!/usr/bin/env bash
# Regenera docs/ESTADO_EN_VIVO.md (snapshot del stack). Lo invoca deploy.sh al final.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="docs/ESTADO_EN_VIVO.md"
FECHA="$(date -u +'%Y-%m-%d %H:%M UTC')"
FRONTEND_PORT_V="${FRONTEND_PORT:-$(awk -F= '$1 == "FRONTEND_PORT" {print $2; exit}' .env 2>/dev/null)}"
FRONTEND_PORT_V="${FRONTEND_PORT_V:-8081}"
FRONTEND_BIND_V="${FRONTEND_BIND_ADDRESS:-$(awk -F= '$1 == "FRONTEND_BIND_ADDRESS" {print $2; exit}' .env 2>/dev/null)}"
FRONTEND_BIND_V="${FRONTEND_BIND_V:-127.0.0.1}"
FRONTEND_PUBLISHED_V="$(docker compose port frontend 80 2>/dev/null | head -n 1 || true)"
FRONTEND_URL_V="${FRONTEND_PUBLIC_URL:-http://${FRONTEND_PUBLISHED_V:-${FRONTEND_BIND_V}:${FRONTEND_PORT_V}}}"
BACKEND_URL_V="${BACKEND_PUBLIC_URL:-$FRONTEND_URL_V}"
OPENWA_URL_V="${OPENWA_PUBLIC_URL:-http://localhost:2785}"

lineas_contenedores="$( { docker ps -a --format '{{.Names}}\t{{.Status}}\t{{.Image}}' 2>/dev/null \
    | grep -E 'dentalcrm|openwa' || true; } )"
mapfile -t LINEAS_C <<<"$lineas_contenedores"

lineas_tabla_contenedores=""
for l in "${LINEAS_C[@]:-}"; do
    nombre="$(printf '%s' "$l" | cut -f1)"
    estado="$(printf '%s' "$l" | cut -f2)"
    imagen="$(printf '%s' "$l" | cut -f3)"
    lineas_tabla_contenedores+="| $nombre | $estado | $imagen |"$'\n'
done

probe() { # $1=url -> ok|no
    if curl -sf --max-time 6 "$1" >/dev/null 2>&1; then echo ok; else echo no; fi
}

{
    echo "# Estado en vivo — $FECHA"
    echo
    echo '> Snapshot automático generado por `scripts/estado-en-vivo.sh` (lo invoca `deploy.sh`).'
    echo '> El documento curado y por módulos es `docs/ESTADO.md`.'
    echo
    echo '## Contenedores'
    echo
    echo '| Contenedor | Estado | Imagen |'
    echo '|---|---|---|'
    if [ -n "$lineas_tabla_contenedores" ]; then
        printf '%s' "$lineas_tabla_contenedores"
    else
        echo '| (no desplegado / docker no responde) | — | — |'
    fi
    echo
    echo '## Salud'
    echo
    echo '| Servicio | Estado |'
    echo '|---|---|'
    printf '| backend  (%s) | %s |\n' "$BACKEND_URL_V/api/v1/ping" "$(probe "$BACKEND_URL_V/api/v1/ping")"
    printf '| openwa   (%s) | %s |\n' "$OPENWA_URL_V/api/health" "$(probe "$OPENWA_URL_V/api/health")"
    printf '| frontend (%s) | %s |\n' "$FRONTEND_URL_V/api/v1/ping" "$(probe "$FRONTEND_URL_V/api/v1/ping")"
    echo
    echo '## Enlaces'
    echo
    echo "- CRM: $FRONTEND_URL_V"
    echo "- API: $BACKEND_URL_V/api/v1"
    echo "- Swagger: $BACKEND_URL_V/api/v1/swagger-ui.html"
    echo "- Dashboard OpenWA (QR): $OPENWA_URL_V"
    echo "- Webhook WhatsApp: $BACKEND_URL_V/api/v1/webhooks/whatsapp"
} >"$OUT"

echo "→ $OUT regenerado"
