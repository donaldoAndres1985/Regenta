import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/configuracion_negocio_vista.dart';
import '../datos/repositorio_de_config_de_caja.dart';
import '../datos/repositorio_de_usuarios.dart';

final repositorioDeUsuariosProvider = Provider<RepositorioDeUsuarios>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeUsuariosProvider al montar el ProviderScope.',
  );
});

/// HU-139 · configuración de caja (métodos que no se cuadran al cerrar).
final repositorioDeConfigDeCajaProvider = Provider<RepositorioDeConfigDeCaja>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeConfigDeCajaProvider al montar el ProviderScope.',
  );
});

final configuracionNegocioProvider =
    FutureProvider.autoDispose<ConfiguracionNegocioVista>((ref) {
  final repo = ref.watch(repositorioDeUsuariosProvider);
  return repo.verConfiguracion();
});
