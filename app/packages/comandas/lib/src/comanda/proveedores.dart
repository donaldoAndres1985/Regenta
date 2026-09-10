import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_comandas.dart';
import 'controlador_de_comanda.dart';
import 'estado_de_comanda.dart';

/// La app lo sobrescribe con un [RepositorioDeComandas] real (sobre el
/// `ClienteHttp` del núcleo); los tests, con un doble.
final repositorioDeComandasProvider = Provider<RepositorioDeComandas>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeComandasProvider al montar el ProviderScope.',
  );
});

/// Una comanda por id.
final controladorDeComandaProvider = StateNotifierProvider.family<ControladorDeComanda,
    EstadoDeComanda, String>(
  (ref, comandaId) =>
      ControladorDeComanda(ref.watch(repositorioDeComandasProvider), comandaId),
);
