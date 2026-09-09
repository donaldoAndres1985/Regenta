package com.regenta.menu.aplicacion;

import java.util.List;

/** Un grupo de modificadores con sus opciones (HU-078). */
public record GrupoConOpciones(GrupoDelNegocio grupo, int orden, List<ModificadorDelNegocio> opciones) {
}
