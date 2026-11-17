/// HU-149 · el IVA cuando el precio ya lo incluye (I2 de `docs/dominio/inventario.md`):
/// base = total / (1 + t), a 2 decimales; IVA = total − base. Igual que el backend.
num redondear2(num valor) => (valor * 100).round() / 100;

num baseSinIva(num totalConIva, num porcentaje) =>
    porcentaje == 0 ? totalConIva : redondear2(totalConIva / (1 + porcentaje / 100));

/// Base e IVA de un grupo de líneas con la misma tarifa.
class IvaDeTarifa {
  const IvaDeTarifa({required this.porcentaje, required this.base, required this.iva});

  final num porcentaje;
  final num base;
  final num iva;
}
