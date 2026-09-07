import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_clientes.dart';
import 'controlador_de_clientes.dart';
import 'estado_de_clientes.dart';

/// La app lo sobrescribe con un [RepositorioDeClientes] real (sobre el
/// `ClienteHttp` y la `BaseLocal` del núcleo); los tests, con un doble.
final repositorioDeClientesProvider = Provider<RepositorioDeClientes>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeClientesProvider al montar el ProviderScope.',
  );
});

final controladorDeClientesProvider =
    StateNotifierProvider<ControladorDeClientes, EstadoDeClientes>(
  (ref) => ControladorDeClientes(ref.watch(repositorioDeClientesProvider)),
);
