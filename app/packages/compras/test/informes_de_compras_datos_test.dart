import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

/// Responde como `InformesDeComprasControlador` y anota lo que se le pidió.
class _Backend implements HttpClientAdapter {
  final List<Uri> pedidos = [];

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream, Future<void>? cancelFuture) async {
    pedidos.add(options.uri);
    final ruta = options.uri.path;
    Object cuerpo = const [];
    if (ruta.endsWith('/por-proveedor')) {
      cuerpo = [
        {
          'proveedorId': 'prov-1',
          'proveedor': 'Andina',
          'recepciones': 2,
          'directas': {'base': 200000.00, 'iva': 38000.00, 'total': 248000.00},
          'conOrden': {'base': 100000.00, 'iva': 19000.00, 'total': 119000.00},
          'devoluciones': {'base': 10000.00, 'iva': 1900.00, 'total': 11900.00},
          'neto': {'base': 290000.00, 'iva': 55100.00, 'total': 355100.00},
        },
      ];
    } else if (ruta.endsWith('/por-producto')) {
      cuerpo = [
        {'productoId': 'p-1', 'producto': 'Acetaminofén', 'cantidad': 19, 'costoTotal': 194500.00, 'costoPromedio': 10236.84},
        {'productoId': 'p-2', 'producto': 'Devuelto todo', 'cantidad': 0, 'costoTotal': 0.00, 'costoPromedio': null},
      ];
    } else if (ruta.endsWith('/historial-de-costos')) {
      cuerpo = [
        {
          'fecha': '2026-11-20T10:15:00-05:00',
          'proveedorId': 'prov-1',
          'proveedor': 'Andina',
          'recepcionId': 'r-1',
          'recepcion': 'REC-1',
          'conOrden': false,
          'cantidad': 10,
          'costoEntrada': 10500.00,
        },
      ];
    } else if (ruta.endsWith('/archivo')) {
      return ResponseBody.fromBytes(const [37, 80, 68, 70], 200, headers: {
        Headers.contentTypeHeader: ['application/pdf'],
      });
    }
    return ResponseBody.fromString(jsonEncode(cuerpo), 200, headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    });
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  late _Backend backend;
  late RepositorioDeInformesDeCompras repo;
  final noviembre = PeriodoDeInforme(DateTime(2026, 11), DateTime(2026, 11, 23));

  setUp(() {
    backend = _Backend();
    final dio = Dio(BaseOptions(baseUrl: 'https://gw.test'))..httpClientAdapter = backend;
    repo = RepositorioDeInformesDeCompras(ClienteHttp(dio));
  });

  test('C1 · Por proveedor pide el periodo y lee directas, con orden, devoluciones y neto', () async {
    final [c] = await repo.porProveedor(noviembre);

    expect(backend.pedidos.single.path, '/api/compras/informes/por-proveedor');
    expect(backend.pedidos.single.queryParameters, {'desde': '2026-11-01', 'hasta': '2026-11-23'});
    expect(c.proveedor, 'Andina');
    expect(c.recepciones, 2);
    expect(c.directas.total, 248000);
    expect(c.conOrden.iva, 19000);
    expect(c.devoluciones.total, 11900);
    expect(c.neto.base, 290000);
  });

  test('C2 · Por producto, con el promedio vacío cuando todo se devolvió', () async {
    final productos = await repo.porProducto(noviembre);

    expect(productos.first.costoPromedio, 10236.84);
    expect(productos.last.costoPromedio, isNull);
  });

  test('C3 · Historial de costos de un producto', () async {
    final [h] = await repo.historialDeCostos('p-1');

    expect(backend.pedidos.single.queryParameters, {'productoId': 'p-1'});
    expect(h.recepcion, 'REC-1');
    expect(h.costoEntrada, 10500);
    expect(h.fecha.toUtc(), DateTime.utc(2026, 11, 20, 15, 15));
  });

  test('C4 · El archivo se pide con formato y periodo, y llegan sus bytes', () async {
    final bytes = await repo.archivo(InformeDeCompras.porProducto, 'XLSX', periodo: noviembre);

    expect(bytes, [37, 80, 68, 70]);
    expect(backend.pedidos.single.path, '/api/compras/informes/por-producto/archivo');
    expect(backend.pedidos.single.queryParameters,
        {'formato': 'XLSX', 'desde': '2026-11-01', 'hasta': '2026-11-23'});
  });
}
