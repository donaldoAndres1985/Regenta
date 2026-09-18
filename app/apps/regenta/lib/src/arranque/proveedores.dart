import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import 'dependencias.dart';

/// Los providers de la carcasa (HU-119).
///
/// El núcleo no conoce Riverpod a propósito —es una librería, no una app— así
/// que el puente entre sus `ChangeNotifier` y los widgets se arma aquí.

/// Lo arma [DependenciasDeLaApp] al montar el `ProviderScope`.
final dependenciasProvider = Provider<DependenciasDeLaApp>((ref) {
  throw UnimplementedError('La app monta el ProviderScope con sus dependencias.');
});

final motorDeSesionProvider = Provider<MotorDeSesion>((ref) {
  throw UnimplementedError('Sobrescribe motorDeSesionProvider al montar el ProviderScope.');
});

final perfilDeSesionProvider = Provider<PerfilDeSesion>((ref) {
  throw UnimplementedError('Sobrescribe perfilDeSesionProvider al montar el ProviderScope.');
});

final clienteHttpProvider = Provider<ClienteHttp>((ref) {
  throw UnimplementedError('Sobrescribe clienteHttpProvider al montar el ProviderScope.');
});

final baseLocalProvider = Provider<BaseLocal>((ref) {
  throw UnimplementedError('Sobrescribe baseLocalProvider al montar el ProviderScope.');
});

/// El perfil como notificador, para que las pantallas se reconstruyan cuando
/// cambia la sesión. Es el único uso de la API legada de Riverpod, y está
/// acotado a este archivo.
final perfilComoNotifierProvider =
    ChangeNotifierProvider<PerfilDeSesion>((ref) => ref.watch(perfilDeSesionProvider));

/// El motor como notificador: la pantalla de entrada necesita saber por qué se
/// cerró la sesión, y eso el enrutador no lo propaga.
final motorComoNotifierProvider =
    ChangeNotifierProvider<MotorDeSesion>((ref) => ref.watch(motorDeSesionProvider));

/// La bodega desde la que se vende.
///
/// Debería salir de la sucursal activa de la sesión, y todavía no viaja en el
/// token: `Sesion` trae negocio, plan, patrón, roles y módulos, pero no
/// sucursal ni bodega. Mientras tanto, el negocio: es lo que ya hace el backend
/// cuando la venta no dice otra cosa.
final bodegaActivaProvider = Provider<String>(
    (ref) => ref.watch(perfilComoNotifierProvider).claims?.negocioId ?? '');

/// La venta que se está armando en el POS.
///
/// El POS no crea un borrador en el servidor: arma el carrito en el celular y
/// manda la venta entera al cobrar, que es lo que permite vender sin señal
/// (HU-043). Así que mientras se está vendiendo **no hay id de venta todavía**,
/// y el selector de cliente guarda a quién se le vende en el estado del POS en
/// vez de llamar a `PUT /api/ventas/{id}/cliente`.
const String sinVentaTodavia = '';

final ventaDelPosProvider = Provider<String>((ref) => sinVentaTodavia);
