#!/usr/bin/env bash
set -euo pipefail

# ——— Configuration ———
SONAR_COMPOSE_FILE="docker/dev/compose.yaml"
SONAR_URL="http://localhost:9000"
SONAR_PROJECT_KEY="server"
SONAR_TIMEOUT_SECONDS=60
SONAR_TOKEN="squ_9f24ba1833985c641d702de3d8fe96dbc77ef194"
# shellcheck disable=SC2034
# just for UI login
SONAR_PASSWORD="AdminPass123!"

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
echo "Running Sonar analysis and build..."
# shellcheck disable=SC2054
SONAR_MAVEN_ARGS=(
  clean
  test-compile
  org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar
  -Dsonar.host.url="${SONAR_URL}"
  -Dsonar.projectKey="${SONAR_PROJECT_KEY}"
  -Dsonar.qualitygate.wait=true
  -Dsonar.java.source=25
  -Dsonar.issue.ignore.multicriteria=e1,e2,e3,e4
  -Dsonar.issue.ignore.multicriteria.e1.ruleKey=java:S3776 # cognitive complexity
  -Dsonar.issue.ignore.multicriteria.e1.resourceKey=**/*.java
  -Dsonar.issue.ignore.multicriteria.e2.ruleKey=java:S1135 # TODOs
  -Dsonar.issue.ignore.multicriteria.e2.resourceKey=**/*.java
  -Dsonar.issue.ignore.multicriteria.e3.ruleKey=java:S6541 # brain method
  -Dsonar.issue.ignore.multicriteria.e3.resourceKey=**/*.java
  -Dsonar.issue.ignore.multicriteria.e4.ruleKey=java:S107 # method params
  -Dsonar.issue.ignore.multicriteria.e4.resourceKey=**/*.java
  -Dsonar.token="${SONAR_TOKEN}"
)
mvn "${SONAR_MAVEN_ARGS[@]}"

echo "Analysis complete."
