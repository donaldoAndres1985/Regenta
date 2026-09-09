import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_mesas.dart';
import 'controlador_del_plano.dart';
import 'estado_del_plano.dart';

/// La app lo sobrescribe con un [RepositorioDeMesas] real (sobre el
/// `ClienteHttp` del núcleo); los tests, con un doble.
final repositorioDeMesasProvider = Provider<RepositorioDeMesas>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeMesasProvider al montar el ProviderScope.',
  );
});

final controladorDelPlanoProvider =
    StateNotifierProvider<ControladorDelPlano, EstadoDelPlano>(
  (ref) => ControladorDelPlano(ref.watch(repositorioDeMesasProvider)),
);
