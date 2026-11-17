import 'package:dio/dio.dart' show FormData, MultipartFile;
import 'package:regenta_core/regenta_core.dart';

import 'bodega_del_negocio.dart';
import 'codigo_resuelto.dart';
import 'ficha_de_producto.dart';
import 'detallado.dart';
import 'historial_de_producto.dart';
import 'importacion.dart';
import 'inventario_general.dart';
import 'tablas_de_fracciones.dart';
import 'producto_encontrado.dart';

/// El acceso a `servicio-inventario` desde la app. Se apoya en el [ClienteHttp]
/// del núcleo, que ya traduce los errores del backend a [ErrorDeApi].
class RepositorioDeInventario {
  RepositorioDeInventario(this._http);

  final ClienteHttp _http;

  /// Busca por nombre, SKU o código. Con [termino] nulo o de menos de tres
  /// caracteres no se manda término y el backend lista todo (HU-035, criterio 1).
  Future<ResultadoDeBusqueda> buscar({
    String? termino,
    String? categoriaId,
    bool soloBajoMinimo = false,
    int? limite,
  }) async {
    final q = (termino ?? '').trim();
    final query = <String, dynamic>{};
    if (q.length >= 3) query['q'] = q;
    if (categoriaId != null) query['categoriaId'] = categoriaId;
    if (soloBajoMinimo) query['soloBajoMinimo'] = true;
    if (limite != null) query['limite'] = limite;

    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/inventario/productos/buscar',
      query: query,
    );
    return ResultadoDeBusqueda.desdeJson(cuerpo);
  }

  /// Las bodegas del negocio (HU-029). El negocio siempre tiene al menos una:
  /// el backend la asegura si todavía no existe ninguna.
  Future<List<BodegaDelNegocio>> bodegas() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/bodegas');
    return cuerpo
        .map((e) => BodegaDelNegocio.desdeJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Resuelve un código de barras (propio o alterno) al producto y su factor
  /// (HU-035, criterios 2 y 4). Lanza [ErrorDesconocido] con código 404 si
  /// ningún producto tiene ese código.
  Future<CodigoResuelto> resolverCodigo(String codigo) async {
    final cuerpo = await _http.get<Map<String, dynamic>>(
      '/api/inventario/productos/codigo/${Uri.encodeComponent(codigo.trim())}',
    );
    return CodigoResuelto.desdeJson(cuerpo);
  }

  /// HU-150 · la ficha completa de un producto.
  Future<FichaDeProducto> verProducto(String id) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/inventario/productos/$id');
    return FichaDeProducto.desdeJson(cuerpo);
  }

  /// HU-150 · guarda la ficha. El backend rechaza con 409 si alguien guardó antes (C5) o si el
  /// SKU o el código de barras ya los usa otro producto (C2).
  Future<FichaDeProducto> editarProducto(String id, EdicionDeProducto edicion) async {
    final cuerpo = await _http.put<Map<String, dynamic>>(
      '/api/inventario/productos/$id',
      datos: edicion.aJson(),
    ) as Map<String, dynamic>;
    return FichaDeProducto.desdeJson(cuerpo);
  }

  /// HU-151 · desactiva o reactiva un producto. Nunca se borra (I8).
  Future<FichaDeProducto> cambiarActivo(String id, {required bool activo}) async {
    final ruta = '/api/inventario/productos/$id/${activo ? 'reactivacion' : 'desactivacion'}';
    final cuerpo = await _http.post<Map<String, dynamic>>(ruta) as Map<String, dynamic>;
    return FichaDeProducto.desdeJson(cuerpo);
  }

  /// HU-151 C1 · cuánto hay del producto en cada bodega (solo las que tienen algo).
  Future<List<(String bodega, num cantidad)>> existenciasDe(String id) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/inventario/productos/$id/stock');
    return [
      for (final e in ((cuerpo['porBodega'] ?? const []) as List).cast<Map<String, dynamic>>())
        if (((e['cantidad'] ?? 0) as num) != 0)
          ((e['bodegaNombre'] ?? '') as String, (e['cantidad'] ?? 0) as num),
    ];
  }

  /// HU-152 · la ubicación del producto en cada bodega del negocio (vacía donde no se dijo).
  Future<List<UbicacionEnBodega>> ubicacionesDe(String id) async {
    final todas = await bodegas();
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/inventario/productos/$id/stock');
    final dichas = <String, String>{
      for (final e in ((cuerpo['porBodega'] ?? const []) as List).cast<Map<String, dynamic>>())
        if (e['ubicacion'] != null) e['bodegaId'] as String: e['ubicacion'] as String,
    };
    return [
      for (final b in todas)
        UbicacionEnBodega(bodegaId: b.id, bodegaNombre: b.nombre, ubicacion: dichas[b.id]),
    ];
  }

  /// HU-152 · fija la ubicación en una bodega; en blanco la quita.
  Future<void> fijarUbicacion(String id, String bodegaId, String ubicacion) async {
    await _http.put<void>('/api/inventario/productos/$id/ubicaciones/$bodegaId',
        datos: {'ubicacion': ubicacion});
  }

  /// HU-153 · el inventario general, filtrado y paginado, con los totales de todo lo filtrado.
  Future<ResultadoDeInventario> inventarioGeneral(ConsultaDeInventario consulta) async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/inventario/general',
        query: consulta.comoQuery());
    return ResultadoDeInventario.desdeJson(cuerpo);
  }

  /// HU-154 · fija la tarifa a los productos elegidos o, sin elegidos, a toda la categoría.
  /// Devuelve cuántos cambiaron. Todo o nada.
  Future<int> cambiarTarifaEnBloque(
      {List<String> productoIds = const [], String? categoriaId, required String tarifaIva}) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
      '/api/inventario/productos/tarifa-en-bloque',
      datos: {
        'productoIds': productoIds,
        'categoriaId': ?categoriaId,
        'tarifaIva': tarifaIva,
      },
    ) as Map<String, dynamic>;
    return ((cuerpo['cambiados'] ?? 0) as num).toInt();
  }

  /// HU-155 · las unidades de medida del negocio, con todos sus datos.
  Future<List<UnidadDeMedida>> unidadesDeMedida() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/unidades');
    return [
      for (final u in cuerpo.cast<Map<String, dynamic>>()) UnidadDeMedida.desdeJson(u),
    ];
  }

  /// HU-155 C1/C2 · crea una unidad; el backend rechaza un código repetido.
  Future<UnidadDeMedida> crearUnidad(String codigo, String nombre, bool permiteDecimales) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/inventario/unidades', datos: {
      'codigo': codigo,
      'nombre': nombre,
      'permiteDecimales': permiteDecimales,
    }) as Map<String, dynamic>;
    return UnidadDeMedida.desdeJson(cuerpo);
  }

  /// Corrige una unidad: código (el backend no deja repetir el de otra), nombre y decimales.
  Future<UnidadDeMedida> editarUnidad(
      String id, String codigo, String nombre, bool permiteDecimales) async {
    final cuerpo = await _http.put<Map<String, dynamic>>('/api/inventario/unidades/$id', datos: {
      'codigo': codigo,
      'nombre': nombre,
      'permiteDecimales': permiteDecimales,
    }) as Map<String, dynamic>;
    return UnidadDeMedida.desdeJson(cuerpo);
  }

  /// HU-155 C3 · desactiva una unidad; el backend la rechaza si algún producto la usa.
  Future<UnidadDeMedida> desactivarUnidad(String id) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
        '/api/inventario/unidades/$id/desactivacion') as Map<String, dynamic>;
    return UnidadDeMedida.desdeJson(cuerpo);
  }

  /// HU-156 · las tablas de fracciones del negocio.
  Future<List<TablaDeFracciones>> tablasDeFracciones() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/fracciones/tablas');
    return [
      for (final t in cuerpo.cast<Map<String, dynamic>>()) TablaDeFracciones.desdeJson(t),
    ];
  }

  /// HU-156 · crea la tabla (sin id) o la reemplaza (con id): aplica a todos sus productos.
  Future<TablaDeFracciones> guardarTablaDeFracciones(TablaDeFracciones tabla) async {
    final Object? cuerpo = tabla.id == null
        ? await _http.post<Map<String, dynamic>>('/api/inventario/fracciones/tablas',
            datos: tabla.aJson())
        : await _http.put<Map<String, dynamic>>('/api/inventario/fracciones/tablas/${tabla.id}',
            datos: tabla.aJson());
    return TablaDeFracciones.desdeJson(cuerpo! as Map<String, dynamic>);
  }

  /// HU-156 C3/C5 · asigna productos a la tabla. Si alguno ya tiene otra, el backend responde
  /// 409 ([RecursoDuplicado]) y hay que reenviar con [confirmarCambio].
  Future<int> asignarTablaDeFracciones(String tablaId, List<String> productoIds,
      {bool confirmarCambio = false}) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
      '/api/inventario/fracciones/tablas/$tablaId/asignacion',
      datos: {'productoIds': productoIds, 'confirmarCambio': confirmarCambio},
    ) as Map<String, dynamic>;
    return ((cuerpo['asignados'] ?? 0) as num).toInt();
  }

  /// HU-156/157 · las fracciones con que se vende un producto, con su origen (TABLA o PROPIO).
  Future<List<FraccionDeTabla>> fraccionesDe(String id) async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/productos/$id/fracciones');
    return [for (final f in cuerpo.cast<Map<String, dynamic>>()) FraccionDeTabla.desdeJson(f)];
  }

  /// HU-157 C1 · precio propio de una fracción para este producto.
  Future<void> fijarPrecioPropio(String id, String medida, num precio) async {
    await _http.put<void>('/api/inventario/productos/$id/fracciones/precio-propio',
        datos: {'medida': medida, 'precio': precio});
  }

  /// HU-157 C3 · vuelve al precio de la tabla.
  Future<void> quitarPrecioPropio(String id, String medida) async {
    await _http.delete<void>(
        '/api/inventario/productos/$id/fracciones/precio-propio?medida=${Uri.encodeQueryComponent(medida)}');
  }

  /// HU-159 · suma, resta o fija por conteo la existencia. El backend rechaza restar más de lo
  /// que hay (I10) con un mensaje que se muestra tal cual.
  Future<void> ajustarExistencia(AjusteDeExistencia ajuste) async {
    await _http.post<Map<String, dynamic>>('/api/inventario/ajustes/rapido', datos: ajuste.aJson());
  }

  /// HU-160 · el historial del producto, del más nuevo al más viejo, filtrado.
  Future<List<MovimientoDeHistorial>> historialDe(String productoId,
      {DateTime? desde, DateTime? hasta, String? bodegaId}) async {
    String dia(DateTime d) =>
        '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    final cuerpo = await _http.get<List<dynamic>>(
      '/api/inventario/movimientos/producto/$productoId/historial',
      query: {
        if (desde != null) 'desde': dia(desde),
        if (hasta != null) 'hasta': dia(hasta),
        'bodegaId': ?bodegaId,
      },
    );
    return [for (final m in cuerpo.cast<Map<String, dynamic>>()) MovimientoDeHistorial.desdeJson(m)];
  }

  /// HU-161 · las equivalencias de detallado del negocio.
  Future<List<EquivalenciaDeDetallado>> equivalencias() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/detallado/equivalencias');
    return [for (final e in cuerpo.cast<Map<String, dynamic>>()) EquivalenciaDeDetallado.desdeJson(e)];
  }

  /// HU-161 C1/C2/C4 · guarda «1 origen = factor destino»; el backend valida unidad, par y factor.
  Future<EquivalenciaDeDetallado> crearEquivalencia(String origenId, String destinoId, num factor) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/inventario/detallado/equivalencias',
        datos: {'origenId': origenId, 'destinoId': destinoId, 'factor': factor}) as Map<String, dynamic>;
    return EquivalenciaDeDetallado.desdeJson(cuerpo);
  }

  /// HU-161 C3 · replica la equivalencia a [origenIds], uniendo cada uno con su par por nombre.
  Future<ResultadoDeReplica> replicarEquivalencia(
      List<String> origenIds, String buscar, String reemplazar, num factor) async {
    final cuerpo = await _http.post<Map<String, dynamic>>(
      '/api/inventario/detallado/equivalencias/replica',
      datos: {'origenIds': origenIds, 'buscar': buscar, 'reemplazar': reemplazar, 'factor': factor},
    ) as Map<String, dynamic>;
    return ResultadoDeReplica.desdeJson(cuerpo);
  }

  /// HU-162 · abre [cantidad] del producto en su destino; devuelve qué pasó, en palabras. El
  /// backend rechaza abrir más de lo que hay o un producto sin equivalencia.
  Future<String> detallar(String origenId, String bodegaId, num cantidad) async {
    final r = await _http.post<Map<String, dynamic>>('/api/inventario/detallado',
        datos: {'origenId': origenId, 'bodegaId': bodegaId, 'cantidad': cantidad}) as Map<String, dynamic>;
    String n(Object? v) {
      final x = (v ?? 0) as num;
      return x == x.roundToDouble() ? '${x.round()}' : '$x';
    }

    return 'Se abrieron ${n(r['sale'])} ${r['origen']} → ${n(r['entra'])} ${r['destino']}';
  }

  /// HU-164 C1 · la plantilla .xlsx.
  Future<List<int>> plantillaDeImportacion() =>
      _http.bytes('GET', '/api/inventario/importacion/plantilla');

  /// HU-164 C2 · cada fila con sus errores, sin grabar nada.
  Future<VistaPreviaDeImportacion> vistaPreviaDeImportacion(ArchivoElegido archivo) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/inventario/importacion/vista-previa',
        datos: _formulario(archivo)) as Map<String, dynamic>;
    return VistaPreviaDeImportacion.desdeJson(cuerpo);
  }

  /// HU-164 C3 · crea las filas válidas y omite las que tienen error.
  Future<ResultadoDeImportacion> importarProductos(ArchivoElegido archivo) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/inventario/importacion',
        datos: _formulario(archivo)) as Map<String, dynamic>;
    return ResultadoDeImportacion.desdeJson(cuerpo);
  }

  /// HU-164 C5 · las filas omitidas con su error, como .xlsx.
  Future<List<int>> omitidasComoExcel(List<FilaDeImportacion> omitidas) =>
      _http.bytes('POST', '/api/inventario/importacion/omitidas',
          datos: [for (final f in omitidas) f.datos]);

  static FormData _formulario(ArchivoElegido archivo) => FormData.fromMap({
        'archivo': MultipartFile.fromBytes(archivo.bytes, filename: archivo.nombre),
      });

  /// Las categorías del negocio, para elegir en la ficha.
  Future<List<OpcionDeFicha>> categorias() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/categorias');
    return [
      for (final c in cuerpo.cast<Map<String, dynamic>>())
        OpcionDeFicha(id: c['id'] as String, nombre: (c['nombre'] ?? '') as String),
    ];
  }

  /// Las unidades de medida del negocio, para elegir en la ficha.
  Future<List<OpcionDeFicha>> unidades() async {
    final cuerpo = await _http.get<List<dynamic>>('/api/inventario/unidades');
    return [
      for (final u in cuerpo.cast<Map<String, dynamic>>())
        OpcionDeFicha(id: u['id'] as String, nombre: '${u['codigo']} · ${u['nombre']}'),
    ];
  }

  /// R12 · registra un producto de verdad. Si el backend lo rechaza, el [ErrorDeApi] sube con su
  /// mensaje (antes se tragaba el error y se inventaba un producto que no existía).
  Future<String> crearProducto(NuevoProducto nuevo) async {
    final cuerpo = await _http.post<Map<String, dynamic>>('/api/inventario/productos',
        datos: nuevo.aJson()) as Map<String, dynamic>;
    return cuerpo['id'] as String;
  }
}
