import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/cliente_de_la_venta.dart';
import '../datos/producto_buscado.dart';
import '../datos/repositorio_de_ventas.dart';
import 'estado_del_pos.dart';
import 'linea_de_carrito.dart';

/// El carrito y la búsqueda del POS. Al agregar un producto sin stock, la línea
/// entra igual pero marcada: el botón *Cobrar* queda deshabilitado con el aviso
/// "Hay líneas sin stock" hasta que se quite (HU-045, criterio 3).
class ControladorDelPos extends StateNotifier<EstadoDelPos> {
  ControladorDelPos(
    this._repo, {
    required String bodegaId,
    Duration? rebote,
    num? montoParaIdentificar,
  }) : _bodegaId = bodegaId,
       _rebote = rebote ?? const Duration(milliseconds: 250),
       _montoParaIdentificar = montoParaIdentificar,
       super(EstadoDelPos(montoParaIdentificar: montoParaIdentificar));

  final RepositorioDeVentas _repo;
  final String _bodegaId;
  final Duration _rebote;
  num? _montoParaIdentificar;
  Timer? _temporizador;
  int _generacion = 0;

  /// R12: las ventas que no se están mostrando, tal como quedaron.
  final List<EstadoDelPos?> _guardadas = List.filled(
    ventasAlAbrir,
    null,
    growable: true,
  );

  /// El término al que corresponden los resultados que hay en pantalla.
  String? _terminoDeResultados;

  EstadoDelPos _ventaNueva() =>
      EstadoDelPos(montoParaIdentificar: _montoParaIdentificar);

  List<int> _conteoProductos(int actual, int siguiente) {
    return [
      for (var k = 0; k < state.ventasAbiertas; k++)
        k == actual
            ? state.lineas.length
            : (k == siguiente ? 0 : state.productosPorVenta[k]),
    ];
  }

  List<num> _totales(int actual, int siguiente) {
    return [
      for (var k = 0; k < state.ventasAbiertas; k++)
        k == actual
            ? state.total
            : (k == siguiente ? 0 : (k < state.totalesPorVenta.length ? state.totalesPorVenta[k] : 0)),
    ];
  }

  List<String?> _clientes(int actual, int siguiente) {
    return [
      for (var k = 0; k < state.ventasAbiertas; k++)
        k == actual
            ? state.cliente?.nombre
            : (k == siguiente ? null : (k < state.clientesPorVenta.length ? state.clientesPorVenta[k] : null)),
    ];
  }

  /// R12: guarda la venta activa y muestra [venta]: la que había, o una en
  /// blanco si nunca se usó. La búsqueda no viaja entre ventas.
  void cambiarDeVenta(int venta) {
    if (venta == state.ventaActiva ||
        venta < 0 ||
        venta >= state.ventasAbiertas) {
      return;
    }
    _temporizador?.cancel();
    _generacion++;
    final actual = state.ventaActiva;
    _guardadas[actual] = state.copiar(
      termino: '',
      resultados: const [],
      buscando: false,
      mensaje: null,
      ventaNumero: null,
      ventaEnCola: false,
    );
    final conteo = _conteoProductos(actual, venta);
    final totales = _totales(actual, venta);
    final clientes = _clientes(actual, venta);

    final siguiente = _guardadas[venta] ?? _ventaNueva();
    _guardadas[venta] = null;
    state = siguiente.copiar(
      ventaActiva: venta,
      productosPorVenta: conteo,
      totalesPorVenta: totales,
      clientesPorVenta: clientes,
      montoParaIdentificar: _montoParaIdentificar,
    );
  }

  /// R12: abre otra venta (hasta [ventasMaximas]) y la deja activa.
  void nuevaVenta() {
    final n = state.ventasAbiertas;
    if (n >= ventasMaximas) return;
    _guardadas.add(null);
    state = state.copiar(
      productosPorVenta: [...state.productosPorVenta, 0],
      totalesPorVenta: [...state.totalesPorVenta, 0],
      clientesPorVenta: [...state.clientesPorVenta, null],
    );
    cambiarDeVenta(n);
  }

  /// Cierra una pestaña de venta. Si es la activa, cambia a la anterior o 0.
  void cerrarVenta(int indice) {
    if (state.ventasAbiertas <= 1 || indice < 0 || indice >= state.ventasAbiertas) return;
    _guardadas.removeAt(indice);
    final nuevoConteo = [...state.productosPorVenta]..removeAt(indice);
    final nuevosTotales = [...state.totalesPorVenta]..removeAt(indice);
    final nuevosClientes = [...state.clientesPorVenta]..removeAt(indice);

    if (indice == state.ventaActiva) {
      final nuevaActiva = (indice > 0 ? indice - 1 : 0).clamp(0, nuevoConteo.length - 1);
      final siguiente = _guardadas[nuevaActiva] ?? _ventaNueva();
      _guardadas[nuevaActiva] = null;
      state = siguiente.copiar(
        ventaActiva: nuevaActiva,
        productosPorVenta: nuevoConteo,
        totalesPorVenta: nuevosTotales,
        clientesPorVenta: nuevosClientes,
      );
    } else {
      final nuevaActiva = indice < state.ventaActiva ? state.ventaActiva - 1 : state.ventaActiva;
      state = state.copiar(
        ventaActiva: nuevaActiva,
        productosPorVenta: nuevoConteo,
        totalesPorVenta: nuevosTotales,
        clientesPorVenta: nuevosClientes,
      );
    }
  }

  /// R12: deja la venta activa guardada y pasa a una en blanco: la primera
  /// vacía que haya, o una nueva.
  void ponerEnEspera() {
    for (var k = 0; k < state.ventasAbiertas; k++) {
      if (k != state.ventaActiva && state.productosEnVenta(k) == 0) {
        cambiarDeVenta(k);
        return;
      }
    }
    nuevaVenta();
  }

  /// R7c: `Esc` cierra la lista sin borrar lo escrito.
  void cerrarDesplegable() => state = state.copiar(desplegableCerrado: true);

  static final _codigoNumerico = RegExp(r'^\d{6,}$');

  /// R7 y R7b: Enter en el buscador. Un número largo es un código (así
  /// escriben los lectores USB/Bluetooth); cualquier otro texto agrega el
  /// resultado [resaltado] —el primero, si nadie se movió con las flechas—.
  Future<void> confirmarBusqueda({int resaltado = 0}) async {
    final texto = state.termino.trim();
    if (texto.isEmpty) return;
    if (_codigoNumerico.hasMatch(texto)) {
      await _agregarPorCodigoExacto(texto);
      return;
    }
    if (texto.length < 3) {
      _noSeEncontro(texto);
      return;
    }
    // Enter antes del rebote: se busca ya, una sola vez.
    if (_temporizador?.isActive ?? false) {
      _temporizador!.cancel();
      await _buscar();
    } else if (state.buscando || _terminoDeResultados != state.termino) {
      await _buscar();
    }
    if (state.termino.trim() != texto) return;
    final resultados = state.resultados;
    if (resultados.isEmpty) {
      _noSeEncontro(texto);
      return;
    }
    agregar(resultados[resaltado.clamp(0, resultados.length - 1)]);
  }

  void _noSeEncontro(String texto, {bool cerrarDesplegable = false}) => state = state.copiar(
    mensaje: 'No se encontró «$texto»',
    buscando: false,
    desplegableCerrado: cerrarDesplegable,
  );

  /// R7: resuelve el código de barras (propio o alterno) y, si no hay, la
  /// referencia o el código exactos entre los resultados de la búsqueda.
  /// Nunca abre el desplegable.
  Future<void> _agregarPorCodigoExacto(String codigo) async {
    _temporizador?.cancel();
    _generacion++;
    state = state.copiar(
      resultados: const [],
      buscando: false,
      desplegableCerrado: true,
    );
    try {
      final resuelto = await _repo.resolverCodigo(codigo);
      final candidatos = await _repo.buscar(
        resuelto.sku.isNotEmpty ? resuelto.sku : codigo,
      );
      final producto =
          candidatos.where((p) => p.id == resuelto.productoId).firstOrNull ??
          await _repo.verProducto(resuelto.productoId);
      agregar(producto);
      return;
    } on ErrorDeRed catch (e) {
      state = state.copiar(mensaje: e.mensaje);
      return;
    } on ErrorDeApi {
      // Ningún código de barras: se prueba por la referencia exacta.
    } on StateError {
      // El código existe pero el producto no apareció: igual que no hallarlo.
    } catch (_) {}
    try {
      final candidatos = await _repo.buscar(codigo);
      final exacto = candidatos
          .where((p) => p.sku == codigo || p.codigoBarras == codigo)
          .firstOrNull;
      if (exacto != null) {
        agregar(exacto);
      } else {
        _noSeEncontro(codigo, cerrarDesplegable: true);
      }
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje, desplegableCerrado: true);
    } catch (_) {
      _noSeEncontro(codigo, cerrarDesplegable: true);
    }
  }

  void cambiarTermino(String termino) {
    state = state.copiar(termino: termino, desplegableCerrado: false);
    _temporizador?.cancel();
    if (termino.trim().length < 3) {
      state = state.copiar(resultados: const [], buscando: false);
      return;
    }
    _temporizador = Timer(_rebote, _buscar);
  }

  Future<void> _buscar() async {
    final gen = ++_generacion;
    state = state.copiar(buscando: true, desplegableCerrado: false);
    try {
      final brutos = await _repo.buscar(state.termino);
      if (gen != _generacion) return;
      final resultados = state.termino.trim().isEmpty
          ? brutos
          : brutos.where((p) => p.coincideCon(state.termino)).toList();
      _terminoDeResultados = state.termino;
      state = state.copiar(
        resultados: resultados,
        buscando: false,
        desplegableCerrado: false,
      );
    } catch (e) {
      if (gen != _generacion) return;
      state = state.copiar(
        buscando: false,
        resultados: const [],
        desplegableCerrado: false,
        mensaje: e is ErrorDeApi ? e.mensaje : null,
      );
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
    _temporizador?.cancel();
    _generacion++;
    _terminoDeResultados = null;
    state = state.copiar(
      lineas: lineas,
      resultados: const [],
      termino: '',
      buscando: false,
      desplegableCerrado: false,
      mensaje: producto.sinStock
          ? 'Agregaste "${producto.nombre}" sin stock: no se puede cobrar hasta quitarlo.'
          : null,
    );
  }

  /// Un código leído por cámara o escrito a mano: mismo camino que Enter (R7).
  Future<void> agregarPorCodigo(String codigo) =>
      _agregarPorCodigoExacto(codigo.trim());

  void cambiarCantidad(int indice, num delta) {
    if (indice < 0 || indice >= state.lineas.length) return;
    final lineas = [...state.lineas];
    final nueva = lineas[indice].cantidad + delta;
    if (nueva <= 0) {
      lineas.removeAt(indice);
    } else {
      lineas[indice] = lineas[indice].conCantidad(nueva);
    }
    state = state.copiar(lineas: lineas, mensaje: null);
  }

  void fijarCantidad(int indice, num cantidad) {
    if (indice < 0 || indice >= state.lineas.length) return;
    final lineas = [...state.lineas];
    if (cantidad <= 0) {
      lineas.removeAt(indice);
    } else {
      lineas[indice] = lineas[indice].conCantidad(cantidad);
    }
    state = state.copiar(lineas: lineas, mensaje: null);
  }

  /// Carga líneas desde una venta existente o cotización convertida (HU-044).
  void cargarLineasDesdeVenta(List<LineaDeVentaResumen> lineasVenta) {
    final nuevasLineas = <LineaDeCarrito>[];
    for (final l in lineasVenta) {
      final producto = ProductoBuscado(
        id: l.productoId,
        sku: l.sku,
        nombre: l.nombre,
        precioVenta: l.precioUnitario,
        existencia: l.cantidad,
        nivelStock: NivelStock.normal,
      );
      nuevasLineas.add(LineaDeCarrito(
        producto: producto,
        cantidad: l.cantidad,
      ));
    }
    state = state.copiar(
      lineas: nuevasLineas,
      mensaje: null,
    );
  }

  void quitar(int indice) {
    if (indice < 0 || indice >= state.lineas.length) return;
    final lineas = [...state.lineas]..removeAt(indice);
    state = state.copiar(lineas: lineas, mensaje: null);
  }

  void fijarNota(String? nota) {
    state = state.copiar(nota: nota);
  }

  void aplicarDescuentoGlobal({num porcentaje = 0, num valor = 0}) {
    state = state.copiar(
      descuentoGeneralPct: porcentaje,
      descuentoGeneralValor: valor,
    );
  }

  void aplicarDescuentoLinea(int indice, num porcentaje) {
    if (indice < 0 || indice >= state.lineas.length) return;
    final lineas = [...state.lineas];
    lineas[indice] = lineas[indice].conDescuento(porcentaje);
    state = state.copiar(lineas: lineas);
  }

  /// HU-137: el monto puede llegar después de abrir el POS.
  void fijarMontoParaIdentificar(num? monto) {
    _montoParaIdentificar = monto;
    state = state.copiar(montoParaIdentificar: monto);
  }

  /// La venta se cobró por el flujo real por pasos (pantalla de Cobro, no el
  /// `/offline` de [cobrar]): deja la pestaña activa en blanco y avisa el
  /// número, igual que hace [cobrar] al terminar.
  void vaciarTrasCobro(String numero) => state = _ventaNueva().copiar(
        ventaNumero: numero,
        ventaActiva: state.ventaActiva,
        productosPorVenta: state.productosPorVenta,
        totalesPorVenta: state.totalesPorVenta,
        clientesPorVenta: state.clientesPorVenta,
      );

  void vaciar() => state = state.copiar(
        lineas: const [],
        nota: null,
        descuentoGeneralPct: 0,
        descuentoGeneralValor: 0,
        mensaje: null,
      );

  /// HU-113: el selector devuelve a quién se le está vendiendo, o `null` si
  /// volvió a consumidor final.
  void fijarCliente(ClienteDeLaVenta? cliente) =>
      state = state.copiar(cliente: cliente);

  Future<void> cobrar() async {
    if (!state.puedeCobrar) return;
    state = state.copiar(cobrando: true, mensaje: null);
    try {
      final venta = await _repo.confirmarVenta(
        bodegaId: _bodegaId,
        lineas: state.lineasParaEnviar(),
        clienteId: state.cliente?.id,
      );
      // Sólo se vacía la venta cobrada; las otras siguen guardadas (R12).
      state = _ventaNueva().copiar(
        ventaNumero: venta.quedoEnLaCola ? null : venta.venta!.numero,
        ventaEnCola: venta.quedoEnLaCola,
        ventaActiva: state.ventaActiva,
        productosPorVenta: state.productosPorVenta,
        totalesPorVenta: state.totalesPorVenta,
        clientesPorVenta: state.clientesPorVenta,
      );
    } on ErrorDeApi catch (e) {
      state = state.copiar(cobrando: false, mensaje: e.mensaje);
    } catch (e) {
      state = state.copiar(cobrando: false, mensaje: e.toString());
    }
  }

  @override
  void dispose() {
    _temporizador?.cancel();
    super.dispose();
  }
}
