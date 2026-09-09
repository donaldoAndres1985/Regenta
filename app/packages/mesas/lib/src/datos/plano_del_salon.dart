import 'mesa_en_plano.dart';

/// Una zona con sus mesas, tal como llega en `GET /api/mesas/plano`.
class ZonaConMesas {
  const ZonaConMesas({
    required this.id,
    required this.nombre,
    required this.orden,
    required this.mesas,
    this.color,
  });

  final String id;
  final String nombre;
  final int orden;
  final String? color;
  final List<MesaEnPlano> mesas;

  factory ZonaConMesas.desdeJson(Map<String, dynamic> json) {
    final zona = (json['zona'] as Map).cast<String, dynamic>();
    final mesas = (json['mesas'] as List<dynamic>? ?? const [])
        .map((e) => MesaEnPlano.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
    return ZonaConMesas(
      id: zona['id'] as String,
      nombre: (zona['nombre'] ?? '') as String,
      orden: (zona['orden'] as num?)?.toInt() ?? 0,
      color: zona['color'] as String?,
      mesas: mesas,
    );
  }
}

/// El plano completo: zonas en orden y las mesas sin zona aparte (HU-081).
class PlanoDelSalon {
  const PlanoDelSalon({required this.zonas, required this.sinZona});

  final List<ZonaConMesas> zonas;
  final List<MesaEnPlano> sinZona;

  static const vacio = PlanoDelSalon(zonas: [], sinZona: []);

  bool get estaVacio =>
      sinZona.isEmpty && zonas.every((z) => z.mesas.isEmpty);

  /// Todas las mesas, sin agrupar.
  List<MesaEnPlano> get todas =>
      [for (final z in zonas) ...z.mesas, ...sinZona];

  factory PlanoDelSalon.desdeJson(Map<String, dynamic> json) => PlanoDelSalon(
        zonas: (json['zonas'] as List<dynamic>? ?? const [])
            .map((e) => ZonaConMesas.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
        sinZona: (json['sinZona'] as List<dynamic>? ?? const [])
            .map((e) => MesaEnPlano.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
      );
}
