import 'package:meta/meta.dart';

import 'modulos_activos.dart';

/// Un modulo encendido y por que lo esta (por el PLAN o como ADD-ON).
@immutable
class ModuloActivo {
  const ModuloActivo({
    required this.codigo,
    required this.nombre,
    required this.tipo,
    required this.origen,
    required this.orden,
  });

  final String codigo;
  final String nombre;
  final String tipo;
  final String origen;
  final int orden;

  factory ModuloActivo.fromJson(Map<String, dynamic> j) => ModuloActivo(
        codigo: j['codigo'] as String,
        nombre: j['nombre'] as String,
        tipo: j['tipo'] as String,
        origen: j['origen'] as String,
        orden: (j['orden'] as num).toInt(),
      );
}

/// El retrato del negocio para pintar la navegacion y los limites. Espejo del
/// DTO `ResumenDelNegocio` del servicio de usuarios.
@immutable
class ResumenDelNegocio {
  const ResumenDelNegocio({
    required this.negocioId,
    required this.nombreComercial,
    required this.plan,
    required this.patronOperativo,
    required this.estado,
    required this.maxUsuarios,
    required this.maxSucursales,
    required this.usuariosActivos,
    required this.modulos,
  });

  final String negocioId;
  final String nombreComercial;
  final String plan;
  final String patronOperativo;
  final String estado;
  final int? maxUsuarios;
  final int maxSucursales;
  final int usuariosActivos;
  final List<ModuloActivo> modulos;

  ModulosActivos get modulosActivos => ModulosActivos(
        plan: plan,
        patron: patronOperativo,
        codigos: modulos.map((m) => m.codigo).toList(),
      );

  factory ResumenDelNegocio.fromJson(Map<String, dynamic> j) => ResumenDelNegocio(
        negocioId: j['negocioId'] as String,
        nombreComercial: j['nombreComercial'] as String,
        plan: j['plan'] as String,
        patronOperativo: j['patronOperativo'] as String,
        estado: j['estado'] as String,
        maxUsuarios: (j['maxUsuarios'] as num?)?.toInt(),
        maxSucursales: (j['maxSucursales'] as num).toInt(),
        usuariosActivos: (j['usuariosActivos'] as num).toInt(),
        modulos: (j['modulos'] as List<dynamic>)
            .map((m) => ModuloActivo.fromJson((m as Map).cast<String, dynamic>()))
            .toList(),
      );
}
