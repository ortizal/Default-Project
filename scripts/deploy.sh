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

env_value() {
    awk -F= -v key="$1" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$ENV_FILE"
}

openwa_mode="${OPENWA_MODE:-$(env_value OPENWA_MODE)}"
openwa_mode="${openwa_mode:-auto}"
openwa_url="${OPENWA_URL:-$(env_value OPENWA_URL)}"
openwa_url="${openwa_url:-http://openwa:2785}"
openwa_running=0
if curl -fsS --max-time 2 http://127.0.0.1:2785/api/health >/dev/null 2>&1; then
    openwa_running=1
fi
openwa_port_in_use=0
if command -v ss >/dev/null 2>&1 && ss -H -ltn | awk '$4 ~ /:2785$/ { found = 1 } END { exit !found }'; then
    openwa_port_in_use=1
fi

case "$openwa_mode" in
    auto)
        if [ "$openwa_running" -eq 1 ]; then
            openwa_mode="external"
            if [ "$openwa_url" = "http://openwa:2785" ]; then
                openwa_url="http://host.docker.internal:2785"
            fi
            echo "OpenWA existente detectado en el host; se usará sin crear otro contenedor."
        elif [ "$openwa_port_in_use" -eq 1 ]; then
            echo "El puerto 2785 está ocupado, pero /api/health no respondió como OpenWA." >&2
            echo "Libera el puerto o configura OPENWA_MODE=external y OPENWA_URL en $ENV_FILE." >&2
            exit 1
        else
            openwa_mode="bundled"
            echo "No se detectó OpenWA en el host; se usará el contenedor incluido."
        fi
        ;;
    external)
        if [ "$openwa_url" = "http://openwa:2785" ]; then
            openwa_url="http://host.docker.internal:2785"
        fi
        echo "Modo OpenWA externo: $openwa_url"
        ;;
    bundled)
        echo "Modo OpenWA incluido en Docker."
        ;;
    *)
        echo "OPENWA_MODE debe ser auto, external o bundled." >&2
        exit 1
        ;;
esac

export OPENWA_URL="$openwa_url"

backend_port="${BACKEND_PORT:-$(env_value BACKEND_PORT)}"
backend_port="${backend_port:-18082}"
backend_bind="${BACKEND_BIND_ADDRESS:-$(env_value BACKEND_BIND_ADDRESS)}"
backend_bind="${backend_bind:-127.0.0.1}"
backend_already_running=0
if docker ps --format '{{.Names}}' | grep -qx 'dentalcrm-backend'; then
    backend_already_running=1
fi
if command -v ss >/dev/null 2>&1 && ss -H -ltn | awk -v port="$backend_port" '$4 ~ (":" port "$") { found = 1 } END { exit !found }'; then
    if [ "$backend_already_running" -eq 1 ]; then
        echo "El backend de este proyecto ya está corriendo en $backend_port; se validará la instancia existente."
        export BACKEND_PORT="$backend_port"
        export BACKEND_BIND_ADDRESS="$backend_bind"
        if curl -fsS "http://127.0.0.1:${backend_port}/actuator/health" | grep -q '"UP"'; then
            echo "Backend ya desplegado y saludable; no es necesario volver a levantarlo."
            echo "== comprobando OpenWA =="
            if curl -fsS http://localhost:2785/api/health >/dev/null; then
                echo "OpenWA saludable; finalizando despliegue sin cambios."
                exit 0
            fi
            echo "OpenWA no está respondiendo en localhost:2785. Revisa la sesión o el contenedor externo." >&2
            exit 1
        fi
        echo "El backend actual no responde correctamente en $backend_port; libera el puerto o cambia BACKEND_PORT." >&2
        exit 1
    fi
    echo "El puerto backend $backend_port está ocupado. Libéralo o configura BACKEND_PORT en $ENV_FILE y actualiza el upstream de Nginx." >&2
    exit 1
fi
export BACKEND_PORT="$backend_port"
export BACKEND_BIND_ADDRESS="$backend_bind"

frontend_root="${FRONTEND_ROOT:-$(env_value FRONTEND_ROOT)}"
frontend_root="${frontend_root:-/var/www/dentalcrm}"
if ! command -v npm >/dev/null 2>&1; then
    echo "No se encontró npm. Instala Node.js/npm en el servidor para compilar el frontend estático." >&2
    exit 1
fi
echo "== compilando frontend para Nginx nativo =="
umask 022
npm --prefix frontend ci
npm --prefix frontend run build -- --configuration production
frontend_dist="frontend/dist/dental-crm-frontend/browser"
if [ ! -f "$frontend_dist/index.html" ]; then
    echo "No se encontró el build Angular en $frontend_dist." >&2
    exit 1
fi
install -d -m 755 "$frontend_root"
cp -a "$frontend_dist"/. "$frontend_root"/
printf '{\n  "apiUrl": "/dental_crm/api/v1"\n}\n' > "$frontend_root/assets/config.json"
chmod 644 "$frontend_root/assets/config.json"
echo "Frontend estático instalado en $frontend_root."
echo "Nginx debe servir /dental_crm/ y proxyear /dental_crm/api/ a ${backend_bind}:${backend_port}."

echo "== retirando frontend Docker anterior (si existe) =="
docker compose --profile docker-frontend rm --stop --force frontend || true

export DOCKER_BUILDKIT=1
echo "== docker compose up --build -d =="
if [ "$openwa_mode" = "bundled" ]; then
    docker compose --profile bundled-openwa up --build -d
else
    docker compose up --build -d
fi

echo "== esperando salud backend y OpenWA =="
ok_b=0
ok_w=0
if [ "$openwa_mode" = "external" ]; then
    ok_w=1
fi
for i in $(seq 1 60); do
    if [ "$ok_b" -eq 0 ] && curl -sf "http://127.0.0.1:${backend_port}/actuator/health" | grep -q '"UP"'; then
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
