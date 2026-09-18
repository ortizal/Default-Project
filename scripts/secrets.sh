#!/usr/bin/env bash
# Genera (solo si faltan) los secretos de los despliegues en .env sin imprimirlos.
set -euo pipefail
cd "$(dirname "$0")/.."

ENV_FILE=".env"
if [ ! -f "$ENV_FILE" ]; then
    cp .env.example "$ENV_FILE"
    echo "creado $ENV_FILE desde .env.example"
fi

reemplazar() { # $1=variable  $2=valor  (no toca si ya existe)
    if grep -q "^$1=" "$ENV_FILE"; then
        sed -i -E "s|^($1)=.*|\1=$2|" "$ENV_FILE"
    else
        printf '\n%s=%s\n' "$1" "$2" >>"$ENV_FILE"
    fi
}

# Para competir con postgres:17 hay que darle un password fuerte; jwt >= 32 bytes.
[ -n "$(openssl rand -hex 16)" ] # verifica openssl disponible

if ! grep -qE '^DATABASE_PASSWORD=.+' "$ENV_FILE"; then
    reemplazar DATABASE_PASSWORD "$(openssl rand -hex 18)"
    echo "  DATABASE_PASSWORD  generado"
fi
if ! grep -qE '^JWT_SECRET=.+' "$ENV_FILE"; then
    reemplazar JWT_SECRET "$(openssl rand -hex 48)"
    echo "  JWT_SECRET         generado"
fi
# OpenWA exige API_MASTER_KEY >= 32 caracteres y webhook secret (HMAC) >= 16.
if ! grep -qE '^OPENWA_API_KEY=.+' "$ENV_FILE"; then
    reemplazar OPENWA_API_KEY "$(openssl rand -hex 24)"
    echo "  OPENWA_API_KEY     generado"
fi
if ! grep -qE '^OPENWA_WEBHOOK_SECRET=.+' "$ENV_FILE"; then
    reemplazar OPENWA_WEBHOOK_SECRET "$(openssl rand -hex 16)"
    echo "  OPENWA_WEBHOOK_SECRET generado"
fi

# Google OAuth: las credenciales NO se generan aquí. Se crean en
# Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client ID.
# Alternativamente, se pueden configurar desde el panel admin en /google
# sección "Credenciales OAuth" (sin necesidad de reiniciar el backend).
# Redirigir a: http://localhost:8080/api/v1/google/callback
if ! grep -qE '^GOOGLE_CLIENT_ID=.+' "$ENV_FILE"; then
    echo "  ADVERTENCIA: GOOGLE_CLIENT_ID no configurado (crear en Google Cloud Console o desde el panel admin)"
fi
if ! grep -qE '^GOOGLE_CLIENT_SECRET=.+' "$ENV_FILE"; then
    echo "  ADVERTENCIA: GOOGLE_CLIENT_SECRET no configurado (crear en Google Cloud Console o desde el panel admin)"
fi

echo "lista de variables obligatorias presentes:"
grep -oE '^(DATABASE_PASSWORD|JWT_SECRET|OPENWA_API_KEY|OPENWA_WEBHOOK_SECRET|GOOGLE_CLIENT_ID|GOOGLE_CLIENT_SECRET)=' "$ENV_FILE" | sed 's/=$//'
echo "OK (valores no mostrados)."
