import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// Un doble de [RepositorioDeVentas] enfocado solo en el flujo de Cobro
/// (Etapa 0): controla cómo responde cada paso para ejercitar las ramas de
/// `ControladorDeCobro` sin levantar widgets ni red real.
class _RepoDeCobroFake implements RepositorioDeVentas {
  bool sinRedAlCrear = false;
  String estadoTrasConfirmar = 'CONFIRMADA';
  bool rechazaCreditoPorCarteraVencida = false;
  bool creditoAutorizado = false;

  final List<LineaParaEnviar> lineasRecibidas = [];
  final List<PagoParaEnviar> pagosRecibidos = [];
  bool cerrada = false;
  bool confirmadaPorPasos = false;
  String? clienteDelBorrador;
  String? notaDelBorrador;

  @override
  Future<VentaEnProceso> crearBorrador({
    required String bodegaId,
    String? clienteId,
    String? notaCliente,
  }) async {
    if (sinRedAlCrear) throw const ErrorDeRed();
    clienteDelBorrador = clienteId;
    notaDelBorrador = notaCliente;
    return const VentaEnProceso(id: 'v1', numero: 'FV-9', estado: 'BORRADOR', total: 0);
  }

  @override
  Future<void> agregarLineaAlBorrador(String ventaId, LineaParaEnviar linea) async {
    lineasRecibidas.add(linea);
  }

  @override
  Future<void> confirmarBorrador(String ventaId) async {
    confirmadaPorPasos = true;
  }

  @override
  Future<VentaEnProceso> verVenta(String ventaId) async =>
      VentaEnProceso(id: ventaId, numero: 'FV-9', estado: estadoTrasConfirmar, total: 0);

  @override
  Future<VentaEnProceso> esperarConfirmacion(
    String ventaId, {
    int intentos = 12,
    Duration intervalo = const Duration(milliseconds: 300),
  }) =>
      verVenta(ventaId);

  @override
  Future<void> confirmarACredito(String ventaId, {bool autorizado = false}) async {
    if (rechazaCreditoPorCarteraVencida && !autorizado) {
      throw const RecursoDuplicado('El cliente tiene cartera vencida hace 12 dias');
    }
    creditoAutorizado = autorizado;
  }

  @override
  Future<ResultadoDeUnPago> registrarPago(String ventaId, PagoParaEnviar pago) async {
    pagosRecibidos.add(pago);
    return ResultadoDeUnPago(
      pagoId: 'pago-${pagosRecibidos.length}',
      metodo: pago.metodo,
      montoAplicado: pago.monto ?? pago.montoRecibido ?? 0,
      cambio: pago.metodo == 'EFECTIVO' && pago.montoRecibido != null
          ? pago.montoRecibido! - (pago.monto ?? pago.montoRecibido!)
          : 0,
      saldoPendiente: 0,
      cubierta: true,
    );
  }

  @override
  Future<void> cerrarVenta(String ventaId) async {
    cerrada = true;
  }

  @override
  Future<List<VentaEnProceso>> listarBorradoresAbiertos(String bodegaId) async => const [];

  // No usados por ControladorDeCobro; cualquier llamada aquí es un error de test.
  @override
  Future<List<ProductoBuscado>> buscar(String termino) => throw UnimplementedError();
  @override
  Future<CodigoResuelto> resolverCodigo(String codigo) => throw UnimplementedError();
  @override
  Future<ProductoBuscado> verProducto(String productoId) => throw UnimplementedError();
  @override
  Future<num?> montoParaIdentificarComprador() => throw UnimplementedError();
  @override
  Future<ResultadoDeCobro> confirmarVenta({
    required String bodegaId,
    required List<LineaParaEnviar> lineas,
    String? clienteId,
  }) =>
      throw UnimplementedError();
}

const _linea = LineaParaEnviar(
  productoId: 'p1',
  sku: 'CEM-050',
  nombre: 'Cemento gris 50 kg',
  cantidad: 2,
  precioUnitario: 32000,
  impuestoPct: 19,
);

ControladorDeCobro _controlador(
  _RepoDeCobroFake repo, {
  num total = 64000,
  bool clienteAsignado = false,
  String? clienteId,
  String? notaCliente,
}) =>
    ControladorDeCobro(
      repo,
      total: total,
      clienteAsignado: clienteAsignado,
      bodegaId: 'bodega-1',
      clienteId: clienteId,
      notaCliente: notaCliente,
      lineas: const [_linea],
    );

void main() {
  test('pago único: cubre el total con un solo método y cierra la venta', () async {
    final repo = _RepoDeCobroFake();
    final controlador = _controlador(repo);

    controlador.pagarSaldoCon('TRANSFERENCIA');
    expect(controlador.state.cubierto, isTrue);

    await controlador.confirmarCobro();

    expect(repo.lineasRecibidas.single, _linea);
    expect(repo.confirmadaPorPasos, isTrue);
    expect(repo.pagosRecibidos.single.metodo, 'TRANSFERENCIA');
    expect(repo.pagosRecibidos.single.monto, 64000);
    expect(repo.cerrada, isTrue);
    expect(controlador.state.ventaNumero, 'FV-9');
    expect(controlador.state.terminado, isTrue);
  });

  test('pago mixto: efectivo con cambio más tarjeta, hasta cubrir el total', () async {
    final repo = _RepoDeCobroFake();
    final controlador = _controlador(repo, total: 64000);

    controlador.agregarPago(const LineaDePagoEnCurso(
      metodo: 'EFECTIVO',
      monto: 40000,
      montoRecibido: 50000,
    ));
    expect(controlador.state.saldoPendiente, 24000);
    controlador.pagarSaldoCon('TARJETA_DEBITO');
    expect(controlador.state.cubierto, isTrue);

    await controlador.confirmarCobro();

    expect(repo.pagosRecibidos, hasLength(2));
    expect(repo.pagosRecibidos.first.metodo, 'EFECTIVO');
    expect(repo.pagosRecibidos.first.montoRecibido, 50000);
    expect(repo.pagosRecibidos.last.metodo, 'TARJETA_DEBITO');
    expect(repo.pagosRecibidos.last.monto, 24000);
    expect(controlador.state.terminado, isTrue);
  });

  test('no deja confirmar si los pagos no cubren el total', () async {
    final repo = _RepoDeCobroFake();
    final controlador = _controlador(repo, total: 64000);
    controlador.agregarPago(const LineaDePagoEnCurso(metodo: 'EFECTIVO', monto: 1000, montoRecibido: 1000));

    await controlador.confirmarCobro();

    expect(repo.cerrada, isFalse, reason: 'no debió ni empezar a mandar nada');
    expect(controlador.state.error, contains('Completa el pago'));
  });

  test('sin stock: la saga devuelve la venta a BORRADOR y se avisa sin cerrar nada', () async {
    final repo = _RepoDeCobroFake()..estadoTrasConfirmar = 'BORRADOR';
    final controlador = _controlador(repo, total: 64000);
    controlador.pagarSaldoCon('EFECTIVO', montoRecibido: 64000);

    await controlador.confirmarCobro();

    expect(repo.cerrada, isFalse);
    expect(controlador.state.error, contains('No hay stock suficiente'));
    expect(controlador.state.terminado, isFalse);
  });

  test('sin conexión: el primer paso falla y se marca para que el POS caiga a /offline', () async {
    final repo = _RepoDeCobroFake()..sinRedAlCrear = true;
    final controlador = _controlador(repo, total: 64000);
    controlador.pagarSaldoCon('EFECTIVO', montoRecibido: 64000);

    await controlador.confirmarCobro();

    expect(controlador.state.sinConexion, isTrue);
    expect(controlador.state.error, isNull, reason: 'no es un error para el usuario: es una caída silenciosa');
    expect(repo.cerrada, isFalse);
  });

  group('crédito (HU-040)', () {
    test('sin cliente asignado no deja intentarlo', () async {
      final repo = _RepoDeCobroFake();
      final controlador = _controlador(repo, clienteAsignado: false);

      await controlador.confirmarACredito();

      expect(repo.lineasRecibidas, isEmpty, reason: 'nunca debió llegar a crear el borrador');
      expect(controlador.state.error, contains('cliente'));
    });

    test('con cliente y cupo disponible, confirma y cierra sin pedir pago', () async {
      final repo = _RepoDeCobroFake();
      final controlador = _controlador(repo, clienteAsignado: true, clienteId: 'c1');

      await controlador.confirmarACredito();

      expect(repo.clienteDelBorrador, 'c1');
      expect(repo.creditoAutorizado, isFalse);
      expect(repo.pagosRecibidos, isEmpty, reason: 'el crédito no registra pago: queda a cuenta por cobrar');
      expect(repo.cerrada, isFalse, reason: 'confirmarACredito ya cierra la venta en el backend');
      expect(controlador.state.ventaNumero, 'FV-9');
    });

    test('criterio 4: cartera vencida se rechaza una vez, y el reintento autorizado sí pasa', () async {
      final repo = _RepoDeCobroFake()..rechazaCreditoPorCarteraVencida = true;
      final controlador = _controlador(repo, clienteAsignado: true, clienteId: 'c1');

      await controlador.confirmarACredito();
      expect(controlador.state.requiereAutorizacionCredito, isTrue);
      expect(controlador.state.error, contains('cartera vencida'));
      expect(controlador.state.terminado, isFalse);

      await controlador.confirmarACredito(autorizado: true);
      expect(repo.creditoAutorizado, isTrue);
      expect(controlador.state.terminado, isTrue);
    });
  });

  test('la nota del cliente y el cliente asignado viajan al crear el borrador', () async {
    final repo = _RepoDeCobroFake();
    final controlador = _controlador(
      repo,
      clienteAsignado: true,
      clienteId: 'c7',
      notaCliente: 'Entregar en obra',
    );
    controlador.pagarSaldoCon('EFECTIVO', montoRecibido: 64000);

    await controlador.confirmarCobro();

    expect(repo.clienteDelBorrador, 'c7');
    expect(repo.notaDelBorrador, 'Entregar en obra');
  });
}
