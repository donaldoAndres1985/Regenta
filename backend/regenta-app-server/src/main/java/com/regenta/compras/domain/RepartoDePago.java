package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-171 · Cómo se reparte un pago entre las cuentas abiertas de un proveedor (I26 de
 * {@code docs/dominio/compras.md}). Cálculo puro.
 *
 * <ul>
 * <li>Sin reparto indicado, va de la cuenta más antigua a la más nueva hasta agotar el monto (las
 * cuentas llegan ya ordenadas por antigüedad).</li>
 * <li>Con reparto indicado, cada cuenta tiene que ser de las abiertas del proveedor, ninguna
 * aplicación puede superar su saldo y la suma tiene que ser el monto del pago.</li>
 * <li>Nunca se paga más de lo que se debe.</li>
 * </ul>
 */
public final class RepartoDePago {

    private RepartoDePago() {
    }

    /** Lo que se abona a una cuenta. */
    public record Abono(CuentaPorPagar cuenta, BigDecimal monto) {
    }

    /**
     * @param abiertas las cuentas del proveedor con saldo, de la más antigua a la más nueva
     * @param indicado el reparto que pidió el usuario (cuenta → monto), o {@code null} para repartir
     *        por antigüedad
     */
    public static List<Abono> repartir(BigDecimal monto, List<CuentaPorPagar> abiertas,
            Map<UUID, BigDecimal> indicado) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El pago debe ser mayor que cero");
        }
        BigDecimal deuda = abiertas.stream().map(CuentaPorPagar::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (monto.compareTo(deuda) > 0) {
            throw new ReglaDeNegocioException("No se paga más de lo que se debe: la deuda con este "
                    + "proveedor es " + deuda.stripTrailingZeros().toPlainString());
        }
        return indicado == null || indicado.isEmpty()
                ? porAntiguedad(monto, abiertas)
                : comoSeIndico(monto, abiertas, indicado);
    }

    private static List<Abono> porAntiguedad(BigDecimal monto, List<CuentaPorPagar> abiertas) {
        List<Abono> abonos = new ArrayList<>();
        BigDecimal falta = monto;
        for (CuentaPorPagar c : abiertas) {
            if (falta.signum() <= 0) {
                break;
            }
            BigDecimal parte = falta.min(c.getSaldo());
            if (parte.signum() > 0) {
                abonos.add(new Abono(c, parte));
                falta = falta.subtract(parte);
            }
        }
        return abonos;
    }

    private static List<Abono> comoSeIndico(BigDecimal monto, List<CuentaPorPagar> abiertas,
            Map<UUID, BigDecimal> indicado) {
        Map<UUID, CuentaPorPagar> porId = new LinkedHashMap<>();
        abiertas.forEach(c -> porId.put(c.getId(), c));
        List<Abono> abonos = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (Map.Entry<UUID, BigDecimal> e : indicado.entrySet()) {
            BigDecimal parte = e.getValue();
            if (parte == null || parte.signum() == 0) {
                continue;
            }
            CuentaPorPagar c = porId.get(e.getKey());
            if (c == null) {
                throw new ReglaDeNegocioException(
                        "Una de las cuentas del reparto no es de este proveedor o no tiene saldo");
            }
            if (parte.signum() < 0) {
                throw new ReglaDeNegocioException("Lo que se abona a una cuenta no puede ser negativo");
            }
            if (parte.compareTo(c.getSaldo()) > 0) {
                throw new ReglaDeNegocioException("A la factura " + documento(c) + " se le deben "
                        + c.getSaldo().stripTrailingZeros().toPlainString() + ": no se le abona más");
            }
            abonos.add(new Abono(c, parte));
            suma = suma.add(parte);
        }
        if (suma.compareTo(monto) != 0) {
            throw new ReglaDeNegocioException("El reparto suma " + suma.stripTrailingZeros().toPlainString()
                    + " y el pago es de " + monto.stripTrailingZeros().toPlainString());
        }
        return abonos;
    }

    private static String documento(CuentaPorPagar c) {
        return c.getNumeroFactura() == null ? "en espera de factura" : c.getNumeroFactura();
    }
}
