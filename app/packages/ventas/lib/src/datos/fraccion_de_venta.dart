import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-158 · una fracción con la que se vende un producto: medida, factor (lo que descuenta) y
/// precio con IVA incluido, tal como lo dice el catálogo (tabla o precio propio).
class FraccionDeVenta {
  const FraccionDeVenta({required this.medida, required this.factor, required this.precio});

  final String medida;
  final num factor;
  final num precio;

  factory FraccionDeVenta.desdeJson(Map<String, dynamic> j) => FraccionDeVenta(
        medida: (j['medida'] ?? '') as String,
        factor: (j['factor'] ?? 0) as num,
        precio: (j['precio'] ?? 0) as num,
      );
}

/// HU-158 · de dónde saca el POS las fracciones de un producto.
abstract class ConsultaDeFracciones {
  Future<List<FraccionDeVenta>> de(String productoId);
}

/// `GET /api/ventas/fracciones/{productoId}` (con el permiso de vender).
class ConsultaDeFraccionesHttp implements ConsultaDeFracciones {
  ConsultaDeFraccionesHttp(this._http);

  final ClienteHttp _http;

  @override
  Future<List<FraccionDeVenta>> de(String productoId) async {
    final cuerpo = await _http.get<List<dynamic>>('/api/ventas/fracciones/$productoId');
    return [for (final f in cuerpo.cast<Map<String, dynamic>>()) FraccionDeVenta.desdeJson(f)];
  }
}

/// Sin servidor (tests, o la app sin cablear): ningún producto se vende fraccionado.
class ConsultaDeFraccionesVacia implements ConsultaDeFracciones {
  const ConsultaDeFraccionesVacia();

  @override
  Future<List<FraccionDeVenta>> de(String productoId) async => const [];
}

final consultaDeFraccionesProvider =
    Provider<ConsultaDeFracciones>((ref) => const ConsultaDeFraccionesVacia());
