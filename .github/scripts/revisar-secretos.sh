#!/usr/bin/env bash
# HU-125 C4 · Ningún secreto versionado. Revisa los archivos que sigue git:
#   1. que no haya llaves, certificados ni archivos .env (solo .env.example);
#   2. que ningún archivo de texto traiga una llave privada o un token con formato conocido.
# Las propiedades de la app las revisa SecretosFueraDelRepoTest (backend).
set -uo pipefail

raiz="$(git rev-parse --show-toplevel)"
cd "$raiz"
fallas=0

prohibidos=$(git ls-files | grep -Ei '\.(p12|pfx|jks|keystore|pem|key)$|(^|/)\.env(\..*)?$|(^|/)google-services\.json$|(^|/)GoogleService-Info\.plist$|firebase-adminsdk.*\.json$' | grep -Ev '(^|/)\.env\.example$' || true)
if [ -n "$prohibidos" ]; then
  while IFS= read -r f; do
    echo "::error file=$f::Archivo de llaves, certificado o .env versionado: va por entorno, no en el repo."
  done <<< "$prohibidos"
  fallas=1
fi

# Formatos de alta confianza; los valores de ejemplo y las pruebas no los cumplen.
patrones=(
  '-----BEGIN ([A-Z]+ )?PRIVATE KEY-----'
  'AKIA[0-9A-Z]{16}'
  'gh[pousr]_[A-Za-z0-9]{36,}'
  'github_pat_[A-Za-z0-9_]{60,}'
  'xox[baprs]-[A-Za-z0-9-]{10,}'
  'AIza[0-9A-Za-z_-]{35}'
  '"private_key"[[:space:]]*:[[:space:]]*"-----BEGIN'
  'sk_live_[0-9A-Za-z]{20,}'
)
for p in "${patrones[@]}"; do
  hallados=$(git grep -nIE -e "$p" -- . ':!.github/scripts/revisar-secretos.sh' || true)
  if [ -n "$hallados" ]; then
    while IFS= read -r h; do
      archivo="${h%%:*}"
      resto="${h#*:}"
      linea="${resto%%:*}"
      echo "::error file=$archivo,line=$linea::Parece un secreto versionado (patrón: $p)."
    done <<< "$hallados"
    fallas=1
  fi
done

if [ "$fallas" -ne 0 ]; then
  echo "Se encontraron secretos versionados. Sácalos del repo, rótalos y configúralos por entorno."
  exit 1
fi
echo "Sin secretos versionados."
