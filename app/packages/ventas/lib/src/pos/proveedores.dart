import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_ventas.dart';
import '../escaner/escaner_de_codigos.dart';
import '../escaner/escaner_mobile_scanner.dart';
import 'controlador_del_pos.dart';
import 'estado_del_pos.dart';

/// La app lo sobrescribe con un [RepositorioDeVentas] real (sobre el
/// `ClienteHttp` del núcleo); los tests, con un doble.
final repositorioDeVentasProvider = Provider<RepositorioDeVentas>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeVentasProvider al montar el ProviderScope.',
  );
});

/// La bodega desde la que se vende. La fija el shell (sucursal / caja).
final bodegaDeVentaProvider = Provider<String>((ref) {
  throw UnimplementedError('Sobrescribe bodegaDeVentaProvider con la bodega activa.');
});

final escanerProvider = Provider<EscanerDeCodigos>((ref) => const EscanerMobileScanner());

final controladorDelPosProvider =
    StateNotifierProvider<ControladorDelPos, EstadoDelPos>(
  (ref) => ControladorDelPos(
    ref.watch(repositorioDeVentasProvider),
    bodegaId: ref.watch(bodegaDeVentaProvider),
  ),
);
