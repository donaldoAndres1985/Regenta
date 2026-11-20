import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/inventario_general.dart';
import '../datos/repositorio_de_inventario.dart';
import 'proveedores.dart';

/// En qué está la lista: los cuatro estados se pintan distinto
/// (`design/comportamiento/Inventario.md`).
enum FaseDeInventario { cargando, conDatos, sinResultados, error }

/// Lo que muestra la pantalla de Inventario: las filas cargadas hasta ahora, los totales de todo
/// lo filtrado y la consulta con que se pidieron.
class EstadoDeInventario {
  const EstadoDeInventario({
    this.consulta = const ConsultaDeInventario(),
    this.fase = FaseDeInventario.cargando,
    this.filas = const [],
    this.totalFilas = 0,
    this.valorCosto,
    this.valorPrecio = 0,
    this.bajoMinimo = 0,
    this.porRevisar = 0,
    this.resumen = const ResumenDelInventario(),
    this.cargandoMas = false,
    this.mensajeError,
  });

  final ConsultaDeInventario consulta;
  final FaseDeInventario fase;
  final List<FilaDeInventario> filas;
  final int totalFilas;
  final num? valorCosto;
  final num valorPrecio;
  final int bajoMinimo;
  final int porRevisar;
  final ResumenDelInventario resumen;
  final bool cargandoMas;
  final String? mensajeError;

  bool get hayMas => filas.length < totalFilas;

  EstadoDeInventario copiar({
    ConsultaDeInventario? consulta,
    FaseDeInventario? fase,
    List<FilaDeInventario>? filas,
    ResultadoDeInventario? totales,
    bool? cargandoMas,
    Object? mensajeError = _sinCambio,
  }) =>
      EstadoDeInventario(
        consulta: consulta ?? this.consulta,
        fase: fase ?? this.fase,
        filas: filas ?? this.filas,
        totalFilas: totales?.totalFilas ?? totalFilas,
        valorCosto: totales != null ? totales.valorCosto : valorCosto,
        valorPrecio: totales?.valorPrecio ?? valorPrecio,
        bajoMinimo: totales?.bajoMinimo ?? bajoMinimo,
        porRevisar: totales?.porRevisar ?? porRevisar,
        resumen: totales?.resumen ?? resumen,
        cargandoMas: cargandoMas ?? this.cargandoMas,
        mensajeError: mensajeError == _sinCambio ? this.mensajeError : mensajeError as String?,
      );

  static const _sinCambio = Object();
}

/// R5 · pide `GET /api/inventario/general` de a 50 y agrega la página siguiente cuando el scroll
/// llega al final. R1 · el texto filtra desde el tercer carácter, con rebote de 300 ms; cada
/// consulta nueva descarta la respuesta de una anterior más lenta.
class ControladorDeInventario extends StateNotifier<EstadoDeInventario> {
  ControladorDeInventario(this._repo, {Duration? rebote})
      : _rebote = rebote ?? const Duration(milliseconds: 300),
        super(const EstadoDeInventario()) {
    _cargar(state.consulta);
  }

  static const minimoParaFiltrar = 3;

  final RepositorioDeInventario _repo;
  final Duration _rebote;
  Timer? _temporizador;
  int _generacion = 0;

  void cambiarTermino(String termino) {
    _temporizador?.cancel();
    _temporizador = Timer(_rebote, () {
      final t = termino.trim();
      final q = t.length >= minimoParaFiltrar ? t : null;
      if (q == state.consulta.q && state.fase != FaseDeInventario.error) return;
      _cargar(state.consulta.conTexto(q));
    });
  }

  /// R2 · buscar ya, sin esperar el rebote (Enter o un código leído).
  void buscarYa(String termino) {
    _temporizador?.cancel();
    final t = termino.trim();
    _cargar(state.consulta.conTexto(t.isEmpty ? null : t));
  }

  /// Otro filtro: vuelve a la página 0.
  void filtrar(ConsultaDeInventario nueva) => _cargar(nueva.copiar(pagina: 0));

  void recargar() => _cargar(state.consulta.copiar(pagina: 0));

  Future<void> cargarMas() async {
    if (!state.hayMas || state.cargandoMas || state.fase != FaseDeInventario.conDatos) return;
    final gen = _generacion;
    final siguiente = state.consulta.copiar(pagina: state.consulta.pagina + 1);
    state = state.copiar(cargandoMas: true);
    try {
      final r = await _repo.inventarioGeneral(siguiente);
      if (gen != _generacion) return;
      state = state.copiar(
          consulta: siguiente, filas: [...state.filas, ...r.filas], totales: r, cargandoMas: false);
    } on ErrorDeApi {
      if (gen != _generacion) return;
      state = state.copiar(cargandoMas: false);
    }
  }

  Future<void> _cargar(ConsultaDeInventario consulta) async {
    final gen = ++_generacion;
    state = state.copiar(consulta: consulta, fase: FaseDeInventario.cargando, mensajeError: null);
    try {
      final r = await _repo.inventarioGeneral(consulta);
      if (gen != _generacion) return;
      state = state.copiar(
        filas: r.filas,
        totales: r,
        cargandoMas: false,
        fase: r.filas.isEmpty ? FaseDeInventario.sinResultados : FaseDeInventario.conDatos,
      );
    } on ErrorDeApi catch (e) {
      if (gen != _generacion) return;
      state = state.copiar(fase: FaseDeInventario.error, mensajeError: _mensajeDe(e));
    }
  }

  String _mensajeDe(ErrorDeApi e) => switch (e) {
        ErrorDeRed() => 'Sin conexión. Revisa tu red e intenta de nuevo.',
        SinPermiso() => 'No tienes permiso para ver el inventario.',
        ErrorDelServidor() => 'El servidor tuvo un problema. Intenta en un momento.',
        _ => e.mensaje,
      };

  @override
  void dispose() {
    _temporizador?.cancel();
    super.dispose();
  }
}

final controladorDeInventarioProvider =
    StateNotifierProvider.autoDispose<ControladorDeInventario, EstadoDeInventario>(
  (ref) => ControladorDeInventario(ref.watch(repositorioDeInventarioProvider)),
);
