package com.regenta.facturacion.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.facturacion.domain.EstadoResolucion;
import com.regenta.facturacion.domain.Resolucion;
import com.regenta.facturacion.domain.TipoDocumento;
import com.regenta.facturacion.infra.ResolucionRepositorio;

/**
 * Toma el siguiente número de la resolución vigente, dentro de la transacción de
 * emisión y con la fila bloqueada. HU-054.
 *
 * <p>El consecutivo se deriva de una fila ya escrita y el avance se guarda en la
 * misma transacción del que emite: si esa transacción hace rollback, el número
 * no se consume (criterio 4). Al llamarlo desde la emisión (HU-053) se une a su
 * transacción; llamado suelto, abre la suya.
 */
@Service
public class AsignadorDeConsecutivos {

    private final ResolucionRepositorio resoluciones;
    private final RegistroDeEventos eventos;

    public AsignadorDeConsecutivos(ResolucionRepositorio resoluciones, RegistroDeEventos eventos) {
        this.resoluciones = resoluciones;
        this.eventos = eventos;
    }

    @Transactional
    public ConsecutivoAsignado asignar(TipoDocumento tipoDocumento, UUID sucursalId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();

        // FOR UPDATE en una sola consulta: dos emisiones a la vez se serializan
        // sobre esta fila y cada una la lee con su versión fresca.
        Resolucion resolucion = resoluciones
                .tomarVigenteParaEmitir(negocioId, tipoDocumento, sucursalId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No hay una resolución vigente para " + tipoDocumento.name()));
        resolucion.exigirVigenteEn(LocalDate.now());

        long numero = resolucion.tomarSiguiente();   // avanza el consecutivo, o rechaza si agotada
        resoluciones.save(resolucion);

        if (resolucion.porDebajoDelUmbral()) {
            eventos.registrar(negocioId, "resolucion", resolucion.getId(),
                    "resolucion_por_agotarse", AvisosDeResolucion.porAgotarse(resolucion));
        }

        return new ConsecutivoAsignado(resolucion.getId(), resolucion.getTipoDocumento().name(),
                resolucion.getPrefijo(), numero, resolucion.numeroCompleto(numero),
                resolucion.getEstado() == EstadoResolucion.AGOTADA);
    }
}
