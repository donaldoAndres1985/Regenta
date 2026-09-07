import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

import 'dobles.dart';

/// HU-109 criterio 1: el token se guarda en flutter_secure_storage, y en
/// Android eso es el keystore (no unos SharedPreferences en claro).
void main() {
  test('en Android el almacen seguro usa el keystore', () {
    final opciones = AlmacenSeguroDeSesion.opcionesAndroid.toMap();
    expect(opciones['encryptedSharedPreferences'], 'true');
  });

  test('guardar y leer devuelve la misma sesion', () async {
    final almacen = AlmacenEnMemoria();
    final sesion = sesionDe(expiraEn: DateTime.utc(2030));

    await almacen.guardar(sesion);
    final leida = await almacen.leer();

    expect(leida, isNotNull);
    expect(leida!.tokenDeAcceso, sesion.tokenDeAcceso);
    expect(leida.tokenDeRefresco, sesion.tokenDeRefresco);
    expect(leida.expiraEn, sesion.expiraEn);
    expect(leida.negocioId, sesion.negocioId);
    expect(leida.roles, sesion.roles);
    expect(leida.modulos, sesion.modulos);
  });

  test('borrar deja el almacen vacio', () async {
    final almacen = AlmacenEnMemoria()..guardar(sesionDe());
    await almacen.borrar();
    expect(await almacen.leer(), isNull);
  });

  test('un almacen vacio lee null, no revienta', () async {
    expect(await AlmacenEnMemoria().leer(), isNull);
  });

  test('la sesion redondea por JSON sin perder nada', () {
    final sesion = sesionDe(expiraEn: DateTime.utc(2031, 5, 4, 3, 2, 1));
    final copia = Sesion.fromJson(sesion.toJson());
    expect(copia.toJson(), sesion.toJson());
  });
}
