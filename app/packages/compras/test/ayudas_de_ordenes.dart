import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

/// Órdenes en memoria, con lo mínimo del backend de HU-173/174.
class RepoOrdenesFake implements RepositorioDeOrdenes {
  RepoOrdenesFake(List<OrdenDeCompraVista> iniciales) : ordenes = [...iniciales];

  final List<OrdenDeCompraVista> ordenes;
  final List<Map<String, dynamic>> creadas = [];
  final List<String> enviadas = [];
  final List<String> pdfsPedidos = [];
  bool exigeAprobacion = false;
  ErrorDeApi? falla;

  OrdenDeCompraVista _con(OrdenDeCompraVista o, {String? estado, bool? espera, bool? automatica}) =>
      OrdenDeCompraVista(
        id: o.id, numero: o.numero, proveedorId: o.proveedorId, proveedorNombre: o.proveedorNombre,
        bodegaId: o.bodegaId, estado: estado ?? o.estado, total: o.total, subtotal: o.subtotal,
        impuestoTotal: o.impuestoTotal, fechaEsperada: o.fechaEsperada, lineas: o.lineas,
        esperaAprobacion: espera ?? o.esperaAprobacion, aprobacionAutomatica: automatica ?? o.aprobacionAutomatica,
      );

  OrdenDeCompraVista _guardar(OrdenDeCompraVista o) {
    ordenes[ordenes.indexWhere((x) => x.id == o.id)] = o;
    return o;
  }

  @override
  Future<List<OrdenDeCompraVista>> listar() async {
    if (falla != null) throw falla!;
    return [...ordenes];
  }

  @override
  Future<OrdenDeCompraVista> ver(String id) async => ordenes.firstWhere((o) => o.id == id);

  @override
  Future<OrdenDeCompraVista> crear(Map<String, dynamic> orden) async {
    creadas.add(orden);
    final nueva = OrdenDeCompraVista(
      id: 'oc-nueva', numero: 'OC-9', proveedorId: orden['proveedorId'] as String, proveedorNombre: 'Andina',
      estado: 'BORRADOR', total: 0,
    );
    ordenes.add(nueva);
    return nueva;
  }

  @override
  Future<OrdenDeCompraVista> editarLineas(String id, List<Map<String, dynamic>> lineas) async => ver(id);

  @override
  Future<OrdenDeCompraVista> confirmar(String id) async {
    final o = await ver(id);
    return _guardar(exigeAprobacion ? _con(o, espera: true) : _con(o, estado: 'APROBADA', automatica: true));
  }

  @override
  Future<OrdenDeCompraVista> aprobar(String id) async {
    if (falla != null) throw falla!;
    return _guardar(_con(await ver(id), estado: 'APROBADA', espera: false));
  }

  @override
  Future<OrdenDeCompraVista> enviar(String id) async {
    enviadas.add(id);
    return _guardar(_con(await ver(id), estado: 'ENVIADA'));
  }

  final List<String> motivos = [];

  @override
  Future<OrdenDeCompraVista> anular(String id, String motivo) async {
    if (falla != null) throw falla!;
    motivos.add(motivo);
    final o = await ver(id);
    return _guardar(OrdenDeCompraVista(
      id: o.id, numero: o.numero, proveedorId: o.proveedorId, proveedorNombre: o.proveedorNombre,
      estado: 'ANULADA', total: o.total, lineas: o.lineas, motivoAnulacion: motivo, anuladaEn: DateTime(2026, 11, 21),
    ));
  }

  @override
  Future<OrdenDeCompraVista> cerrar(String id, String motivo) async {
    if (falla != null) throw falla!;
    motivos.add(motivo);
    final o = await ver(id);
    return _guardar(OrdenDeCompraVista(
      id: o.id, numero: o.numero, proveedorId: o.proveedorId, proveedorNombre: o.proveedorNombre,
      estado: 'CERRADA', total: o.total, lineas: o.lineas, motivoCierre: motivo, cerradaEn: DateTime(2026, 11, 21),
    ));
  }

  @override
  Future<List<int>> pdf(String id) async {
    pdfsPedidos.add(id);
    return [0x25, 0x50, 0x44, 0x46];
  }
}

class CompartidorFake implements CompartidorDePdf {
  final List<String> compartidos = [];

  @override
  Future<void> compartir(List<int> bytes, String nombreDeArchivo) async => compartidos.add(nombreDeArchivo);
}

OrdenDeCompraVista orden(String id, String numero, String estado,
        {String proveedorId = 'p-1', String proveedor = 'Andina', num total = 100000, bool espera = false,
        List<LineaDeOrdenVista> lineas = const []}) =>
    OrdenDeCompraVista(
      id: id, numero: numero, proveedorId: proveedorId, proveedorNombre: proveedor, estado: estado, total: total,
      fechaEsperada: DateTime(2026, 12, 1), esperaAprobacion: espera, lineas: lineas,
    );

Future<RepoOrdenesFake> montarCompras(WidgetTester tester, List<OrdenDeCompraVista> ordenes,
    {Size tamano = const Size(1440, 900), ValueChanged<String>? onRecibir, CompartidorFake? compartidor,
    VoidCallback? onProveedores, VoidCallback? onCompraDirecta, VoidCallback? onCuentasPorPagar,
    VoidCallback? onConfiguracion}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = RepoOrdenesFake(ordenes);
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeOrdenesProvider.overrideWithValue(repo),
      compartidorDePdfProvider.overrideWithValue(compartidor ?? CompartidorFake()),
    ],
    child: MaterialApp(
      home: PantallaCompras(
        onOrdenSeleccionada: onRecibir ?? (_) {},
        onProveedores: onProveedores,
        onCompraDirecta: onCompraDirecta,
        onCuentasPorPagar: onCuentasPorPagar,
        onConfiguracion: onConfiguracion,
      ),
    ),
  ));
  await tester.pumpAndSettle();
  return repo;
}

