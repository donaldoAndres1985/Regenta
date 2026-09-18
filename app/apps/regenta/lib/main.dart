import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import 'dart:async';

import 'package:workmanager/workmanager.dart';

import 'src/arranque/dependencias.dart';
import 'src/arranque/sincronizacion.dart';
import 'src/navegacion/catalogo.dart';
import 'src/ui/pantalla_inicio.dart';
import 'src/ui/pantalla_login.dart';

/// Dónde vive el gateway. Se fija al compilar:
/// `flutter build web --dart-define=REGENTA_GATEWAY=https://api.midominio.co`.
const String urlDelGateway =
    String.fromEnvironment('REGENTA_GATEWAY', defaultValue: 'http://localhost:8080');

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final dependencias = DependenciasDeLaApp.crear(urlDelGateway: urlDelGateway);
  // HU-119 criterio 6: si había sesión guardada, se restaura antes del primer
  // frame y la pantalla de entrada no llega a verse.
  await dependencias.motor.iniciar();

  if (hayTrabajoEnSegundoPlanoAqui) {
    await Workmanager().initialize(despachadorDeTareas);
    dependencias.motor.addListener(() => _seguirLaSesion(dependencias));
    _seguirLaSesion(dependencias);
  }

  // HU-120 criterio 4: en Web no hay segundo plano, así que la cola se procesa
  // al abrir. En Android también conviene: es el momento en que más
  // probablemente hay red y alguien mirando.
  unawaited(subirLoPendiente(dependencias));

  runApp(ProviderScope(
    overrides: dependencias.overrides(),
    child: RegentaApp(dependencias: dependencias),
  ));
}

/// Con sesión, la tarea periódica corre; sin ella, no tiene sentido seguir
/// intentando subir la cola de alguien que ya no está (HU-120 criterio 5).
void _seguirLaSesion(DependenciasDeLaApp dependencias) {
  if (dependencias.motor.sesion == null) {
    unawaited(cancelarSincronizacionEnSegundoPlano());
  } else {
    unawaited(registrarSincronizacionEnSegundoPlano());
  }
}

/// La carcasa. HU-119.
///
/// Arma el enrutador con las rutas de todos los módulos y deja que la guardia
/// del núcleo decida qué se puede abrir: el plan y el patrón del negocio salen
/// del token, no de una preferencia local.
class RegentaApp extends StatefulWidget {
  const RegentaApp({super.key, required this.dependencias});

  final DependenciasDeLaApp dependencias;

  @override
  State<RegentaApp> createState() => _RegentaAppState();
}

class _RegentaAppState extends State<RegentaApp> {
  late final GoRouter _enrutador = crearEnrutador(
    rutas: rutasDeLaApp,
    perfil: widget.dependencias.perfil,
    tambienEscuchar: widget.dependencias.motor,
    login: (context, estado) => const PantallaLogin(),
    inicio: (context, estado) => const PantallaInicio(),
  );

  @override
  Widget build(BuildContext context) {
    final patron = _patronDe(widget.dependencias.perfil);
    return MaterialApp.router(
      title: 'Regenta',
      debugShowCheckedModeBanner: false,
      theme: regentaTheme(patron),
      routerConfig: _enrutador,
    );
  }

  /// El color secundario sale del patrón operativo del negocio. Sin sesión
  /// todavía, el de Venta directa: es el más común y la pantalla de entrada no
  /// muestra nada que dependa de él.
  static PatronOperativo _patronDe(PerfilDeSesion perfil) {
    final claim = perfil.claims?.patron;
    if (claim == null || claim.isEmpty) return PatronOperativo.ventaDirecta;
    try {
      return PatronOperativo.desdeClaim(claim);
    } on ArgumentError {
      return PatronOperativo.ventaDirecta;
    }
  }
}
