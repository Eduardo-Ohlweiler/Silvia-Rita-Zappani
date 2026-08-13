#!/usr/bin/env bash
# Sobe a API em desenvolvimento carregando as variáveis do .env.
#
#   ./run-dev.sh              sobe a aplicação
#   ./run-dev.sh test         roda os testes
#   ./run-dev.sh package      gera o jar
#
# Pré-requisitos: JDK 21 e PostgreSQL local com o banco já criado.
#   createdb -U postgres -h localhost nutridb
#   createdb -U postgres -h localhost nutridb_test

set -euo pipefail
cd "$(dirname "$0")"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
else
  echo "AVISO: .env não encontrado. Copie .env.example para .env." >&2
fi

# O Maven usa JAVA_HOME, não o `java` do PATH. Em máquina com mais de um JDK
# instalado é comum o JAVA_HOME apontar para o 17 e o build falhar com
# "release version 21 not supported". Localiza o 21 e usa.
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/javac" ]] \
   || ! "${JAVA_HOME}/bin/javac" -version 2>&1 | grep -q ' 21'; then
  for candidato in /usr/lib/jvm/java-21-openjdk-amd64 \
                   /usr/lib/jvm/java-1.21.0-openjdk-amd64 \
                   /usr/lib/jvm/openjdk-21 \
                   /usr/lib/jvm/temurin-21-jdk-amd64; do
    if [[ -x "$candidato/bin/javac" ]]; then
      export JAVA_HOME="$candidato"
      break
    fi
  done
fi

if [[ -z "${JAVA_HOME:-}" ]] || ! "${JAVA_HOME}/bin/javac" -version 2>&1 | grep -q ' 21'; then
  echo "ERRO: JDK 21 não encontrado. Instale-o ou exporte JAVA_HOME apontando para ele." >&2
  exit 1
fi

for var in DB_URL DB_USER DB_PASSWORD JWT_SECRET SUPERADMIN_EMAIL SUPERADMIN_PASSWORD SUPERADMIN_NAME; do
  if [[ -z "${!var:-}" ]]; then
    echo "ERRO: variável obrigatória não definida: $var" >&2
    exit 1
  fi
done

if (( ${#JWT_SECRET} < 32 )); then
  echo "ERRO: JWT_SECRET precisa de no mínimo 32 caracteres (tem ${#JWT_SECRET})." >&2
  exit 1
fi

MVN="./mvnw"
[[ -x "$MVN" ]] || MVN="mvn"

case "${1:-run}" in
  run)     exec "$MVN" spring-boot:run ;;
  test)    exec "$MVN" test ;;
  package) exec "$MVN" clean package ;;
  *)       exec "$MVN" "$@" ;;
esac
