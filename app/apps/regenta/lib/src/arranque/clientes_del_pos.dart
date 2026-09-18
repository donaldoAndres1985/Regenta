import 'package:regenta_ventas/regenta_ventas.dart';

/// El selector de cliente, atado a cómo vende de verdad el POS.
///
/// `PUT /api/ventas/{id}/cliente` (HU-113) supone una venta en borrador en el
/// servidor. El POS no crea ninguna: arma el carrito en el celular y manda la
/// venta entera al cobrar, que es lo que permite vender sin señal (HU-043).
///
/// Así que mientras se está vendiendo no hay a qué venta asignarle nada: el
/// cliente elegido se guarda en el estado del POS y viaja con la venta. Esta
/// clase es el sitio donde esa diferencia queda escrita, en vez de dejar que la
/// pantalla llame a una ruta con el id vacío.
class ClientesDelPos implements RepositorioDeClientesDeVenta {
  ClientesDelPos(this._real);

  final RepositorioDeClientesDeVenta _real;

  static bool esVentaDelServidor(String ventaId) => ventaId.trim().isNotEmpty;

  @override
  Future<List<ClienteDeLaVenta>> buscar(String termino) => _real.buscar(termino);

  @override
  Future<ResultadoDeAlta> crear(ClienteNuevo nuevo) => _real.crear(nuevo);

  @override
  Future<void> asignar({required String ventaId, required String clienteId}) async {
    if (!esVentaDelServidor(ventaId)) return;
    await _real.asignar(ventaId: ventaId, clienteId: clienteId);
  }

  @override
  Future<void> quitar({required String ventaId}) async {
    if (!esVentaDelServidor(ventaId)) return;
    await _real.quitar(ventaId: ventaId);
  }
}
