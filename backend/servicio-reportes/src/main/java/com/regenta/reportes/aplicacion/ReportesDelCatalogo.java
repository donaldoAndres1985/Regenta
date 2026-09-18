package com.regenta.reportes.aplicacion;

import java.time.LocalDate;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los reportes exportables, cada uno delegando en el servicio que ya sabe
 * calcularlo (HU-100). Aquí no hay lógica de negocio: solo qué columnas tiene
 * cada reporte y en qué orden.
 *
 * <p>Los de Reserva y Comanda pasan por {@link MetricasDeReserva} y
 * {@link MetricasDeComanda}, que ya rechazan al negocio del patrón que no es
 * (HU-099 criterio 3). Programar la ocupación en una ferretería falla, y falla
 * en el mismo sitio en que falla consultarla.
 */
@Configuration
public class ReportesDelCatalogo {

    @Bean
    ReporteExportable ventasPorCategoria(ReporteDeVentas ventas) {
        return new ReporteExportable() {
            @Override
            public String codigo() {
                return "VENTAS_POR_CATEGORIA";
            }

            @Override
            public String nombre() {
                return "Ventas por categoría";
            }

            @Override
            public String patron() {
                return "VENTA_DIRECTA";
            }

            @Override
            public List<String> columnas() {
                return List.of("Categoría", "Monto", "Unidades", "Margen");
            }

            @Override
            public List<List<Object>> filas(LocalDate desde, LocalDate hasta) {
                return ventas.porCategoria(desde, hasta, null).stream()
                        .map(v -> List.<Object>of(v.categoria(), v.monto(), v.unidades(), v.margen()))
                        .toList();
            }
        };
    }

    @Bean
    ReporteExportable rotacionDeInventario(ReporteDeVentas ventas) {
        return new ReporteExportable() {
            @Override
            public String codigo() {
                return "ROTACION_INVENTARIO";
            }

            @Override
            public String nombre() {
                return "Rotación de inventario";
            }

            @Override
            public String patron() {
                return "VENTA_DIRECTA";
            }

            @Override
            public List<String> columnas() {
                return List.of("Producto", "Último movimiento", "Días sin movimiento");
            }

            @Override
            public List<List<Object>> filas(LocalDate desde, LocalDate hasta) {
                // La rotación mira hacia atrás desde hoy: no toma rango.
                return ventas.rotacion(null).stream()
                        .map(r -> java.util.Arrays.<Object>asList(r.nombre(),
                                r.ultimoMovimiento() == null ? "" : r.ultimoMovimiento().toString(),
                                r.diasSinMovimiento()))
                        .toList();
            }
        };
    }

    @Bean
    ReporteExportable ocupacionDiaria(MetricasDeReserva reserva) {
        return new ReporteExportable() {
            @Override
            public String codigo() {
                return "OCUPACION";
            }

            @Override
            public String nombre() {
                return "Ocupación, ADR y RevPAR";
            }

            @Override
            public String patron() {
                return "RESERVA";
            }

            @Override
            public List<String> columnas() {
                return List.of("Fecha", "Tipo de recurso", "Totales", "Ocupados", "Ocupación %",
                        "ADR", "RevPAR");
            }

            @Override
            public List<List<Object>> filas(LocalDate desde, LocalDate hasta) {
                return reserva.ocupacion(desde, hasta).stream()
                        .map(o -> java.util.Arrays.<Object>asList(o.fecha().toString(),
                                o.tipoRecurso(), o.recursosTotales(), o.recursosOcupados(),
                                o.ocupacionPct(), o.adr(), o.revpar()))
                        .toList();
            }
        };
    }

    @Bean
    ReporteExportable mesasDelPeriodo(MetricasDeComanda comanda) {
        return new ReporteExportable() {
            @Override
            public String codigo() {
                return "MESAS";
            }

            @Override
            public String nombre() {
                return "Rotación de mesas";
            }

            @Override
            public String patron() {
                return "COMANDA";
            }

            @Override
            public List<String> columnas() {
                return List.of("Comandas", "Mesas usadas", "Comensales", "Rotación",
                        "Minutos por mesa", "Venta neta", "Ticket por comensal");
            }

            @Override
            public List<List<Object>> filas(LocalDate desde, LocalDate hasta) {
                MesasDelPeriodo m = comanda.mesas(desde, hasta);
                return List.of(List.<Object>of(m.comandas(), m.mesasUsadas(), m.comensales(),
                        m.rotacion(), m.tiempoMedioMesaMin(), m.ventaNeta(), m.ticketPorComensal()));
            }
        };
    }

    @Bean
    ReporteExportable preparacionDePlatos(MetricasDeComanda comanda) {
        return new ReporteExportable() {
            @Override
            public String codigo() {
                return "PREPARACION";
            }

            @Override
            public String nombre() {
                return "Tiempos de preparación";
            }

            @Override
            public String patron() {
                return "COMANDA";
            }

            @Override
            public List<String> columnas() {
                return List.of("Plato", "Veces", "Minutos promedio", "Minutos máximo");
            }

            @Override
            public List<List<Object>> filas(LocalDate desde, LocalDate hasta) {
                return comanda.preparacion(desde, hasta).stream()
                        .map(p -> java.util.Arrays.<Object>asList(p.plato(), p.veces(),
                                p.minutosPromedio(), p.minutosMaximo()))
                        .toList();
            }
        };
    }
}
