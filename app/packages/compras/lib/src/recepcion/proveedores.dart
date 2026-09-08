import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_recepciones.dart';
import '../escaner/escaner_de_codigos.dart';
import '../escaner/escaner_mobile_scanner.dart';
import 'controlador_de_recepcion.dart';
import 'estado_de_recepcion.dart';

/// La app lo sobrescribe con un [RepositorioDeRecepciones] real (sobre el
/// `ClienteHttp` del núcleo); los tests, con un doble.
final repositorioDeRecepcionesProvider = Provider<RepositorioDeRecepciones>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeRecepcionesProvider al montar el ProviderScope.',
  );
});

final escanerDeRecepcionProvider =
    Provider<EscanerDeCodigos>((ref) => const EscanerMobileScanner());

final controladorDeRecepcionProvider =
    StateNotifierProvider<ControladorDeRecepcion, EstadoDeRecepcion>(
  (ref) => ControladorDeRecepcion(ref.watch(repositorioDeRecepcionesProvider)),
);
