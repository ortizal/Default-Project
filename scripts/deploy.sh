#!/usr/bin/env bash
# Construye y despliega el stack (backend + frontend + postgres + redis + openwa).
set -euo pipefail
cd "$(dirname "$0")/.."

ENV_FILE=".env"
for v in DATABASE_PASSWORD JWT_SECRET OPENWA_API_KEY OPENWA_WEBHOOK_SECRET; do
    if ! grep -qE "^${v}=.+" "$ENV_FILE"; then
        echo "Falta $v en $ENV_FILE -> ejecuta: ./scripts/secrets.sh" >&2
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
