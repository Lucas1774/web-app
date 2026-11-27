#!/usr/bin/env bash
set -euo pipefail

# ——— Configuration ———
SONAR_COMPOSE_FILE="docker/dev/compose.yaml"
SONAR_URL="http://localhost:9000"
SONAR_PROJECT_KEY="server"
SONAR_TIMEOUT_SECONDS=60
SONAR_TOKEN="squ_490efb7f8eeb6d32ad782a81fadee99a5f272e75"
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
docker compose -p sonar -f "${SONAR_COMPOSE_FILE}" --profile sonar up -d sonarqube

echo "Waiting for SonarQube to be ready..."
SECONDS=0
until curl -fsS "${SONAR_URL}/api/system/status" | grep -q '"status":"UP"'; do
  if (( SECONDS >= SONAR_TIMEOUT_SECONDS )); then
    echo "Error: SonarQube did not become ready within ${SONAR_TIMEOUT_SECONDS}s." >&2
    exit 1
  fi
  sleep 2
done

echo "Compiling the project and resolving dependencies..."
MAVEN_OPTS="${MAVEN_OPTS:-} --sun-misc-unsafe-memory-access=allow" \
mvn clean test-compile dependency:build-classpath \
  -Dmdep.outputFile=target/sonar-classpath.txt \
  -DincludeScope=test

MAVEN_CLASSPATH="$(<target/sonar-classpath.txt)"
LOMBOK_JAR="$(tr ';' '\n' <<<"${MAVEN_CLASSPATH}" | grep -i 'lombok-.*\.jar$' | head -n 1 || true)"
if [[ -z "${LOMBOK_JAR}" || ! -f "${LOMBOK_JAR}" ]]; then
  echo "Error: Could not find Lombok on the Maven dependency classpath." >&2
  exit 1
fi

echo "Generating temporary delomboked Java sources..."
java --sun-misc-unsafe-memory-access=allow -jar "${LOMBOK_JAR}" delombok src/main/java \
  --target=target/sonar-delombok/main \
  --classpath="${MAVEN_CLASSPATH}" \
  --sourcepath=src/main/java \
  --encoding=UTF-8 \
  --disable-preview
java --sun-misc-unsafe-memory-access=allow -jar "${LOMBOK_JAR}" delombok src/test/java \
  --target=target/sonar-delombok/test \
  --classpath="target/classes;target/test-classes;${MAVEN_CLASSPATH}" \
  --sourcepath=src/test/java \
  --encoding=UTF-8 \
  --disable-preview

# ——— Exclude pure-Lombok files from the duplication check ———
# A file qualifies only if it contains @lombok.Generated and every method/constructor
# declaration in it is directly annotated with it (no hand-written methods).
CPD_EXCLUSIONS=""
while IFS= read -r -d '' f; do
  grep -q '@lombok\.Generated' "$f" || continue
  awk '
    /^[[:space:]]*$/ || /^[[:space:]]*(\/\/|\/\*|\*)/ { next }
    /^[[:space:]]*@/ { if ($0 ~ /@lombok\.Generated/) gen = 1; next }
    /\)[^()]*\{[[:space:]]*$/ && !/^[[:space:]]*(\}[[:space:]]*)?(if|else|for|while|switch|catch|try|do)[[:space:](\{]/ {
      if (!gen) hand++
    }
    { gen = 0 }
    END { exit (hand > 0) }
  ' "$f" || continue
  CPD_EXCLUSIONS+="${CPD_EXCLUSIONS:+,}**/${f#target/sonar-delombok/main/}"
done < <(find target/sonar-delombok/main -name '*.java' -print0)
echo "Excluded from duplication check: ${CPD_EXCLUSIONS:-none}"

# ——— Run analysis ———
echo "Running Sonar analysis on delomboked sources..."
# shellcheck disable=SC2054
SONAR_MAVEN_ARGS=(
  org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar
  -Dsonar.host.url="${SONAR_URL}"
  -Dsonar.projectKey="${SONAR_PROJECT_KEY}"
  -Dsonar.qualitygate.wait=true
  -Dsonar.scm.disabled=true
  -Dsonar.sources=target/sonar-delombok/main,pom.xml
  -Dsonar.tests=target/sonar-delombok/test
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
if [[ -n "${CPD_EXCLUSIONS}" ]]; then
  SONAR_MAVEN_ARGS+=("-Dsonar.cpd.exclusions=${CPD_EXCLUSIONS}")
fi
mvn "${SONAR_MAVEN_ARGS[@]}"

echo "Analysis complete."
