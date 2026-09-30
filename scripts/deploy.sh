#!/usr/bin/env bash
# Construye y despliega el stack (backend + frontend + postgres + redis + openwa).
set -euo pipefail
cd "$(dirname "$0")/.."

ENV_FILE=".env"
if [ ! -f "$ENV_FILE" ]; then
    echo "No existe $ENV_FILE. Se creará desde .env.example."
    if [ ! -f ".env.example" ]; then
        echo "No se encontró .env.example; no se puede preparar el entorno." >&2
        exit 1
    fi

    umask 077
    cp .env.example "$ENV_FILE"
    chmod 600 "$ENV_FILE"

    reemplazar_env() {
        local variable="$1" valor="$2"
        if grep -qE "^${variable}=" "$ENV_FILE"; then
            sed -i "s|^${variable}=.*|${variable}=${valor}|" "$ENV_FILE"
        else
            printf '%s=%s\n' "$variable" "$valor" >> "$ENV_FILE"
        fi
    }

    if [ -t 0 ]; then
        read -r -p "Usuario de PostgreSQL [dentalcrm]: " db_user
        db_user="${db_user:-dentalcrm}"
        if [[ ! "$db_user" =~ ^[A-Za-z0-9_.-]+$ ]]; then
            echo "El usuario solo puede contener letras, números, punto, guion y guion bajo." >&2
            rm -f "$ENV_FILE"
            exit 1
        fi
        reemplazar_env DATABASE_USERNAME "$db_user"

        while true; do
            read -r -s -p "Contraseña de PostgreSQL (Enter para generar una segura): " db_password
            printf '\n'
            if [ -z "$db_password" ]; then
                break
            fi
            if [[ "$db_password" =~ ^[A-Za-z0-9_.@%+=:,/-]+$ ]]; then
                reemplazar_env DATABASE_PASSWORD "$db_password"
                unset db_password
                break
            fi
            echo "Usa letras, números y estos símbolos: . _ @ % + = : , / -"
            unset db_password
        done
        unset db_user
    else
        echo "Sin terminal interactiva: se usará el usuario PostgreSQL dentalcrm y se generarán secretos seguros."
    fi

    ./scripts/secrets.sh
    echo "$ENV_FILE creado con permisos privados (600)."
else
    echo "$ENV_FILE ya existe; se conservará sin modificar."
fi

for v in DATABASE_PASSWORD JWT_SECRET OPENWA_API_KEY OPENWA_WEBHOOK_SECRET; do
    if ! grep -qE "^${v}=.+" "$ENV_FILE"; then
        echo "Falta $v en $ENV_FILE. Completa esa variable antes de desplegar." >&2
        exit 1
    fi
done

export DOCKER_BUILDKIT=1
echo "== docker compose down =="
docker compose down || true
echo "== docker compose up --build -d =="
docker compose up --build -d

echo "== esperando salud backend/openwa =="
ok_b=0; ok_w=0
for i in $(seq 1 60); do
    if [ "$ok_b" -eq 0 ] && curl -sf http://localhost:8080/actuator/health | grep -q '"UP"'; then
        ok_b=1; echo "  backend   UP (tras ${i}s)"
    fi
    if [ "$ok_w" -eq 0 ] && curl -sf http://localhost:2785/api/health >/dev/null; then
        ok_w=1; echo "  openwa    UP (tras ${i}s)"
    fi
    [ "$ok_b" -eq 1 ] && [ "$ok_w" -eq 1 ] && break
    sleep 1
done
[ "$ok_b" -eq 1 ] && [ "$ok_w" -eq 1 ] || { echo "Fallo en el arranque (revisa 'docker compose logs -f')" >&2; exit 1; }

./scripts/health.sh
./scripts/estado-en-vivo.sh
