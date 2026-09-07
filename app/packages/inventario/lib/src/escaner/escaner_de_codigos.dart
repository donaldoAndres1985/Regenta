import 'dart:async';

import 'package:flutter/widgets.dart';

/// La cámara para leer códigos de barras. Detrás de una interfaz a propósito:
/// en Android es `mobile_scanner`, en la web no existe y siempre se cae al
/// ingreso manual (HU-035, criterio 3), y en los tests es un doble.
abstract interface class EscanerDeCodigos {
  /// Si hay cámara y permiso para usarla. En la web es siempre `false`.
  Future<bool> get disponible;

  /// Abre la cámara y devuelve el primer código leído, o `null` si la persona
  /// cancela. Lanza [EscaneoNoDisponible] si no se puede escanear.
  Future<String?> escanearUnCodigo(BuildContext context);
}

/// No se pudo abrir la cámara: no hay, no hay permiso, o es la web. Quien llama
/// debe caer al ingreso manual.
class EscaneoNoDisponible implements Exception {
  const EscaneoNoDisponible([this.motivo = 'La cámara no está disponible']);
  final String motivo;

  @override
  String toString() => 'EscaneoNoDisponible: $motivo';
}

/// Escáner que nunca escanea: el que se usa en la web. `disponible` es `false`
/// y cualquier intento de escanear obliga al ingreso manual.
class EscanerNoDisponible implements EscanerDeCodigos {
  const EscanerNoDisponible();

  @override
  Future<bool> get disponible async => false;

  @override
  Future<String?> escanearUnCodigo(BuildContext context) async =>
      throw const EscaneoNoDisponible();
}
