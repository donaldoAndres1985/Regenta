package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.reportes.BaseDeReportes;

/**
 * HU-100 criterios 3 y 4. Exportar no bloquea: se pide, se responde al
 * instante con el comprobante y el archivo se hace aparte. Si falla, queda
 * escrito por qué y se vuelve a intentar.
 */
@Import(ExportacionDeReportesTest.ReporteQueFalla.class)
class ExportacionDeReportesTest extends BaseDeReportes {

    private static final Set<String> VER = Set.of("REPORTES_REPORTE_VER", "REPORTES_REPORTE_EXPORTAR");

    /** Un reporte que revienta siempre: así se prueba el criterio 3 sin romper otro. */
    @TestConfiguration
    static class ReporteQueFalla {
        @Bean
        ReporteExportable reporteQueFalla() {
            return new ReporteExportable() {
                @Override
                public String codigo() {
                    return "PRUEBA_QUE_FALLA";
                }

                @Override
                public String nombre() {
                    return "Prueba que falla";
                }

                @Override
                public List<String> columnas() {
                    return List.of("Nada");
                }

                @Override
                public List<List<Object>> filas(LocalDate desde, LocalDate hasta) {
                    throw new IllegalStateException("la consulta se cayo");
                }
            };
        }
    }

    @Autowired
    private GestionDeExportaciones exportaciones;

    private final UUID negocio = UUID.randomUUID();
    private final UUID otroNegocio = UUID.randomUUID();
    private final UUID contador = UUID.randomUUID();

    private final LocalDate desde = LocalDate.now().minusDays(7);
    private final LocalDate hasta = LocalDate.now();

    @BeforeEach
    void negocioDePruebaConfigurado() {
        for (UUID quien : List.of(negocio, otroNegocio)) {
            ejecutarComoElServicio(quien, "insert into config_negocio (negocio_id, zona_horaria) "
                    + "values ('" + quien + "', 'America/Bogota') on conflict do nothing");
        }
    }

    /**
     * El barrido corre dentro del negocio que lo dispara, igual que el resto
     * de los barridos del sistema: sin negocio fijado, la RLS no dejaria ver
     * una sola fila pendiente.
     */
    private void barrer(UUID deQuien) {
        enContexto(deQuien, contador, VER, () -> exportaciones.procesarPendientes());
    }

    private EjecucionDeReporte pedir(UUID deQuien, String codigo, String formato) {
        return enContexto(deQuien, contador, VER,
                () -> exportaciones.solicitar(new SolicitudDeExportacion(codigo, formato, desde, hasta)));
    }

    @Test
    @DisplayName("Criterio 4: pedir la exportación responde al instante, sin el archivo hecho")
    void pedirNoBloquea() {
        EjecucionDeReporte pedida = pedir(negocio, "VENTAS_POR_CATEGORIA", "CSV");

        assertThat(pedida.estado()).isEqualTo("EN_CURSO");
        assertThat(pedida.archivoNombre()).isNull();
        assertThat(contar("select count(*) from archivos_reporte where ejecucion_id = '"
                + pedida.id() + "'")).isZero();
    }

    @Test
    @DisplayName("Criterio 4: el barrido lo genera aparte y avisa que está listo")
    void elBarridoGeneraElArchivoYAvisa() {
        EjecucionDeReporte pedida = pedir(negocio, "VENTAS_POR_CATEGORIA", "XLSX");

        barrer(negocio);

        EjecucionDeReporte tras = enContexto(negocio, contador, VER, () -> exportaciones.ver(pedida.id()));
        assertThat(tras.estado()).isEqualTo("COMPLETADO");
        assertThat(tras.archivoNombre()).endsWith(".xlsx");
        assertThat(contar("select count(*) from archivos_reporte where ejecucion_id = '"
                + pedida.id() + "'")).isEqualTo(1);
        assertThat(comoElServicio(negocio, "select tipo_evento from outbox_eventos where agregado_id = '"
                + pedida.id() + "'")).containsExactly("reporte_listo");
    }

    @Test
    @DisplayName("El archivo generado se puede descargar, y solo desde su negocio")
    void elArchivoSeDescarga() {
        EjecucionDeReporte pedida = pedir(negocio, "VENTAS_POR_CATEGORIA", "CSV");
        barrer(negocio);

        ArchivoDeReporte archivo = enContexto(negocio, contador, VER,
                () -> exportaciones.archivo(pedida.id()));
        assertThat(archivo.contenido()).isNotEmpty();
        assertThat(archivo.tipoMime()).contains("csv");

        assertThatThrownBy(() -> enContexto(otroNegocio, contador, VER,
                () -> exportaciones.archivo(pedida.id())))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Criterio 3: una ejecución fallida queda con su error y con fecha de reintento")
    void laFallidaQuedaRegistradaYSeReintenta() {
        EjecucionDeReporte pedida = pedir(negocio, "PRUEBA_QUE_FALLA", "CSV");

        barrer(negocio);

        EjecucionDeReporte tras = enContexto(negocio, contador, VER, () -> exportaciones.ver(pedida.id()));
        assertThat(tras.estado()).isEqualTo("FALLIDO");
        assertThat(tras.error()).contains("la consulta se cayo");
        assertThat(tras.intentos()).isEqualTo(1);
        assertThat(comoElServicio(negocio, "select proximo_intento_en > now() from ejecuciones_reporte "
                + "where id = '" + pedida.id() + "'"))
                .as("se reintenta después, no de inmediato")
                .containsExactly("t");
    }

    @Test
    @DisplayName("Criterio 3: se reintenta hasta un tope y después deja de intentarlo")
    void elReintentoTieneTope() {
        EjecucionDeReporte pedida = pedir(negocio, "PRUEBA_QUE_FALLA", "CSV");

        for (int intento = 0; intento < 5; intento++) {
            barrer(negocio);
            ejecutarComoElServicio(negocio, "update ejecuciones_reporte set proximo_intento_en = now() "
                    + "where id = '" + pedida.id() + "' and proximo_intento_en is not null");
        }

        EjecucionDeReporte tras = enContexto(negocio, contador, VER, () -> exportaciones.ver(pedida.id()));
        assertThat(tras.estado()).isEqualTo("FALLIDO");
        assertThat(tras.intentos())
                .as("tres intentos y se rinde: reintentar para siempre es un bucle, no una garantía")
                .isEqualTo(3);
        assertThat(comoElServicio(negocio, "select proximo_intento_en is null from ejecuciones_reporte "
                + "where id = '" + pedida.id() + "'")).containsExactly("t");
    }

    @Test
    @DisplayName("Un reporte que no existe se rechaza al pedirlo, no al ejecutarlo")
    void codigoDesconocido() {
        assertThatThrownBy(() -> pedir(negocio, "REPORTE_INVENTADO", "CSV"))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("El historial de un negocio no muestra las exportaciones del otro")
    void aislamientoDelHistorial() {
        pedir(negocio, "VENTAS_POR_CATEGORIA", "CSV");
        EjecucionDeReporte delOtro = pedir(otroNegocio, "VENTAS_POR_CATEGORIA", "CSV");

        List<EjecucionDeReporte> mias = enContexto(negocio, contador, VER,
                () -> exportaciones.historial());

        assertThat(mias).extracting(EjecucionDeReporte::id).doesNotContain(delOtro.id());
        assertThatThrownBy(() -> enContexto(negocio, contador, VER,
                () -> exportaciones.ver(delOtro.id()))).isInstanceOf(NoEncontradoException.class);
    }
}
