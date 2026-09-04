#!/bin/sh
# Lo ejecuta el entrypoint de postgres la primera vez que arranca el volumen.
# Existe solo para pasarle la clave al script SQL: psql no lee variables de
# entorno por si mismo.
set -e
: "${REGENTA_DB_PASSWORD:?falta REGENTA_DB_PASSWORD}"
psql -v ON_ERROR_STOP=1 \
     -v clave="$REGENTA_DB_PASSWORD" \
     --username "$POSTGRES_USER" \
     --dbname postgres \
     -f /sql/init-databases.sql
