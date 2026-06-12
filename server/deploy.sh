#!/usr/bin/env bash
set -euo pipefail
# Deploys code to Oracle VM

# ——— Configuration ———
VM_USER="ubuntu"
VM_HOST="ferafera.ddns.net"
VM_CONN="${VM_USER}@${VM_HOST}"
REMOTE_DIR="/home/${VM_USER}/deploy"

COMPOSE_DIR="docker/prod"
COMPOSE_FILE="${COMPOSE_DIR}/compose.yaml"
ENV_FILE="${COMPOSE_DIR}/.env"
DOCKERFILE="${COMPOSE_DIR}/Dockerfile"

# ——— Ensure local files exist ———
echo "Checking required local files..."
for f in "${COMPOSE_FILE}" "${ENV_FILE}" "${DOCKERFILE}"; do
  echo "Verifying existence of $f..."
  if [[ ! -f "$f" ]]; then
    echo "Error: $f not found." >&2
    exit 1
  fi
done

# ——— Build locally ———
echo "Building artifact locally..."
mvn -DskipTests package

ARTIFACT="target/server.jar"
if [[ ! -f "${ARTIFACT}" ]]; then
  echo "Error: ${ARTIFACT} not found after build." >&2
  exit 1
fi

# ——— Transfer & deploy ———
echo "Starting deployment to ${VM_CONN} ..."
tar czf - "${ARTIFACT}" "${COMPOSE_DIR}" | ssh "${VM_CONN}" "set -euo pipefail; \
  sudo rm -rf '${REMOTE_DIR}'/* || true; \
  mkdir -p '${REMOTE_DIR}'; \
  echo 'Extracting archive...'; \
  tar xzf - -C '${REMOTE_DIR}'; \
  mv '${REMOTE_DIR}/target/server.jar' '${REMOTE_DIR}/${COMPOSE_DIR}/server.jar'; \
  cd '${REMOTE_DIR}/${COMPOSE_DIR}'; \
  echo 'Stopping and removing any existing containers...'; \
  docker compose down || true; \
  echo 'Building application image and starting services...'; \
  docker compose up --build -d; \
  echo 'Cleaning up dangling Docker images...'; \
  docker image prune -f; \
  echo 'Remote deployment complete.'"

echo "All done."
