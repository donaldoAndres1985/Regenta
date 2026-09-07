import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/producto_buscado.dart';
import '../datos/repositorio_de_ventas.dart';
import 'estado_del_pos.dart';
import 'linea_de_carrito.dart';

/// El carrito y la búsqueda del POS. Al agregar un producto sin stock, la línea
/// entra igual pero marcada: el botón *Cobrar* queda deshabilitado con el aviso
/// "Hay líneas sin stock" hasta que se quite (HU-045, criterio 3).
class ControladorDelPos extends StateNotifier<EstadoDelPos> {
  ControladorDelPos(this._repo, {required String bodegaId, Duration? rebote})
      : _bodegaId = bodegaId,
        _rebote = rebote ?? const Duration(milliseconds: 300),
        super(const EstadoDelPos());

  final RepositorioDeVentas _repo;
  final String _bodegaId;
  final Duration _rebote;
  Timer? _temporizador;
  int _generacion = 0;

  void cambiarTermino(String termino) {
    state = state.copiar(termino: termino);
    _temporizador?.cancel();
    if (termino.trim().length < 3) {
      state = state.copiar(resultados: const [], buscando: false);
      return;
    }
    _temporizador = Timer(_rebote, _buscar);
  }

  Future<void> _buscar() async {
    final gen = ++_generacion;
    state = state.copiar(buscando: true);
    try {
      final resultados = await _repo.buscar(state.termino);
      if (gen != _generacion) return;
      state = state.copiar(resultados: resultados, buscando: false);
    } on ErrorDeApi catch (e) {
      if (gen != _generacion) return;
      state = state.copiar(buscando: false, mensaje: e.mensaje);
    }
  }

  void agregar(ProductoBuscado producto) {
    final indice = state.lineas.indexWhere((l) => l.producto.id == producto.id);
    final lineas = [...state.lineas];
    if (indice >= 0) {
      lineas[indice] = lineas[indice].conCantidad(lineas[indice].cantidad + 1);
    } else {
      lineas.add(LineaDeCarrito(producto: producto, cantidad: 1));
    }
    state = state.copiar(
      lineas: lineas,
      resultados: const [],
      termino: '',
      mensaje: producto.sinStock
          ? 'Agregaste "${producto.nombre}" sin stock: no se puede cobrar hasta quitarlo.'
          : null,
    );
  }

  Future<void> agregarPorCodigo(String codigo) async {
    try {
      final resuelto = await _repo.resolverCodigo(codigo);
      final producto = await _repo.verProducto(resuelto.productoId);
      agregar(producto);
    } on ErrorDeApi {
      state = state.copiar(mensaje: 'Ningún producto tiene el código «$codigo».');
    } on StateError {
      state = state.copiar(mensaje: 'Ningún producto tiene el código «$codigo».');
    }
  }

  void cambiarCantidad(int indice, int delta) {
    final lineas = [...state.lineas];
    final nueva = lineas[indice].cantidad + delta;
    if (nueva <= 0) {
      lineas.removeAt(indice);
    } else {
      lineas[indice] = lineas[indice].conCantidad(nueva);
    }
    state = state.copiar(lineas: lineas, mensaje: null);
  }

  void quitar(int indice) {
    final lineas = [...state.lineas]..removeAt(indice);
    state = state.copiar(lineas: lineas, mensaje: null);
  }

  void vaciar() => state = state.copiar(lineas: const [], mensaje: null);

  Future<void> cobrar() async {
    if (!state.puedeCobrar) return;
    state = state.copiar(cobrando: true, mensaje: null);
    try {
      final venta = await _repo.confirmarVenta(
        bodegaId: _bodegaId,
        lineas: [
          for (final l in state.lineas)
            LineaParaEnviar(
              productoId: l.producto.id,
              sku: l.producto.sku,
              nombre: l.producto.nombre,
              cantidad: l.cantidad,
              precioUnitario: l.producto.precioVenta,
              impuestoPct: l.producto.impuestoPct,
              costoUnitario: l.producto.costoUnitario,
            ),
        ],
      );
      state = const EstadoDelPos().copiar(ventaNumero: venta.numero);
    } on ErrorDeApi catch (e) {
      state = state.copiar(cobrando: false, mensaje: e.mensaje);
    }
  }

  @override
  void dispose() {
    _temporizador?.cancel();
    super.dispose();
  }
}
