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

/// Cada cuánto el plano se refresca solo (HU-084 criterio 2). `Duration.zero`
/// lo apaga; los tests lo bajan a milisegundos o lo apagan.
final intervaloRefrescoDelPlanoProvider = Provider<Duration>((ref) {
  return const Duration(seconds: 15);
});

final controladorDelPlanoProvider =
    StateNotifierProvider<ControladorDelPlano, EstadoDelPlano>(
  (ref) => ControladorDelPlano(
    ref.watch(repositorioDeMesasProvider),
    intervaloRefresco: ref.watch(intervaloRefrescoDelPlanoProvider),
  ),
);
