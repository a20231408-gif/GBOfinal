# Android 1.4.7 (21) - Pavimentos

16/10/2026, Lima. Este encargo está separado de la optimización Apps Script. Las correcciones conservadas se retomaron después del cambio de prioridad al backend. La APK estable 1.4.6 (20), sus informes y respaldos se conservan. No se instala ni distribuye esta candidata automáticamente.

## Estado de construcción

**COMPLETADO localmente:** candidata Release 1.4.7 (21) generada con `assembleRelease`, puerta de configuración y verificación histórica de firma/alineación aprobadas. No es Debug ni testOnly. Misma identidad `com.tuempresa.inventariovial`, minSdk 26 y targetSdk 37. **PARCIAL:** aceptación de teclado/diálogo y PDF nativo en tablet; no se ha instalado ni aprobado para distribución.

Archivo: `output/GBO_RUTA_CLARA_CANDIDATA_1.4.7_21/GBO_Ruta_Clara_1.4.7_21_CANDIDATA.apk`.

SHA-256: `98529d15561aad1243ab8358f501a95815a6ab7df0d644d1d57404f768d08810`.

Certificado SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. La APK estable 1.4.6 conserva su SHA-256 `ae61c79fefdb6e02d9af7a6ffdf7abbaa26a710d42e2ba75fd67db8affbd4ea0`.

## Diagnóstico e implementación

### Daño 22 - completado en código y pruebas locales

La receta anterior CORNER dibujaba siempre una diagonal irregular en la misma parte superior de un rectángulo; al localizar el daño se recortaba sobre el rectángulo de los toques, no sobre los bordes reales de la losa. Esa era la causa demostrada del trazo corto y la esquina fija.

`PavementCornerGeometry` identifica la losa, transforma ambos puntos a sus coordenadas métricas locales, calcula las cuatro distancias y acepta únicamente dos bordes adyacentes y no ambiguos. Proyecta los extremos a esos bordes y usa `PavementSketch.crack()` dentro de los límites de esa losa. El orden de los toques se conserva. Para P1=(1,2), P2=(3,1) en una losa de 10x10, los extremos son (0,2) y (3,0).

Se prueban las cuatro esquinas, ambos órdenes y losas de 10x10, 6x12 y 12x6, además de varias losas y transformaciones de viewport con zoom/pan. Se rechazan mismo borde, bordes opuestos, puntos demasiado cercanos, empate/ambigüedad y puntos junto a divisiones internas sin losa inequívoca. La tolerancia táctil se convierte de píxeles a metros y se acota según el tamaño de losa. El mensaje permite corregir los puntos; no elige una esquina a escondidas.

Los valores X1/Y1/X2/Y2 medidos siguen almacenados tal como fueron introducidos. Los extremos proyectados son derivados para representación, no una longitud o área contractual nueva. La cantidad sigue siendo el número de losas afectadas. No se cambia la función irregular usada por otras grietas.

En la revisión final se corrigió también la ruta genérica «símbolo por losa»: las nuevas capturas del daño 22 requieren dos puntos; un histórico sin coordenadas muestra la losa afectada y «sin localizar», sin inventar una esquina. Un histórico con coordenadas ambiguas sigue disponible y se advierte que debe revisarse antes de corregirlo. No hay migración ni edición masiva de históricos.

### Daño 26 - completado en código y pruebas locales

El comportamiento anterior era intencional para GLOBAL: los toques se descartaban y solo existía un indicador general. Se mantuvo GLOBAL y su única entrada por muestra. Ahora, después de registrar ese daño con severidad, «Marcar juntas en el croquis» habilita referencias opcionales.

El toque identifica el segmento interior longitudinal/transversal más próximo, limitado a una sola fila/columna de la malla. Cerca de un cruce y dentro de un radio menor selecciona únicamente el nodo. Un toque alejado no marca nada. Tocar de nuevo elimina esa marca; un conjunto evita duplicados. «Limpiar todas las marcas» pide confirmación y conserva el daño, la severidad, observaciones y fotografías.

La representación usa líneas nativas rojas/naranjas y círculos para nodos. Pantalla y PDF llaman al mismo `PavementDrawing`; no hay imágenes externas ni un croquis independiente para PDF. Los tramos aparecen solo en los segmentos de paginación que intersectan. El PDF incluye nombre, severidad, leyenda de evaluación GLOBAL y aviso de que las marcas no son nuevas unidades de medición. Sin marcas no se inventan juntas observadas y se muestra «Daño GLOBAL sin juntas marcadas».

**Persistencia:** metadatos versionados `_gboJointAnnotations` dentro del `sicDataJson` ya existente en Room: sampleId, damageId, versión, filas/columnas/ancho/largo y referencias V/H/N con fila/columna. No se añadieron tablas ni columnas Room. El namespace es independiente de los formatos SIC existentes y estos conservan sus campos. Los registros anteriores sin ese namespace tienen cero marcas.

Solo el propietario de la muestra en borrador puede cambiar marcas; una muestra cerrada debe reabrirse por la acción autorizada. Guardarlas incrementa la revisión local; el cierre normal encola la nueva entrega. El repositorio ya prohíbe cambiar dimensiones o malla después de registrar daños. Además, si una malla histórica/importada no coincide con la guardada, las marcas se conservan pero no se proyectan sobre juntas distintas: se solicita revisión/limpieza explícita.

El JSON nativo exportado ya incluye `sicData`, de modo que las marcas viajan en el archivo de datos existente y en el PDF, bajo el mismo protocolo, hashes y ACK. No se añade un endpoint ni una capacidad. El contrato local del servidor trata ese JSON como archivo opaco: una prueba aislada conservó sus bytes y actualizó el mismo fileId/carpeta con reintentos. No requiere el backend optimizado propuesto. La comprobación contra el servidor publicado con la candidata queda para una prueba física autorizada; no se afirma paridad de sus fuentes.

### Modificar cabecera - implementación completada; validación física parcial

La estructura anterior combinaba un diálogo dependiente del tamaño del contenido con una columna de peso sin relleno y contenido desplazable. La actualización de medidas/progresiva y el cambio de espacio por teclado podían variar su anclaje. Se reemplazó por un viewport acotado al espacio de ventana, anclaje superior, gestión de insets/IME, una sola columna desplazable y botones fuera del scroll.

No se añadieron solicitudes automáticas de foco ni se cambiaron cálculos/validaciones o valores del usuario. La prueba Compose edita un campo, reduce/aumenta altura y anchura, usa fuente 1.6x, comprueba que el scroll se estabiliza y que aceptar/cancelar conservan los valores.

**Límite:** esa prueba aloja el cuerpo real del diálogo, pero no demuestra cómo se comporta la ventana/teclado de las dos tablets afectadas. No se logró reproducir físicamente su oscilación. La causa exacta en esos dispositivos no se declara cerrada; el ajuste estructural requiere la prueba descrita abajo.

## Integridad contractual y regresiones

Los archivos de cálculo, catálogo/modelos, reglas de cantidad/severidad, consolidación de losas, plantilla/exportador SIC, DAO y original fotográfico son idénticos al respaldo previo. La parte de Excel de `PavementExports.kt` no se modifica; solo cambia el renderizado PDF. La prueba compara el contenido completo de los ZIP XLSX antes/después de añadir marcas y exige igualdad; compara también el resultado PCI completo. El daño 26 no añade filas y el 22 sigue medido en losas.

El Excel de datos de la muestra mantiene un daño 26 y un daño 22 en el ensayo. Los aportes SIC-15 siguen requiriendo los campos manuales contractuales existentes; las marcas no crean mapeos, columnas ni filas SIC nuevas. Una muestra puede entregar sus archivos aunque esos aportes específicos sigan pendientes de completar según el flujo aprobado.

Las preparaciones/recibos históricos no se invalidan ni se regeneran al instalar. Un PDF ya confirmado en Drive no se reescribe masivamente; una corrección autorizada y cerrada crea una nueva revisión por el flujo normal. Esto conserva UUID, numeración y ACK anteriores.

## Archivos Android cambiados en este encargo

| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts` | Versión candidata 1.4.7/21, sin cambiar firma ni applicationId. |
| `pavement/PavementCornerGeometry.kt` | Nuevo cálculo métrico de bordes y grieta del daño 22. |
| `pavement/PavementJointAnnotations.kt` | Nuevas referencias visuales versionadas y detección de juntas/nodos. |
| `pavement/PavementDamageFields.kt` | Validación específica de la esquina, sin medir su línea como cantidad. |
| `pavement/PavementRepository.kt` | Guardado/toggle transaccionales de marcas con propiedad y estado controlados. |
| `pavement/PavementSketch.kt` | Límites de paginación de grietas proyectadas y marcas GLOBAL. |
| `pavement/PavementExports.kt` | Leyendas y datos de croquis en PDF; Excel intacto. |
| `pavement/ui/PavementScheme.kt` | Dibujo compartido, tolerancia métrica del toque y tratamiento histórico sin esquina inventada. |
| `pavement/ui/PavementWorkspace.kt` | Captura de dos puntos, modo de marcación y limpieza con confirmación. |
| `pavement/ui/PavementDialogs.kt` | Viewport estable del diálogo. |

Los archivos `pavement/...` de la tabla están bajo `app/src/main/java/com/tuempresa/inventariovial/`. Pruebas nuevas: `PavementRigid147Test.kt`, `PavementEditor147Test.kt` y `PavementAnnotationSync147Test.kt` en `app/src/test/java/com/tuempresa/inventariovial/`.

Respaldos no destructivos: `tmp/rigidos-1.4.7-20261015/respaldo` (antes de corregir Pavimentos) y `tmp/rigidos-1.4.7-20261016/respaldo-reanudacion`. El APK genérico anterior de build también se conservó antes de recompilar.

## Pruebas y evidencia

Antes de la interrupción: 79 pruebas de dominio aprobadas (Pavimentos rígidos/flexibles/no pavimentados, cálculo, fotos, persistencia, PDF/Excel, SIC/SCAP y migraciones). Se conservó esa evidencia y no se repitió toda la batería.

En total hay **118 casos distintos con resultado final aprobado**, 0 fallidos pendientes y 0 omitidos. Se conservan 79 de dominio del trabajo previo; en la reanudación pasaron 42 de compatibilidad/geometría, cinco de Fotos V2 ordinario y el ensayo nuevo de Pavimentos rígidos V2. Hay solapamiento entre esas tandas: no deben sumarse como casos distintos. Los resultados por caso, perfiles y tiempos están en `docs/rigidos-1.4.7/evidencia/RESUMEN_PRUEBAS.json`. El primer intento mezcló simuladores legacy sin reserva V2 con la configuración ordinaria V2: produjo 17 fallos de esos simuladores. Se conserva el registro y se ejecuta cada perfil con su contrato correspondiente; no se deshabilita V2 en la candidata.

La prueba nueva utiliza HTTP interceptado y PDF de pruebas: falta de Internet, BUSY de reserva sin retryable, pérdida de ACK de staging, pérdida de ACK final, reenvío idempotente y posterior edición autorizada. Exige fotos originales idénticas, tres tipos de marcas, mismos UUID/folderId/fileId, revisión nueva y ACK completo. No envía registros reales.

**PDF:** los ensayos ejecutan el renderizador real sobre Canvas nativo y un adaptador de PDF exclusivo de pruebas porque Robolectric no inicializa PdfDocument nativo en este entorno. Los PDF resultantes están rotulados como simulados; no acreditan el escritor PDF nativo de Android ni su rendimiento. Ese adaptador no forma parte de la APK. La generación e inspección física de PDF 1.4.7 queda pendiente.

## Prueba física obligatoria antes de aprobar uso en campo

En una tablet de pruebas autorizada y con respaldo, confirmar primero firma compatible y versión. No instalar esta candidata automáticamente, no desinstalar si Android rechaza la actualización y no borrar datos para resolver una incompatibilidad con Debug.

1. Abrir en modo avión. Crear una muestra `TEST_147` rígida con malla de 16 losas y dimensiones válidas. Registrar cuatro daños 22 en losas distintas, uno por esquina, invirtiendo el orden de los toques en dos casos. Probar un par ambiguo: debe pedir corregir, sin guardar otra esquina.
2. Registrar una sola evaluación 26, elegir severidad, marcar un tramo longitudinal, otro transversal y un cruce. Desmarcar uno y volver a marcarlo. Confirmar que no se pintan los otros tramos ni aparecen daños adicionales. Probar «Limpiar» y Cancelar; no tocar datos de ingenieros.
3. Guardar/cerrar y reiniciar la tablet. Reabrir: conservar identidades, medidas, fotos y marcas. Reabrir para corregir solo con la misma cuenta autorizada. Un GLOBAL histórico sin marcas debe seguir sin marcas.
4. Abrir Modificar cabecera repetidamente en vertical/horizontal y con fuente grande: enfocar distintos campos, mostrar/ocultar teclado, desplazarse, aceptar/cancelar. No debe oscilar ni perder valores. Con daños existentes, intentar cambiar la malla debe seguir rechazándose por la validación vigente.
5. Generar Reporte PDF nativo y Excel en almacenamiento local. Abrirlos: cuatro esquinas correctas, solo las juntas/nodos elegidos, leyenda GLOBAL/severidad, fotos y texto legibles; Excel sin nuevas filas por cada marca. Revisar todas las páginas y comparar con pantalla en ambas orientaciones.
6. Mantener offline los registros ficticios hasta autorizar expresamente su entrega al servidor publicado. En esa prueba autorizada: finalizar/cerrar, confirmar revisión local=confirmada con ACK, verificar PDF/JSON/fotos/Excel y reintentar sin duplicados. No desplegar el backend optimizado para ejecutar esta prueba.

## Encargo Apps Script, separado

La propuesta de ordenamiento dinámico está en `docs/PROPUESTA_ORDENAMIENTO_DINAMICO_DRIVE_1_4_7.md`: recomienda índice técnico derivado y carpetas estables; no se implementó renumeración ni cambio productivo.

El paquete `APPS_SCRIPT_OPTIMIZADO_LISTO_PARA_REVISION` conserva 43 pruebas locales aprobadas y sigue pendiente de fuentes exactas desplegadas, paridad 10H/10K y mediciones Google autorizadas. No se modificó para construir Android 1.4.7. No se publica Apps Script, no se cambia Servicio de Usuarios y no se hace commit/push.


La comparación de conservación verificó configuración privada intacta, fuentes contractuales/Room sin cambios y ausencia de adaptadores ficticios de PDF/HTTP en los DEX de la APK. Se inspeccionaron las dos páginas del PDF de ensayo y se validaron ZIP/XML del XLSX: un GLOBAL, un daño 22 y tres referencias visuales. La cantidad del 22 fue 1 y la densidad 12,5%; 26 mantuvo densidad GLOBAL. Las curvas no digitalizadas siguen informándose como pendientes, sin inventar un índice PCI.
