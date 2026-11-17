/// HU-161 · "1 [origen] = [factor] [destino]" (1 cuñete = 5 galones).
class EquivalenciaDeDetallado {
  const EquivalenciaDeDetallado({
    required this.id,
    required this.origenId,
    required this.origen,
    required this.destinoId,
    required this.destino,
    required this.factor,
  });

  final String id;
  final String origenId;
  final String origen;
  final String destinoId;
  final String destino;
  final num factor;

  factory EquivalenciaDeDetallado.desdeJson(Map<String, dynamic> j) => EquivalenciaDeDetallado(
        id: j['id'] as String,
        origenId: j['origenId'] as String,
        origen: (j['origen'] ?? '') as String,
        destinoId: j['destinoId'] as String,
        destino: (j['destino'] ?? '') as String,
        factor: (j['factor'] ?? 0) as num,
      );
}

/// HU-161 C3 · cuántas equivalencias se crearon al replicar y qué productos quedaron sin par.
class ResultadoDeReplica {
  const ResultadoDeReplica({required this.creadas, required this.sinPar});

  final int creadas;
  final List<String> sinPar;

  factory ResultadoDeReplica.desdeJson(Map<String, dynamic> j) => ResultadoDeReplica(
        creadas: ((j['creadas'] ?? 0) as num).toInt(),
        sinPar: [for (final s in (j['sinPar'] ?? const []) as List) s as String],
      );
}
