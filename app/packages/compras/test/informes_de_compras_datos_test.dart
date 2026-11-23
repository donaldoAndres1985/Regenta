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
    } else if (ruta.endsWith('/antiguedad')) {
      cuerpo = {
        'fecha': '2026-11-23',
        'proveedores': [
          {
            'proveedorId': 'prov-1',
            'proveedor': 'Andina',
            'cuentas': 3,
            'moraMaxima': 100,
            'saldo': {'alDia': 248000.00, 'de1a30': 100000.00, 'de31a60': 0.00, 'de61a90': 0.00, 'masDe90': 50000.00, 'total': 398000.00},
          },
        ],
        'totales': {'alDia': 248000.00, 'de1a30': 100000.00, 'de31a60': 0.00, 'de61a90': 0.00, 'masDe90': 50000.00, 'total': 398000.00},
      };
    } else if (ruta.endsWith('/pendientes-por-recibir')) {
      cuerpo = [
        {
          'ordenId': 'oc-1',
          'numero': 'OC-0231',
          'proveedorId': 'prov-1',
          'proveedor': 'Andina',
          'estado': 'ENVIADA',
          'fechaEsperada': '2026-11-20',
          'atrasada': true,
          'diasDeAtraso': 3,
          'lineas': [
            {'linea': 1, 'productoId': 'p-1', 'producto': 'Acetaminofén', 'pedida': 120, 'recibida': 0, 'faltante': 120, 'costoUnitario': 8400.00, 'valor': 1008000.00},
          ],
          'valorPendiente': 1008000.00,
        },
      ];
    } else if (ruta.endsWith('/faltantes')) {
      cuerpo = [
        {
          'ordenId': 'oc-6',
          'numero': 'OC-0220',
          'proveedorId': 'prov-1',
          'proveedor': 'Andina',
          'cerradaEn': '2026-11-12T10:00:00-05:00',
          'motivo': 'El proveedor no tenía más',
          'lineas': [
            {'linea': 1, 'productoId': 'p-1', 'producto': 'Acetaminofén', 'pedida': 10, 'recibida': 7, 'faltante': 3, 'costoUnitario': 10000.00, 'valor': 30000.00},
          ],
          'valorFaltante': 30000.00,
        },
      ];
    } else if (ruta.endsWith('/pagos')) {
      cuerpo = {
        'desde': '2026-11-01',
        'hasta': '2026-11-23',
        'filas': [
          {'proveedorId': 'prov-1', 'proveedor': 'Andina', 'metodo': 'TRANSFERENCIA', 'pagos': 2, 'total': 150000.00},
        ],
        'porMetodo': {'TRANSFERENCIA': 150000.00},
        'total': 150000.00,
      };
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

  test('HU-184 C1 · Antigüedad: tramos por proveedor y totales', () async {
    final a = await repo.antiguedad();

    expect(backend.pedidos.single.path, '/api/compras/informes/antiguedad');
    expect(a.proveedores.single.moraMaxima, 100);
    expect(a.proveedores.single.saldo.de1a30, 100000);
    expect(a.totales.masDe90, 50000);
    expect(a.totales.total, 398000);
  });

  test('HU-184 C2 · Pendientes por recibir con su fecha esperada y si va atrasada', () async {
    final [o] = await repo.pendientesPorRecibir();

    expect(o.fechaEsperada, DateTime(2026, 11, 20));
    expect(o.atrasada, isTrue);
    expect(o.diasDeAtraso, 3);
    expect(o.lineas.single.faltante, 120);
    expect(o.valorPendiente, 1008000);
  });

  test('HU-184 C3 · Faltantes de las órdenes cerradas', () async {
    final [f] = await repo.faltantes();

    expect(f.motivo, 'El proveedor no tenía más');
    expect(f.cerradaEn!.toUtc(), DateTime.utc(2026, 11, 12, 15));
    expect(f.lineas.single.recibida, 7);
    expect(f.valorFaltante, 30000);
  });

  test('HU-184 C4 · Pagos del periodo por proveedor y método', () async {
    final p = await repo.pagos(noviembre);

    expect(backend.pedidos.single.queryParameters, {'desde': '2026-11-01', 'hasta': '2026-11-23'});
    expect(p.filas.single.metodo, 'TRANSFERENCIA');
    expect(p.filas.single.pagos, 2);
    expect(p.porMetodo, {'TRANSFERENCIA': 150000});
    expect(p.total, 150000);
    expect(nombreDelMetodo('TRANSFERENCIA'), 'Transferencia');
  });

  test('C4 · El archivo se pide con formato y periodo, y llegan sus bytes', () async {
    final bytes = await repo.archivo(InformeDeCompras.porProducto, 'XLSX', periodo: noviembre);

    expect(bytes, [37, 80, 68, 70]);
    expect(backend.pedidos.single.path, '/api/compras/informes/por-producto/archivo');
    expect(backend.pedidos.single.queryParameters,
        {'formato': 'XLSX', 'desde': '2026-11-01', 'hasta': '2026-11-23'});
  });
}
