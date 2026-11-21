import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/datos/producto_del_proveedor.dart';
import 'datos.dart';
import 'estado.dart';

/// La app lo sobrescribe con un [RepositorioDeCompraDirecta] real; los tests, con un doble.
/// HU-180 C4 · Si quien recibe puede crear productos (`INVENTARIO_PRODUCTO_CREAR`). La app lo cablea desde
/// los permisos del token; el backend lo vuelve a validar (403).
final puedeCrearProductosProvider = Provider<bool>((ref) => false);

final repositorioDeCompraDirectaProvider = Provider<RepositorioDeCompraDirecta>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeCompraDirectaProvider al montar el ProviderScope.',
  );
});

final controladorDeCompraDirectaProvider =
    StateNotifierProvider.autoDispose<ControladorDeCompraDirecta, EstadoDeCompraDirecta>(
  (ref) => ControladorDeCompraDirecta(ref.watch(repositorioDeCompraDirectaProvider)),
);

/// Maneja la compra directa (HU-167): elegir proveedor y bodega, escribir la factura, agregar
/// productos y registrarla. Los totales se calculan aquí igual que en el backend, pero lo que vale
/// es lo que el servidor confirma.
class ControladorDeCompraDirecta extends StateNotifier<EstadoDeCompraDirecta> {
  ControladorDeCompraDirecta(this._repo) : super(const EstadoDeCompraDirecta()) {
    cargar();
  }

  final RepositorioDeCompraDirecta _repo;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, error: null);
    try {
      final proveedores = (await _repo.proveedoresActivos()).where((p) => p.activo).toList();
      final bodegas = await _repo.bodegas();
      final porDefecto = bodegas.where((b) => b.esDefault).firstOrNull ?? bodegas.firstOrNull;
      final cajaAbierta = await _repo.tengoCajaAbierta();
      state = state.copiar(
        proveedores: proveedores,
        bodegas: bodegas,
        bodegaId: state.bodegaId ?? porDefecto?.id,
        cajaAbierta: cajaAbierta,
        desdeLaCaja: cajaAbierta,
        cargando: false,
      );
    } on ErrorDeRed {
      state = state.copiar(cargando: false, error: 'No hay conexión: no se pudo preparar la compra.');
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(cargando: false, error: fallo.mensaje);
    }
  }

  /// Cambiar de proveedor descarta las líneas: los costos sugeridos son de cada proveedor.
  void elegirProveedor(String? id) {
    if (id == state.proveedorId) return;
    // Un borrador que quedó de un intento fallido no cambia de proveedor: se empieza otra compra.
    // HU-169 · Un proveedor sin días de crédito se le compra de contado; con días, a crédito.
    final proveedor = state.proveedores.where((p) => p.id == id).firstOrNull;
    state = state.copiar(
      proveedorId: id,
      lineas: const [],
      resultados: const [],
      error: null,
      borradorId: null,
      condicion: proveedor != null && proveedor.diasCredito == 0
          ? CondicionDePago.contado
          : CondicionDePago.credito,
    );
  }

  void elegirBodega(String? id) {
    if (id == state.bodegaId) return;
    state = state.copiar(bodegaId: id, borradorId: null);
  }
  void cambiarFactura(String numero) => state = state.copiar(factura: numero);
  void cambiarFecha(DateTime? fecha) => state = state.copiar(fechaFactura: fecha);
  void cambiarFlete(num flete) => state = state.copiar(flete: flete < 0 ? 0 : flete);
  void cambiarCondicion(CondicionDePago condicion) => state = state.copiar(condicion: condicion);
  void cambiarMetodo(MetodoDePagoDeCompra metodo) => state = state.copiar(metodo: metodo);
  void cambiarDesdeLaCaja(bool desdeLaCaja) => state = state.copiar(desdeLaCaja: desdeLaCaja);
  void cambiarReferenciaPago(String referencia) => state = state.copiar(referenciaPago: referencia);

  void cambiarDescuentoGlobal(num descuento) =>
      state = state.copiar(descuentoGlobal: descuento < 0 ? 0 : descuento);

  Future<void> buscar(String termino) async {
    if (state.proveedorId == null) {
      state = state.copiar(error: 'Elige primero el proveedor.');
      return;
    }
    if (termino.trim().length < 3) {
      state = state.copiar(error: 'Escribe al menos tres letras del nombre, el SKU o el código.');
      return;
    }
    state = state.copiar(buscando: true, error: null, sinResultados: null);
    try {
      final encontrados = await _repo.buscarProductos(termino);
      state = state.copiar(
        buscando: false,
        resultados: encontrados,
        sinResultados: encontrados.isEmpty ? termino.trim() : null,
        error: encontrados.isEmpty ? 'No hay productos con «${termino.trim()}».' : null,
      );
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(buscando: false, error: _motivo(fallo));
    }
  }

  /// Agrega un producto con la tarifa del catálogo y el costo sugerido por el proveedor (C3).
  Future<void> agregar(ProductoEncontrado encontrado) async {
    final proveedorId = state.proveedorId;
    if (proveedorId == null) return;
    if (state.lineas.any((l) => l.producto.productoId == encontrado.id)) {
      state = state.copiar(resultados: const [], error: '«${encontrado.nombre}» ya está en la factura.');
      return;
    }
    try {
      final producto = await _repo.paraComprar(proveedorId, encontrado.id);
      state = state.copiar(
        resultados: const [],
        error: null,
        lineas: [
          ...state.lineas,
          LineaEnEdicion(
            producto: producto,
            costo: producto.costoSugerido,
            codigoProveedor: producto.codigoProveedor,
          ),
        ],
      );
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(error: _motivo(fallo));
    }
  }

  /// HU-180 C3 · El producto recién creado entra a la factura con su costo y su código del proveedor.
  void agregarCreado(ProductoParaComprar producto) {
    state = state.copiar(
      resultados: const [],
      sinResultados: null,
      error: null,
      lineas: [
        ...state.lineas,
        LineaEnEdicion(producto: producto, costo: producto.costoSugerido, codigoProveedor: producto.codigoProveedor),
      ],
    );
  }

  void editarLinea(int i, LineaEnEdicion linea) {
    final lineas = [...state.lineas]..[i] = linea;
    state = state.copiar(lineas: lineas);
  }

  void quitarLinea(int i) {
    final lineas = [...state.lineas]..removeAt(i);
    state = state.copiar(lineas: lineas);
  }

  /// Registra y confirma la compra. Devuelve lo que quedó registrado, o `null` si no se pudo; el
  /// motivo queda en `error` y lo escrito se conserva para corregirlo.
  Future<CompraRegistrada?> registrar() async {
    final falta = state.falta;
    if (falta != null) {
      state = state.copiar(error: falta);
      return null;
    }
    state = state.copiar(guardando: true, error: null);
    try {
      final hecha = await _repo.registrar(state.aJson(), borradorId: state.borradorId);
      state = EstadoDeCompraDirecta(
        proveedores: state.proveedores,
        bodegas: state.bodegas,
        bodegaId: state.bodegaId,
        cajaAbierta: state.cajaAbierta,
        desdeLaCaja: state.cajaAbierta,
      );
      return hecha;
    } on ConfirmacionFallida catch (fallo) {
      state = state.copiar(guardando: false, borradorId: fallo.borradorId, error: _motivo(fallo.causa));
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(guardando: false, error: _motivo(fallo));
    }
    return null;
  }

  static String _motivo(ErrorDeApi fallo) {
    if (fallo is ErrorDeRed) return 'No hay conexión: la compra no se registró. Intenta de nuevo.';
    if (fallo is ErroresDeValidacion) {
      for (final campo in fallo.porCampo.entries) {
        if (campo.value.isNotEmpty) return '${campo.key}: ${campo.value.first}';
      }
    }
    return fallo.mensaje;
  }
}
