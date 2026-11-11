import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_ventas.dart';
import '../pos/proveedores.dart';
import 'controlador_de_cobro.dart';
import 'estado_del_cobro.dart';

/// Lo que la pantalla de Cobro necesita para armar la venta: el carrito ya
/// congelado al momento de abrirla. Se construye una sola vez por apertura
/// (en `initState` de quien la abre), nunca dentro de `build`: la igualdad de
/// identidad es lo que hace que el `family` de abajo no reconstruya el
/// controlador en cada rebuild y pierda los pagos que ya se habían armado.
class ParametrosDeCobro {
  const ParametrosDeCobro({
    required this.total,
    required this.clienteAsignado,
    required this.bodegaId,
    required this.lineas,
    this.clienteId,
    this.notaCliente,
  });

  final num total;
  final bool clienteAsignado;
  final String bodegaId;
  final List<LineaParaEnviar> lineas;
  final String? clienteId;
  final String? notaCliente;
}

final controladorDeCobroProvider = StateNotifierProvider.autoDispose
    .family<ControladorDeCobro, EstadoDelCobro, ParametrosDeCobro>((ref, params) {
  return ControladorDeCobro(
    ref.watch(repositorioDeVentasProvider),
    total: params.total,
    clienteAsignado: params.clienteAsignado,
    bodegaId: params.bodegaId,
    clienteId: params.clienteId,
    notaCliente: params.notaCliente,
    lineas: params.lineas,
  );
});
