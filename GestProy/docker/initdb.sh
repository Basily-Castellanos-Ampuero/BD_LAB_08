#!/bin/bash
# ============================================================
#  GestProy — inicialización de la BD en Docker
#
#  Este script lo ejecuta AUTOMÁTICAMENTE la imagen oficial de
#  PostgreSQL la primera vez que arranca con un volumen de datos
#  vacío (mecanismo /docker-entrypoint-initdb.d). Aplica todos
#  los scripts SQL del proyecto en el mismo orden que
#  db/scripts/apply-all.ps1: esquema -> triggers -> funciones ->
#  vistas -> seed. Dentro de cada carpeta el orden alfabético de
#  los archivos (prefijos 01, 02, ... / 10, 11, 20, ...) coincide
#  con el orden de dependencias requerido.
#
#  La carpeta db/ se monta de solo lectura en /gestproy-db
#  (ver docker-compose.yml). Como sólo corre en la primera
#  inicialización: si cambias los scripts SQL, recrea el volumen
#  con `docker compose down -v` y vuelve a levantar.
# ============================================================
set -euo pipefail

DB_DIR=/gestproy-db

echo "GestProy: aplicando scripts SQL sobre la base '$POSTGRES_DB'..."
for dir in schema triggers functions views seed; do
  for f in "$DB_DIR/$dir"/*.sql; do
    [ -e "$f" ] || continue
    echo "==> $dir/$(basename "$f")"
    psql -v ON_ERROR_STOP=1 \
         --username "$POSTGRES_USER" \
         --dbname "$POSTGRES_DB" \
         -f "$f"
  done
done
echo "GestProy: todos los scripts SQL se aplicaron correctamente."
