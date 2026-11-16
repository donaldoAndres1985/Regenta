import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'controlador_de_reportes_de_caja.dart';
import 'repositorio_de_reportes_de_caja.dart';

final repositorioDeReportesDeCajaProvider =
    Provider<RepositorioDeReportesDeCaja>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeReportesDeCajaProvider al montar el ProviderScope.',
  );
});

/// HU-142 · reimprime la tirilla de cierre de una sesión. La conecta la app con el módulo
/// de caja; si no está conectada (`null`), el listado no ofrece reimprimir.
final reimprimirTirillaDeCajaProvider =
    Provider<Future<void> Function(String sesionId)?>((ref) => null);

final controladorDeReportesDeCajaProvider =
    ChangeNotifierProvider<ControladorDeReportesDeCaja>((ref) {
  final repo = ref.watch(repositorioDeReportesDeCajaProvider);
  return ControladorDeReportesDeCaja(repo);
});
