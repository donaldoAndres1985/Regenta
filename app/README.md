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
