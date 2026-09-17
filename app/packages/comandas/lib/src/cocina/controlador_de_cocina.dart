import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_cocina.dart';
import '../datos/ticket_de_cocina.dart';
import 'estado_de_cocina.dart';

/// Maneja la pantalla KDS (HU-088): carga las estaciones y los tickets de la
/// seleccionada, y sondea cada [intervaloRefresco] para verse al día sin que
/// nadie recargue (criterio 5), igual que el plano de mesas (HU-084).
class ControladorDeCocina extends StateNotifier<EstadoDeCocina> {
  ControladorDeCocina(
    this._repo, {
    this.intervaloRefresco = const Duration(seconds: 5),
  }) : super(const EstadoDeCocina()) {
    cargar();
    if (intervaloRefresco > Duration.zero) {
      _latido = Timer.periodic(intervaloRefresco, (_) => _refrescarTickets(silencioso: true));
    }
  }

  final RepositorioDeCocina _repo;
  final Duration intervaloRefresco;
  Timer? _latido;

  @override
  void dispose() {
    _latido?.cancel();
    super.dispose();
  }

  Future<void> cargar() async {
    state = state.copiar(cargando: true, errorAlCargar: false, mensaje: null);
    try {
      final estaciones = (await _repo.estaciones()).where((e) => e.activa).toList()
        ..sort((a, b) => a.orden.compareTo(b.orden));
      final seleccionada = state.estacionSeleccionadaId ??
          (estaciones.isEmpty ? null : estaciones.first.id);
      final tickets =
          seleccionada == null ? const <TicketDeCocina>[] : await _repo.ticketsDeEstacion(seleccionada);
      state = state.copiar(
        estaciones: estaciones,
        estacionSeleccionadaId: seleccionada,
        tickets: tickets,
        cargando: false,
      );
    } on ErrorDeApi catch (e) {
      state = state.copiar(cargando: false, errorAlCargar: true, mensaje: e.mensaje);
    }
  }

  /// Cambia de estación (pestañas del encabezado) y carga sus tickets.
  Future<void> seleccionarEstacion(String estacionId) async {
    if (estacionId == state.estacionSeleccionadaId) return;
    state = state.copiar(estacionSeleccionadaId: estacionId, cargando: true, mensaje: null);
    await _refrescarTickets();
  }

  Future<void> _refrescarTickets({bool silencioso = false}) async {
    final estacionId = state.estacionSeleccionadaId;
    if (estacionId == null) return;
    try {
      final tickets = await _repo.ticketsDeEstacion(estacionId);
      state = state.copiar(tickets: tickets, cargando: false);
    } on ErrorDeApi catch (e) {
      if (silencioso) return; // un fallo de red en el sondeo no rompe la pantalla
      state = state.copiar(cargando: false, mensaje: e.mensaje);
    }
  }

  void limpiarMensaje() => state = state.copiar(mensaje: null);

  /// «Empezar»: NUEVO → EN_PREPARACION.
  Future<void> marcarEnPreparacion(String ticketId) async {
    await _avanzar(ticketId);
  }

  /// «Listo»: lleva el ticket a LISTO de una vez, avanzando de más si hacía
  /// falta (un ticket NUEVO no obliga a pasar por «Empezar»).
  Future<void> marcarListo(String ticketId) async {
    final tras = await _avanzar(ticketId);
    if (tras != null && tras.estado == 'EN_PREPARACION') {
      await _avanzar(ticketId);
    }
  }

  /// El ticket ya se sirvió: LISTO → ENTREGADO. Con esto sale de la columna
  /// «Listos para servir» (criterio 2: solo se ven los tickets vivos).
  Future<void> marcarEntregado(String ticketId) async {
    await _avanzar(ticketId);
  }

  Future<TicketDeCocina?> _avanzar(String ticketId) async {
    try {
      final ticket = await _repo.avanzarTicket(ticketId);
      await _refrescarTickets();
      return ticket;
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
      await _refrescarTickets();
      return null;
    }
  }
}
