import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_facturas.dart';
import 'estado_de_factura.dart';

/// Carga la factura y su trazabilidad, y maneja el envío al cliente (HU-058).
class ControladorDeFactura extends StateNotifier<EstadoDeFactura> {
  ControladorDeFactura(this._repo, {required this.facturaId})
      : super(const EstadoDeFactura()) {
    cargar();
  }

  final RepositorioDeFacturas _repo;
  final String facturaId;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, error: null);
    try {
      final factura = await _repo.ver(facturaId);
      state = state.copiar(factura: factura, cargando: false);
    } on ErrorDeApi catch (e) {
      state = state.copiar(cargando: false, error: e.mensaje);
    }
  }

  Future<void> enviar({String? correo}) async {
    if (!state.puedeEnviar) return;
    state = state.copiar(enviando: true, avisoEnvio: null);
    try {
      final destinatario = await _repo.enviarPorCorreo(facturaId, correo: correo);
      state = state.copiar(enviando: false, avisoEnvio: 'Enviada a $destinatario');
    } on ErrorDeApi catch (e) {
      state = state.copiar(enviando: false, avisoEnvio: e.mensaje);
    }
  }
}
