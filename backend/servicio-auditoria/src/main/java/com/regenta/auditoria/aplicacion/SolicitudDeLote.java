package com.regenta.auditoria.aplicacion;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** El lote que sube un dispositivo. HU-102. */
public record SolicitudDeLote(@NotBlank String identificadorDispositivo, @NotBlank String plataforma,
        String versionApp, @NotNull @NotEmpty List<OperacionEntrante> operaciones) {
}
