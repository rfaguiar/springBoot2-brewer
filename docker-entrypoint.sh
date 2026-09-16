#!/bin/sh
# Waits until the MySQL host:port accepts TCP connections before starting
# the Spring Boot app. This is a defense-in-depth guard on top of the
# docker-compose "depends_on: condition: service_healthy" rule, since a
# healthcheck can report healthy right as MySQL is cycling between its
# internal bootstrap instance and the real server on first-time init.
set -e

DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
MAX_WAIT_SECONDS="${DB_WAIT_TIMEOUT:-90}"
elapsed=0

echo "Waiting for MySQL at ${DB_HOST}:${DB_PORT} to accept connections..."
until nc -z "$DB_HOST" "$DB_PORT" 2>/dev/null; do
  elapsed=$((elapsed + 2))
  if [ "$elapsed" -ge "$MAX_WAIT_SECONDS" ]; then
    echo "ERROR: MySQL at ${DB_HOST}:${DB_PORT} did not become available after ${MAX_WAIT_SECONDS}s." >&2
    exit 1
  fi
  sleep 2
done

echo "MySQL is accepting connections. Starting BrewerApplication..."
exec java $JAVA_OPTS -jar /app/app.jar
