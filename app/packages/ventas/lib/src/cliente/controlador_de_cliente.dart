import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/cliente_de_la_venta.dart';
import '../datos/repositorio_de_clientes_de_venta.dart';
import 'estado_de_cliente.dart';

/// El selector de cliente de la venta (HU-113 y HU-114).
///
/// La lista arranca cargada: el que vuelve es el que se busca, y obligar a
/// escribir tres letras antes de ver nada convierte el camino más común en un
/// trámite.
class ControladorDeCliente extends StateNotifier<EstadoDeCliente> {
  ControladorDeCliente(
    this._repo, {
    required this.ventaId,
    ClienteDeLaVenta? asignado,
    Duration? rebote,
  })  : _rebote = rebote ?? const Duration(milliseconds: 300),
        super(EstadoDeCliente(asignado: asignado)) {
    unawaited(_buscar(''));
  }

  final RepositorioDeClientesDeVenta _repo;
  final String ventaId;
  final Duration _rebote;
  Timer? _temporizador;

  @override
  void dispose() {
    _temporizador?.cancel();
    super.dispose();
  }

  void cambiarTermino(String termino) {
    state = state.copiar(termino: termino);
    _temporizador?.cancel();
    _temporizador = Timer(_rebote, () => unawaited(_buscar(termino)));
  }

  Future<void> _buscar(String termino) async {
    state = state.copiar(buscando: true);
    try {
      final encontrados = await _repo.buscar(termino);
      state = state.copiar(resultados: encontrados, buscando: false);
    } on ErrorDeApi catch (fallo) {
      // No poder buscar nunca bloquea el cobro: se avisa y se sigue.
      state = state.copiar(buscando: false, mensaje: fallo.mensaje);
    }
  }

  /// HU-113 criterio 2.
  Future<ClienteDeLaVenta?> asignar(ClienteDeLaVenta cliente) async {
    if (state.trabajando) return null;
    state = state.copiar(trabajando: true, mensaje: null);
    try {
      await _repo.asignar(ventaId: ventaId, clienteId: cliente.id);
      state = state.copiar(asignado: cliente, trabajando: false, creando: false, duplicado: false);
      return cliente;
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(trabajando: false, mensaje: fallo.mensaje);
      return null;
    }
  }

  /// HU-113 criterio 4: vuelve a consumidor final.
  Future<void> quitar() async {
    if (state.trabajando) return;
    state = state.copiar(trabajando: true, mensaje: null);
    try {
      await _repo.quitar(ventaId: ventaId);
      state = state.copiar(asignado: null, trabajando: false);
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(trabajando: false, mensaje: fallo.mensaje);
    }
  }

  /// El cliente que la venta ya traía, sin volver a preguntarle al servidor:
  /// lo trae el POS, que acaba de leerlo con la venta.
  void sembrar(ClienteDeLaVenta? cliente) {
    if (cliente != null && state.asignado == null) {
      state = state.copiar(asignado: cliente);
    }
  }

  void abrirAlta() => state = state.copiar(creando: true, mensaje: null, duplicado: false);

  void cerrarAlta() => state = state.copiar(creando: false, duplicado: false);

  /// HU-114 criterios 1, 2 y 5.
  Future<ClienteDeLaVenta?> crearYAsignar(ClienteNuevo nuevo) async {
    if (state.trabajando) return null;
    state = state.copiar(trabajando: true, mensaje: null, duplicado: false);
    try {
      final alta = await _repo.crear(nuevo);
      final cliente = alta.cliente ??
          ClienteDeLaVenta(
            id: alta.id,
            nombre: nuevo.nombre,
            tipoDocumento: nuevo.tipoDocumento,
            numeroDocumento: nuevo.numeroDocumento,
            digitoVerificacion: nuevo.digitoVerificacion,
          );
      // Sin señal el cliente todavía no existe en el servidor, así que no se
      // puede asignar por HTTP: viaja con la venta, que también sube en la
      // cola, y el POS se queda con él para mandarlo al cobrar.
      if (!alta.quedoEnLaCola) {
        await _repo.asignar(ventaId: ventaId, clienteId: cliente.id);
      }
      state = state.copiar(
        asignado: cliente,
        trabajando: false,
        creando: false,
        mensaje: alta.quedoEnLaCola
            ? 'Cliente guardado sin señal: sube junto con la venta'
            : null,
      );
      return cliente;
    } on RecursoDuplicado catch (fallo) {
      // Criterio 2: no se duplica; se ofrece el que ya existe.
      state = state.copiar(
        trabajando: false,
        duplicado: true,
        mensaje: '${fallo.mensaje}. Búscalo y asigna el que ya existe.',
      );
      return null;
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(trabajando: false, mensaje: fallo.mensaje);
      return null;
    }
  }
}
