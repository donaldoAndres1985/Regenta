import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_inventario.dart';
import '../escaner/escaner_de_codigos.dart';
import '../escaner/escaner_mobile_scanner.dart';
import 'controlador_de_busqueda.dart';
import 'estado_de_busqueda.dart';

/// El repositorio contra `servicio-inventario`. La app lo sobrescribe al armar
/// el `ProviderScope` con un [RepositorioDeInventario] real (que envuelve el
/// `ClienteHttp` del núcleo); los tests, con un doble.
final repositorioDeInventarioProvider = Provider<RepositorioDeInventario>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeInventarioProvider con un RepositorioDeInventario '
    'real al montar el ProviderScope.',
  );
});

/// El escáner de códigos. Por defecto el real (Android); en la web `disponible`
/// es false y el módulo cae al ingreso manual. Los tests lo sobrescriben.
/// HU-150 C4 · si quien usa la app puede editar productos (`INVENTARIO_PRODUCTO_EDITAR`).
/// La app lo cablea desde los permisos del token; el backend lo vuelve a validar (403).
final puedeEditarProductosProvider = Provider<bool>((ref) => false);

final escanerProvider =Provider<EscanerDeCodigos>((ref) => const EscanerMobileScanner());

final controladorDeBusquedaProvider =
    StateNotifierProvider<ControladorDeBusqueda, EstadoDeBusqueda>(
  (ref) => ControladorDeBusqueda(ref.watch(repositorioDeInventarioProvider)),
);
