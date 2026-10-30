# SCAP V2 — integración sobre las fuentes actuales recibidas

27/10/2026. **GO para ensayo aislado asistido en Google. NO-GO para despliegue productivo por ahora.**

Se compararon los nueve archivos recibidos (ocho `.gs` y `appsscript.json`), no nueve módulos `.gs`. Los siete módulos auxiliares coinciden byte a byte con la referencia V20. `Código.gs` coincide en código tras omitir la raíz privada y normalizar saltos de línea. No aparecieron correcciones adicionales que deban fusionarse. La integración conserva las fuentes efectivamente recibidas.

**Resultado local: 25/25 pruebas PASS**, usando los ocho módulos completos actuales, luego los integrados y el proyecto aislado completo. Se verificaron 54 fotografías, 20 categorías, 3 croquis, SCAP.xlsx, manifiesto, SIC-17/17A/17B, hashes, ACK repetido, números estables, compatibilidad histórica, rechazo BUSY, recuperación de revisión y aislamiento. Los cuatro XLSX de entrada también superaron comprobación ZIP/estructura. No hay declaraciones globales duplicadas en los conjuntos integrados.

**Google real: NO EJECUTADO en esta entrega.** No se abrió el navegador ni se creó, publicó o modificó ningún proyecto Google. Las simulaciones locales no acreditan el rendimiento, permisos, cuotas ni comportamiento real de Google. Se entrega un proyecto de ensayo para que el propietario lo copie y ejecute en el editor. La comprobación de capacidades llama a `doGet()` dentro del editor; no acredita todavía un GET HTTP del despliegue productivo.

La causa funcional sigue siendo el catálogo ausente y la falta de `scapPhotoPanelVersion >= 2`. Cambiar solo esa propiedad no resuelve reservas ni rutas. El parche incorpora el catálogo y valida/nombra las categorías nuevas, conservando las antiguas y el contrato de ACK. La APK actual 1.4.15 dispone del cliente necesario; no se ha generado ni modificado otra APK.

## Contenido y siguiente paso

- `COMPARACION_9_ARCHIVOS.md`: paridad y cambios exactos.
- `PARCHE_PRODUCCION_NO_PUBLICAR`: cuatro `.gs` finales y un fragmento de capacidades. **No aplicarlos todavía.**
- `PROYECTO_AISLADO_SOLO_PRUEBAS`: fuentes completas con adaptadores de aislamiento. **Nunca copiarlas a producción.**
- `GOOGLE_TEST_SETUP.md`: único procedimiento que debe ejecutar ahora el propietario, en un proyecto nuevo e independiente.
- `DESPLIEGUE_Y_REVERSION_NO_AUTORIZADO.md`: propuesta posterior, condicionada al ensayo y autorización expresa.
- `pruebas`, `evidencias`: pruebas y resultados exclusivamente locales; referencias con la raíz privada omitida.

El libro SCAP contractual mantiene sus 32 espacios F2. Las 54 fotografías originales y su manifiesto sí se entregan; el HTML de continuación que produce Android permanece en la exportación local y no forma parte del ACK remoto. No se añadió un archivo HTML remoto ni se prometió esa función.

Los límites continúan en 80 archivos, 24 MiB por staging y 160 MiB por revisión. El caso ficticio usa 59 archivos más tres filas SIC, 738.036 bytes decodificados en total; el mayor archivo tiene 318.860 bytes. Está por debajo de los límites. No se conoce aquí el tamaño de las fotografías del ingeniero; una ficha real podría necesitar revisión de límites aparte.

El ensayo de Google debe confirmar recepción, hashes, carpetas y ACK antes de solicitar autorización productiva. Conservar las evidencias y fichas pendientes, sin recrearlas ni cambiar sus UUID.
