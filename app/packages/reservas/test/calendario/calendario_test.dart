import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_reservas/regenta_reservas.dart';

class _RepoFake implements RepositorioDeCalendario {
  _RepoFake(this._ocupacion);

  final OcupacionDelCalendario _ocupacion;
  final List<DateTime> desdesPedidos = [];

  @override
  Future<OcupacionDelCalendario> ocupacion({
    required DateTime desde,
    required DateTime hasta,
    String? tipoRecursoId,
  }) async {
    desdesPedidos.add(desde);
    return _ocupacion;
  }
}

DateTime _d(int dia, [int hora = 0]) => DateTime.utc(2026, 11, dia, hora);

OcupacionDelCalendario _ocupacion({
  List<BarraDeReserva> reservas = const [],
  List<BarraDeBloqueo> bloqueos = const [],
}) {
  return OcupacionDelCalendario(
    desde: _d(1),
    hasta: _d(7),
    dias: [for (var i = 1; i <= 7; i++) _d(i)],
    recursos: const [
      RecursoDelCalendario(id: 'r1', codigo: '101', nombre: 'Suite'),
      RecursoDelCalendario(id: 'r2', codigo: '102', nombre: 'Doble'),
    ],
    reservas: reservas,
    bloqueos: bloqueos,
  );
}

BarraDeReserva _reserva(
  String id, {
  String recurso = 'r1',
  EstadoDeReserva estado = EstadoDeReserva.confirmada,
  int desde = 2,
  int hasta = 4,
}) =>
    BarraDeReserva(
      id: id,
      recursoId: recurso,
      numero: 'RES-$id',
      estado: estado,
      desde: _d(desde, 15),
      hasta: _d(hasta, 11),
    );

Future<ControladorDeCalendario> _montar(
  WidgetTester tester,
  _RepoFake repo, {
  Size tamano = const Size(1100, 700),
  void Function(String)? onAbrir,
}) async {
  final c = ControladorDeCalendario(repo);
  await c.cargar(desde: _d(1));
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Center(
          child: SizedBox(
            width: tamano.width,
            height: tamano.height,
            child: CalendarioDeOcupacion(
              controlador: c,
              onAbrirReserva: onAbrir ?? (_) {},
            ),
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return c;
}

void main() {
  testWidgets('criterio 1: una fila por recurso, una columna por día, reservas como barras',
      (tester) async {
    final repo = _RepoFake(_ocupacion(reservas: [_reserva('a')]));
    await _montar(tester, repo);

    expect(find.byKey(const Key('calendario-ocupacion')), findsOneWidget);
    expect(find.byKey(const Key('fila-recurso-r1')), findsOneWidget);
    expect(find.byKey(const Key('fila-recurso-r2')), findsOneWidget);
    for (var i = 1; i <= 7; i++) {
      expect(find.byKey(Key('dia-2026-11-0$i')), findsOneWidget);
    }
    expect(find.byKey(const Key('barra-reserva-a')), findsOneWidget);
  });

  testWidgets('criterio 2: el color de la barra sale del estado de la reserva', (tester) async {
    final repo = _RepoFake(_ocupacion(reservas: [
      _reserva('conf', estado: EstadoDeReserva.confirmada, desde: 1, hasta: 3),
      _reserva('pend', recurso: 'r2', estado: EstadoDeReserva.pendiente, desde: 4, hasta: 6),
    ]));
    await _montar(tester, repo);

    Color fondoDe(String id) {
      final cont = tester.widget<Container>(find
          .descendant(of: find.byKey(Key('barra-reserva-$id')), matching: find.byType(Container))
          .first);
      return (cont.decoration as BoxDecoration).color!;
    }

    expect(fondoDe('conf'), RegentaColors.reservaSoft);
    expect(fondoDe('pend'), RegentaColors.warnSoft);
    expect(EstadoDeReserva.checkIn.fondo, RegentaColors.okSoft);
  });

  testWidgets('criterio 3: en móvil se ve la rejilla desplazable en horizontal', (tester) async {
    final repo = _RepoFake(_ocupacion(reservas: [_reserva('a')]));
    await _montar(tester, repo, tamano: const Size(380, 640));

    final scroll = find.byKey(const Key('calendario-scroll-horizontal'));
    expect(scroll, findsOneWidget);
    expect(tester.widget<SingleChildScrollView>(scroll).scrollDirection, Axis.horizontal);
    // Los siete días están en el árbol aunque solo entren tres en pantalla.
    expect(find.byKey(const Key('dia-2026-11-07')), findsOneWidget);
  });

  testWidgets('criterio 4: un bloqueo se distingue de una reserva', (tester) async {
    final repo = _RepoFake(_ocupacion(bloqueos: [
      BarraDeBloqueo(recursoId: 'r1', motivo: 'MANTENIMIENTO', desde: _d(3), hasta: _d(5)),
    ]));
    await _montar(tester, repo);

    final barra = find.byKey(const Key('barra-bloqueo-r1-0'));
    expect(barra, findsOneWidget);
    expect(find.byKey(const Key('barra-reserva-r1-0')), findsNothing);
    expect(find.textContaining('Bloqueo'), findsOneWidget);

    final cont = tester.widget<Container>(
        find.descendant(of: barra, matching: find.byType(Container)).first);
    expect((cont.decoration as BoxDecoration).color, RegentaColors.critSoft);
  });

  testWidgets('criterio 5: tocar una barra abre esa reserva', (tester) async {
    String? abierta;
    final repo = _RepoFake(_ocupacion(reservas: [_reserva('xyz')]));
    await _montar(tester, repo, onAbrir: (id) => abierta = id);

    await tester.tap(find.byKey(const Key('barra-reserva-xyz')));
    await tester.pump();

    expect(abierta, 'xyz');
  });

  testWidgets('la navegación pide la semana anterior y la siguiente', (tester) async {
    final repo = _RepoFake(_ocupacion());
    final c = await _montar(tester, repo);

    await c.semanaSiguiente();
    await c.semanaAnterior();
    await c.semanaAnterior();

    // El controlador normaliza a fecha local (sin hora).
    expect(repo.desdesPedidos, [
      DateTime(2026, 11, 1),
      DateTime(2026, 11, 8),
      DateTime(2026, 11, 1),
      DateTime(2026, 10, 25),
    ]);
    expect(c.desde, DateTime(2026, 10, 25));
  });

  test('EstadoDeReserva.desde mapea los códigos del backend', () {
    expect(EstadoDeReserva.desde('CONFIRMADA'), EstadoDeReserva.confirmada);
    expect(EstadoDeReserva.desde('check_in'), EstadoDeReserva.checkIn);
    expect(EstadoDeReserva.desde('CANCELADA'), EstadoDeReserva.otro);
    expect(EstadoDeReserva.desde(null), EstadoDeReserva.otro);
  });

  test('OcupacionDelCalendario.desdeJson arma recursos, reservas y bloqueos', () {
    final o = OcupacionDelCalendario.desdeJson({
      'desde': '2026-11-01',
      'hasta': '2026-11-03',
      'dias': ['2026-11-01', '2026-11-02', '2026-11-03'],
      'recursos': [
        {'id': 'r1', 'codigo': '101', 'nombre': 'Suite'},
      ],
      'reservas': [
        {
          'id': 'a',
          'recursoId': 'r1',
          'numero': 'RES-1',
          'estado': 'CHECK_IN',
          'desde': '2026-11-01T15:00:00Z',
          'hasta': '2026-11-02T11:00:00Z',
        },
      ],
      'bloqueos': [
        {
          'recursoId': 'r1',
          'motivo': 'LIMPIEZA',
          'desde': '2026-11-02T00:00:00Z',
          'hasta': '2026-11-03T00:00:00Z',
        },
      ],
    });

    expect(o.dias, hasLength(3));
    expect(o.recursos.single.codigo, '101');
    expect(o.reservasDe('r1').single.estado, EstadoDeReserva.checkIn);
    expect(o.bloqueosDe('r1').single.motivo, 'LIMPIEZA');
  });
}
