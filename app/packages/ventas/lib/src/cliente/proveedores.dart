import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_clientes_de_venta.dart';
import 'controlador_de_cliente.dart';
import 'estado_de_cliente.dart';

/// La app lo sobrescribe con un [RepositorioDeClientesHttp] real; los tests,
/// con un doble.
final repositorioDeClientesDeVentaProvider = Provider<RepositorioDeClientesDeVenta>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeClientesDeVentaProvider al montar el ProviderScope.',
  );
});

/// La venta que se está armando. La fija el POS al abrir el selector.
final ventaEnCursoProvider = Provider<String>((ref) {
  throw UnimplementedError('Sobrescribe ventaEnCursoProvider con la venta en curso.');
});

/// Los permisos de quien entró, tal como vienen en el token.
///
/// Por defecto vacío: sin saber qué puede, la pantalla no ofrece crear. Fallar
/// cerrado aquí es barato —se busca y se asigna igual— y fallar abierto sería
/// enseñar un botón que el backend va a rechazar con un 403.
final permisosDeLaSesionProvider = Provider<Set<String>>((ref) => const {});

/// HU-114 criterio 4.
final puedeCrearClientesProvider = Provider<bool>(
    (ref) => ref.watch(permisosDeLaSesionProvider).contains('CLIENTES_CLIENTE_CREAR'));

final controladorDeClienteProvider =
    StateNotifierProvider<ControladorDeCliente, EstadoDeCliente>(
  (ref) => ControladorDeCliente(
    ref.watch(repositorioDeClientesDeVentaProvider),
    ventaId: ref.watch(ventaEnCursoProvider),
  ),
);
