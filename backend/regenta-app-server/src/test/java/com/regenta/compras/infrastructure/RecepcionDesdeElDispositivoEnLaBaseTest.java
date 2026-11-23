package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-182 · I32 contra PostgreSQL (compras V21): el identificador del celular no se repite en el negocio. */
@DataJpaTest
class RecepcionDesdeElDispositivoEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private RecepcionRepositorio recepciones;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocio = UUID.randomUUID();

    private Recepcion nueva(Proveedor p, String numero, UUID idDispositivo) {
        Recepcion r = Recepcion.crear(negocio, numero, null, p.getId(), UUID.randomUUID(), null, null, null);
        if (idDispositivo != null) {
            r.desdeElDispositivo(idDispositivo);
        }
        return r;
    }

    @Test
    @DisplayName("C2 · Se encuentra por su identificador, y el mismo identificador no entra dos veces")
    void unaSolaVez() {
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
        UUID delCelular = UUID.randomUUID();
        Recepcion r = recepciones.saveAndFlush(nueva(p, "REC-1", delCelular));

        assertThat(recepciones.findByNegocioIdAndIdDispositivo(negocio, delCelular))
                .hasValueSatisfying(e -> assertThat(e.getId()).isEqualTo(r.getId()));
        assertThatThrownBy(() -> recepciones.saveAndFlush(nueva(p, "REC-2", delCelular)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Las que no vienen del celular no chocan entre sí")
    void sinIdentificador() {
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123457",
                "Pinturas del Valle SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));

        recepciones.saveAndFlush(nueva(p, "REC-1", null));
        recepciones.saveAndFlush(nueva(p, "REC-2", null));

        assertThat(jdbc.queryForObject("select count(*) from compras.recepciones where negocio_id = ?", Integer.class,
                negocio)).isEqualTo(2);
    }
}
