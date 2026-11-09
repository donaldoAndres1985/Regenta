import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/configuracion_negocio_vista.dart';
import '../datos/repositorio_de_usuarios.dart';

final repositorioDeUsuariosProvider = Provider<RepositorioDeUsuarios>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeUsuariosProvider al montar el ProviderScope.',
  );
});

final configuracionNegocioProvider =
    FutureProvider.autoDispose<ConfiguracionNegocioVista>((ref) {
  final repo = ref.watch(repositorioDeUsuariosProvider);
  return repo.verConfiguracion();
});
