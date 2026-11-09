import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'controlador_de_caja.dart';
import 'datos/repositorio_de_caja.dart';
import 'estado_de_caja.dart';

/// Inyectado por la app (`dependencias.dart`) o dobles de test.
final repositorioDeCajaProvider = Provider<RepositorioDeCaja>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeCajaProvider al montar el ProviderScope.',
  );
});

final controladorDeCajaProvider =
    StateNotifierProvider<ControladorDeCaja, EstadoDeCaja>((ref) {
  final repo = ref.watch(repositorioDeCajaProvider);
  return ControladorDeCaja(repo);
});
