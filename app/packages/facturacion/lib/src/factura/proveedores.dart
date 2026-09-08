import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_facturas.dart';
import 'controlador_de_factura.dart';
import 'estado_de_factura.dart';

/// La app lo sobrescribe con un [RepositorioDeFacturas] real (sobre el
/// `ClienteHttp` del núcleo); los tests, con un doble.
final repositorioDeFacturasProvider = Provider<RepositorioDeFacturas>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeFacturasProvider al montar el ProviderScope.',
  );
});

/// Una pantalla de factura por id.
final controladorDeFacturaProvider = StateNotifierProvider.family<ControladorDeFactura,
    EstadoDeFactura, String>(
  (ref, facturaId) =>
      ControladorDeFactura(ref.watch(repositorioDeFacturasProvider), facturaId: facturaId),
);
