import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import 'calendario_de_ocupacion.dart';
import 'controlador_de_calendario.dart';
import 'repositorio_de_calendario.dart';

/// Pantalla del calendario de ocupación (HU-075). Muestra una semana; los
/// botones de navegación llevan a la anterior y a la siguiente.
class PantallaCalendario extends StatefulWidget {
  const PantallaCalendario({
    super.key,
    required this.repositorio,
    required this.onAbrirReserva,
    this.desdeInicial,
    this.tipoRecursoId,
  });

  final RepositorioDeCalendario repositorio;
  final void Function(String reservaId) onAbrirReserva;
  final DateTime? desdeInicial;
  final String? tipoRecursoId;

  @override
  State<PantallaCalendario> createState() => _PantallaCalendarioState();
}

class _PantallaCalendarioState extends State<PantallaCalendario> {
  late final ControladorDeCalendario _controlador;

  @override
  void initState() {
    super.initState();
    _controlador = ControladorDeCalendario(widget.repositorio);
    _controlador.cargar(
      desde: widget.desdeInicial,
      tipoRecursoId: widget.tipoRecursoId,
    );
  }

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      key: const Key('pantalla-calendario'),
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _BarraDeSemana(controlador: _controlador),
        Expanded(
          child: SingleChildScrollView(
            child: CalendarioDeOcupacion(
              controlador: _controlador,
              onAbrirReserva: widget.onAbrirReserva,
            ),
          ),
        ),
      ],
    );
  }
}

class _BarraDeSemana extends StatelessWidget {
  const _BarraDeSemana({required this.controlador});

  final ControladorDeCalendario controlador;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controlador,
      builder: (context, _) {
        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: const BoxDecoration(
            border: Border(bottom: BorderSide(color: RegentaColors.line)),
          ),
          child: Row(
            children: [
              IconButton(
                key: const Key('semana-anterior'),
                onPressed: controlador.cargando ? null : controlador.semanaAnterior,
                icon: const Icon(Icons.chevron_left),
                tooltip: 'Semana anterior',
              ),
              Expanded(
                child: Text(
                  '${_fecha(controlador.desde)} – ${_fecha(controlador.hasta)}',
                  textAlign: TextAlign.center,
                  style: RegentaType.codigo
                      .copyWith(fontSize: 13, color: RegentaColors.ink),
                ),
              ),
              IconButton(
                key: const Key('semana-siguiente'),
                onPressed: controlador.cargando ? null : controlador.semanaSiguiente,
                icon: const Icon(Icons.chevron_right),
                tooltip: 'Semana siguiente',
              ),
            ],
          ),
        );
      },
    );
  }

  static String _fecha(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}';
}
