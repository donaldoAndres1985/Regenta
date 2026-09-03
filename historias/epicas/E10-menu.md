# E10 · Menú · catálogo del patrón Comanda

La carta, los modificadores y las recetas que unen la comanda con el inventario.

| | |
|---|---|
| Historias | 5 |
| Puntos | 24 |
| Plan mínimo | Básico |

---

### HU-076 · Cartas y categorías de menú por horario

**Como** administrador del restaurante, **quiero** tener cartas distintas por franja horaria **para** no ofrecer desayunos a las ocho de la noche

| | |
|---|---|
| Épica | `E10` · Menú · catálogo del patrón Comanda |
| Puntos | 3 |
| Microservicio | `servicio-menu` |
| Paquete Flutter | `menu` |
| Tablas | `cartas` · `categorias_menu` |
| Depende de | HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `menu` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una carta con vigencia de 6:00 a 11:00, cuando abro la app a las 15:00, entonces no aparece entre las disponibles.
2. Dada una carta activa, cuando la consulto, entonces veo sus categorías en el orden definido.
3. Dada una carta con ítems, cuando intento eliminarla, entonces responde 409.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-077 · Ítems de menú con estación de cocina

**Como** administrador del restaurante, **quiero** cargar los platos con su precio y la estación que los prepara **para** que cada comanda llegue a la parrilla o a la fría según corresponda

| | |
|---|---|
| Épica | `E10` · Menú · catálogo del patrón Comanda |
| Puntos | 5 |
| Microservicio | `servicio-menu` |
| Paquete Flutter | `menu` |
| Tablas | `items_menu` · `estaciones_cocina` |
| Depende de | HU-076 (Cartas y categorías de menú por horario) |
| Etiquetas | `menu` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un ítem, cuando lo creo, entonces le asigno categoría, precio, estación y tiempo de preparación.
2. Dado un código de ítem repetido, cuando lo creo, entonces responde 409.
3. Dados sus atributos (alérgenos, vegano, picante), cuando los guardo, entonces quedan en el JSONB.
4. Dado un ítem marcado no disponible, cuando el mesero abre la carta, entonces aparece agotado y no se puede pedir.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-078 · Modificadores con mínimos y máximos

**Como** administrador del restaurante, **quiero** definir opciones y adiciones por plato **para** poder pedir el término de la carne y cobrar el queso extra

| | |
|---|---|
| Épica | `E10` · Menú · catálogo del patrón Comanda |
| Puntos | 5 |
| Microservicio | `servicio-menu` |
| Paquete Flutter | `menu` |
| Tablas | `grupos_modificadores` · `modificadores` · `item_grupos_modificadores` |
| Pantalla | `design/pantallas/ComandaWeb.html` |
| Depende de | HU-077 (Ítems de menú con estación de cocina) |
| Etiquetas | `menu` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un grupo obligatorio de mínimo 1, cuando pido el plato sin elegir, entonces se rechaza.
2. Dado un grupo con máximo 5, cuando intento elegir 6, entonces se rechaza.
3. Dado un modificador con precio extra, cuando lo elijo, entonces suma al total de la línea.
4. Dado un grupo con máximo menor que el mínimo, cuando lo creo, entonces el CHECK lo rechaza.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ComandaWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-079 · Recetas: el puente entre la comanda y el inventario

**Como** administrador del restaurante, **quiero** definir qué insumos consume cada plato **para** saber cuánta carne me queda sin contarla a mano

| | |
|---|---|
| Épica | `E10` · Menú · catálogo del patrón Comanda |
| Puntos | 8 |
| Microservicio | `servicio-menu` |
| Paquete Flutter | `menu` |
| Tablas | `recetas` · `items_menu` |
| Depende de | HU-077 (Ítems de menú con estación de cocina) · HU-029 (Bodegas y existencias por producto y bodega) |
| Etiquetas | `menu` · `backend` · `clave` |

> Sin esta historia el patrón Comanda queda desconectado de Inventario y el dueño nunca sabe cuánta carne le queda. Ni el documento original ni el diseño la contemplaban.

**Criterios de aceptación**

1. Dado un ítem de menú, cuando defino su receta, entonces asocio productos de inventario con su cantidad y merma.
2. Dada una receta, cuando cambian los costos de los insumos, entonces el costo estimado del ítem se recalcula.
3. Dado el cierre de una comanda, cuando se explotan las líneas contra sus recetas, entonces se publica `insumos_consumidos` con los productos y cantidades.
4. Dado un modificador enlazado a un insumo, cuando se elige, entonces ese insumo también se descuenta.
5. Dado un ítem sin receta, cuando se vende, entonces no descuenta inventario y no falla.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Test que verifica que vender 3 bandejas descuenta exactamente 3 veces la receta.
- [ ] Revisada en PR por otra persona.

---

### HU-080 · Disponibilidad diaria de ítems

**Como** jefe de cocina, **quiero** marcar un plato como agotado durante el servicio **para** que el mesero no lo siga vendiendo cuando ya no hay

| | |
|---|---|
| Épica | `E10` · Menú · catálogo del patrón Comanda |
| Puntos | 3 |
| Microservicio | `servicio-menu` |
| Paquete Flutter | `menu` |
| Tablas | `disponibilidad_diaria` · `items_menu` |
| Depende de | HU-077 (Ítems de menú con estación de cocina) |
| Etiquetas | `menu` · `backend` · `eventos` |

**Criterios de aceptación**

1. Dado un ítem, cuando lo marco agotado, entonces desaparece de la carta de los meseros en tiempo real.
2. Dado el cambio de día, cuando empieza el servicio, entonces la disponibilidad se reinicia.
3. Dado un evento `stock_actualizado` que deja un insumo en cero, cuando llega, entonces los ítems que lo requieren se marcan agotados.
4. Dado un ítem con cupo diario, cuando se agota el cupo, entonces se marca solo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---
