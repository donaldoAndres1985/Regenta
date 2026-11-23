import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_reportes/regenta_reportes.dart';

/// Responde como `ReportesDeCajaControlador` (HU-143) y anota lo que se le pidió.
class _Backend implements HttpClientAdapter {
  final List<Uri> pedidos = [];

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream, Future<void>? cancelFuture) async {
    pedidos.add(options.uri);
    final ruta = options.uri.path;
    const cierre = {
      'sesionId': 's-12',
      'cerradaEn': '2026-11-20T18:30:00-05:00',
      'cajaId': 'c-1',
      'caja': 'Caja Principal',
      'numero': 'CAJA-12',
      'cajeroId': 'ana',
      'cajero': 'Ana Ruiz',
      'totalEsperado': 170000.0,
      'diferenciaTotal': -2000.0,
      'estado': 'DESCUADRADA',
    };
    Object cuerpo = const {};
    if (ruta == '/api/caja/reportes/cierres') {
      cuerpo = {
        'cierres': [cierre],
        'pagina': 0,
        'tamano': 20,
        'total': 41,
        'hayMas': true,
      };
    } else if (ruta == '/api/caja/reportes/cajeros') {
      cuerpo = [
        {'id': 'ana', 'nombre': 'Ana Ruiz'},
      ];
    } else if (ruta == '/api/caja/reportes/cierres/s-12') {
      cuerpo = {
        'cierre': cierre,
        'cerradoPor': 'Luis Gómez',
        'abiertaEn': '2026-11-20T08:00:00-05:00',
        'montoApertura': 100000.0,
        'ventasContado': 50000.0,
        'ventasCredito': 50000.0,
        'devoluciones': 0,
        'recaudos': 0,
        'ingresos': 10000.0,
        'retiros': 40000.0,
        'gastos': 0,
        'pagosAProveedor': 0,
        'arqueo': {
          'lineas': [
            {'metodo': 'EFECTIVO', 'cuadrable': true, 'esperado': 100000.0, 'contado': 98000.0, 'diferencia': -2000.0, 'movimientos': 6},
          ],
          'totalEsperado': 100000.0,
          'totalContado': 98000.0,
          'totalDiferencia': -2000.0,
        },
        'observaciones': 'Faltó sencillo',
      };
    } else if (ruta == '/api/caja/reportes/cierres/s-12/movimientos') {
      cuerpo = {
        'sesionId': 's-12',
        'movimientos': [
          {
            'id': 'm-2',
            'ocurridoEn': '2026-11-20T10:00:00-05:00',
            'tipo': 'VENTA',
            'tipoNombre': 'Venta',
            'documento': 'V-1',
            'metodo': 'EFECTIVO',
            'metodoNombre': 'Efectivo',
            'monto': 30000.0,
            'anulado': false,
          },
        ],
        'total': 30000.0,
      };
    } else if (ruta.endsWith('/pdf')) {
      return ResponseBody.fromBytes(const [37, 80, 68, 70], 200, headers: {
        Headers.contentTypeHeader: ['application/pdf'],
      });
    } else if (ruta == '/api/caja/reportes/movimientos/m-2') {
      cuerpo = {
        'id': 'm-2',
        'sesionId': 's-12',
        'tipo': 'VENTA',
        'tipoNombre': 'Venta',
        'documento': 'V-1',
        'ocurridoEn': '2026-11-20T10:00:00-05:00',
        'metodo': 'EFECTIVO',
        'metodoNombre': 'Efectivo',
        'monto': 30000.0,
        'propina': 0,
        'concepto': 'Cobro venta',
        'registradoPor': 'Ana Ruiz',
        'autorizadoPor': null,
        'anuladoPor': null,
        'anuladoEn': null,
        'motivoAnulacion': null,
        'anulaA': null,
        'origenTipo': 'VENTA',
        'origenId': 'venta-1',
        'ventaId': 'venta-1',
        'pagos': [
          {'movimientoId': 'm-2', 'metodo': 'EFECTIVO', 'metodoNombre': 'Efectivo', 'monto': 30000.0, 'propina': 0},
          {'movimientoId': 'm-3', 'metodo': 'TARJETA_DEBITO', 'metodoNombre': 'Tarjeta débito', 'monto': 20000.0, 'propina': 0},
        ],
      };
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
  late RepositorioDeCierresDeCaja repo;

  setUp(() {
    backend = _Backend();
    repo = RepositorioDeCierresDeCaja(ClienteHttp(Dio(BaseOptions(baseUrl: 'https://gw.test'))..httpClientAdapter = backend));
  });

  test('C1 · Busca con rango, cajero y página, y lee la página', () async {
    final p = await repo.buscar(desde: DateTime(2026, 11, 1), hasta: DateTime(2026, 11, 23), cajeroId: 'ana', pagina: 2);

    expect(backend.pedidos.single.queryParameters,
        {'desde': '2026-11-01', 'hasta': '2026-11-23', 'cajeroId': 'ana', 'pagina': '2', 'tamano': '20'});
    expect(p.total, 41);
    expect(p.hayMas, isTrue);
    final c = p.cierres.single;
    expect(c.cajero, 'Ana Ruiz');
    expect(c.totalEsperado, 170000);
    expect(c.diferenciaTotal, -2000);
    expect(c.descuadrado, isTrue);
    expect(c.cerradaEn!.toUtc(), DateTime.utc(2026, 11, 20, 23, 30));
    expect((await repo.cajeros()).single.nombre, 'Ana Ruiz');
  });

  test('C2 · El resumen y el arqueo por método', () async {
    final r = await repo.resumen('s-12');

    expect(r.cierre.numero, 'CAJA-12');
    expect(r.cerradoPor, 'Luis Gómez');
    expect(r.ventasContado, 50000);
    expect(r.retiros, 40000);
    expect(r.arqueo.single.contado, 98000);
    expect(r.totalDiferencia, -2000);
  });

  test('C3 · Los movimientos con el total', () async {
    final m = await repo.movimientos('s-12');

    expect(m.movimientos.single.tipoNombre, 'Venta');
    expect(m.movimientos.single.documento, 'V-1');
    expect(m.total, 30000);
  });

  test('HU-144 · Los PDF de un cierre y de un rango se piden con su tipo y sus filtros', () async {
    expect(await repo.pdfDeCierre('s-12', detallado: true), [37, 80, 68, 70]);
    expect(backend.pedidos.last.path, '/api/caja/reportes/cierres/s-12/pdf');
    expect(backend.pedidos.last.queryParameters, {'tipo': 'detallado'});

    await repo.pdfDeCierre('s-12');
    expect(backend.pedidos.last.queryParameters, {'tipo': 'general'});

    await repo.pdfDeCierres(desde: DateTime(2026, 11, 1), hasta: DateTime(2026, 11, 23), cajeroId: 'ana');
    expect(backend.pedidos.last.path, '/api/caja/reportes/cierres/pdf');
    expect(backend.pedidos.last.queryParameters, {'desde': '2026-11-01', 'hasta': '2026-11-23', 'cajeroId': 'ana'});
  });

  test('C4 / C5 · El detalle con la venta y todos los métodos del cobro', () async {
    final d = await repo.movimiento('m-2');

    expect(d.ventaId, 'venta-1');
    expect(d.registradoPor, 'Ana Ruiz');
    expect(d.esMixto, isTrue);
    expect(d.pagos.map((p) => p.metodoNombre), ['Efectivo', 'Tarjeta débito']);
  });
}
