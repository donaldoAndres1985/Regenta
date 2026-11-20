import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_proveedores.dart';
import 'controlador_de_proveedores.dart';
import 'estado_de_proveedores.dart';

/// La app lo sobrescribe con un [RepositorioDeProveedores] real (sobre el `ClienteHttp` del
/// núcleo); los tests, con un doble.
final repositorioDeProveedoresProvider = Provider<RepositorioDeProveedores>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeProveedoresProvider al montar el ProviderScope.',
  );
});

final controladorDeProveedoresProvider =
    StateNotifierProvider.autoDispose<ControladorDeProveedores, EstadoDeProveedores>(
  (ref) => ControladorDeProveedores(ref.watch(repositorioDeProveedoresProvider)),
);
