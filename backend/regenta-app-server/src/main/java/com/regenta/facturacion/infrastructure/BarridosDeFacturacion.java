package com.regenta.facturacion.infrastructure;

import java.time.Duration;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.regenta.facturacion.application.CorreosDeFactura;
import com.regenta.shared.barridos.Barrido;

/** Los barridos de facturación. */
@Configuration
public class BarridosDeFacturacion {

    /** HU-121 C3: lo firmado mientras el almacén no respondía se sube cuando vuelve. */
    @Bean
    public Barrido barridoDeDocumentosPorSubir(AlmacenDeDocumentosEnObjetos documentos) {
        return new Barrido("documentos-por-subir", """
                SELECT DISTINCT negocio_id FROM facturacion.documentos_por_subir
                 WHERE proximo_intento_en <= now()
                """, Set.of("FACTURACION"), Set.of("FACTURACION_FACTURA_CREAR"), Duration.ofMinutes(1),
                documentos::subirPendientes);
    }

    /** HU-122 C4: la factura que no salió por correo se reintenta con espera creciente. */
    @Bean
    public Barrido barridoDeCorreosDeFactura(CorreosDeFactura correos) {
        return new Barrido("correos-de-factura", """
                SELECT DISTINCT negocio_id FROM facturacion.correos_de_factura
                 WHERE estado = 'PENDIENTE' AND proximo_intento_en <= now()
                """, Set.of("FACTURACION"), Set.of("FACTURACION_FACTURA_VER"), Duration.ofMinutes(1),
                correos::reintentarPendientes);
    }
}
