# Facturación electrónica · transmisión a la DIAN (HU-117)

Cómo sale una factura de Regenta hacia la DIAN, qué vuelve y cómo se pasa de habilitación a producción.

## El recorrido

1. **Emisión** (`GestionDeFacturas.emitirDesdeEvento`): si no hay un certificado vigente, se rechaza antes de pedir el consecutivo (HU-116 C4).
2. **Firma** (`GestionDeFirmaYTransmision.firmar`):
   - `FacturaUblDian` arma el UBL 2.1 del anexo técnico 1.9:
     - extensiones DIAN: resolución, proveedor del software, `SoftwareSecurityCode` y QR;
     - emisor y adquiriente con sus responsabilidades;
     - medios de pago;
     - impuestos por tarifa;
     - totales y líneas.
   - El **CUFE** es SHA-384 de `NumFac + FecFac + HorFac + ValFac + 01 + IVA + 04 + INC + 03 + ICA + ValTot + NitOFE + NumAdq + ClTec + TipoAmbiente`. Se calcula con los mismos valores que quedan en el XML. La prueba `FacturaUblDianTest` reproduce el ejemplo oficial del anexo.
   - `FirmadorXadesEpes` firma con XAdES-EPES en la segunda `ext:UBLExtension`. El certificado sale cifrado de la bóveda (HU-116).
   - El XML firmado se guarda en el almacén como `fv{NIT10}{PT}{aa}{hex8}.xml` (anexo 6.5.7).
3. **Transmisión** (`ClienteDeLaDianSoap`):
   - El mensaje es SOAP 1.2 con WS-Security: `Timestamp`, el certificado como `BinarySecurityToken`, y RSA-SHA256 sobre `wsa:To` con C14N exclusiva que incluye los prefijos `wsa soap wcf`.
   - La factura viaja en `z….zip`.
   - Sin `DIAN_TEST_SET_ID` se usa `SendBillSync`. Con él se usa `SendTestSetAsync` y luego se consulta `GetStatusZip` hasta salir de "en proceso" (98).
4. **Respuesta**:
   - **Aceptada (C1):** queda el CUFE que devolvió la DIAN (`XmlDocumentKey`). Si difiere del calculado, manda el de la DIAN y el calculado queda en `respuesta_dian.cufe_calculado`. Las notificaciones también se guardan.
   - **Rechazada (C2 y C4):**
     - `RechazosDeLaDian` traduce cada regla a un dato corregible. Las reglas `FAJ` son del emisor (datos fiscales del negocio) y las `FAK` del adquiriente (datos del cliente). El número de la regla indica el campo: nombre, documento, DV, responsabilidades o dirección.
     - El detalle de la factura muestra el código y el mensaje.
     - El evento `factura_rechazada` llega a Alertas como `FACTURA_RECHAZADA`, para el administrador en la app y por correo.
   - **Sin respuesta (C3):** un tiempo agotado, un fallo de red, un 5xx sin SOAP o un SOAP Fault se tratan como "no disponible". La factura queda `ENVIADA` y se reintenta. Al tercer intento sin acuse se abre la contingencia (HU-057).

Todo el intercambio (request y response) queda en `facturacion.transmisiones`.

## Habilitación → producción (C5)

Pasar de un ambiente al otro solo cambia la configuración; el código es el mismo.

| Variable | Habilitación | Producción |
|---|---|---|
| `DIAN_URL` | `https://vpfe-hab.dian.gov.co/WcfDianCustomerServices.svc` | `https://vpfe.dian.gov.co/WcfDianCustomerServices.svc` |
| `DIAN_AMBIENTE` | `2` | `1` |
| `DIAN_TEST_SET_ID` | el `TestSetId` del portal, mientras se corre el set | vacío |
| `DIAN_SOFTWARE_ID` / `DIAN_SOFTWARE_PIN` | los del software registrado | los mismos |

En cada negocio:
- La resolución lleva su **clave técnica**; en habilitación es la del rango de pruebas `SETP`.
- El certificado de firma va cargado en la bóveda (HU-116).

Sin `DIAN_URL`, la DIAN es la simulada, que siempre acepta. Sirve solo para desarrollo, y el log lo advierte al arrancar.

## Adquiriente genérico

"Consumidor final" con el número `222222222222` (doce doses), tipo de documento 13 y responsabilidad R-99-PN. Así lo manda la Resolución DIAN 000165 de 2023, art. 11, num. 3.3, vigente con la modificación de la Resolución 000202 de 2025. El valor vive en configuración (`AdquirienteGenerico`).

## Primera emisión real en habilitación (pendiente: requiere las credenciales)

1. En el portal de habilitación de la DIAN: registrar el software propio con un PIN, y anotar el **ID del software** y el **TestSetId**.
2. Configurar `DIAN_URL` (la de habilitación), `DIAN_AMBIENTE=2`, `DIAN_SOFTWARE_ID`, `DIAN_SOFTWARE_PIN` y `DIAN_TEST_SET_ID`.
3. En el negocio de pruebas:
   - cargar el certificado `.p12`;
   - registrar la resolución de pruebas `18760000001`, prefijo `SETP`, rango 990000000–995000000, con la clave técnica `fc8eac422eba16e22ffd8c6f94b3f40a6e38162c`;
   - completar los datos fiscales con el NIT habilitado.
4. Emitir las facturas que pide el set y revisar `facturacion.transmisiones` y el estado en el portal.
5. Registrar aquí la fecha, el `TrackId`, el CUFE aceptado y cualquier regla que haya fallado.

| Fecha | TrackId | CUFE | Resultado |
|---|---|---|---|
| _pendiente_ | | | |
