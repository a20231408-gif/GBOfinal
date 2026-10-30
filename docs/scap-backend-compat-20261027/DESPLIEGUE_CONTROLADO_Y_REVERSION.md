# Procedimiento posterior a autorización — NO ejecutado

Este paquete no publica nada. Se limita a compatibilidad SCAP sobre las fuentes V20 aportadas. No contiene `Codigo.gs` completo ni las optimizaciones de lock/workbook pendientes de otro encargo.

## Archivos exactos

| Archivo del paquete | Acción en el editor Apps Script |
|---|---|
| `ScapPhotoPanelV2.gs` | Añadir un único módulo nuevo con los 20 códigos, nombres, orden y carpetas. Sin servicios ni configuración. |
| `PhotoNamesV2Core.gs` | Integrar el diff o sustituir **solo si coincide con su base V20**. Nuevas reservas exclusivas SCAP/PUENTE; mantiene UUID, contadores y nombres históricos. |
| `SyncV3Core.gs` | Mismo criterio de paridad. Extiende únicamente la validación de rutas/categorías oficiales. Conserva `validate`, `commit`, hashes, ACK, Excel, ordenamiento y transacciones. |
| `VideoNamesV2Core.gs` | Mismo criterio de paridad. Nuevos nombres/carpetas oficiales; mantiene reservas, IDs, videos anteriores y protocolo. Necesario para anunciar compatibilidad completa del panel. |
| `CAPACIDAD_Codigo_FRAGMENTO_NO_REEMPLAZAR.txt` | Añadir **una propiedad** dentro del objeto que devuelve el `doGet` existente de `Codigo.gs`. No es una ScriptProperty. No sustituir el archivo completo. |

Conservar `SyncV3.gs`, `SicObservationsV1Core.gs`, `VideoV1.gs`, `VideoV1Core.gs`, manifiesto, permisos y configuración existentes. No crear segundas copias de los cores con otro nombre: cada global `SyncV3`, `PhotoNamesV2` y `VideoNamesV2` debe tener una sola definición efectiva.

## 1. Respaldo y paridad

El propietario debe conservar las ocho fuentes y el manifiesto actuales, número de versión y despliegue, y configuración privada recuperable. No copiar tokens o propiedades a este paquete.

Comparar las tres fuentes que se sustituirían con `BASES_SHA256.json`. El GET publicado **no permite descargar ni verificar las fuentes privadas**; lo contrastado aquí son las fuentes V20 aportadas. Si el hash difiere, revisar `CAMBIOS_SCAP_SOBRE_V20.patch` e integrar solo sus bloques tras revisar las diferencias. No sobrescribir optimizaciones ni correcciones productivas desconocidas. El `Codigo.gs` de referencia tiene la raíz censurada: no se utiliza para sustituir configuración ni como checksum íntegro del archivo productivo.

No modificar `GBO - Servicio de Usuarios` ni GBO LAB productivo.

## 2. Ensayo Google aislado, antes de producción

1. Usar un proyecto de pruebas independiente con la raíz de pruebas autorizada, credencial exclusiva y el aislamiento vigente. No copiar raíz, token o URL productivos. Si se parte del paquete anterior de ensayos, conservar sus helpers de aislamiento y usar una subraíz/corrida nueva; no reanudar ni mezclar los ensayos BASE/CANDIDATO anteriores con este parche.
2. Cargar los cuatro módulos indicados y el fragmento de capacidad en esa copia, sin añadir las optimizaciones pendientes. Mantener Drive avanzado v3 y los permisos ya necesarios. No introducir scopes nuevos.
3. Comprobar con GET que anuncia `scapPhotoPanelVersion=2`, además de `syncProtocol=3`, `pathVersion=4`, `stableFamilyNumbering=true`, `photoCategoryVersion=1` y `readablePhotoNamingVersion=2`. Para videos: `videoProtocol=1`, `videoScope=SCAP_ONLY`, `readableVideoNamingVersion=2`.
4. Ensayar con datos ficticios: todas las 20 categorías, varias fotos por categoría y más de 32 fotos. El fixture Android adjunto tiene 54 fotos, 3 croquis, SCAP.xlsx, manifest.json y tres filas SIC: **59 archivos de la ficha**, más los tres consolidados SIC. Los ensayos locales ya comprobaron este payload; aún falta ejecutar su equivalente con los servicios reales de Google en la raíz de pruebas.
5. Verificar hashes de los 59 archivos y del manifiesto, pertenencia a la subraíz, identidad/revisión/contentHash del ACK, carpeta estable y una fila por UUID en cada SIC. Repetir la misma revisión después de simular pérdida de respuesta: mismos IDs y sin duplicaciones. Probar una edición y una ficha histórica sin renombrar sus reservas. Comprobar videos y su ACK **separado**, posterior al padre. Registrar el resultado real antes de autorizar producción.

Las pruebas Node no miden latencia ni cuotas Google. No ejecutar `testHoldLock`, barridos de limpieza ni pruebas artificiales de carga en producción. El parche conserva el lock funcional y los límites de V20.

## 3. Publicación productiva, solo tras autorización expresa

1. Una vez aprobadas paridad y prueba aislada, incorporar el conjunto completo de cambios en GBO_Inventario y crear una nueva versión del proyecto. No publicar una versión parcial ni anunciar `2` con cores antiguos.
2. Editar el **despliegue existente** para que utilice esa nueva versión. Conservar su misma URL `/exec`, identidad de ejecución, permisos, raíz y ScriptProperties. No crear otra URL para las tablets.
3. Consultar de nuevo capacidades sin token y registrar solo los campos permitidos. La propiedad nueva devuelve 2 únicamente si están presentes los módulos compatibles, Drive avanzado y las habilitaciones V3/fotos V2 que ya existían. No activar banderas productivas para eludir una comprobación.
4. No cambiar `photoCategoryVersion` a 2: debe seguir en **1**. No cambiar `syncProtocol=3`, `pathVersion=4`, el protocolo/scope de video ni el `scapBundleVersion` heredado para intentar resolver este bloqueo.

**Alcance global:** anunciar la capacidad permite que todas las tablets compatibles con conexión reanuden sus pendientes. No es una activación selectiva por tablet. Preparar el ensayo y la coordinación del piloto antes de ese anuncio.

## 4. Reanudar la ficha existente

En la APK 1.4.15 (29), el transporte consulta capacidades en cada nueva ejecución del drenaje; no hay que instalar una APK especial ni modificar el registro. `PENDING_VALIDATION` ya está incluido en la recuperación automática si la ficha está cerrada.

Después de la actualización autorizada, abrir GBO con conexión. Un nuevo inicio de la aplicación programa el drenaje y existe recuperación periódica de 15 minutos, sujeta a Android y recursos. Volver de una pantalla a otra no garantiza un nuevo inicio del proceso. No crear otra ficha, cambiar UUID, reenviar como GENERAL, borrar preparación ni volver a tomar fotos para evitar el bloqueo. El aviso anterior puede permanecer hasta que el trabajador vuelva a consultar y ejecutar.

El botón global actual omite las entradas `PENDING_VALIDATION` al programar manualmente; por sí solo no garantiza un reintento inmediato de esta ficha. El recuperador automático sí las incluye. No se modificó esa interfaz ni se necesita cambiar el registro para que la nueva capacidad se reconozca en el siguiente drenaje.

Aceptar la entrega solo con ACK válido de la revisión actual y archivos completos; mantener el historial. Los videos necesitan su propia confirmación. Si aparece otro error, conservar la evidencia y diagnosticar su código; no confundir compatibilidad resuelta con garantía de entrega inmediata.

## 5. Reversión

Volver el mismo despliegue a la versión respaldada. El GET dejará de anunciar el panel 2 y las nuevas categorías volverán a esperar en Android. No cambiar la URL ni reinstalar APK antiguas.

La reversión es de **código**, no borra los datos que ya se hayan escrito. Conservar nuevas carpetas, archivos, reservas, números, STAGING, registros y journals para recuperarlos al restablecer el backend compatible. No resetear propiedades ni limpiar Drive.

## Reproducción local

Las pruebas se ejecutan desde este repositorio y utilizan su harness Drive simulado y las referencias V20, no Google:

```powershell
$env:GBO_ACK_OUTPUT = 'ACK_LOCAL_OTRA_CORRIDA.json'
node --test --test-reporter=tap docs/scap-backend-compat-20261027/pruebas/ScapPanelBackend.test.cjs docs/scap-backend-compat-20261027/pruebas/Compatibilidad54.test.cjs
```

Elegir un nombre nuevo para conservar cada ACK. Los fixtures solo contienen datos ficticios. El código de la prueba JVM se adjunta para inspección; se cargó mediante un init script temporal, fuera de `app/src`, sin modificar ni recompilar la APK.
