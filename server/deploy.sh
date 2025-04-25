#!/usr/bin/env bash
set -euo pipefail
# Deploys code to Azure VM and builds and runs docker image with it
# Deploys also heartbeat script unless specified with --no-heart-beat
# but does not attempt to set up a chron script

# ——— Configuration ———
VM_USER="azureuser"
VM_HOST="ferafera.ddns.net"
VM_CONN="${VM_USER}@${VM_HOST}"
REMOTE_DIR="/home/${VM_USER}/deploy"

COMPOSE_DIR="docker/prod"
COMPOSE_FILE="${COMPOSE_DIR}/compose.yaml"
ENV_FILE="${COMPOSE_DIR}/.env"
DOCKERFILE="${COMPOSE_DIR}/Dockerfile"
NO_HEARTBEAT=false

# ——— Parse flags ———
echo "Parsing command-line flags..."
for arg in "$@"; do
  case "$arg" in
    --no-heart-beat)
      echo "Flag --no-heart-beat detected."
      NO_HEARTBEAT=true
      ;;
    *)
      echo "Unrecognized flag: $arg"
      ;;
  esac
done

# ——— Ensure local files exist ———
echo "Checking required local files..."
for f in "${COMPOSE_FILE}" "${ENV_FILE}" "${DOCKERFILE}"; do
  echo "Verifying existence of $f..."
  if [[ ! -f "$f" ]]; then
    echo "Error: $f not found." >&2
    exit 1
  fi
done

# ——— Prepare remote directory ———
echo "Creating remote directory ${REMOTE_DIR} on ${VM_CONN}..."
ssh "${VM_CONN}" "mkdir -p ${REMOTE_DIR}"

# ——— Transfer entire project to remote ———
echo "Removing previous files..."
ssh "${VM_CONN}" "sudo rm -rf ${REMOTE_DIR}/*"
echo "Archiving and transferring full project directory to VM..."
tar czf - . | ssh "${VM_CONN}" "tar xzf - -C ${REMOTE_DIR}"
echo "Transfer complete."

# ——— (Optional) heartbeat script ———
if [[ "${NO_HEARTBEAT}" = false ]]; then
  echo "Checking for heartbeat script..."
  if [[ -f "check_app.sh" ]]; then
    echo "Heartbeat script included in archive."
  else
    echo "Warning: check_app.sh not found; skipping heartbeat."
  fi
else
  echo "Flag --no-heart-beat set; skipping heartbeat script."
fi

# ——— Deploy on VM ———
echo "Initiating deployment on remote VM..."
ssh "${VM_CONN}" bash <<EOF
  set -euo pipefail

  echo "Entering deployment directory ${REMOTE_DIR}/${COMPOSE_DIR}..."
  cd ${REMOTE_DIR}/${COMPOSE_DIR}

  echo "Stopping and removing any existing containers..."
  docker-compose down

  echo "Building application image..."
  docker-compose build

  echo "Starting services..."
  docker-compose up -d

  echo "Cleaning up dangling Docker images..."
  docker image prune -f

  echo "Remote deployment complete."
EOF


echo "All done."
