/// La comanda como la pinta la pantalla (HU-085): cabecera, totales y líneas,
/// cada línea con su propio estado.
class ComandaVista {
  const ComandaVista({
    required this.id,
    required this.numero,
    required this.estado,
    required this.numComensales,
    required this.subtotal,
    required this.impuestoTotal,
    required this.propinaSugerida,
    required this.total,
    required this.lineas,
    this.mesaId,
  });

  final String id;
  final String numero;
  final String estado;
  final int numComensales;
  final num subtotal;
  final num impuestoTotal;
  final num propinaSugerida;
  final num total;
  final String? mesaId;
  final List<LineaVista> lineas;

  int get itemsCount => lineas.where((l) => l.estado != 'ANULADA').length;

  factory ComandaVista.desdeJson(Map<String, dynamic> json) => ComandaVista(
        id: json['id'] as String,
        numero: (json['numero'] ?? '') as String,
        estado: (json['estado'] ?? 'ABIERTA') as String,
        numComensales: (json['numComensales'] as num?)?.toInt() ?? 1,
        subtotal: (json['subtotal'] as num?) ?? 0,
        impuestoTotal: (json['impuestoTotal'] as num?) ?? 0,
        propinaSugerida: (json['propinaSugerida'] as num?) ?? 0,
        total: (json['total'] as num?) ?? 0,
        mesaId: json['mesaId'] as String?,
        lineas: (json['lineas'] as List<dynamic>? ?? const [])
            .map((e) => LineaVista.desdeJson((e as Map).cast<String, dynamic>()))
            .toList(),
      );
}

class LineaVista {
  const LineaVista({
    required this.id,
    required this.linea,
    required this.nombre,
    required this.estado,
    required this.curso,
    required this.cantidad,
    required this.total,
    required this.modificadores,
    this.notas,
  });

  final String id;
  final int linea;
  final String nombre;
  final String estado;
  final String curso;
  final num cantidad;
  final num total;
  final String? notas;
  final List<String> modificadores;

  factory LineaVista.desdeJson(Map<String, dynamic> json) => LineaVista(
        id: json['id'] as String,
        linea: (json['linea'] as num?)?.toInt() ?? 0,
        nombre: (json['nombre'] ?? '') as String,
        estado: (json['estado'] ?? 'PENDIENTE') as String,
        curso: (json['curso'] ?? 'FUERTE') as String,
        cantidad: (json['cantidad'] as num?) ?? 1,
        total: (json['total'] as num?) ?? 0,
        notas: json['notas'] as String?,
        modificadores: (json['modificadores'] as List<dynamic>? ?? const [])
            .map((m) => ((m as Map)['nombre'] ?? '').toString())
            .toList(),
      );
}

/// Un ítem de la carta, para el buscador de «Añadir».
class ItemDeCarta {
  const ItemDeCarta({
    required this.id,
    required this.nombre,
    required this.precio,
    required this.disponible,
  });

  final String id;
  final String nombre;
  final num precio;
  final bool disponible;

  factory ItemDeCarta.desdeJson(Map<String, dynamic> json) => ItemDeCarta(
        id: json['id'] as String,
        nombre: (json['nombre'] ?? '') as String,
        precio: (json['precio'] as num?) ?? 0,
        disponible: (json['disponible'] as bool?) ?? true,
      );
}

/// Un grupo de modificadores de un ítem, con sus opciones.
class GrupoModificadores {
  const GrupoModificadores({
    required this.id,
    required this.nombre,
    required this.min,
    required this.max,
    required this.opciones,
  });

  final String id;
  final String nombre;
  final int min;
  final int max;
  final List<OpcionModificador> opciones;

  bool get obligatorio => min > 0;

  factory GrupoModificadores.desdeJson(Map<String, dynamic> json) {
    final grupo = (json['grupo'] as Map?)?.cast<String, dynamic>() ?? json;
    return GrupoModificadores(
      id: grupo['id'] as String,
      nombre: (grupo['nombre'] ?? '') as String,
      min: (grupo['minSelecciones'] as num?)?.toInt() ?? 0,
      max: (grupo['maxSelecciones'] as num?)?.toInt() ?? 1,
      opciones: (json['opciones'] as List<dynamic>? ?? const [])
          .map((e) => OpcionModificador.desdeJson((e as Map).cast<String, dynamic>()))
          .toList(),
    );
  }
}

class OpcionModificador {
  const OpcionModificador({required this.id, required this.nombre, required this.precioExtra});

  final String id;
  final String nombre;
  final num precioExtra;

  factory OpcionModificador.desdeJson(Map<String, dynamic> json) => OpcionModificador(
        id: json['id'] as String,
        nombre: (json['nombre'] ?? '') as String,
        precioExtra: (json['precioExtra'] as num?) ?? 0,
      );
}
