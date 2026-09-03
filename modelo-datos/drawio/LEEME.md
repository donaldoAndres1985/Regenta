# Diagramas draw.io del modelo de Regenta

Generados leyendo el catálogo de PostgreSQL **después** de ejecutar el DDL de `../sql/`.
No se dibujaron a mano: nombres, tipos, PK, FK y UNIQUE son exactamente los de los scripts,
así que no puede haber divergencia entre el diagrama y la base.

## Archivos

- **`regenta-modelo-datos.drawio`** — archivo maestro, 22 páginas (pestañas abajo en draw.io).
- **`por-servicio/er-<esquema>.drawio`** — el mismo ER de cada microservicio, suelto.
- `regenta-er-por-servicio.zip` — copia comprimida de la carpeta anterior; se puede borrar.

## Páginas del archivo maestro

| # | Página | Qué muestra |
|---|---|---|
| 00 | Léeme | Índice y convenciones de lectura |
| 01 | Mapa de contextos | Los 15 servicios; qué es REST y qué es evento |
| 02 | Patrones operativos | Venta directa / Reserva / Comanda lado a lado |
| 03 | Saga Ventas ↔ Inventario | Descuento de stock sin transacción distribuida |
| 04 | Mapa de eventos | Quién publica qué y quién lo consume |
| 05 | Referencias lógicas | Los UUID que cruzan servicios sin FK física |
| 06–21 | ER por servicio | Todas las columnas, tipos, PK, FK y UQ |

## Cómo leer las tablas

Cada entidad es una tabla nativa de draw.io: cabecera con el nombre y una fila por columna.

- **PK** clave primaria (en negrita) · **FK** foránea real · **UQ** restricción única de una sola columna
- **?** la columna admite NULL · **⚙** columna `GENERATED` por Postgres
- Color de cabecera = patrón: gris azulado Core, ocre Venta directa, verde azulado Reserva, rojo Comanda
- Flecha continua = FK real dentro del servicio. **Ninguna cruza de un servicio a otro**: eso está en la página 05, con línea punteada.

## Abrirlo

En <https://app.diagrams.net> (Archivo → Abrir desde → Dispositivo), en la app de escritorio de
draw.io, o con la extensión *Draw.io Integration* de VS Code. El XML va sin comprimir, así que
también se puede versionar en git y ver el diff.

## Regenerarlo

El generador es `gen_drawio.py`: levanta un Postgres, carga los 16 scripts de `../sql/`, lee
`pg_catalog` y emite el XML. Si cambias el DDL, se regenera; si cambias el diagrama a mano, el
DDL manda.
