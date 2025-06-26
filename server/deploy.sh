#!/usr/bin/env bash
set -euo pipefail
# Deploys code to Oracle VM and builds and runs docker image with it

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

# ——— Transfer entire project to remote ———
echo "Removing previous files..."
ssh "${VM_CONN}" "sudo rm -rf ${REMOTE_DIR}/*"
ssh "${VM_CONN}" "mkdir -p ${REMOTE_DIR}"
echo "Archiving and transferring full project directory to VM..."
tar czf - . | ssh "${VM_CONN}" "tar xzf - -C ${REMOTE_DIR}"
echo "Transfer complete."

# ——— Deploy on VM ———
echo "Initiating deployment on remote VM..."
ssh "${VM_CONN}" bash <<EOF
  set -euo pipefail

  echo "Entering deployment directory ${REMOTE_DIR}/${COMPOSE_DIR}..."
  cd ${REMOTE_DIR}/${COMPOSE_DIR}

  echo "Stopping and removing any existing containers..."
  docker compose down

  echo "Building application image and starting services..."
  docker compose up --build -d

  echo "Cleaning up dangling Docker images..."
  docker image prune -f

  echo "Remote deployment complete."
EOF


echo "All done."
