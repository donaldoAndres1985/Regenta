import 'package:meta/meta.dart';

/// Lo que la app muestra de la cola de sincronización (HU-111 criterio 3):
/// cuántas operaciones faltan por subir y cuántas quedaron en conflicto.
@immutable
class ResumenDeCola {
  const ResumenDeCola({required this.pendientes, required this.conflictos});

  final int pendientes;
  final int conflictos;

  bool get hayConflictos => conflictos > 0;

  @override
  bool operator ==(Object other) =>
      other is ResumenDeCola && other.pendientes == pendientes && other.conflictos == conflictos;

  @override
  int get hashCode => Object.hash(pendientes, conflictos);
}
