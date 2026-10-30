# GBO_Inventario — SCAP V2 preparado mediante clasp

**Código cargado y versión inmutable 21 creada y verificada. NO desplegada. Producción sigue en V20.**

La sesión Google funciona. Tras habilitar el propietario Apps Script API en su cuenta, se cargó únicamente la corrección SCAP V2 al mismo proyecto. No se crearon proyectos ni despliegues nuevos. Antes de escribir se volvió a descargar el código y se comprobó que coincidía con el respaldo. Después se descargaron de nuevo el HEAD y la versión 21: sus diez archivos coinciden por SHA-256 con el candidato probado. Los cuatro despliegues existentes permanecen sin cambios. Un GET real posterior confirma que el endpoint de las tablets sigue respondiendo igual que V20, sin panel 2 todavía.

El primer intento de carga fue rechazado por el permiso de cuenta de Apps Script API. Se verificó mediante descarga que no había cambiado ninguna fuente, versión ni despliegue antes de reanudar. No se borró ni sustituyó evidencia de ese intento.

## Resultado

- Comparación real de HEAD, V20 desplegada y nueve copias entregadas: PASS, mismo texto.
- 19 pruebas locales sobre los módulos descargados e integrados: PASS.
- Catálogo 20; 54 fotos y 3 croquis; SCAP.xlsx/manifiesto; SIC-17/17A/17B; SHA, idempotencia, BUSY, recuperación, historia y videos: cubiertos localmente.
- Archivo remoto PhotoNamesV2Core preservado en plural; sin duplicados.
- ScriptLock, ACK, hashes, consolidación, numeración, manifiesto y servicios: conservados.
- 1362 archivos locales protegidos de Android, artefactos y configuración: hashes intactos.
- Sin APK nueva, cambios de Drive, fixtures productivos, modificaciones de ScriptProperties ni Servicio de Usuarios. Sin commit ni push Git.
- No existe todavía un ACK de entrega SCAP real de Google verificado para este parche.

## Respaldo y documentación

Respaldo privado original: `D:/Downloads/GBO_CLASP_PRIVADO/SCAP_V2_20261027/RESPALDO_ANTES_SCAP_V2_PRIVADO.zip`. Contiene código HEAD, V20, manifiestos y metadatos privados de versión/despliegue/URL. No contiene el archivo de autenticación. La versión 21 también tiene respaldo privado independiente. ScriptProperties no se exportaron ni modificaron; no afirmar que el ZIP es una exportación de esas propiedades.

Véanse COMPARACION_Y_PRUEBAS.md, CAMBIOS_REVISADOS.patch y evidencias. PARCHE_REVISADO_NO_DESPLEGAR contiene los cuatro módulos revisados y el fragmento de Código para revisión; el código completo con raíz privada está fuera del repositorio.

## Único paso pendiente de autorización

Actualizar **el despliegue productivo existente** de V20 a V21, conservando exactamente su URL y configuración. No crear otro despliegue. Después comprobar por GET real que panel=2 y que todas las capacidades anteriores permanecen.

**Publicar panel 2 puede activar automáticamente las colas pendientes de otras tablets.** Antes de autorizar, coordinar el momento con el equipo; para un primer reintento acotado, mantener las otras tablets sin conexión y usar una sola con APK 1.4.15 (29). No recrear la ficha ni cambiar UUID, revisiones o fotos. La entrega se considera confirmada únicamente tras ACK real coincidente y revisión de archivos en Drive. El operador debe conservar toda la evidencia si falla; una capacidad correcta no prueba por sí sola la entrega.

**Estado: listo para solicitar autorización de publicación controlada; no aprobado todavía para dar por confirmadas las entregas de campo.**
