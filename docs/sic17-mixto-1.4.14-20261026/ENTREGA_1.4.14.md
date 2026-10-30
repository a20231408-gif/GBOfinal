# Hotfix SIC-17 mixto y Release candidata 1.4.14 (28)

**Compilación: PASS. GO para validación física controlada; no es aprobación de distribución general.**

## Corrección demostrada

La prueba del resultado esperado falló antes del arreglo. Ahora un puente SCAP A, un SIC-17 independiente B y otro SCAP C de PE-28H producen un único `EXCEL_SIC/SIC-17.xlsx` con tres filas. Se verificaron edición de A y B, reexportación de C sin duplicados, identidades estables aunque coincidan los códigos visibles, orden PR/metros, Observaciones literales, otra ruta separada, reapertura y recuperación ante fallos de publicación. Las fichas Room no se modifican al exportar.

La causa era una representación diferente del mismo esquema: 20 columnas SCAP frente a 20 más OBSERVACIONES del exportador independiente, distintas alturas de encabezado y bloques/rangos superiores. Los estilos base y anchos contractuales coinciden. Se reutiliza la plantilla primaria existente A:T, con U adicional sin formato ni filtro propio. El adaptador exige plantillas reconocidas y conserva los originales históricos; no elimina los rechazos de incompatibilidades peligrosas. También se corrige el filtro local SIC-17 para que abarque las tres filas. Diagnóstico y correspondencias completas: [DIAGNOSTICO_Y_ESQUEMA.md](DIAGNOSTICO_Y_ESQUEMA.md).

## Artefacto verificado

- APK: `D:\Downloads\GBO_PRUEBAS_8B\output\GBO_RUTA_CLARA_1.4.14_28_RELEASE_CANDIDATA\GBO_Ruta_Clara_1.4.14_28_RELEASE_CANDIDATA.apk`.
- versionName **1.4.14**, versionCode **28**, applicationId **com.tuempresa.inventariovial**. El propietario confirmó que no existe una instalación/distribución 28 o superior; el inventario local previo llegaba a 27.
- SHA-256 APK: `f123264033cab94ac8bb8e937fcfe104468caeeb82459b99915deb51da3db943`.
- Certificado SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. Firma válida y compatible con las APK históricas verificadas 1.4.9 (23) y 1.4.13 (27), esquemas v2/v3. No se inspeccionó la firma de la instalación concreta de una tablet.
- APK completo, no depurable, no test ni split; alineación ZIP y de páginas 16 KiB comprobadas. Tamaño: 26454197 bytes.
- `assembleRelease` con configuración real, `validateReleaseConfiguration`, `lintVitalRelease` y `verifyReleaseIdentity`; sin `gboValidationBuild=true`. Compilación: 365.89 segundos.
- Presencia del adaptador y consolidador en el mapa R8, marcador de código nuevo en DEX, Room 20 y catálogos actuales en el APK comprobados contra las fuentes. Todos los archivos del manifiesto de compilación mantuvieron sus hashes durante el build.

## Validación y conservación

**78 pruebas locales aprobadas, cero fallos, errores u omisiones en las ejecuciones finales.** Incluyen seis casos nuevos del hotfix y cinco de durabilidad, regresiones SIC-17A/B, SCAP, escritor Excel, Observaciones, progresivas, Room/Compose, MediaStore/SAF, carpetas públicas EXCEL_SIC, historial y recuperación de pendientes/ACK simulados. Los XML y resultados por clase están en `evidencias/`; se conservan aparte los fallos intermedios, sin contarlos como aprobados.

El XLSX ficticio `evidencias/SIC-17_TRES_FILAS_MIXTAS.xlsx` se abrió mediante un lector independiente y se renderizó/revisó visualmente. C14:C16 contienen texto `0010`, `0010`, `0011`; D14:D16, 100, 300 y 200 metros. Son pruebas JVM/Robolectric y archivos de prueba reales, **no ejecución física Android ni operaciones Google**. Falta comprobar el libro en Excel/Archivos de la tablet.

Se conservaron 15 APK históricas, propiedades privadas, esquemas Room y fuentes de backend. Los 15 campos efectivos Release comparados por hash permanecen iguales, excluyendo únicamente nombre/código de versión. No se cambiaron servicios, credenciales, firma, ACK, UUID, WorkManager ni fotografías. No hubo instalación, distribución, commit, push o acceso de escritura a Drive. Archivos modificados: [ARCHIVOS_MODIFICADOS.md](ARCHIVOS_MODIFICADOS.md).

## Límites que siguen pendientes

1. Este arreglo unifica el **consolidado local**. No cambia los XLSX de snapshots remotos ni acredita la mezcla histórica en el consolidado productivo V20. La equivalencia exacta con SIC-17/17A/17B descargados de Drive de procedencia confirmada sigue pendiente; no se declara una réplica global.
2. Las 20 categorías SCAP conservan la barrera `scapPhotoPanelVersion >= 2` y las capacidades de nombres correspondientes. La última consulta documentada del 23/10 no anunciaba esa capacidad; no se consultó ni modificó el servidor hoy. Un servidor incompatible debe dejar las evidencias locales pendientes y sin entrega confirmada. La activación del backend requiere revisión/prueba/publicación autorizadas por separado.
3. Una plantilla histórica distinta de las dos variantes reconocidas se rechaza conservando el archivo anterior. No se inventa un mapeo para documentos desconocidos. Los textos de defectos/conclusiones SCAP mantienen sus ubicaciones originales; no se confunden con la observación general de la ficha vial.

## Checklist físico breve

1. Conservar respaldo. En una tablet de pruebas con Release de firma histórica compatible, abrir la APK y elegir **Actualizar**. Si Android indica incompatibilidad de firma, detenerse: no desinstalar ni limpiar datos.
2. Sin Internet, comprobar registros, borradores, fotos y configuración existentes. Preparar datos ficticios en un entorno de prueba autorizado; toda subida real a Drive se autoriza por separado.
3. Exportar SIC-17 con SCAP + independiente + SCAP de la misma ruta. Abrir `Descargas/GBO Inventario Vial/PE-28H/EXCEL_SIC/SIC-17.xlsx`: tres filas, PR/metros correctos, Observaciones y dimensiones en sus columnas.
4. Editar cada origen y reexportar; deben seguir tres filas. Cerrar/reabrir, comprobar conservación de datos, otra ruta independiente y SIC-17A/B. Revisar la legibilidad en Excel y la copia pública.
5. Verificar el aviso claro de backend pendiente para categorías SCAP incompatibles. No confundir copia local con ACK ni activar una prueba de sincronización productiva sin autorización.

**Dictamen: GO para la validación física anterior. Distribución general y compatibilidad remota completa siguen pendientes.**
