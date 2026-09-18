import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import 'src/arranque/dependencias.dart';
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
  // Criterio 6: si había sesión guardada, se restaura antes del primer frame y
  // la pantalla de entrada no llega a verse.
  await dependencias.motor.iniciar();

  runApp(ProviderScope(
    overrides: dependencias.overrides(),
    child: RegentaApp(dependencias: dependencias),
  ));
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
