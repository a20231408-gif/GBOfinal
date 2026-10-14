# Entrega 10I — observaciones de campo y manual

Rama: codex/observaciones-campo-manual-10I. Base: 3b3a698 (10H).

## Resultado

Se corrigieron en código la localización parcial de daños rígidos, los campos técnicos de ahuellamiento y la exportación literal de Observaciones al Excel SIC. Se comprobó el flujo actual de fotos de Pavimentos sin reescribirlo. Se ajustaron las acciones de administración al ancho disponible y se documentó el incidente de altas sin atribuirle una causa de servidor no demostrada. El login usuario/PIN se conserva.

Se entrega un manual editable y PDF de 14 páginas, 30 capítulos y cuatro diagramas explícitamente identificados, con portada, índice actualizado y numeración. Todas las páginas finales se renderizaron e inspeccionaron individualmente; Word abrió el DOCX y exportó el PDF. No se utilizaron capturas de ingenieros ni datos personales.

La Debug ordinaria está compilada con servicios privados locales y funciones existentes. No se genera una nueva Release 10I: quedan dos casos de diálogos pendientes de ejecución en Android y aceptación física. La última Release distribuida sigue en 16; la versión preparada es 17 / 1.4.3, autorizada por el propietario. Las candidatas anteriores y su respaldo permanecen intactos.

## Cambios y comprobaciones focalizadas

| Etapa | Resultado y evidencia |
| --- | --- |
| A — Pavimentos | Seis casos aprobados: geometría parcial, varias posiciones/tipos/severidades, edición, reapertura offline, históricos, XLSX, renderizador PDF nativo y catálogo de campos. Coordenadas Room existentes; cantidades contractuales y PCI/URCI conservados |
| B — Observaciones | Tres casos Android/JVM aprobados con XLSX real y Room: última columna, varios elementos, literalidad, vacío, texto tipo fórmula y edición del mismo UUID. Ocho casos Node del motor real V3/V4 sobre almacenamiento ficticio: plantillas mixtas, conservación histórica, reintentos/ACK y fila única |
| B — Fotos | Dos casos aprobados: componente de cámara e Intent de imagen con UUID exacto; persistencia offline de consulta/retiro/reemplazo, historial y asociación en snapshot de sincronización. No se añade video a Pavimentos |
| C — Usuarios | Dos casos aprobados: acciones reales a 260 dp con callbacks y controles ADMIN/OPERADOR/token vigente-vencido. Servicio ficticio; ninguna cuenta real creada |
| D — Manual | DOCX, PDF y Markdown publicados; cuatro figuras con procedencia; QA estructural y visual de las 14 páginas |
| E — Compilación | assembleDebug, validateReleaseConfiguration y assembleDebugAndroidTest aprobados; certificado histórico verificado, sin mostrar secretos |

Total: **13 casos nuevos Android/JVM y ocho Node aprobados** en ejecuciones focalizadas separadas. No se repitió una batería general 8B–10H. Las trece aprobaciones no son trece pruebas ejecutadas en tablet: usaron Robolectric/Room/Canvas/componentes reales en Windows. El motor Node utilizó almacenamiento de prueba, no Drive de producción.

Dos aserciones de diálogos fallaron con AppNotIdleException o bloqueo del ejecutor Robolectric y se detuvo el intento. Quedaron trasladadas a app/src/androidTest/java/com/tuempresa/inventariovial/PavementDialogs10ITest.kt; la APK de pruebas compila, pero esos dos casos **no se ejecutaron en Android**. No se cuentan como aprobados. El primer intento de contenedor PdfDocument también falló por soporte nativo del entorno; el caso final comprueba el renderizador de páginas real sobre Canvas, sin afirmar que haya validado un PDF físico completo.

Los informes de cada grupo y las limitaciones están en docs/campo-10i/evidencia. Los logs completos locales están en app/build/cierre-10i. Pruebas de cámara física, rotación, PDF completo en tablet y aceptación visual de formularios continúan pendientes.

## Artefactos

- Manual editable: docs/manual-10i/MANUAL_USUARIO_GBO_INVENTARIO_VIAL.docx.
- Manual presentable: docs/manual-10i/MANUAL_USUARIO_GBO_INVENTARIO_VIAL.pdf; fuente Markdown y herramientas de generación incluidas.
- Debug nueva: app/build/outputs/apk/debug/app-debug.apk, 40 911 647 bytes; SHA-256 **0b60affbb85497fad60e2372674c4f038a538efabf99f7fafed0359f1122adf6**.
- APK verificada: com.tuempresa.inventariovial, 17 / 1.4.3, firma Debug existente, testOnly=false; SIC, SCAP, Pavimentos, fotos V2 y cliente de videos presentes.
- Respaldo privado: output/respaldo-10H-1.4.3, seis archivos y manifiesto SHA-256. Originales comparados de nuevo al cierre; no alterados.

No confundir app/build/outputs/apk/release/app-release.apk o app/release/app-release.apk con una Release nueva 10I: se conservaron como estaban. Los APK, claves y configuraciones privadas permanecen fuera de Git. Los cambios de código posteriores al build son documentación y evidencias; el APK corresponde al código de 8ecdd81.

## Backend preparado, sin despliegue

CAMBIOS_APPS_SCRIPT_10I.md detalla módulos completos, actualización y reversión: agregar SicObservationsV1Core.js como .gs, actualizar SyncV3Core.js y SyncV3.gs e incorporar únicamente la capacidad GET en Code.gs. **Preservar ROOT_FOLDER_ID, tokens y configuración publicados**; no reemplazar el Code.gs de producción con la referencia local. No modificar el servicio separado de Usuarios.

La consulta GET de lectura realizada en 10I anuncia syncProtocol=3, pathVersion=4, fotos V2 y videoProtocol=1 / SCAP_ONLY. No anuncia sicObservationsVersion ni readableVideoNamingVersion: integración remota de Observaciones y nombres de videos 10H quedan pendientes de publicación coordinada y prueba. No se activaron propiedades ni se enviaron registros ficticios a producción.

La nueva columna se agrega al extremo derecho sin mover columnas anteriores; puede ampliar el ancho y área de impresión contractual. Requiere revisión visual del propietario por formato antes de distribuir. Los registros históricos sin celda quedan vacíos en el consolidado hasta una exportación/revisión del cliente nuevo; no se hace una reescritura masiva de fichas antiguas.

## Prueba física corta propuesta

La instalación compatible y los bloqueos están en CHECKLIST_RELEASE_10I.md. No se instaló ni accedió a la tablet en esta ejecución.

1. En la Debug compatible, crear una muestra ficticia identificada TEST_10I, sin modificar muestras reales. Registrar una zona parcial y una línea/punto en otra posición de la misma losa; guardar, salir y reabrir. Comprobar posiciones, identidades, severidades y cantidad contractual; consultar Excel y PDF del esquema.
2. Abrir Ahuellamiento y comprobar Profundidad media del ahuellamiento, sin Ancho de fisura. Guardar área y profundidad medidas de prueba.
3. Tomar una foto de prueba en cada uno de dos daños, consultar, girar la tablet y reabrir. Reemplazar una y retirar otra; comprobar que se aplicó al UUID correcto y que el historial se conserva. No grabar video en Pavimentos.
4. Exportar localmente dos elementos SIC ficticios con Observaciones diferentes, uno PINTADO OK 57 E2; editar uno y exportar otra vez. Revisar última columna, misma fila/UUID, vacío y texto que comienza con '='. El envío a producción se hará solo cuando el propietario autorice la ficha ficticia y el parche esté publicado; entonces comprobar ACK y consolidado sin duplicados.
5. Revisar administración con el rol autorizado: botones visibles y autorización normal. Para el incidente real de altas, aportar versión, rol y mensaje sin PIN; no crear cuentas reales ni cambiar roles para probar.

La anulación y restauración remotas continúan pospuestas. No se cambiaron firmas, applicationId, identidad de tablet, migraciones ni registros de ingenieros.

## Commits de avance

- 3bfc67b: correcciones de Pavimentos.
- 37fac5f: Observaciones SIC y parche de consolidación.
- 62002ad: manual completo DOCX/PDF revisado.
- 7a05cab: persistencia de fotos comprobada y límite de pruebas de diálogos registrado.
- 8ecdd81: acciones de usuarios y diagnóstico comprobado.

Cada avance se publicó en origin/codex/observaciones-campo-manual-10I. El commit de cierre de este informe y su coincidencia remota se comunicarán al terminar; el SHA no se incrusta en su propio contenido.
