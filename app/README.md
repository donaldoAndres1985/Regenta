# app — monorepo Flutter de Regenta

Un solo codigo que compila a **APK (Android)** y a **PWA (Web)**. Monorepo
[melos](https://melos.invertase.dev): un paquete por modulo, una sola app.

```
app/
  melos.yaml            orquesta bootstrap, analyze, test
  analysis_options.yaml lint compartido; cualquier aviso es un fallo
  tool/
    check_module_deps.dart   un modulo depende de core y de ningun otro modulo
  packages/
    core/                tema, sesion, navegacion, base offline
    usuarios/ reportes/ facturacion/
    inventario/ ventas/ compras/           patron Venta directa
    recursos/ reservas/                    patron Reserva
    menu/ mesas/ comandas/                 patron Comanda
  apps/
    regenta/             la app: Android + Web, cuelga de core
```

## Reglas

- Un paquete de modulo depende de `regenta_core` y **nunca** de otro modulo. Lo
  verifica `tool/check_module_deps.dart` (y el CI).
- Ningun `Color(0xFF...)` a mano: los tokens salen de `packages/core` (HU-106).
- Una sola pantalla que se adapta con `LayoutBuilder`, no dos widgets.

## Comandos

```bash
dart pub global activate melos    # una vez
melos bootstrap                   # resuelve dependencias de todos los paquetes
melos run analyze                 # analisis estatico, sin tolerar avisos
melos run test                    # pruebas de todos los paquetes
melos run verify                  # deps-check + analyze + test (lo mismo que el CI)

flutter run -d chrome  --project-name regenta   # desde apps/regenta
flutter run -d android
flutter build web
flutter build apk
```

## Golden tests (HU-128)

Las pantallas con golden se comparan contra una imagen de referencia en las dos
composiciones de `design/pantallas/`: móvil (390×844) y escritorio (1440×900).
Hoy las tienen el POS y el selector de cliente (`packages/ventas`) y el listado
de clientes (`packages/clientes`). Las referencias viven junto al test, en
`test/goldens/referencias/`.

- **Dónde se comparan:** en Linux, con la misma versión de Flutter que CI. En
  Windows o macOS el texto se rasteriza distinto, así que ahí los goldens se
  saltan (`skip`), no se desactivan: CI los corre siempre.
- **Fuentes:** cada paquete con goldens carga Archivo, IBM Plex Mono y los
  íconos de Material en `test/flutter_test_config.dart`
  (`cargarFuentesDeRegenta`). Sin eso el golden se pinta con la fuente de
  pruebas y no dice nada de la tipografía.
- **Si un golden falla en CI**, la corrida sube el artefacto `goldens-fallidos`
  con la imagen esperada, la obtenida y la diferencia.

```bash
tool/goldens.sh              # compara, en el contenedor de Flutter para Linux
tool/goldens.sh actualizar   # regenera las referencias
```

**Cuándo es legítimo regenerar:** solo cuando la pantalla cambió a propósito
para acercarse al mockup de `design/pantallas/`, o porque el mockup cambió. Las
PNG nuevas viajan en el mismo PR, para que quien revisa vea qué se aprobó. Si el
golden falla y la pantalla no debía cambiar, se corrige el widget, no la
referencia.
