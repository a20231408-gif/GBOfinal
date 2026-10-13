# Videos SCAP: nombres y rutas (10H)

## Causa y alcance

`ScapPhotoRecords.kt` conserva una categoría interna `T_` + SHA-256 abreviado del nombre del apartado contractual; reutiliza la categoría de videos anteriores. `ScapMedia.kt` usa códigos como UPSTREAM/ROAD_DECK para la ficha anterior. Video V1 escribía literalmente esa categoría como carpeta y `V_` + hash de mediaId como nombre. No era una avería del ACK.

Se conserva esa categoría, el padre SCAP, UUID/mediaId, fileId, SHA-256, original local y subida reanudable. Ningún consumidor local se cambia para usar la carpeta legible como identidad. El servidor resuelve archivos por fileId y diario; ahora guarda el nombre y carpeta finales en ese mismo diario. No se renombra ni mueve ninguna reserva histórica, completa o incompleta. No se modifica fotografía V2.

## Contrato

- Transporte: `videoProtocol=1`, `videoScope=SCAP_ONLY`.
- Capacidad independiente: GET `readableVideoNamingVersion=2`. Android solo entonces envía `videoNamingVersion=2`. Sin ella sigue Video V1 ordinario; no se requiere una APK especial ni una nueva bandera local.
- Para reservas nuevas: `NNN_PUE<elemento>_<categoría>.<extensión>`, máximo 20 caracteres. Ejemplo: `001_PUE7_ARR.mp4`.
- El elemento se obtiene de `binding_v4_<hash(identityId)>.json`, validando familia PUENTE, identidad y folderId de la ficha confirmada. Nunca se acepta un número propuesto por Android.
- Consecutivo independiente por código de categoría y puente, compartido entre tablets; ARR y ABA empiezan cada uno en 001. Abreviaciones de nombres editados que coincidan comparten contador para evitar colisiones.
- Extensión derivada de MIME admitido: MP4 → mp4, WebM → webm, 3GPP → 3gp. No se convierte el video ni se altera su hash. El protocolo conserva su validación existente de MIME/tamaño/hash; esto no añade una inspección de códec al servidor.
- Los campos de recibo nuevos son `videoNamingVersion`, `fileName`, `categoryFolder`, `categoryCode`, `videoNumber`, `elementNumber`. Reservas anteriores devuelven versión 1 y `historicalName=true` sin renombrarse. Clientes anteriores ignoran estos campos y conservan su comportamiento.
- Android valida el nombre/extensión/carpeta y conserva la asociación a mediaId/parentKey/hash/fileId en preferencias privadas. Un recibo que reasigne el nombre o fileId no puede marcar el video como sincronizado. Room no cambia; el original no se renombra. La tarjeta de video muestra el nombre reservado cuando está disponible.

## Reserva y recuperación

El ScriptLock existente abarca reserva, diario, fileId, creación de binario y sesión. La reserva se guarda en `._GBO_SYNC_V3/VIDEO_V1/names_v2_<hash(identityId)>.json`, antes del diario por mediaId; el diario se guarda antes de crear el binario. Una interrupción puede dejar un consecutivo reservado sin archivo, pero no reutiliza números. El mismo mediaId recupera su reserva. Dos tablets que entren mientras el bloqueo está ocupado reciben BUSY y reintentan.

Los chunks siguen enviándose al mismo fileId. La confirmación exige tamaño, SHA-256 calculado por Drive, MIME y asociación padre/mediaId/fileId. Perder begin o finish no crea una segunda evidencia. Un cambio manual del nombre remoto se detecta y requiere conciliación; el cliente no lo sobrescribe. El bloqueo coordina llamadas al mismo proyecto Apps Script, no proyectos independientes que escriban la misma raíz.

## Carpetas

Videos nuevos V2: `<ruta>/PUENTES/PUENTE <número>/VIDEOS/<apartado legible>/<nombre>`.

Ejemplos: `VIDEOS/AGUAS_ARRIBA/001_PUE1_ARR.mp4`, `VIDEOS/AGUAS_ABAJO/001_PUE1_ABA.mp4`. ROAD_DECK de la ficha anterior usa `CARRETERA_TABLERO`; el catálogo LOSA/MODULAR tiene literalmente `SUPERFICIE DE RODARURA` y utiliza ROD. Se respeta ese texto contractual, sin sustituirlo por un TAB inventado.

Históricos: permanecen en `VIDEOS/T_<hash>/V_<hash>.<extensión>` o su ruta original. Categoría original y etiqueta permanecen en metadatos. No hay migración histórica automática.

## Catálogo real

`VideoNamesV2Core.js` contiene los 31 nombres exactos de `scap_slab_catalog.json` y `scap_modular_catalog.json`; una prueba compara ambos catálogos completos. Abreviaciones: ACI/AFI/ACD/AFD/ROD/ARR/ABA/AIAR/ADAR/AIAB/ADAB/PEI/PED/FPED/VLAR/VLAB/VTR/FVTR/LCI/FLM/VEAR/VEAF/VEAB/BAAR/TBAR/BAAB/TBAB/TDRE/CAAR/CAAB/SIAI.

También se admiten las categorías reales de la ficha anterior: RIGHT_SIDE, LEFT_SIDE, UPSTREAM, DOWNSTREAM, ROAD_DECK, INFORMATIVE_SIGN, RAILINGS_SIDEWALKS_PIPES, DEFECT, BRIDGE_ELEMENT y sus categorías históricas. Los nombres libres que permite el formulario se normalizan para la carpeta y obtienen una abreviación legible de hasta cuatro caracteres; no se reemplaza su identidad interna.

## Validación física pendiente

Después de la actualización manual del backend y de una APK con la misma firma que la instalación de prueba: en un puente ficticio autorizado, importar/grabar dos videos del mismo apartado y otro apartado. Confirmar padre, desconectar durante un envío y reconectar. Comparar nombre/fileId remoto antes y después del reintento, comprobar un archivo por mediaId y estado confirmado. Un video histórico debe mantener su ruta y nombre. No repetir el formulario SCAP completo ni usar fichas de ingenieros.
