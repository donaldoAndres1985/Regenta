#!/usr/bin/env bash
# =====================================================================
# Golden tests (HU-128) en Linux, con la misma version de Flutter que CI.
#
#   tool/goldens.sh              compara contra las referencias versionadas
#   tool/goldens.sh actualizar   regenera las referencias (y solo eso)
#
# Por que en un contenedor: el texto se rasteriza distinto en Windows y en
# macOS, y un golden generado ahi falla en CI por el sistema operativo, no por
# la pantalla. CI corre en Linux; aqui tambien.
#
# El codigo se copia dentro del contenedor y se trabaja sobre la copia: un
# `pub get` de Linux sobre la carpeta del repo dejaria .dart_tool con rutas que
# Windows no entiende. Lo unico que vuelve es lo que se pidio: las referencias.
# =====================================================================
set -euo pipefail

IMAGEN="ghcr.io/cirruslabs/flutter:3.38.9"
MODO="${1:-comparar}"
APP="$(cd "$(dirname "$0")/.." && pwd)"
PAQUETES="packages/ventas packages/clientes"

case "$MODO" in
  comparar)   BANDERA="" ;;
  actualizar) BANDERA="--update-goldens" ;;
  *) echo "uso: $0 [comparar|actualizar]" >&2; exit 2 ;;
esac

# MSYS_NO_PATHCONV: en Git Bash de Windows, que no reescriba /fuente y /salida.
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$APP:/fuente:ro" -v "$APP:/salida" \
  -e BANDERA="$BANDERA" -e MODO="$MODO" -e PAQUETES="$PAQUETES" \
  "$IMAGEN" bash -euo pipefail -c '
    mkdir -p /tmp/app
    tar -C /fuente --exclude=.dart_tool --exclude=build --exclude="*.lock" \
        --exclude="test/goldens/failures" -cf - . | tar -C /tmp/app -xf -
    cd /tmp/app
    flutter --version > /dev/null
    dart pub global activate melos 6.3.2 > /dev/null
    export PATH="$PATH:$HOME/.pub-cache/bin"
    melos bootstrap > /dev/null
    estado=0
    for p in $PAQUETES; do
      (cd "$p" && flutter test --tags golden $BANDERA) || estado=$?
      if [ "$MODO" = actualizar ]; then
        mkdir -p "/salida/$p/test/goldens/referencias"
        cp "$p"/test/goldens/referencias/*.png "/salida/$p/test/goldens/referencias/"
      elif [ -d "$p/test/goldens/failures" ]; then
        rm -rf "/salida/$p/test/goldens/failures"
        cp -r "$p/test/goldens/failures" "/salida/$p/test/goldens/failures"
      fi
    done
    exit $estado
  '
