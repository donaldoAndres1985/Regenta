# E06 · Facturación electrónica DIAN

Emisión, firma, transmisión y notas crédito. Sirve a los tres patrones.

| | |
|---|---|
| Historias | 7 |
| Puntos | 57 |
| Plan mínimo | Profesional |

---

### HU-052 · Administrar resoluciones de numeración DIAN

**Como** administrador del negocio, **quiero** cargar mis resoluciones de facturación con su rango **para** poder facturar legalmente y saber cuándo se me está agotando el rango

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 5 |
| Microservicio | `servicio-facturacion` |
| Paquete Flutter | `facturacion` |
| Tablas | `resoluciones` |
| Depende de | HU-018 (Configuración fiscal y de operación del negocio) |
| Etiquetas | `facturacion` · `backend` |

**Criterios de aceptación**

1. Dada una resolución, cuando la cargo, entonces registro número, prefijo, rango, clave técnica y vigencia.
2. Dado un rango con `hasta` menor que `desde`, cuando lo guardo, entonces se rechaza.
3. Dada una resolución vigente del mismo tipo y sucursal, cuando activo otra, entonces se rechaza — solo puede haber una.
4. Dada una resolución con menos del 10% del rango disponible, cuando se emite una factura, entonces se genera una alerta.
5. Dada una resolución vencida, cuando intento facturar con ella, entonces se rechaza.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-053 · Emitir factura desde cualquiera de los tres patrones

**Como** arquitecto, **quiero** que Facturación emita a partir de una venta, una estancia o una comanda **para** no tener que reescribir el servicio cuando se activa otro patrón

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 13 |
| Microservicio | `servicio-facturacion` |
| Tablas | `facturas` · `factura_lineas` · `factura_impuestos` · `resoluciones` |
| Pantalla | `design/pantallas/FacturaWeb.html` |
| Depende de | HU-054 (Asignación del consecutivo dentro del rango autorizado) · HU-038 (Saga de confirmación de venta con reserva de stock) |
| Etiquetas | `facturacion` · `backend` · `eventos` · `clave` |

> Por eso `origen_tipo` + `origen_id` es polimórfico y no una FK a `ventas`: con tres patrones, «Facturación depende de Ventas» deja de ser cierto.

**Criterios de aceptación**

1. Dado `venta_completada`, cuando llega, entonces se emite la factura con origen `VENTA`.
2. Dado `estancia_finalizada`, cuando llega, entonces se emite con origen `RESERVA` sin cambiar el código del servicio.
3. Dado `pedido_completado`, cuando llega, entonces se emite con origen `COMANDA`.
4. Dado el mismo evento entregado dos veces, cuando llega el duplicado, entonces no se emite una segunda factura para el mismo documento origen.
5. Dada la emisión, cuando se guarda, entonces `emisor_snapshot` y `cliente_snapshot` quedan congelados en JSONB.
6. Dado que mañana cambian los datos del cliente, cuando reimprimo la factura, entonces muestra los datos de la fecha de emisión.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/FacturaWeb.html` en medidas y color.
- [ ] El servicio no conoce las tablas de Ventas, Reservas ni Comandas: solo consume eventos.
- [ ] Revisada en PR por otra persona.

---

### HU-054 · Asignación del consecutivo dentro del rango autorizado

**Como** contador, **quiero** que la numeración sea continua y no se salga del rango **para** que la DIAN no rechace la facturación por huecos o números fuera de rango

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 8 |
| Microservicio | `servicio-facturacion` |
| Tablas | `resoluciones` · `facturas` |
| Depende de | HU-052 (Administrar resoluciones de numeración DIAN) |
| Etiquetas | `facturacion` · `backend` · `clave` |

> Un `BIGSERIAL` no sirve: deja huecos ante cualquier rollback y no conoce el rango autorizado.

**Criterios de aceptación**

1. Dada una emisión, cuando se asigna el número, entonces se toma con bloqueo sobre la resolución dentro de la transacción.
2. Dadas dos emisiones concurrentes, cuando ocurren a la vez, entonces reciben números distintos y consecutivos, sin huecos.
3. Dado el último número del rango, cuando se emite, entonces la resolución pasa a `AGOTADA` y las siguientes emisiones se rechazan.
4. Dado un rollback de la transacción, cuando ocurre, entonces el consecutivo no se consume.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Test de concurrencia con 50 emisiones simultáneas verificando que no hay huecos ni repetidos.
- [ ] Revisada en PR por otra persona.

---

### HU-055 · Firma digital y transmisión a la DIAN

**Como** contador, **quiero** que la factura se firme y se transmita, y quede constancia de la respuesta **para** poder demostrar qué se envió y qué respondieron

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 13 |
| Microservicio | `servicio-facturacion` |
| Tablas | `facturas` · `transmisiones` · `certificados` |
| Depende de | HU-053 (Emitir factura desde cualquiera de los tres patrones) |
| Etiquetas | `facturacion` · `backend` · `seguridad` |

**Criterios de aceptación**

1. Dada una factura generada, cuando se firma, entonces se calcula el CUFE y se guarda el XML firmado en almacenamiento externo, no en la base.
2. Dada la transmisión, cuando responde la DIAN, entonces queda el request y el response completos en `transmisiones`.
3. Dado un rechazo, cuando llega, entonces la factura queda `RECHAZADA` con el código de error y se genera una alerta.
4. Dado un fallo de red, cuando ocurre, entonces se reintenta con backoff y no se pierde la factura.
5. Dado un certificado por vencer en menos de 30 días, cuando se revisa, entonces se alerta.
6. Dado el archivo `.p12`, cuando reviso dónde está, entonces está en un gestor de secretos y en la base solo queda la referencia.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] El certificado nunca se versiona ni se guarda en la base de datos.
- [ ] Revisada en PR por otra persona.

---

### HU-056 · Notas crédito

**Como** contador, **quiero** emitir una nota crédito contra una factura **para** poder anular o corregir legalmente una factura ya aceptada

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 8 |
| Microservicio | `servicio-facturacion` |
| Paquete Flutter | `facturacion` |
| Tablas | `facturas` · `factura_lineas` |
| Depende de | HU-055 (Firma digital y transmisión a la DIAN) |
| Etiquetas | `facturacion` · `backend` |

> Una factura emitida no se edita ni se borra: se anula con una nota crédito que la referencia.

**Criterios de aceptación**

1. Dada una factura aceptada, cuando emito una nota crédito, entonces queda referenciada a la factura origen con su código de motivo DIAN.
2. Dada una nota crédito sin factura origen, cuando intento emitirla, entonces el CHECK de la base lo rechaza.
3. Dado un evento `devolucion_registrada`, cuando llega, entonces se emite la nota crédito por el monto devuelto.
4. Dada una nota crédito emitida, cuando consulto la factura origen, entonces se ve enlazada.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-057 · Modo de contingencia cuando la DIAN no responde

**Como** contador, **quiero** poder seguir facturando cuando el servicio de la DIAN está caído **para** no tener que cerrar el negocio porque un tercero falló

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 5 |
| Microservicio | `servicio-facturacion` |
| Tablas | `contingencias` · `facturas` |
| Depende de | HU-055 (Firma digital y transmisión a la DIAN) |
| Etiquetas | `facturacion` · `backend` |

**Criterios de aceptación**

1. Dado que la DIAN no responde tras los reintentos, cuando se supera el umbral, entonces se abre una contingencia y se avisa.
2. Dada una contingencia abierta, cuando se emite una factura, entonces se marca `CONTINGENCIA` y se entrega al cliente.
3. Dado que la DIAN vuelve, cuando se cierra la contingencia, entonces las facturas pendientes se transmiten en orden.
4. Dada la contingencia, cuando la consulto, entonces sé cuántas facturas quedaron afectadas.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-058 · Consulta y envío de facturas al cliente

**Como** cajero, **quiero** ver una factura emitida, imprimirla y enviarla por correo **para** entregarle al cliente su comprobante como lo pida

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 5 |
| Paquete Flutter | `facturacion` |
| Tablas | `facturas` · `transmisiones` |
| Pantalla | `design/pantallas/FacturaMovil.html` |
| Depende de | HU-053 (Emitir factura desde cualquiera de los tres patrones) |
| Etiquetas | `facturacion` · `flutter` |

**Criterios de aceptación**

1. Dada una factura, cuando la abro, entonces veo emisor, adquiriente, líneas, impuestos, CUFE, QR y su trazabilidad.
2. Dada una factura aceptada, cuando la envío por correo, entonces se adjuntan el PDF y el XML.
3. Dado el celular, cuando la abro, entonces el CUFE se ve completo y se puede copiar.
4. Dada una factura rechazada, cuando la abro, entonces el motivo del rechazo está visible sin tener que buscarlo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/FacturaMovil.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-115 · Datos fiscales del emisor en la factura

**Como** contador, **quiero** que la factura salga con el NIT, la razón social y las responsabilidades fiscales de mi negocio **para** que sea un documento válido y no un formato en blanco

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 5 |
| Microservicio | `servicio-facturacion` |
| Tablas | `facturacion.facturas.emisor_snapshot` · `core_identidad.configuracion_negocio` |
| Depende de | HU-018 (Configuración fiscal del negocio) · HU-053 (Emitir factura) |
| Etiquetas | `facturacion` · `backend` |

> `Factura.emitir` recibe el emisor del evento de cierre y ningún emisor lo manda, así que hoy
> `emisor_snapshot` se guarda como `{}`. Es el mismo hueco que R9 de `ClienteVenta.md` pero del
> lado del vendedor, y es lo que bloquea una emisión real: Facturación no puede consultar
> `configuracion_negocio`, que vive en la base de `servicio-usuarios`.

**Criterios de aceptación**

1. Dado un negocio creado, cuando llega `negocio_creado`, entonces Facturación guarda su copia local de los datos fiscales, igual que hace Reportes con la zona horaria.
2. Dada una factura emitida, cuando se guarda, entonces `emisor_snapshot` lleva razón social, NIT con dígito de verificación, dirección, municipio, régimen y responsabilidades fiscales.
3. Dado que los datos fiscales cambian, cuando se emite una factura nueva, entonces sale con los datos de hoy y las facturas ya emitidas no cambian.
4. Dado un negocio sin datos fiscales completos, cuando se intenta emitir, entonces se rechaza diciendo qué falta, antes de consumir un consecutivo.
5. Dado el municipio del emisor, cuando se arma el adquiriente genérico, entonces se usa ese mismo municipio, como pide el anexo técnico.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Ningún servicio consulta la base de otro.
- [ ] Revisada en PR por otra persona.

---

### HU-116 · El certificado de firma en una bóveda de verdad

**Como** dueño del negocio, **quiero** que mi certificado de firma esté guardado con llave **para** que nadie pueda firmar en mi nombre

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 5 |
| Microservicio | `servicio-facturacion` |
| Tablas | `facturacion.certificados` |
| Depende de | HU-055 (Firma digital y transmisión a la DIAN) |
| Etiquetas | `facturacion` · `seguridad` · `infra` |

> `BovedaDeSecretosStub` y `FirmadorDeXmlStub` cerraron HU-055 sin tocar un certificado real.

**Criterios de aceptación**

1. Dado un certificado cargado, cuando se guarda, entonces su clave privada queda cifrada en la bóveda y no en la base del servicio.
2. Dada una firma, cuando se hace, entonces el XML queda firmado con XAdES-EPES y la firma valida contra el certificado.
3. Dado un certificado próximo a vencer, cuando faltan menos de treinta días, entonces se avisa por el motor de alertas.
4. Dado un certificado vencido, cuando se intenta emitir, entonces se rechaza antes de consumir un consecutivo.
5. Dado un negocio, cuando reviso los registros, entonces no puede leer ni usar el certificado de otro.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados y un certificado de prueba.
- [ ] La clave de la bóveda no está en el repo.
- [ ] Revisada en PR por otra persona.

---

### HU-117 · Transmisión real a la DIAN

**Como** dueño del negocio, **quiero** que mis facturas lleguen a la DIAN y me devuelvan el CUFE **para** poder entregárselas al cliente con validez legal

| | |
|---|---|
| Épica | `E06` · Facturación electrónica DIAN |
| Puntos | 8 |
| Microservicio | `servicio-facturacion` |
| Tablas | `facturacion.facturas` · `facturacion.transmisiones` |
| Depende de | HU-115 (Datos fiscales del emisor) · HU-116 (Certificado en bóveda) |
| Etiquetas | `facturacion` · `infra` · `backend` |

> `ClienteDeLaDianStub` responde siempre que sí. Todo el flujo de HU-055, HU-057 y HU-058 está
> construido contra esa respuesta.

**Criterios de aceptación**

1. Dada una factura firmada, cuando se transmite al ambiente de habilitación, entonces la DIAN la acepta y el CUFE que devuelve queda guardado.
2. Dada una factura rechazada, cuando responde la DIAN, entonces el código y el mensaje de rechazo quedan guardados y visibles para quien la emitió.
3. Dada la DIAN sin responder, cuando se agota el reintento, entonces la factura entra en contingencia, como ya define HU-057.
4. Dado un rechazo por un dato del emisor o del adquiriente, cuando ocurre, entonces el mensaje dice qué campo lo causó y no solo que falló.
5. Dado el ambiente de producción, cuando se configura, entonces es un cambio de configuración: el código no distingue entre habilitación y producción.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con la DIAN simulada por contrato.
- [ ] Hay al menos una emisión real contra el ambiente de habilitación, documentada.
- [ ] El número del adquiriente genérico quedó confirmado contra el anexo técnico vigente.
- [ ] Revisada en PR por otra persona.

---
