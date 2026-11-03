import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

void main() {
  test('los montos en COP llevan espacio tras el signo y punto de miles', () {
    expect(formatearPesos(1250000), r'$ 1.250.000');
    expect(formatearPesos(500), r'$ 500');
    expect(formatearPesos(0), r'$ 0');
    expect(formatearPesos(-18400), r'-$ 18.400');
  });
}
