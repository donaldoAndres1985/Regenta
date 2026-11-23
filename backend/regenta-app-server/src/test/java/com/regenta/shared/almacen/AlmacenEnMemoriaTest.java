package com.regenta.shared.almacen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.NoEncontradoException;

/** HU-121 · Las reglas del almacén, iguales en memoria y en S3: clave por negocio y nada ajeno. */
class AlmacenEnMemoriaTest {

    private final AlmacenEnMemoria almacen = new AlmacenEnMemoria();
    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final byte[] xml = "<Invoice/>".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("C1 · Guarda y devuelve una URL bajo la carpeta del negocio, con el nombre limpio")
    void guardarYLeer() {
        String url = almacen.guardar(negocioA, "facturas", "SETT 1234 Factura ñandú.xml", xml, "application/xml");

        assertThat(url).startsWith("memoria://regenta/" + negocioA + "/facturas/").endsWith("/sett-1234-factura-nandu.xml");
        assertThat(almacen.leer(negocioA, url)).isEqualTo(xml);
    }

    @Test
    @DisplayName("C2 · Otro negocio no lee ni borra el objeto: para él no existe")
    void otroNegocio() {
        String url = almacen.guardar(negocioA, "facturas", "f.xml", xml, "application/xml");

        assertThatThrownBy(() -> almacen.leer(negocioB, url)).isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> almacen.borrar(negocioB, url)).isInstanceOf(NoEncontradoException.class);
        assertThat(almacen.leer(negocioA, url)).isEqualTo(xml);
    }

    @Test
    @DisplayName("C2 · Una URL armada a mano (otro bucket, rutas con ..) no lleva a nada")
    void urlsTramposas() {
        String url = almacen.guardar(negocioA, "facturas", "f.xml", xml, "application/xml");
        String clave = url.substring("memoria://regenta/".length());

        assertThatThrownBy(() -> almacen.leer(negocioA, "memoria://otro/" + clave))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> almacen.leer(negocioA, "memoria://regenta/" + negocioA + "/../" + negocioB + "/x"))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> almacen.leer(negocioA, null)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Borrar quita el objeto; dos archivos con el mismo nombre no se pisan")
    void borrarYNoPisar() {
        String uno = almacen.guardar(negocioA, "reportes", "ventas.xlsx", new byte[] {1}, "x");
        String otro = almacen.guardar(negocioA, "reportes", "ventas.xlsx", new byte[] {2}, "x");
        assertThat(uno).isNotEqualTo(otro);

        almacen.borrar(negocioA, uno);

        assertThatThrownBy(() -> almacen.leer(negocioA, uno)).isInstanceOf(NoEncontradoException.class);
        assertThat(almacen.leer(negocioA, otro)).containsExactly(2);
    }

    @Test
    @DisplayName("C3 · Caído, avisa con AlmacenNoDisponibleException para que quien llama reintente")
    void caido() {
        almacen.simularCaida(true);

        assertThatThrownBy(() -> almacen.guardar(negocioA, "facturas", "f.xml", xml, "application/xml"))
                .isInstanceOf(AlmacenNoDisponibleException.class);
    }
}
