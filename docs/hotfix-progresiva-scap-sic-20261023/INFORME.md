# Hotfix SCAP → SIC: kilometraje y ubicación PR

Fecha: 23/10/2026. Proyecto: `D:\Downloads\GBO_PRUEBAS_8B`. Rama de trabajo: `codex/correcciones-campo-20261023`. Se conservan los avances locales anteriores. No se genera APK ni se realizan operaciones en Google, tablet o Git remoto.

## Causa raíz y alcance real

La sección A guarda cada edición de `progressive` mediante `ScapController.edit`, su diario persistente y `ScapRepository.setField`, dentro de una transacción Room. El texto completo `10+100` se conserva correctamente en `scap_values` y por eso el encabezado SCAP lo muestra bien.

Antes del hotfix, `setField` copiaba la ubicación al registro vial únicamente si `startPrCode.isBlank()`. El primer carácter `1` es válido según el contrato existente de SCAP: un número sin `+` expresa metros. Ese primer guardado generaba PR `0000`, distancia `1.0` y origen `SCAP_PROGRESSIVE`. Las siguientes pulsaciones no actualizaban el registro, porque el PR ya no estaba vacío. No existe un valor predeterminado universal `0000 + 1.0`: resulta de congelar el primer carácter aceptado.

`ScapToSicMapper.location` leía ese registro vinculado, no el texto de sección A. `ScapSicExporter.preview/write` usa ese mismo mapper; por tanto el defecto podía alcanzar el XLSX, además de su presentación en pantalla.

La regresión previa usa el campo Compose real de sección A, el controlador real, el diario y Room: introduce `1`, `10`, `10+`, `10+1`, `10+10`, `10+100` y abre el panel SIC real. Falló por obtener `0000` en lugar de `0010`. Evidencia: `evidencias/ANTES_ScapProgressiveUiTest.xml`. La fuente anterior está conservada en el respaldo local `tmp/scap-progresiva-20261023/fuentes-antes-hotfix.zip`.

## Corrección y precedencia

- La ubicación sigue las ediciones del kilometraje mientras su origen sea `SCAP_PROGRESSIVE`, o el PR esté ausente y no haya confirmación SIC. Las ediciones ajenas a ubicación no recalculan el PR.
- El formato convencional se separa en código PR y metros: `10+100` → `0010` / `100`; `10+010` → `0010` / `10`; `10+000` → `0010` / `0`; `1+250` → `0001` / `250`; `0+050` → `0000` / `50`. No se evalúa una suma ni un decimal combinado. Se conserva la entrada histórica de metros sin `+`.
- `SIC_CONFIRMED` conserva la ubicación elegida manualmente, incluso al cambiar sección A. La pantalla muestra ambas ubicaciones cuando difieren, identifica la confirmación manual y permite modificar SIC o elegir explícitamente **Usar kilometraje SCAP**. Esta última opción exige igualdad de ruta; no inventa equivalencias entre carreteras.
- La elección explícita vuelve a habilitar la herencia del inicio desde SCAP. Conserva UUID, calzada, PR final, distancia final y los demás campos. Solo sustituye los dos campos de inicio elegidos por el operador.
- Una ubicación heredada se etiqueta como pendiente de revisión SIC, nunca como confirmada manualmente. Un dato histórico heredado contradictorio o un kilometraje incompleto se conserva en Room y queda señalado; no se exporta silenciosamente como ubicación actual de SIC-17. Debe corregirse o confirmarse expresamente.
- El editor SIC se actualiza al cambiar la ubicación heredada si no contiene edición local en curso. Los campos manuales pendientes no se sustituyen por una recomposición. Los valores auxiliares generados al elegir SCAP no ocultan una herencia posterior.

Esto identifica una ubicación. No calcula longitud contractual entre PR distintas, no modifica los catálogos PR, el ordenamiento ni la ubicación final del puente.

## Verificación del Excel y formatos

El exportador real SIC-17 genera el código PR como texto en `C14` y los metros como número en `D14`. Las pruebas abren el XLSX generado y verifican tanto el contenido como el tipo de celda, antes y después de corregir nuevamente el kilometraje. Se conservan archivos ficticios en `evidencias/`.

SIC-17A y SIC-17B no tienen columnas PR/metros en `BridgeSicContract`. No se añaden equivalencias ni columnas nuevas. Se verifica que sus valores proyectados no cambien al editar únicamente el kilometraje SCAP y se mantienen las pruebas de sus exportaciones existentes.

## Pruebas y límites

Resultados finales: **66 pruebas distintas aprobadas, 0 fallos, 0 errores, 0 omitidas**, en diez clases focalizadas. Detalle y XML JUnit por clase: `evidencias/RESULTADOS.json` y `evidencias/DESPUES_*.xml`.

La reproducción anterior al hotfix falló intencionalmente (1/1), demostrando PR `0000` tras escribir el valor completo. En la primera ejecución posterior pasaron los siete nuevos tests de datos/XLSX y las regresiones SCAP; un test nuevo de interfaz comprobaba el texto antes de que la observación asíncrona Room → Compose terminara de actualizarse. Se conservó ese resultado intermedio (`INTERMEDIO_Espera_UI.xml`) y se corrigió exclusivamente la espera del test. La ejecución final de los dos tests integrados de interfaz, `BridgeSicFlowTest` y `ContractualLength111Test` terminó `BUILD SUCCESSFUL`. No se oculta el fallo intermedio ni se contabilizan ejecuciones repetidas como pruebas adicionales.

Advertencias no bloqueantes de la configuración existente: opción experimental AGP, uso de JDK 25 con fallback de target Kotlin, advertencias de acceso nativo y deprecación de `ScrollableTabRow`. No se cambió la configuración de compilación para ocultarlas.

Se prueban: entrada Compose real, observación de Room, diario, cinco ejemplos, cierre/reapertura completa de la base, modificaciones posteriores, ubicación manual confirmada, discrepancia real del vínculo, rechazo de equivalencias entre rutas, entrada incompleta sin pérdida del último dato, corrección explícita de un registro histórico ficticio, XLSX real y permanencia de identificadores. Se ejecutan las regresiones SCAP y longitud contractual directamente relacionadas. Son pruebas locales Robolectric/Room/Compose; no son una prueba física ni una entrega confirmada por Google.

No se ejecutan migraciones masivas ni se reescriben registros al leerlos. No se modifican el esquema Room, sincronización, ACK, servicios, firma, versión ni Apps Script. El build de pruebas no ensambla APK.

## Registros anteriormente guardados y aceptación física

Los registros previos pueden contener el primer prefijo que alcanzó a persistirse (`0000 + 1.0`, `0000 + 10.0` u otro, según la velocidad de escritura y procesamiento). No todos necesariamente presentan el defecto: pegar el valor completo o confirmar manualmente SIC podía evitarlo.

Los registros históricos no se corrigen masivamente. Al abrir un vínculo contradictorio se muestra la discrepancia. Si ya estaba completo, debe reabrirse mediante el flujo existente para editarlo. En un registro de origen heredado puede corregirse el kilometraje o elegirse SCAP explícitamente; una ubicación manual confirmada permanece hasta que el operador decida cambiarla. Los Excel ya exportados o entregados no se reparan retroactivamente: requieren revisión y nueva exportación deliberada. No hay escrituras productivas en este trabajo.

Prueba física pendiente, para una futura candidata autorizada: crear una inspección ficticia PE-28H/CD, escribir lentamente `10+100`, abrir SIC-17 y comprobar `0010 + 100.0`; cerrar/reabrir y exportar SIC-17. Cambiar a `10+010` y verificar `0010 + 10.0`. Confirmar manualmente otro PR en SIC, cambiar sección A y comprobar que se conserva SIC con aviso. Revisar `C14`/`D14` del Excel. No usar una ficha de ingenieros como prueba.

**GO para generar la siguiente candidata cuando se autorice su compilación. Pendiente validación física; NO es una aprobación de distribución a campo.** No se compiló, instaló ni distribuyó una APK en este hotfix.

## Archivos de esta corrección

1. `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt`: actualizaciones heredadas y elección explícita.
2. `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapSicLocation.kt`: identificación PR/metros, precedencia y discrepancias; nuevo.
3. `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapToSicMapper.kt`: evita exportar una herencia contradictoria no confirmada; conserva cambios previos.
4. `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapBridgeSicPanel.kt`: origen comprensible, discrepancia, elección y estado del editor; conserva cambios previos.
5. `app/src/test/java/com/tuempresa/inventariovial/ScapProgressiveTest.kt`: regresiones Room, persistencia y XLSX; nuevo.
6. `app/src/test/java/com/tuempresa/inventariovial/ScapProgressiveUiTest.kt`: regresiones integradas Compose/Room/controlador; nuevo.

El resto de modificaciones locales anteriores se conserva. El manifiesto de conservación permite distinguir este hotfix del trabajo previo sin confirmar.
