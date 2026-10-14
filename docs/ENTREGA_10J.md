# Entrega 10J

Fecha: 14 de octubre de 2026. Rama: codex/observaciones-campo-manual-10I. Base: 8f686724590067a94a2e9501ce36ad096d706111.

## Resultado de Pavimentos

DIAGNOSTICO_EXPORTACION_PAVIMENTOS_10J.md responde cuándo se genera cada archivo, qué se encola, destinos, ACK y exportaciones que permanecen locales. El worker vigente delega en AutomaticSync V3/V4; no llama al cargador protocol 1 uploadPavementFile. Al cerrar, guarda una intención en Room; cuando procesa la entrega prepara PDF, Excel **por muestra**, JSON y fotos activas. Los SIC completos se consolidan como filas en EXCEL_SIC. Los incompletos no se acreditan por el ACK global.

El **Excel de superficie con varias muestras** guardado por el operador no está incluido en esa entrega automática; además, el panel filtra solo por superficie, no por ruta/tramo ni por CLOSED. Las copias locales exportadas tampoco reciben automáticamente el recibo por localPath del cargador antiguo. El informe distingue estas brechas comprobadas de una prueba real en Drive que no se hizo. No se reactivó el protocolo antiguo ni se reescribió sincronización aprobada.

## Cambio funcional realizado

Se ocultan los accesos de Anular/Restaurar remotos en RecordDetailScreen y las dos presentaciones de RecordHistoryScreen mediante la política InventoryLifecyclePolicy.enabled ya existente (false). Antes aparecían deshabilitados. Se conservaron los diseños, callbacks y bloqueo de repositorio para el futuro. Las acciones de descarte/recuperación de borradores exclusivamente locales, y todas sus comprobaciones, permanecen iguales. Restaurar SCAP de campos técnicos no es anulación remota y no se ocultó.

Comprobación proporcional al cambio: inspección de los tres accesos y **assembleDebug aprobado**, sin repetir baterías 8B–10I. La APK resultante conserva package com.tuempresa.inventariovial, 1.4.3 / 17 y la firma Debug existente; firma y metadatos verificados. Evidencia: campo-10j/evidencia/apk-debug.json; log local app/build/cierre-10j/compile-debug.log. No se instaló APK ni se generó una Release.

APK: app/build/outputs/apk/debug/app-debug.apk, 40 305 994 bytes. SHA-256: **c91e76c2d71a8db831f8947aa238498d2c12fc37f92745a9e5a2526649870c30**. No confundir con el hash del APK 10I documentado en la entrega anterior; este build incorpora el ocultamiento de botones.

## Reinicio para operación real

PLAN_REINICIO_PRODUCCION_10J.md inventaría los almacenes del código, compara raíz nueva frente a reinicio de la existente y define respaldo, aislamiento, selección por UUID, tratamiento de Room/medios/colas/recibos/números, verificación y recuperación. Recomienda raíz de datos limpia conservando íntegra la de pruebas, condicionada al control de todos los emisores o a una barrera de conjunto de datos futura.

Cambiar ROOT_FOLDER_ID por sí solo no bloquea una tablet offline ni una sesión de video antigua. No existe actualmente un reinicio selectivo validado ni una época de datos exigida por todos los endpoints. La limpieza asistida propuesta necesita desarrollo/ensayo y autorización posteriores; no se entrega como script de borrado listo para ejecutar. No se ejecutaron inventarios de datos reales, SQL, cancelación de trabajos, borrados, cambios de raíz, propiedades ni carpetas.

## Manual y capturas

El propietario canceló la obtención de capturas ADB y trabajará las imágenes por separado con ChatGPT. Antes de la cancelación solo se consultaron devices y la actividad: ningún dispositivo detectado; no se ejecutó screencap/pull ni navegación. Después no se intentó otra conexión.

Se deja la estructura abreviada GUIA_VISUAL_GBO_10J.docx y .pdf, fuente Markdown, doce posiciones identificadas y manifiesto de capturas vacío. **No es un manual visual terminado con capturas reales**. Tiene siete páginas y nueve capítulos con pasos cortos y notas técnicas; todas sus páginas se renderizaron y revisaron individualmente. No se insertaron imágenes ficticias ni flechas sin pantalla real. El helper de capturas queda solo como procedimiento opcional ya preparado; se comprobó su sintaxis y no se ejecutó.

El constructor solo incorpora archivos existentes aprobados con privacyReviewed=true y SHA-256 coincidente; no incorpora pendientes privadas de manera automática. El manual original 10I y sus figuras no se modificaron. Respaldo adicional de DOCX/PDF/Markdown en output/respaldo-manual-10i, con manifiesto verificable en manual-10i/respaldo-10i.json. El PDF nuevo se exportó con Word porque el renderizador canónico informó que LibreOffice no está disponible; PDFium generó las siete páginas inspeccionadas.

## Límites y pendientes

- La publicación real y entrega de Pavimentos requieren comprobación autorizada del servidor y recibos; el diagnóstico actual acredita código, no una subida de producción.
- No se implementa el envío global del Excel de superficie. Requeriría acordar ámbito/propietario y no sobrescribir datos de otras tablets con un libro local.
- El propietario elegirá campaña/raíz y autorizará inventario privado, respaldo recuperable, mantenimiento por tablet y cualquier publicación posterior. Los dispositivos no localizados no se consideran limpios.
- Capturas y elaboración visual final quedan a cargo del propietario, conforme a su última instrucción.
- Siguen vigentes los pendientes documentados de 10I; no se repitieron pruebas ni se presentan como resueltos por esta tarea.

No se cambiaron login, PIN, identidad, firmas, versionCode, credenciales, rutas, KMZ, plantillas, esquemas Room, despliegues ni Apps Script. No se distribuyó ni instaló una Release. Los informes y cambios se guardan en la rama existente; el SHA final se informa después del push y comprobación remota.
