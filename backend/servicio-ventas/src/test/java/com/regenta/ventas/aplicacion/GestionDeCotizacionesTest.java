package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.ventas.BaseDeVentas;

/** HU-044. Cotizaciones que se convierten en venta. */
class GestionDeCotizacionesTest extends BaseDeVentas {

    private static final Set<String> SETUP = Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER",
            "VENTAS_COTIZACION_CREAR", "VENTAS_COTIZACION_CONVERTIR", "VENTAS_COTIZACION_VER");

    @Autowired
    private GestionDeCotizaciones cotizaciones;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private UUID crearCotizacion(LocalDate validaHasta) {
        return enContexto(negocio, usuario, SETUP, () -> cotizaciones.crear(new SolicitudDeCotizacion(
                null, validaHasta,
                List.of(new LineaDeCotizacion(UUID.randomUUID(), "SKU-1", "Martillo", "UND",
                                new BigDecimal("2"), new BigDecimal("100"), BigDecimal.ZERO, "IVA0",
                                BigDecimal.ZERO, new BigDecimal("40")),
                        new LineaDeCotizacion(UUID.randomUUID(), "SKU-2", "Clavos", "KG",
                                new BigDecimal("5"), new BigDecimal("20"), BigDecimal.ZERO, "IVA0",
                                BigDecimal.ZERO, new BigDecimal("8"))))).id());
    }

    @Test
    @DisplayName("Criterio 1: convertir una cotización crea una venta en borrador con las mismas líneas")
    void convertirCreaVentaConLasMismasLineas() {
        UUID cot = crearCotizacion(LocalDate.now().plusDays(15));

        ResultadoDeConversion r = enContexto(negocio, usuario, SETUP,
                () -> cotizaciones.convertir(cot, bodega));

        assertThat(comoElServicio(negocio, "select estado from ventas where id = '" + r.ventaId()
                + "'")).containsExactly("BORRADOR");
        assertThat(comoElServicio(negocio, "select sku_snapshot from venta_lineas where venta_id = '"
                + r.ventaId() + "' order by linea")).containsExactly("SKU-1", "SKU-2");
        assertThat(comoElServicio(negocio, "select cantidad from venta_lineas where venta_id = '"
                + r.ventaId() + "' order by linea")).containsExactly("2.000000", "5.000000");
        assertThat(comoElServicio(negocio, "select estado from cotizaciones where id = '" + cot + "'"))
                .containsExactly("CONVERTIDA");
        assertThat(r.preciosDesactualizados()).isFalse();
    }

    @Test
    @DisplayName("Criterio 2: convertir una cotización vencida avisa que los precios pueden haber cambiado")
    void cotizacionVencidaAvisa() {
        UUID cot = crearCotizacion(LocalDate.now().minusDays(1));

        ResultadoDeConversion r = enContexto(negocio, usuario, SETUP,
                () -> cotizaciones.convertir(cot, bodega));

        assertThat(r.preciosDesactualizados()).isTrue();
        // Igual se convierte: la advertencia no bloquea.
        assertThat(comoElServicio(negocio, "select estado from cotizaciones where id = '" + cot + "'"))
                .containsExactly("CONVERTIDA");
    }

    @Test
    @DisplayName("Criterio 3: la cotización convertida queda enlazada a la venta resultante")
    void quedaEnlazadaALaVenta() {
        UUID cot = crearCotizacion(null);

        ResultadoDeConversion r = enContexto(negocio, usuario, SETUP,
                () -> cotizaciones.convertir(cot, bodega));

        CotizacionDelNegocio vista = enContexto(negocio, usuario, SETUP,
                () -> cotizaciones.ver(cot));
        assertThat(vista.ventaId()).isEqualTo(r.ventaId());
    }

    @Test
    @DisplayName("Una cotización ya convertida no se vuelve a convertir")
    void noSeConvierteDosVeces() {
        UUID cot = crearCotizacion(null);
        enContexto(negocio, usuario, SETUP, () -> cotizaciones.convertir(cot, bodega));

        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP,
                () -> cotizaciones.convertir(cot, bodega)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Las cotizaciones de otro negocio no se ven")
    void aisladoPorNegocio() {
        crearCotizacion(null);
        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from cotizaciones"))
                .containsExactly("0");
    }
}
