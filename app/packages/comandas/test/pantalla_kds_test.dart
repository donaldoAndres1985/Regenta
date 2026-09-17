import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_comandas/regenta_comandas.dart';

class _RepoFake implements RepositorioDeCocina {
  _RepoFake({this.estacionesFake = const [], Map<String, List<TicketDeCocina>>? porEstacion})
      : porEstacion = porEstacion ?? {};

  List<EstacionDeCocina> estacionesFake;
  Map<String, List<TicketDeCocina>> porEstacion;
  ErrorDeApi? errorAlCargarEstaciones;
  final List<String> avances = [];

  @override
  Future<List<EstacionDeCocina>> estaciones() async {
    if (errorAlCargarEstaciones != null) throw errorAlCargarEstaciones!;
    return estacionesFake;
  }

  @override
  Future<List<TicketDeCocina>> ticketsDeEstacion(String estacionId) async =>
      porEstacion[estacionId] ?? const [];

  @override
  Future<TicketDeCocina> avanzarTicket(String ticketId) async {
    avances.add(ticketId);
    for (final entrada in porEstacion.entries) {
      final i = entrada.value.indexWhere((t) => t.id == ticketId);
      if (i == -1) continue;
      final t = entrada.value[i];
      final siguiente = switch (t.estado) {
        'NUEVO' => 'EN_PREPARACION',
        'EN_PREPARACION' => 'LISTO',
        'LISTO' => 'ENTREGADO',
        _ => t.estado,
      };
      final actualizado = TicketDeCocina(
        id: t.id,
        comandaId: t.comandaId,
        comandaNumero: t.comandaNumero,
        estacionId: t.estacionId,
        secuencia: t.secuencia,
        estado: siguiente,
        minutosTranscurridos: t.minutosTranscurridos,
        demorado: t.demorado,
        lineas: t.lineas,
      );
      final lista = [...entrada.value];
      lista[i] = actualizado;
      porEstacion[entrada.key] = lista;
      return actualizado;
    }
    throw const ErrorDeRed('Ese ticket no existe');
  }
}

TicketDeCocina _ticket({
  required String id,
  String estado = 'NUEVO',
  int minutos = 3,
  bool demorado = false,
  String estacionId = 'parrilla',
  String comandaNumero = 'CMD-0421',
  int secuencia = 1,
}) =>
    TicketDeCocina(
      id: id,
      comandaId: 'c-$id',
      comandaNumero: comandaNumero,
      estacionId: estacionId,
      secuencia: secuencia,
      estado: estado,
      minutosTranscurridos: minutos,
      demorado: demorado,
      lineas: const [LineaDeTicket(id: 'l1', nombre: 'Churrasco 300 g', cantidad: 1, notas: null)],
    );

const _parrilla = EstacionDeCocina(id: 'parrilla', nombre: 'Parrilla', codigo: 'PARRILLA', activa: true, orden: 1);
const _bar = EstacionDeCocina(id: 'bar', nombre: 'Bar', codigo: 'BAR', activa: true, orden: 2);

Future<void> _montar(WidgetTester tester, _RepoFake repo, {Size size = const Size(1440, 900)}) async {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);

  await tester.pumpWidget(
    ProviderScope(
      overrides: [repositorioDeCocinaProvider.overrideWithValue(repo)],
      child: const MaterialApp(home: PantallaKds()),
    ),
  );
  await tester.pump();
}

void main() {
  testWidgets('carga las estaciones y abre en la primera activa', (tester) async {
    final repo = _RepoFake(
      estacionesFake: [_parrilla, _bar],
      porEstacion: {
        'parrilla': [_ticket(id: 't1')],
      },
    );
    await _montar(tester, repo);

    expect(find.byKey(const Key('kds-tab-parrilla')), findsOneWidget);
    expect(find.byKey(const Key('kds-tab-bar')), findsOneWidget);
    expect(find.byKey(const Key('ticket-t1')), findsOneWidget);
  });

  testWidgets('en escritorio los tickets se agrupan en sus tres columnas', (tester) async {
    final repo = _RepoFake(
      estacionesFake: [_parrilla],
      porEstacion: {
        'parrilla': [
          _ticket(id: 'nuevo', estado: 'NUEVO'),
          _ticket(id: 'prep', estado: 'EN_PREPARACION'),
          _ticket(id: 'listo', estado: 'LISTO'),
        ],
      },
    );
    await _montar(tester, repo);

    expect(find.byKey(const Key('kds-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('ticket-nuevo')), findsOneWidget);
    expect(find.byKey(const Key('ticket-prep')), findsOneWidget);
    expect(find.byKey(const Key('ticket-listo')), findsOneWidget);
    // Cada uno con el botón de su propio siguiente paso.
    expect(find.byKey(const Key('ticket-empezar-nuevo')), findsOneWidget);
    expect(find.byKey(const Key('ticket-listo-prep')), findsOneWidget);
    expect(find.byKey(const Key('ticket-entregado-listo')), findsOneWidget);
  });

  testWidgets('en móvil los tickets van en una sola lista', (tester) async {
    final repo = _RepoFake(
      estacionesFake: [_parrilla],
      porEstacion: {
        'parrilla': [_ticket(id: 't1', estado: 'NUEVO')],
      },
    );
    await _montar(tester, repo, size: const Size(390, 844));

    expect(find.byKey(const Key('kds-movil')), findsOneWidget);
    expect(find.byKey(const Key('ticket-t1')), findsOneWidget);
  });

  testWidgets('«Empezar» avanza el ticket de NUEVO a EN_PREPARACION', (tester) async {
    final repo = _RepoFake(
      estacionesFake: [_parrilla],
      porEstacion: {
        'parrilla': [_ticket(id: 't1', estado: 'NUEVO')],
      },
    );
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ticket-empezar-t1')));
    await tester.pumpAndSettle();

    expect(repo.avances, ['t1']);
    expect(find.byKey(const Key('ticket-listo-t1')), findsOneWidget);
    expect(find.byKey(const Key('ticket-empezar-t1')), findsNothing);
  });

  testWidgets('criterio 4: «Listo» desde NUEVO llega a LISTO de una vez (avanza de más)', (tester) async {
    final repo = _RepoFake(
      estacionesFake: [_parrilla],
      porEstacion: {
        'parrilla': [_ticket(id: 't1', estado: 'NUEVO')],
      },
    );
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ticket-listo-t1')));
    await tester.pumpAndSettle();

    expect(repo.avances, ['t1', 't1']); // NUEVO->EN_PREPARACION->LISTO
    expect(find.byKey(const Key('ticket-entregado-t1')), findsOneWidget);
  });

  testWidgets('criterio 2: al entregar, el ticket sale de las columnas visibles', (tester) async {
    final repo = _RepoFake(
      estacionesFake: [_parrilla],
      porEstacion: {
        'parrilla': [_ticket(id: 't1', estado: 'LISTO')],
      },
    );
    await _montar(tester, repo);

    expect(find.byKey(const Key('ticket-t1')), findsOneWidget);
    await tester.tap(find.byKey(const Key('ticket-entregado-t1')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('ticket-t1')), findsNothing);
  });

  testWidgets('cambiar de pestaña carga los tickets de la otra estación', (tester) async {
    final repo = _RepoFake(
      estacionesFake: [_parrilla, _bar],
      porEstacion: {
        'parrilla': [_ticket(id: 'p1', estacionId: 'parrilla')],
        'bar': [_ticket(id: 'b1', estacionId: 'bar')],
      },
    );
    await _montar(tester, repo);

    expect(find.byKey(const Key('ticket-p1')), findsOneWidget);
    expect(find.byKey(const Key('ticket-b1')), findsNothing);

    await tester.tap(find.byKey(const Key('kds-tab-bar')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('ticket-b1')), findsOneWidget);
    expect(find.byKey(const Key('ticket-p1')), findsNothing);
  });

  testWidgets('columna vacía muestra su texto propio', (tester) async {
    final repo = _RepoFake(estacionesFake: [_parrilla], porEstacion: {'parrilla': []});
    await _montar(tester, repo);

    expect(find.text('Nada nuevo'), findsOneWidget);
    expect(find.text('Nada en preparación'), findsOneWidget);
    expect(find.text('Nada listo por ahora'), findsOneWidget);
  });

  testWidgets('error al cargar por primera vez ofrece reintentar', (tester) async {
    final repo = _RepoFake()..errorAlCargarEstaciones = const ErrorDeRed('Sin conexión');
    await _montar(tester, repo);

    expect(find.byKey(const Key('kds-reintentar')), findsOneWidget);

    repo.errorAlCargarEstaciones = null;
    repo.estacionesFake = [_parrilla];
    repo.porEstacion = {
      'parrilla': [_ticket(id: 't1')],
    };
    await tester.tap(find.byKey(const Key('kds-reintentar')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('ticket-t1')), findsOneWidget);
  });

  testWidgets('criterio 3: un ticket demorado destaca en rojo (RegentaColors.crit)', (tester) async {
    final repo = _RepoFake(
      estacionesFake: [_parrilla],
      porEstacion: {
        'parrilla': [_ticket(id: 't1', estado: 'EN_PREPARACION', minutos: 20, demorado: true)],
      },
    );
    await _montar(tester, repo);

    final tarjeta = tester.widget<Container>(find.byKey(const Key('ticket-t1')));
    final decoracion = tarjeta.decoration as BoxDecoration;
    expect((decoracion.border as Border).top.color, RegentaColors.crit);
  });
}
