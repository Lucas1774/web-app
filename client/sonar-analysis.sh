#!/usr/bin/env bash
set -euo pipefail

# ——— Configuration ———
SONAR_COMPOSE_FILE="../server/docker/dev/compose.yaml"
SONAR_URL="http://localhost:9000"
SONAR_PROJECT_KEY="client"
SONAR_TIMEOUT_SECONDS=60
SONAR_TOKEN="squ_9f24ba1833985c641d702de3d8fe96dbc77ef194"

# ——— Ensure local files exist ———
echo "Checking required local files..."
if [[ ! -f "${SONAR_COMPOSE_FILE}" ]]; then
  echo "Error: ${SONAR_COMPOSE_FILE} not found." >&2
  exit 1
fi

# ——— Start SonarQube ———
echo "Starting the SonarQube service from ${SONAR_COMPOSE_FILE}..."
docker compose -f "${SONAR_COMPOSE_FILE}" --profile sonar up -d sonarqube

echo "Waiting for SonarQube to be ready..."
SECONDS=0
until curl -fsS "${SONAR_URL}/api/system/status" | grep -q '"status":"UP"'; do
  if (( SECONDS >= SONAR_TIMEOUT_SECONDS )); then
    echo "Error: SonarQube did not become ready within ${SONAR_TIMEOUT_SECONDS}s." >&2
    exit 1
  fi
  sleep 2
done

# ——— Run analysis ———
echo "Running Sonar analysis..."
if ! command -v npx >/dev/null 2>&1; then
  echo "Error: npx is required to run the Sonar scanner." >&2
  exit 1
fi

# cognitive complexity and nested ternary operators ignored
npx --yes sonarqube-scanner \
  -Dsonar.host.url="${SONAR_URL}" \
  -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
  -Dsonar.projectName="${SONAR_PROJECT_KEY}" \
  -Dsonar.sources=src \
  -Dsonar.tests=src \
  -Dsonar.test.inclusions='**/*.test.js,**/*.test.jsx,**/*.spec.js,**/*.spec.jsx' \
  -Dsonar.inclusions='**/*.js,**/*.jsx' \
  -Dsonar.exclusions='**/node_modules/**,**/build/**,cordova_env/**,src/assets/**,public/**,build/**' \
  -Dsonar.issue.ignore.multicriteria=e1,e2,e3,e4 \
  -Dsonar.issue.ignore.multicriteria.e1.ruleKey=javascript:S3776 \
  -Dsonar.issue.ignore.multicriteria.e1.resourceKey='**/*.js' \
  -Dsonar.issue.ignore.multicriteria.e2.ruleKey=javascript:S3776 \
  -Dsonar.issue.ignore.multicriteria.e2.resourceKey='**/*.jsx' \
  -Dsonar.issue.ignore.multicriteria.e3.ruleKey=javascript:S3358 \
  -Dsonar.issue.ignore.multicriteria.e3.resourceKey='**/*.js' \
  -Dsonar.issue.ignore.multicriteria.e4.ruleKey=javascript:S3358 \
  -Dsonar.issue.ignore.multicriteria.e4.resourceKey='**/*.jsx' \
  -Dsonar.sourceEncoding=UTF-8 \
  -Dsonar.token="${SONAR_TOKEN}" \
  -Dsonar.language=js

echo "Analysis complete."
