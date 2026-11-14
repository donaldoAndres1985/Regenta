import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'controlador_de_reportes_de_caja.dart';
import 'repositorio_de_reportes_de_caja.dart';

final repositorioDeReportesDeCajaProvider =
    Provider<RepositorioDeReportesDeCaja>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeReportesDeCajaProvider al montar el ProviderScope.',
  );
});

final controladorDeReportesDeCajaProvider =
    ChangeNotifierProvider<ControladorDeReportesDeCaja>((ref) {
  final repo = ref.watch(repositorioDeReportesDeCajaProvider);
  return ControladorDeReportesDeCaja(repo);
});
