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

/// La cola de salida de este dispositivo, para mirarla sin tocarla.
final trabajadorDeSincronizacionProvider = Provider<TrabajadorDeSincronizacion>((ref) {
  final dependencias = ref.watch(dependenciasProvider);
  return TrabajadorDeSincronizacion(dio: dependencias.dio, db: dependencias.base);
});

/// Cuántas operaciones faltan por subir y cuántas chocaron (HU-120 criterio 3).
///
/// Una lectura puntual y no el `observar()` reactivo del trabajador: ese
/// devuelve un stream de Drift que no termina nunca, y una pantalla que lo
/// escucha no deja de reconstruirse —en un test, `pumpAndSettle` no vuelve—.
/// Inicio es una pantalla que se abre, no un tablero en vivo; cuando exista la
/// pantalla de sincronización, ahí sí vale la pena el stream.
final resumenDeColaProvider = FutureProvider<ResumenDeCola>(
    (ref) => ref.watch(trabajadorDeSincronizacionProvider).resumen());

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
