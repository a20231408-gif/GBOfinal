# Documentos del puente — P04/P17, 02/11/2026

Base limpia local/remota `6d946320d0c8f955add9b341de332d6da0442c1d`.
Rama `codex/scap-navegacion-archivos-20261102`. A y B ya integrados: no se volvieron a fusionar.
PR [#3](https://github.com/a20231408-gif/GBOfinal/pull/3) fusionado por merge normal
`7840526da106ebb3bf580f7dcb3b0f777878b126`, con el mismo árbol probado. Main intacto.

## Causa y solución

Los documentos se publican mediante MediaStore Downloads o el árbol SAF previamente
autorizado. LocalSharedFile conserva URI de archivo, relativePath, nombre, revisión, tamaño
y SHA. SharedFileActions.location(media) llama ACTION_VIEW_DOWNLOADS: ambos gestores abren
Descargas porque ese es el Intent solicitado. El URI del archivo no es un URI de carpeta;
relativePath es descriptivo. Para SAF solo se conoce el árbol autorizado, no necesariamente
el padre exacto. No se construye un supuesto URI padre a partir de texto ni se piden permisos.

Nueva pantalla interna **DOCUMENTOS DEL PUENTE**, accesible desde la ficha SCAP: nombre del
puente, carretera, progresiva, revisión, Excel/PDF/ZIP/fotos/croquis/otros, estado provisional
y verificación local diferenciada del ACK. Acciones existentes por URI exacto: abrir/compartir,
ubicación descriptiva/copiar, Guardar como y recuperación explícita si se perdió acceso.
Consulta Room de solo lectura: no genera archivos, duplica trabajos ni programa Drive.
Conserva la última generación con archivos READY ante fallo/espera y el fallo actual visible.
Historial explícito; si no existe, lo dice. No se renombra ni borra ningún histórico.

Después de confirmar cierre, observa COMPLETE persistido, guardado sin pendientes y sin error:
abre Documentos del puente. No hace esperar al escritor por la UI. Si aún no hay READY,
informa la espera; una copia anterior sigue disponible. La llegada de archivos actualiza la
vista mediante Flow. El cierre conserva el destino y contrato anteriores (solo dispositivo
no se enrola en Drive); el cambio es navegación, no una nueva operación de cierre.

Provisionales: nombres/rutas PROVISIONAL/BORRADOR y generación actual de una ficha incompleta
se etiquetan PROVISIONAL, incluso ZIP sin ese sufijo. Los históricos no se reclasifican usando
el estado actual. Ningún READY se presenta como entrega remota final sin ACK.

Guardar como conserva ACTION_CREATE_DOCUMENT y comprobación de bytes mediante rememberSaf.
No se suministra EXTRA_INITIAL_URI ficticio: falta un URI padre acreditado en la mayoría de
publicaciones. El usuario elige destino. Android permite una sugerencia inicial mediante URI,
no convierte una cadena de ruta en permiso: [documentación oficial SAF](https://developer.android.com/training/data-storage/shared/documents-files).

## Archivos y pruebas

- `scap/ui/BridgeDocumentsScreen.kt`: proyección/categorías y pantalla interna, sin escrituras.
- `scap/ui/ScapWorkspace.kt`: acceso permanente y navegación tras cierre persistido.
- `BridgeDocuments38Test.kt`: siete casos de documento único, revisiones, ausencia de historial,
  provisional/READY local sin ACK, fallo con copia anterior, pendiente sin generación nueva,
  categorías y contratos archivo/árbol/MediaStore. Acceso perdido/archivo exacto se integra
  con las cinco regresiones LocalFiles148UiTest y las existentes de publicación/reintento.
- `ScapFinalP0UiTest.kt`: cierre real de formulario abre documentos; solo una intención.
- `app/build.gradle.kts`: únicamente identidad 1.4.24 (38).

**129/129 únicos PASS**, 24 clases, cero fallos/errores/omitidos, 54 s. Los 113 anteriores
más siete nuevos, ScapFinalP0UiTest (2) y ScapP0Test (7); perfil V2 mocks sin credenciales.
Incluye cierre solo dispositivo, exportación y replay, fallo de publicación, URI perdido,
ACK, UUID/revisión, filtros/longitud/descarte/PCI. No es suite histórica completa ni HTTP.
Primera selección: 21/22 PASS, fallo de espera al escribir/navegar corregido usando estado
persistido. Incluyó una regresión local sintética grande por coincidencia del filtro;
no se repitió en la selección final ni se suma al total. XML/logs separados en
`tmp/scap-documentos-20261102/`. No se relajaron aserciones para aprobar.

## Candidata privada y aceptación pendiente

Fuente APK `afae07245bdd8ba24d79fd40c5ce4d51837449d0`, misma que el PR salvo documentación.
Debug original **1.4.24 (38)**, package com.tuempresa.inventariovial, Room 21 intacto.
Compilación original sin perfil mocks: PASS, 11 s. Firma Debug histórica D2E54C… verificada,
alineación 16 KiB PASS. Endpoint/configuración original acreditados offline, sin HTTP ni
endpoints de ensayo, V2=true/17A=false/17B=false. Sin valores secretos en Git/informe.

`C:\Users\ASUS\GBO_CANDIDATA_PRIVADA_20261102_1_4_24_38\GBO_1.4.24_38_DEBUG_ORIGINAL_NO_INSTALAR.apk`

SHA256 `4b75b39d682741bb85329dc1a6f4ca8f453ac40ab17a13c6a6d5190a45121d1b`.
No se compiló Release ni se instaló/distribuyó. Tablet y ambos respaldos no accedidos.
P09/P06/P08 PASS reportados por el propietario sobre 1.4.23; no validan esta navegación nueva.

Pendiente físico: instalación autorizada con respaldo actual, cerrar puente solo dispositivo,
espera READY/última válida, abrir/compartir exactos, ver/copiar ubicación, historial/no historial,
permiso perdido y Guardar como en Xiaomi/Files. No prometer carpeta exacta externa.
P11–P15, OAuth/journals, colisión real, raíces/permisos y coexistencia/backend siguen pendientes.
**NO-GO productivo.** No Apps Script, Drive, ROOT, sincronización real, datos reales ni main.
