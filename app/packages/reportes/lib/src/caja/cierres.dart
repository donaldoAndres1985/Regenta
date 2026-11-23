import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

num _n(Object? v) => (v as num?) ?? 0;

DateTime? _fecha(Object? v) => v == null ? null : DateTime.parse(v as String).toLocal();

List<Map<String, dynamic>> _lista(Object? v) =>
    [for (final e in (v as List<dynamic>? ?? const [])) (e as Map).cast<String, dynamic>()];

/// HU-143 C1 · Un cierre en el buscador.
class CierreDeCajaVista {
  const CierreDeCajaVista({
    required this.sesionId,
    required this.numero,
    this.cerradaEn,
    this.caja = '',
    this.cajero = '',
    this.totalEsperado = 0,
    this.diferenciaTotal = 0,
    this.estado = '',
  });

  final String sesionId;
  final String numero;
  final DateTime? cerradaEn;
  final String caja;
  final String cajero;
  final num totalEsperado;
  final num diferenciaTotal;
  final String estado;

  bool get descuadrado => estado == 'DESCUADRADA';

  factory CierreDeCajaVista.desdeJson(Map<String, dynamic> json) => CierreDeCajaVista(
        sesionId: json['sesionId'] as String,
        numero: (json['numero'] ?? '') as String,
        cerradaEn: _fecha(json['cerradaEn']),
        caja: (json['caja'] ?? '') as String,
        cajero: (json['cajero'] ?? '') as String,
        totalEsperado: _n(json['totalEsperado']),
        diferenciaTotal: _n(json['diferenciaTotal']),
        estado: (json['estado'] ?? '') as String,
      );
}

/// HU-143 C1 · Una página del buscador; `pagina` empieza en 0.
class PaginaDeCierresVista {
  const PaginaDeCierresVista({this.cierres = const [], this.pagina = 0, this.total = 0, this.hayMas = false});

  final List<CierreDeCajaVista> cierres;
  final int pagina;
  final int total;
  final bool hayMas;

  factory PaginaDeCierresVista.desdeJson(Map<String, dynamic> json) => PaginaDeCierresVista(
        cierres: [for (final c in _lista(json['cierres'])) CierreDeCajaVista.desdeJson(c)],
        pagina: _n(json['pagina']).toInt(),
        total: _n(json['total']).toInt(),
        hayMas: (json['hayMas'] as bool?) ?? false,
      );
}

/// Quien ha abierto caja, para el filtro.
class CajeroVista {
  const CajeroVista({required this.id, required this.nombre});

  final String id;
  final String nombre;
}

/// HU-143 C2 · Una línea del arqueo por método. `contado`/`diferencia` nulos: no se contó.
class LineaDeArqueoVista {
  const LineaDeArqueoVista({required this.metodo, this.esperado = 0, this.contado, this.diferencia});

  final String metodo;
  final num esperado;
  final num? contado;
  final num? diferencia;

  factory LineaDeArqueoVista.desdeJson(Map<String, dynamic> json) => LineaDeArqueoVista(
        metodo: (json['metodo'] ?? 'OTRO') as String,
        esperado: _n(json['esperado']),
        contado: json['contado'] as num?,
        diferencia: json['diferencia'] as num?,
      );
}

/// HU-143 C2 · El resumen de un cierre.
class ResumenDeCierreVista {
  const ResumenDeCierreVista({
    required this.cierre,
    this.cerradoPor,
    this.abiertaEn,
    this.montoApertura = 0,
    this.ventasContado = 0,
    this.ventasCredito = 0,
    this.devoluciones = 0,
    this.recaudos = 0,
    this.ingresos = 0,
    this.retiros = 0,
    this.gastos = 0,
    this.pagosAProveedor = 0,
    this.arqueo = const [],
    this.totalEsperado = 0,
    this.totalContado = 0,
    this.totalDiferencia = 0,
    this.observaciones,
  });

  final CierreDeCajaVista cierre;
  final String? cerradoPor;
  final DateTime? abiertaEn;
  final num montoApertura;
  final num ventasContado;
  final num ventasCredito;
  final num devoluciones;
  final num recaudos;
  final num ingresos;
  final num retiros;
  final num gastos;
  final num pagosAProveedor;
  final List<LineaDeArqueoVista> arqueo;
  final num totalEsperado;
  final num totalContado;
  final num totalDiferencia;
  final String? observaciones;

  factory ResumenDeCierreVista.desdeJson(Map<String, dynamic> json) {
    final arqueo = (json['arqueo'] as Map?)?.cast<String, dynamic>() ?? const {};
    return ResumenDeCierreVista(
      cierre: CierreDeCajaVista.desdeJson((json['cierre'] as Map).cast<String, dynamic>()),
      cerradoPor: json['cerradoPor'] as String?,
      abiertaEn: _fecha(json['abiertaEn']),
      montoApertura: _n(json['montoApertura']),
      ventasContado: _n(json['ventasContado']),
      ventasCredito: _n(json['ventasCredito']),
      devoluciones: _n(json['devoluciones']),
      recaudos: _n(json['recaudos']),
      ingresos: _n(json['ingresos']),
      retiros: _n(json['retiros']),
      gastos: _n(json['gastos']),
      pagosAProveedor: _n(json['pagosAProveedor']),
      arqueo: [for (final l in _lista(arqueo['lineas'])) LineaDeArqueoVista.desdeJson(l)],
      totalEsperado: _n(arqueo['totalEsperado']),
      totalContado: _n(arqueo['totalContado']),
      totalDiferencia: _n(arqueo['totalDiferencia']),
      observaciones: json['observaciones'] as String?,
    );
  }
}

/// HU-143 C3 · Un movimiento en la lista del cierre; `monto` con su signo.
class MovimientoDeCierreVista {
  const MovimientoDeCierreVista({
    required this.id,
    required this.tipoNombre,
    this.ocurridoEn,
    this.documento,
    this.metodoNombre = '',
    this.monto = 0,
    this.anulado = false,
  });

  final String id;
  final String tipoNombre;
  final DateTime? ocurridoEn;
  final String? documento;
  final String metodoNombre;
  final num monto;
  final bool anulado;

  factory MovimientoDeCierreVista.desdeJson(Map<String, dynamic> json) => MovimientoDeCierreVista(
        id: json['id'] as String,
        tipoNombre: (json['tipoNombre'] ?? json['tipo'] ?? '') as String,
        ocurridoEn: _fecha(json['ocurridoEn']),
        documento: json['documento'] as String?,
        metodoNombre: (json['metodoNombre'] ?? '') as String,
        monto: _n(json['monto']),
        anulado: (json['anulado'] as bool?) ?? false,
      );
}

/// HU-143 C3 · Los movimientos y el total.
class MovimientosDeCierreVista {
  const MovimientosDeCierreVista({this.movimientos = const [], this.total = 0});

  final List<MovimientoDeCierreVista> movimientos;
  final num total;

  factory MovimientosDeCierreVista.desdeJson(Map<String, dynamic> json) => MovimientosDeCierreVista(
        movimientos: [for (final m in _lista(json['movimientos'])) MovimientoDeCierreVista.desdeJson(m)],
        total: _n(json['total']),
      );
}

/// HU-143 C5 · Uno de los métodos de un cobro.
class PagoDelCobroVista {
  const PagoDelCobroVista({required this.metodoNombre, this.monto = 0, this.propina = 0});

  final String metodoNombre;
  final num monto;
  final num propina;
}

/// HU-143 C4 · El detalle de un movimiento.
class DetalleDeMovimientoVista {
  const DetalleDeMovimientoVista({
    required this.id,
    required this.tipoNombre,
    this.documento,
    this.ocurridoEn,
    this.metodoNombre = '',
    this.monto = 0,
    this.propina = 0,
    this.concepto,
    this.registradoPor,
    this.autorizadoPor,
    this.anuladoPor,
    this.anuladoEn,
    this.motivoAnulacion,
    this.ventaId,
    this.pagos = const [],
  });

  final String id;
  final String tipoNombre;
  final String? documento;
  final DateTime? ocurridoEn;
  final String metodoNombre;
  final num monto;
  final num propina;
  final String? concepto;
  final String? registradoPor;
  final String? autorizadoPor;
  final String? anuladoPor;
  final DateTime? anuladoEn;
  final String? motivoAnulacion;
  final String? ventaId;
  final List<PagoDelCobroVista> pagos;

  /// Un cobro con más de un método (C5).
  bool get esMixto => pagos.length > 1;

  factory DetalleDeMovimientoVista.desdeJson(Map<String, dynamic> json) => DetalleDeMovimientoVista(
        id: json['id'] as String,
        tipoNombre: (json['tipoNombre'] ?? json['tipo'] ?? '') as String,
        documento: json['documento'] as String?,
        ocurridoEn: _fecha(json['ocurridoEn']),
        metodoNombre: (json['metodoNombre'] ?? '') as String,
        monto: _n(json['monto']),
        propina: _n(json['propina']),
        concepto: json['concepto'] as String?,
        registradoPor: json['registradoPor'] as String?,
        autorizadoPor: json['autorizadoPor'] as String?,
        anuladoPor: json['anuladoPor'] as String?,
        anuladoEn: _fecha(json['anuladoEn']),
        motivoAnulacion: json['motivoAnulacion'] as String?,
        ventaId: json['ventaId'] as String?,
        pagos: [
          for (final p in _lista(json['pagos']))
            PagoDelCobroVista(
              metodoNombre: (p['metodoNombre'] ?? p['metodo'] ?? '') as String,
              monto: _n(p['monto']),
              propina: _n(p['propina']),
            ),
        ],
      );
}

/// Acceso a la consulta de cierres de `caja` (HU-143). Todo exige `CAJA_TURNO_VER`.
class RepositorioDeCierresDeCaja {
  RepositorioDeCierresDeCaja(this._http);

  static const _base = '/api/caja/reportes';
  static const tamanoDePagina = 20;

  final ClienteHttp _http;

  Future<PaginaDeCierresVista> buscar({
    required DateTime desde,
    required DateTime hasta,
    String? cajeroId,
    int pagina = 0,
  }) async {
    final res = await _http.get<Map<String, dynamic>>('$_base/cierres', query: {
      'desde': fechaIso(desde),
      'hasta': fechaIso(hasta),
      'cajeroId': ?cajeroId,
      'pagina': '$pagina',
      'tamano': '$tamanoDePagina',
    });
    return PaginaDeCierresVista.desdeJson(res);
  }

  Future<List<CajeroVista>> cajeros() async {
    final res = await _http.get<List<dynamic>>('$_base/cajeros');
    return [
      for (final e in res)
        CajeroVista(id: (e as Map)['id'] as String, nombre: (e['nombre'] ?? 'Usuario') as String),
    ];
  }

  Future<ResumenDeCierreVista> resumen(String sesionId) async =>
      ResumenDeCierreVista.desdeJson(await _http.get<Map<String, dynamic>>('$_base/cierres/$sesionId'));

  Future<MovimientosDeCierreVista> movimientos(String sesionId) async => MovimientosDeCierreVista.desdeJson(
      await _http.get<Map<String, dynamic>>('$_base/cierres/$sesionId/movimientos'));

  Future<DetalleDeMovimientoVista> movimiento(String movimientoId) async => DetalleDeMovimientoVista.desdeJson(
      await _http.get<Map<String, dynamic>>('$_base/movimientos/$movimientoId'));
}

/// «2026-11-05».
String fechaIso(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

/// «05/11/2026 18:40».
String fechaYHora(DateTime d) =>
    '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year} '
    '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';

/// «$32.000», «-$2.000».
String pesos(num monto) {
  final entero = monto.round().abs().toString();
  final b = StringBuffer();
  for (var i = 0; i < entero.length; i++) {
    if (i != 0 && (entero.length - i) % 3 == 0) b.write('.');
    b.write(entero[i]);
  }
  return '${monto < 0 ? '-' : ''}\$$b';
}

/// «Efectivo» para `EFECTIVO` (el arqueo trae el código del método).
String nombreDelMetodoDeCaja(String metodo) => switch (metodo) {
      'EFECTIVO' => 'Efectivo',
      'TARJETA_DEBITO' => 'Tarjeta débito',
      'TARJETA_CREDITO' => 'Tarjeta crédito',
      'TRANSFERENCIA' => 'Transferencia',
      'QR' => 'QR',
      'BONO' => 'Bono',
      'CREDITO' => 'Crédito',
      _ => 'Otro',
    };

final repositorioDeCierresDeCajaProvider = Provider<RepositorioDeCierresDeCaja>(
  (ref) => throw UnimplementedError('Sobrescribe repositorioDeCierresDeCajaProvider al montar el ProviderScope.'),
);

/// HU-143 C4 · Ir a la venta de un movimiento. La conecta la app con Ventas; nula: no se ofrece.
final abrirVentaDesdeCajaProvider =
    Provider<void Function(BuildContext context, String ventaId, String? numero)?>((ref) => null);
