#!/usr/bin/env bash
# Utilidades para WhatsApp contra el CRM (backend). Uso:
#   ./scripts/whatsapp.sh login
#   ./scripts/whatsapp.sh crear SES_PRINCIPAL "Consultorio principal"
#   ./scripts/whatsapp.sh qr SES_PRINCIPAL
#   ./scripts/whatsapp.sh estado SES_PRINCIPAL
#   ./scripts/whatsapp.sh enviar <idConversacion> "Hola"
#   ./scripts/whatsapp.sh webhook-test
set -euo pipefail
cd "$(dirname "$0")/.."

BACKEND="${BACKEND_URL:-http://localhost:8080}"
OPENWA="${OPENWA_PUBLIC_URL:-http://localhost:2785}"
TOKEN_FILE="${TMPDIR:-/tmp}/dentalcrm_token_${USER}"

# ---------- auth ----------
login() {
    curl -s -X POST "$BACKEND/api/v1/auth/login" -H 'Content-Type: application/json' \
        -d "{\"username\":\"${BACKEND_USER:-admin}\",\"password\":\"${BACKEND_PASS:-admin123}\"}" \
        >"$TOKEN_FILE"
    python3 - "$TOKEN_FILE" <<'PY'
import json, sys
d = json.load(open(sys.argv[1]))
tok = d.get("token")
if not tok:
    print("Login falló:", d, file=sys.stderr); sys.exit(1)
open(sys.argv[1]+".jwt", "w").write(tok)
print("token guardado")
PY
}
token() { # imprime el JWT (login implícito si hace falta)
    if [ ! -s "$TOKEN_FILE" ]; then login >/dev/null; fi
    python3 -c "import json,sys;print(json.load(open('$TOKEN_FILE'))['token'])"
}

# ---------- helpers ----------
id_de() { # $1=sesionId interno -> id de BD (el repo lo usa en el path)
    curl -s "$BACKEND/api/v1/whatsapp/sesiones" -H "Authorization: Bearer $(token)" \
      | python3 -c 'import sys,json;a=json.load(sys.stdin);a=a if isinstance(a,list) else a.get("content",[]);print(next((str(x["id"]) for x in a if x.get("sesionId")=="'$1'"),""))'
}

cmd_crear() {
    sesionId="$1"; nombre="$2"
    curl -s -X POST "$BACKEND/api/v1/whatsapp/sesiones" \
        -H "Authorization: Bearer $(token)" -H 'Content-Type: application/json' \
        -d "{\"sesionId\":\"$sesionId\",\"nombre\":\"$nombre\"}" | python3 -m json.tool
}
cmd_qr() {
    id="$(id_de "$1")"; [ -n "$id" ] || { echo "Sesión $1 no encontrada" >&2; exit 1; }
    qr="$(curl -s -X POST "$BACKEND/api/v1/whatsapp/sesiones/$id/conectar" \
        -H "Authorization: Bearer $(token)")"
    echo "$qr" | python3 -c 'import sys,json,base64,re;d=json.load(sys.stdin);b=d.get("qr") or "";print("estado:",d.get("estado"));m=re.search(r"base64,(.*)",b,re.S);open("/tmp/whatsapp-qr.png","wb").write(base64.b64decode(m.group(1))) if m else None;print("QR guardado en /tmp/whatsapp-qr.png") if m else print("(sin QR aún: "+d.get("detalle","")+")")'
}
cmd_estado() {
    id="$(id_de "$1")"; [ -n "$id" ] || { echo "Sesión $1 no encontrada" >&2; exit 1; }
    curl -s "$BACKEND/api/v1/whatsapp/sesiones/$id" -H "Authorization: Bearer $(token)" | python3 -m json.tool
}
cmd_enviar() {
    conv="$1"; shift
    curl -s -X POST "$BACKEND/api/v1/whatsapp/conversaciones/$conv/mensajes" \
        -H "Authorization: Bearer $(token)" -H 'Content-Type: application/json' \
        -d "{\"texto\":\"$*\"}" | python3 -m json.tool
}
cmd_webhook_test() {
    # Simula un evento message.received firmado (HMAC-SHA256) hacia el webhook del backend.
    secret="$(awk -F= '/^OPENWA_WEBHOOK_SECRET=/{print $2}' .env)"
    [ -n "$secret" ] || { echo "Falta OPENWA_WEBHOOK_SECRET en .env" >&2; exit 1; }
    idExt="$(curl -s -H "X-API-Key: $(awk -F= '/^OPENWA_API_KEY=/{print $2}' .env)" "$OPENWA/api/sessions" \
        | python3 -c 'import sys,json;a=json.load(sys.stdin);print(next((x["id"] for x in a if x.get("name")=="SES_PRINCIPAL"),""))')"
    payload='{"event":"message.received","sessionId":"'"$idExt"'","data":{"from":"5215512345678@c.us","body":"Hola desde el script","notifyName":"Script"}}'
    sig=$(printf '%s' "$payload" | openssl dgst -sha256 -hmac "$secret" | awk '{print "sha256="$NF}')
    curl -s -i -X POST "$BACKEND/api/v1/webhooks/whatsapp" -H 'Content-Type: application/json' \
        -H "X-OpenWA-Signature: $sig" -d "$payload"
}

case "${1:-}" in
    login) login ;;
    crear) shift; cmd_crear "$1" "$2" ;;
    qr) shift; cmd_qr "$1" ;;
    estado) shift; cmd_estado "$1" ;;
    enviar) shift; cmd_enviar "$1" "$2" ;;
    webhook-test) cmd_webhook_test ;;
    *)
        echo "Uso: $0 {login|crear|qr|estado|enviar|webhook-test}" >&2; exit 1 ;;
esac
