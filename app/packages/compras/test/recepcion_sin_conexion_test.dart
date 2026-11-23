import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

/// El servidor de mentiras: sin señal, o respondiendo lo que se le diga por ruta. Cuenta lo que le
/// llega para comprobar que una recepción entra una sola vez.
class _Servidor implements HttpClientAdapter {
  bool conSenal = false;

  /// La respuesta de la próxima recepción del celular: 200 (entra) o un rechazo.
  ({int codigo, Map<String, dynamic> cuerpo}) alRecibir = (codigo: 200, cuerpo: {'id': 'r-9', 'numero': 'REC-9'});

  final List<String> recibidas = [];
  final Set<String> registradasEnElServidor = {};

  static const _orden = {
    'id': 'oc-1',
    'numero': 'OC-0231',
    'proveedorId': 'prov-1',
    'bodegaDestinoId': 'bod-1',
    'estado': 'ENVIADA',
    'lineas': [
      {
        'id': 'l1',
        'linea': 1,
        'productoId': 'p-ace',
        'nombre': 'Acetaminofén 500 mg x100',
        'cantidadPedida': 120,
        'cantidadRecibida': 0,
        'costoUnitario': 8400,
      },
    ],
  };

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream, Future<void>? cancelFuture) async {
    if (!conSenal) {
      throw DioException(requestOptions: options, type: DioExceptionType.connectionError, error: 'sin señal');
    }
    final ruta = options.path;
    if (ruta.startsWith(rutaDeRecepcionDesdeElCelular)) {
      recibidas.add(ruta);
      final r = alRecibir;
      if (r.codigo >= 400) {
        throw DioException(
          requestOptions: options,
          type: DioExceptionType.badResponse,
          response: Response(requestOptions: options, statusCode: r.codigo, data: r.cuerpo),
        );
      }
      // El backend es idempotente por el identificador (I32): la misma ruta no suma dos veces.
      registradasEnElServidor.add(ruta);
      return _json(r.cuerpo);
    }
    if (ruta == '/api/compras/ordenes/oc-1') return _json(_orden);
    if (ruta == '/api/compras/recepciones/precios-actuales') return _json(<Object>[]);
    return _json(const {});
  }

  ResponseBody _json(Object cuerpo) => ResponseBody.fromString(jsonEncode(cuerpo), 200, headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      });

  @override
  void close({bool force = false}) {}
}

void main() {
  late BaseLocal db;
  late _Servidor servidor;
  late Dio dio;
  late ColaDeSalidaLocal cola;
  late ClienteHttp http;
  late RepositorioDeRecepciones repo;
  late BandejaDeRecepciones bandeja;

  setUp(() {
    db = BaseLocal(NativeDatabase.memory());
    servidor = _Servidor();
    dio = Dio(BaseOptions(baseUrl: 'https://gw.test'))..httpClientAdapter = servidor;
    cola = ColaDeSalidaLocal(db);
    http = ClienteHttp(dio, cola: cola, respaldo: RespaldoDeConsultasLocal(db));
    repo = RepositorioDeRecepciones(http, cola: cola);
    bandeja = BandejaDeRecepciones(http: http, cola: cola);
  });
  tearDown(() => db.close());

  Future<ResultadoDeRecepcion> recibir({required String id, num cantidad = 120}) => repo.registrar(
        ordenId: 'oc-1',
        bodegaId: 'bod-1',
        ordenNumero: 'OC-0231',
        idDispositivo: id,
        lineas: [LineaParaRecibir(ordenLineaId: 'l1', cantidad: cantidad, exigeLote: false)],
      );

  Future<List<RecepcionPorEnviar>> porEnviar() => bandeja.observar().first;

  test('C1 · Sin señal queda guardada en el celular con su identificador, «por enviar»', () async {
    final r = await recibir(id: 'id-1');

    expect(r.encolada, isTrue);
    expect(r.idDispositivo, 'id-1');
    final [guardada] = await porEnviar();
    expect(guardada.id, 'id-1');
    expect(guardada.conError, isFalse);
    expect(guardada.ordenNumero, 'OC-0231');
    expect(guardada.lineas, 1);
  });

  test('C1 · La orden se abre sin señal con lo que se vio la última vez', () async {
    servidor.conSenal = true;
    await repo.verOrden('oc-1');
    servidor.conSenal = false;

    final orden = await repo.verOrden('oc-1');

    expect(orden.numero, 'OC-0231');
    expect(orden.lineas.single.cantidadPedida, 120);
  });

  test('C2 · Guardada dos veces y sincronizada dos veces, llega una sola vez', () async {
    await recibir(id: 'id-1');
    await recibir(id: 'id-1');
    expect(await porEnviar(), hasLength(1));

    servidor.conSenal = true;
    final trabajador = TrabajadorDeSincronizacion(dio: dio, db: db);
    await trabajador.ejecutarUnaPasada();
    await trabajador.ejecutarUnaPasada();

    expect(servidor.recibidas, ['${rutaDeRecepcionDesdeElCelular}id-1']);
    expect(servidor.registradasEnElServidor, hasLength(1));
    expect(await porEnviar(), isEmpty);
  });

  test('C3 · Si el backend la rechaza al sincronizar, queda en el celular con el motivo', () async {
    await recibir(id: 'id-1', cantidad: 130);
    servidor
      ..conSenal = true
      ..alRecibir = (codigo: 422, cuerpo: {'detail': 'La línea 1 pasa lo pedido más la tolerancia de 5 %'});

    await TrabajadorDeSincronizacion(dio: dio, db: db).ejecutarUnaPasada();

    final [rechazada] = await porEnviar();
    expect(rechazada.conError, isTrue);
    expect(rechazada.error, 'La línea 1 pasa lo pedido más la tolerancia de 5 %');
    expect(rechazada.lineasRecibidas['l1']!['cantidad'], 130);
  });

  test('C3 · Corregida y reenviada con el mismo identificador, entra y sale de la cola', () async {
    await recibir(id: 'id-1', cantidad: 130);
    servidor
      ..conSenal = true
      ..alRecibir = (codigo: 422, cuerpo: {'detail': 'Pasa la tolerancia'});
    await TrabajadorDeSincronizacion(dio: dio, db: db).ejecutarUnaPasada();
    servidor.alRecibir = (codigo: 200, cuerpo: {'id': 'r-9', 'numero': 'REC-9'});

    final r = await recibir(id: 'id-1', cantidad: 120);

    expect(r.numero, 'REC-9');
    expect(await porEnviar(), isEmpty);
    expect(servidor.registradasEnElServidor, {'${rutaDeRecepcionDesdeElCelular}id-1'});
  });

  test('«Enviar ahora» sin señal la deja por enviar; con un rechazo, con el motivo', () async {
    await recibir(id: 'id-1');
    final [guardada] = await porEnviar();

    expect(await bandeja.enviarAhora(guardada), contains('no hay señal'));
    expect((await porEnviar()).single.conError, isFalse);

    servidor
      ..conSenal = true
      ..alRecibir = (codigo: 409, cuerpo: {'detail': 'La factura FV-1 ya está registrada para ese proveedor'});
    expect(await bandeja.enviarAhora(guardada), 'La factura FV-1 ya está registrada para ese proveedor');
    expect((await porEnviar()).single.conError, isTrue);
  });
}
