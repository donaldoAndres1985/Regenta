import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_cocina.dart';
import 'controlador_de_cocina.dart';
import 'estado_de_cocina.dart';

/// La app lo sobrescribe con un [RepositorioDeCocina] real (sobre el
/// `ClienteHttp` del núcleo); los tests, con un doble.
final repositorioDeCocinaProvider = Provider<RepositorioDeCocina>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeCocinaProvider al montar el ProviderScope.',
  );
});

final controladorDeCocinaProvider =
    StateNotifierProvider<ControladorDeCocina, EstadoDeCocina>(
  (ref) => ControladorDeCocina(ref.watch(repositorioDeCocinaProvider)),
);
