# Fuentes empaquetadas con `regenta_core`

Van versionadas a proposito: la app es offline-first y las dos familias tienen
que cargar igual en Android y en Web sin pedir la red (HU-106, criterio 2).

| Familia | Pesos | Origen | Licencia |
|---|---|---|---|
| Archivo | 400, 500, 600, 700 | github.com/Omnibus-Type/Archivo (`fonts/ttf/`) | OFL 1.1 — `OFL-Archivo.txt` |
| IBM Plex Mono | 400, 500, 600 | github.com/google/fonts (`ofl/ibmplexmono/`) | OFL 1.1 — `OFL-IBMPlexMono.txt` |

Se declaran en `../pubspec.yaml`, bajo `flutter: fonts:`. Los nombres de familia
(`Archivo`, `IBMPlexMono`) son los que usa `RegentaType`.
