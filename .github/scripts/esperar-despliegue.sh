#!/usr/bin/env bash
# HU-125 · Espera a que la versión recién desplegada sea la que atiende y esté sana.
#
#   esperar-despliegue.sh <url-base> <commit> [minutos]
#
# Railway solo pasa el tráfico a la versión nueva cuando su /actuator/health/readiness responde: hasta
# entonces sigue atendiendo la anterior. Por eso aquí se mira /actuator/info hasta que diga el commit
# nuevo y luego /actuator/health, que lista cada componente. Si el tiempo se agota, el despliegue no
# entró (por ejemplo, una migración falló y la app no arrancó): el job falla y la versión anterior
# sigue en pie.
set -euo pipefail

base="${1:?falta la URL base}"
commit="${2:?falta el commit}"
minutos="${3:-15}"
limite=$(( $(date +%s) + minutos * 60 ))

echo "Esperando que ${base} atienda con ${commit}..."
while true; do
  version=$(curl -fsS --max-time 10 "${base}/actuator/info" 2>/dev/null \
    | sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' || true)
  if [ "${version}" = "${commit}" ]; then
    break
  fi
  if [ "$(date +%s)" -ge "${limite}" ]; then
    echo "::error::La versión ${commit} no llegó a atender en ${minutos} minutos (atiende: ${version:-nadie}). Revisa los logs del despliegue en Railway: si falló una migración, la versión anterior sigue en pie."
    exit 1
  fi
  sleep 15
done

salud=$(curl -sS --max-time 15 "${base}/actuator/health" || true)
echo "Salud: ${salud}"
{
  echo "### Despliegue del backend"
  echo ""
  echo "- Versión: \`${commit}\`"
  echo "- Salud: \`${salud}\`"
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"

estado=$(printf '%s' "${salud}" | sed -n 's/^{"status"[[:space:]]*:[[:space:]]*"\([A-Z_]*\)".*/\1/p')
if [ "${estado}" != "UP" ]; then
  echo "::warning::La versión ${commit} atiende, pero /actuator/health dice ${estado:-sin respuesta}: revisa qué componente está caído."
fi
echo "Listo: ${commit} atiende."
