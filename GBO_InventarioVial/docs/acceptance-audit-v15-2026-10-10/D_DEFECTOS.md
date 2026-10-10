# D. Hallazgos de aceptación — Release 15

Esta auditoría no modifica producción. Una prueba emulada demuestra un comportamiento local; no certifica cámara, red, servidor publicado ni una entrega completa. P0 significa pérdida/corrupción o bloqueo crítico comprobado; P1 alto, P2 medio y P3 menor. No se ha demostrado un P0. Los hallazgos siguientes impiden aprobar la entrega definitiva.

## D01 · P1 · Contratos de carpetas incompatibles con la solicitud de aceptación

**Esperado:** SIC en RUTA/SIB/ELEMENTO N, consolidado por lote a nivel SIB; pavimentos en RUTA/EVALUACION_SUPERFICIAL/TRAMO/SUPERFICIE, con hojas de campo/panel/data separados. **Implementado:** DrivePaths.inventory omite SIB; RouteDelivery genera un XLSX por elemento. DrivePaths.pavement omite TRAMO y utiliza PAVIMENTOS/SUPERFICIE/MUESTRA_NNN; documentos de categorías diferentes comparten carpeta de muestra. Aunque los UUID separan muestras, la jerarquía exigida no existe.

**Reproducción:** ejecutar AcceptanceDeliveryAuditTest, abrir manifest.local.json de escenario 09 y comparar logicalPath y kind=sic con B_DRIVE.md; en pavimentos comparar DrivePaths.pavement con PAVIMENTOS_TDR_DRIVE §Drive. **Evidencia:** cliente actual y manifiestos locales; lectura real de PRUEBA_FINAL/PE-22A/SIB-02, cuyos XLSX consolidados están fuera de los elementos. Los archivos observados son anteriores a v15.

**Archivos:** core/DrivePaths.kt:30,47; delivery/RouteDelivery.kt:78; core/DriveAssetPathRouter.kt:46. **Recomendación pendiente de autorización:** decidir expresamente cuál contrato se exige y preparar una corrección coherente cliente/servidor. Publicar el script local actual por sí solo no resuelve esta discrepancia con la solicitud.

## D02 · P1 · Backend publicado diferente del código auditado

GET sin credenciales, sobre los dos endpoints públicos extraídos de la APK Release: Drive anuncia **v6.2**, deliveryProtocol=1, scapProtocol=2, scapBundleVersion=4; no anuncia pathVersion=2 ni pavementProtocol. El script local anuncia v7.0, pathVersion=2, pavementProtocol=1 y bundle 6. Android utiliza **ScapDelivery.SCAP_BUNDLE_VERSION=7**. El segundo endpoint anuncia users-v2.4-tablet-admin.

**Reproducción segura:** acceptance_audit.py health. Solo GET público, sin leer/enviar tokens. Ver evidence/endpoint-health.json. **Impacto:** compatibilidad v15→servidor real no demostrada; no deducir rechazo exacto de un POST únicamente a partir de capacidades GET. **Archivos:** google-apps-script/Code.gs:20; scap/export/ScapDelivery.kt:25. **Recomendación:** verificar implementación efectiva, permisos, raíz y ACK en despliegue de pruebas aislado; conservar propiedades y datos; cualquier publicación requiere autorización explícita del usuario.

## D03 · P1 · SIC admite ACK que no acredita la ruta solicitada

DeliverySync comprueba ok, SHA-256, deliveryId, protocolo, nombre e IDs, pero no valida pathVersion/logicalPath. Un servidor compatible con protocolo 1 puede confirmar los mismos bytes en otra estructura y el lote acabar SYNCED. Se verificará la reproducción controlada en scenarios/wrong-path-ack.json; su transporte es sintético y se identifica como tal, nunca como subida real.

**Reproducción:** preparar lote real con Room/exportadores; contestar cada archivo con hash/IDs correctos, pathVersion=1 y logicalPath=WRONG/PATH; consultar estado del lote. **Esperado de aceptación:** no aprobar carpeta equivocada. **Código:** delivery/DeliverySync.kt:75–90. **Recomendación:** acordar ACK de ubicación y verificarlo antes de SYNCED; exigir posteriormente IDs/parents observados en Drive. Pavimentos y SCAP también admiten compatibilidad antigua; revisar su alcance antes de prometer V2.

## D04 · P1 · PNG importado puede entregarse con nombre y MIME JPEG

El selector común acepta image/*, copia bytes sin conversión a un archivo .jpg y solo valida que BitmapFactory reconozca dimensiones. CleanPhotoPolicy devuelve el archivo sin verificar su formato. RouteDelivery y PavementDrive lo copian como .jpg/image/jpeg. SCAP tiene conversión explícita de formatos no JPEG; no debe generalizarse ese comportamiento a SIC/Pavimentos.

**Reproducción local:** AcceptanceDeliveryAuditTest.importedPngBytesRemainPngUnderJpegDeliveryName, que utiliza el preparador SIC real. Inspección externa con Pillow sobre scenarios/photo-png-contract-probe/001_F01.jpg. **Reproducción física:** importar PNG desde galería en un SIC, preparar entrega, descargar copia y decodificar, sin modificar el original. **Archivos:** core/PhotoFilePicker.kt:23–31; core/CleanPhotoPolicy.kt:12; delivery/RouteDelivery.kt:57–61; pavement/PavementDrive.kt:102–109. **Recomendación:** decidir y aplicar validación/conversión uniforme preservando originales y orientación; no basta cambiar extensión.

## D05 · P1 · Numeración local sin asignador central para varias tablets

DrivePaths.element guarda un contador por ruta/familia en preferencias de cada instalación. Dos instalaciones nuevas pueden proponer, por ejemplo, PE-22A/ALCANTARILLAS/ALCANTARILLA_001 para UUID distintos. El servidor local protege propiedad de carpeta y rechaza conflictos; esa protección evita mezclar datos pero puede impedir la segunda entrega. La coordinación multi-tablet no está acreditada.

**Reproducción pendiente real:** dos tablets limpias de pruebas, misma ruta/familia, UUID diferentes; preparar y comparar logicalPath, enviar al entorno aislado y guardar ambos ACK. **Fuente:** core/DrivePaths.kt:10–21; DRIVE_V2_SERVER_CHANGE_REQUIRED §Estructura implementada; google-apps-script/Code.gs, driveV2Folder. **Recomendación:** definir asignación compatible con contrato aprobado; no introducir silenciosamente sufijos o estándares nuevos. Estado del riesgo: demostrado por diseño de contador, impacto remoto NO PROBADO.

## D06 · P1 · Capturas posteriores a una entrega iniciada no tienen nuevo lote disponible

RouteDelivery.prepare reutiliza la primera entrega no PREPARED si coincide fingerprint; si cambian registros/fotos, rechaza la preparación. enqueue también impide otra entrega iniciada para la ruta. Se conserva correctamente la copia previa, pero la siguiente jornada en esa ruta queda bloqueada sin mecanismo de nueva versión acordado.

**Reproducción:** preparar → marcar QUEUED/SYNCED → crear otro registro de la misma ruta → preparar. Mensaje: «La ruta ya tiene una entrega iniciada con otro contenido…». RouteDeliveryTest ya reproduce QUEUED+captura posterior. **Fuente:** delivery/RouteDelivery.kt:39–44,119–129. **Recomendación:** acordar ciclos de lotes/revisiones y definir qué se consolida; no borrar lotes ni reiniciar preferencias como solución. Es una limitación explícita de diseño, con impacto alto para capturas continuas.

## D07 · P1 · PCI/URCI final y entregables TDR completos no disponibles

Curvas VD/CDV sin digitalizar/validar: el cálculo debe permanecer pendiente; no hay base para aprobar índices o calificaciones. El Excel nativo PAV_ASF/RIG/NPV no demuestra equivalencia con EVALPAV/URCI ni todos los productos TDR (estadística, sectores homogéneos, VIZIR, etc.).

**Reproducción:** escenarios 06–08, abrir calculation.json/M_001.json y Resumen del Excel: index/rating pendientes y motivo de curva faltante. **Fuentes:** PAVIMENTOS_TDR_DRIVE §Relación con el TDR/Curvas PCI-URCI; PAVEMENT_MANUAL_MAPPING §Reglas de medición; pavement/PavementCalculation.kt:56. **Recomendación:** aprobación técnica de curvas/productos y prueba contra cálculo independiente antes de utilizar el módulo como evaluación final. La negativa a inventar índices es el comportamiento correcto, no una razón para cambiar null a cero.

## D08 · P1 · Inconsistencias normativas pendientes en formatos

SIC-14A bloquea filas 07/13 por contradicción de la referencia; SIC-34 conserva ACCIDENT_TYPE_SOURCE_CONFLICT para causa geotécnica/geológica y bloquea finalización/exportación. Plantillas SCAP originales tienen #REF!, clasificación ambigua y casos indefinidos; LOSA carece de fórmulas O/P para parte del bloque de entradas. Preservar una fuente no equivale a demostrar un cálculo correcto.

**Reproducción:** elegir los códigos afectados y comprobar advertencia/borrador; comparar source y XLSX generado en evidence/reference-workbooks.json/artifact-inspection.json; recalcular copia en Excel físico sin guardar fuentes. **Fuentes:** SIC14A_PENDING_DECISION; SIC34_TYPE_CODE_PENDING_DECISION; SCAP_TEMPLATE_INCONSISTENCIES:5,7; SCAP_PENDING_CLIENT_DECISIONS:5–7. **Recomendación:** decisión técnica/documento corregido; mantener bloqueos y pendientes hasta obtenerla. Son defectos/ambigüedades de contrato, no evidencia de corrupción introducida por v15.

## D09 · P2 · Contratos históricos y comentarios no reflejan el flujo actual

DRIVE_SCAP_CONTRACT exige COMPLETE y PNG con watermark; el flujo actual permite incompletos confirmados, JPEG limpio y bundle 7. SCAP_EXPORT_CONTRACT documenta bundle 5 y límites previos; corrección F.1/F.2/G.1 amplía continuaciones. Los reportes de Room 7/8/14 son históricos; producción está en 15. Los 15 tests históricos mezclan fixtures obsoletos y expectativas del exportador SCAP histórico.

**Reproducción:** comparación REQUISITOS_Y_CONTRADICCIONES.md/E_15_TESTS_HISTORICOS.md. **Recomendación:** consolidar contratos vigentes sin borrar antecedentes; actualizar fixtures solo después de acordar cada expectativa, conservando controles de integridad. No se han modificado esas pruebas.

## D10 · P2 · Fotografías y metadatos históricos no garantizan requisito limpio

La imagen real histórica leída de Drive es una lámina QA con logo/texto. CleanPhotoPolicy no puede quitar marcas ya presentes en el único original ni inventar EXIF/GPS ausentes. Además, los modelos nativos de fotos de pavimentos no capturan coordenadas propias de cada foto; su JSON enlaza foto/daño/fecha/categoría, sin inventar GPS.

**Evidencia:** evidence/remote/historical-001_PAN.jpg, EXIF SIMULATED_QA; pavement/PavementModels.kt:101 y PavementDrive.nativeJson. **Recomendación:** inventariar capturas históricas y reconocer faltantes; exigir GPS fotográfico solo conforme a una especificación aprobada, o registrar la limitación. No limpiar/remarcar ni reasignar silenciosamente fotos existentes. Esto no demuestra que la cámara v15 esté estampando fotos nuevas.

## D11 · P2 · Control de autorización antigua deshabilitado en configuración Release

BuildConfig Release genera ACCESS_CONTROL_ENABLED=false; DeviceAccessManager.check devuelve permitido tras obtener fingerprint cuando ese indicador está deshabilitado. La autenticación central de usuarios y los tokens individuales son mecanismos separados; no se concluye un bypass de login ni de TABLET_ADMIN.

**Fuente:** app/build/generated/source/buildConfig/release/com/tuempresa/inventariovial/BuildConfig.java:13; access/DeviceAccessManager.kt:58–60; USER_AUTH_IMPLEMENTATION §Sincronización y login. **Recomendación:** confirmar qué control de dispositivos se exige para este despliegue y probar tablet no provisionada, revocada y token rotado contra el servidor real. No se cambia la configuración en esta auditoría.

## D12 · P3 · Evidencia emulada insuficiente para PDF e impresión física

Los intentos del exportador Android de PDF en Robolectric dejan archivos de cero bytes; la inspección independiente los rechaza. No se presentan como PDFs aprobados ni se sustituyen por PDFs fabricados con otro motor. Los renderizadores bitmap se verifican separadamente; dispositivo Android/Excel deben producir, abrir y revisar archivos finales.

**Evidencia:** scenarios/06–08/HC_001.pdf y fixture.json; artifact-inspection.json. **Clasificación:** limitación del entorno de prueba; fallo productivo de PDF NO DEMOSTRADO. **Recomendación:** protocolo F, generación en tablet real y lector PDF independiente, impresión SCAP con sus áreas originales/continuaciones.

La lectura/reconstrucción con openpyxl no ejecuta fórmulas. El intento adicional con Excel COM solo lectura no completó: primero acceso denegado en sandbox, después RPC_E_CALL_REJECTED del motor. No se certifica recálculo nativo ni impresión. Ese resultado de entorno no demuestra que el libro esté corrupto y no se modifican fórmulas para eludirlo.
