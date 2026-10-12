# Matriz de cierre — Prompt 9B

12/10/2026. Cierre inmediato solicitado por el usuario: se conserva lo avanzado y no se abren ciclos adicionales de implementación o corrección. **Esta entrega no es una aceptación integral para producción.**

| Requisito | IMPLEMENTADO | VALIDADO LOCALMENTE | VALIDADO EN TABLET | DEPENDIENTE DEL BACKEND | PENDIENTE | BLOQUEADO |
|---|---|---|---|---|---|---|
| Condición compartida SIC-17…22 y captura SIC desde SCAP | Sí | Catálogos, códigos y Room; selección Compose | No, cambios 9B | No | Recorridos físicos de todos los formatos | Solo validación física: sin dispositivo |
| Categorías fotográficas predefinidas CORE y SCAP | Sí | Categorías, originales, historial y controles; 19+2 pruebas | No, cambios 9B | No | Captura/importación SAF real y plantillas en tablet | Solo validación física |
| Galería y GENERAL histórico | Sí | 8 escenarios PhotoDrawing9Test aprobados | Ambos crashes anteriores aceptados manualmente por el usuario | No | Revalidar nueva interfaz y rotación en tablet | Solo validación física |
| Listas, números locales y navegación | Sí, SIC/SCAP/pavimentos | Lista común y navegación aprobadas | No | No | Comprobar todos los módulos e históricos en tablet | Solo validación física |
| Borrador, revisión, confirmación y outbox | Sí | Resumen sin entrega, validación incompleta, finalización, revisiones e identidad | No | Entrega real requiere servidor configurado | Aceptación integral por formato | Solo prueba física/remota |
| Prefijos numéricos de nuevas fotografías | Cliente y parche preparados; flag desactivado | Persistencia, reintentos, histórico y backend simulado | No | Sí: photoNamingVersion=1 | Activación y aceptación coordinada | Activación remota fuera de este cierre |
| KML/KMZ offline y acumulación local | Sí | Segmentos, huecos, inversión, cruces, imprecisión y posición manual | No | No | Importación/GPS físico | Solo validación física |
| Integridad del orden contractual/remoto | Separación local implementada | Revisiones y UUID aprobados; **6 fallos en AssetOrderingTest al cierre** | No | No para revisar los fallos locales | Analizar los seis fallos; no atribuidos a causa sin investigación | Correcciones detenidas por solicitud del usuario |
| Posición del operador sobre eje | Sí | Cálculo probado; captura nativa sin probar | No | No | GNSS y permisos reales | Sin hardware accesible |
| Ayuda GNSS y salida roja Material 3 | Sí | Compilación y 24 pruebas de autenticación aprobadas entre ejecuciones | No, cambios 9B | No | Verificación visual y conservación de datos al salir | Solo validación física |
| Carpeta técnica ._GBO_SYNC_V3 | Conservada; guía actualizada | Sin despliegue ni modificación de producción | No aplica | Nuevo journal de videos solo tras despliegue | Confirmar servidor de prueba | Ninguno local |
| Videos múltiples offline, MIME, tamaño, SHA e identidad | Sí | 10 escenarios JVM aprobados entre ejecuciones; codec sustituido solo en test | No | No para almacenamiento local | Cámara, miniatura, audio/video y codecs en tablet | Sin hardware accesible |
| Videos reanudables y ACK verificado | Cliente y parche preparados; remoto desactivado | Reanudación, caducidad, ACK inválido, retiro y adapter simulado | No | Sí: videoProtocol=1 y flags | OAuth, checksum Drive, corte de red y aceptación remota | Backend no desplegado por instrucción |
| Excel contractual, SCAP y pavimentos | Columnas/plantillas conservadas | Exportación SCAP, revisiones SIC, 19 pruebas de pavimentos y 2 UI aprobadas | No, cambios 9B | No para Excel local | Revisión física de archivos y recorridos completos | Ninguno para generar localmente |
| Debug, commits, push y documentos | Código y documentos guardados; resultado APK en informe | Evidencia en campo-9b/evidencia | Instalación pendiente | No | Instalar APK de validación y ejecutar PRUEBAS_TABLET_9.md | No se hicieron instrumentadas físicas |

## Ejecuciones conservadas

- Primera focalizada: 75 pruebas, 63 aprobadas, 12 fallos. XML: `campo-9b/evidencia/primera-ejecucion`.
- Segunda: 79 pruebas, 76 aprobadas, 3 fallos. XML: `segunda-ejecucion`.
- Tercera focalizada de correcciones: **30/30 aprobadas**. XML: `tercera-ejecucion`.
- Última ejecución ya iniciada antes del cierre: **11 pruebas, 5 aprobadas y 6 fallos**. XML: `cierre`. No se repitió ni se corrigió después de la orden de cierre.
- Resultado más reciente por prueba actualmente vigente: **124 aprobadas y 6 fallos pendientes (130 pruebas)**. Se excluye del cómputo el nombre antiguo de una prueba de empates sustituida para reflejar el comparador contractual vigente. No se afirma que una ejecución única de 130 pruebas haya pasado.
- Backend Node: **38/38 aprobadas** (PhotoCategories5, SyncedEdits2B, Media9 y VideoV1Adapter); servicios simulados, sin aceptación Drive real.

Los fallos pendientes son `AssetOrderingTest`: `retroactiveCulvertRenumbersOnlyItsFamilyAndKeepsIdentityAndPhotos`, `mixedFamiliesUseSeparateCountersAndIgnoreCaptureTimeAndManualPr`, `fallbackIsExplicitAndDoesNotMixContractualAndGeometricOrigins`, `tiesAreDeterministicAndRouteSibFamilyDefineCountersNotRoadbed`, `folderPlanUsesCurrentOrderAndRealPhotoIndexWithoutMutatingAnything` y `catalogUsesDetailClassesAndReportsUnknownsWithoutInventingSib`. Su causa no se investigó en el cierre.

La prueba de importación JVM usa un proveedor de documentos simulado por la incompatibilidad de separadores Windows/Android de FileProvider. La autenticación recibió un ANDROID_ID simulado válido. Los hashes SCAP se ajustaron a las plantillas ya existentes, cuyos blobs Git se verificaron iguales al HEAD; las plantillas no se modificaron.

Las imágenes y ejemplos regenerados por tests se conservan en `campo-9b/capturas` y `campo-9b/examples`, restaurando los fixtures históricos para no reemplazar su evidencia. Los XML registran también los fallos intermedios; no se ocultan.

El usuario comunicó que los dos crashes previos desaparecieron en su tablet. Se registra como aceptación manual comunicada, **no como instrumentación ejecutada por Codex**. No se ejecutó ADB/emulador, despliegue Apps Script, renombrado remoto ni carga real de videos.
