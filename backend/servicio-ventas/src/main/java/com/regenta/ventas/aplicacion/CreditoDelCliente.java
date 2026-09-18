package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;

/**
 * Lo que Ventas necesita saber del crédito de un cliente para decidir si le
 * puede vender a plazo (HU-040). El dato vive en servicio-clientes (HU-022);
 * esto es la copia con la que se decide en el momento de confirmar, no una
 * réplica que haya que mantener.
 *
 * @param diasCredito el plazo pactado; de ahí sale la fecha de vencimiento
 * @param carteraVencida si hoy tiene alguna cuenta en mora (criterio 4)
 * @param diasMoraMaxima la mora más vieja, para poder decirla en la advertencia
 */
public record CreditoDelCliente(
        boolean habilitado,
        BigDecimal cupo,
        BigDecimal saldo,
        BigDecimal disponible,
        int diasCredito,
        boolean carteraVencida,
        long diasMoraMaxima) {

    public static CreditoDelCliente habilitado(BigDecimal cupo, BigDecimal saldo,
            BigDecimal disponible, int diasCredito, boolean carteraVencida, long diasMoraMaxima) {
        return new CreditoDelCliente(true, cupo, saldo, disponible, diasCredito, carteraVencida,
                diasMoraMaxima);
    }

    public static CreditoDelCliente sinCredito() {
        return new CreditoDelCliente(false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0,
                false, 0);
    }

    /** Cuánto se pasa del cupo una venta de {@code monto}, o cero si cabe. */
    public BigDecimal excesoPara(BigDecimal monto) {
        BigDecimal exceso = monto.subtract(disponible);
        return exceso.signum() > 0 ? exceso : BigDecimal.ZERO;
    }

    public boolean cabe(BigDecimal monto) {
        return habilitado && excesoPara(monto).signum() == 0;
    }
}
