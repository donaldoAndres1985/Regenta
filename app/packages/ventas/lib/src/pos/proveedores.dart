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

/// HU-137: lo que el negocio configuró, preguntado una vez a Ventas.
final reglasDeCobroProvider = FutureProvider<num?>(
    (ref) => ref.watch(repositorioDeVentasProvider).montoParaIdentificarComprador());

/// HU-137: sobre este total la venta no se cobra a consumidor final. `null`
/// —lo de siempre— es que no se exige nada: también mientras la respuesta no
/// llega o si no hay señal. El backend lo vuelve a exigir al cobrar en línea.
final montoParaIdentificarCompradorProvider =
    Provider<num?>((ref) => ref.watch(reglasDeCobroProvider).valueOrNull);

final controladorDelPosProvider =
    StateNotifierProvider<ControladorDelPos, EstadoDelPos>((ref) {
  final controlador = ControladorDelPos(
    ref.watch(repositorioDeVentasProvider),
    bodegaId: ref.watch(bodegaDeVentaProvider),
    montoParaIdentificar: ref.read(montoParaIdentificarCompradorProvider),
  );
  // Se escucha y no se observa: si el monto llega con el carrito ya armado,
  // reconstruir el controlador vaciaría la venta en curso.
  ref.listen<num?>(montoParaIdentificarCompradorProvider,
      (_, monto) => controlador.fijarMontoParaIdentificar(monto));
  return controlador;
});
