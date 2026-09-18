#!/usr/bin/env bash
# Salud del stack: contenedores (docker ps), endpoints y sesiones WhatsApp.
# NO depende de nombres hardcodeados: lee `docker ps`.
set -euo pipefail
cd "$(dirname "$0")/.."

backend="${BACKEND_URL:-http://localhost:8080}"
openwa="${OPENWA_PUBLIC_URL:-http://localhost:2785}"
frontend="${FRONTEND_URL:-http://localhost}"

echo "== Contenedores =="
docker ps -a --format '| {{.Names}} | {{.Status}} | {{.Image}} |' \
    | grep -E 'dentalcrm|openwa|postgres|redis' || true
echo

echo "== Salud de endpoints =="
probe() { # $1=url
    if curl -sf --max-time 5 "$1" >/dev/null; then echo "  ✓ $1"; else echo "  ✗ $1"; fi
}
probe "$backend/actuator/health"
probe "$openwa/api/health"
probe "$frontend/api/v1/ping"
echo

echo "== Sesiones WhatsApp =="
TOK="${TMPDIR:-/tmp}/dentalcrm_token_${USER}"
if ! curl -s -X POST "$backend/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"${BACKEND_USER:-admin}\",\"password\":\"${BACKEND_PASS:-admin123}\"}" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin).get("token",""))' >"$TOK"; then
    echo "  (login fallido)" ; exit 0
fi
T="$(cat "$TOK")"
if [ -z "$T" ]; then echo "  (token vacío: revisa BACKEND_USER/BACKEND_PASS)"; exit 0; fi
curl -s "$backend/api/v1/whatsapp/sesiones" -H "Authorization: Bearer $T" \
    | python3 -c 'import sys,json
d=json.load(sys.stdin)
r=d if isinstance(d,list) else d.get("content",[])
for x in r:
    print("  {}  {:<12}  {}".format(x.get("sesionId"), x.get("estado"), x.get("proveedor","")))'
