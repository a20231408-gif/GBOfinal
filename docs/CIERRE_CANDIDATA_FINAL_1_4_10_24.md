# Cierre de candidata final GBO Ruta Clara 1.4.10 (24)

19/10/2026. **GO para iniciar validación física controlada del propietario. No aprobada para campo, distribución general ni uso como única fuente de información.**

## Artefacto y compatibilidad

- APK: `D:\Downloads\GBO_PRUEBAS_8B\output\GBO_RUTA_CLARA_CANDIDATA_FINAL_1.4.10_24_20261019\GBO_Ruta_Clara_1.4.10_24_FINAL_CANDIDATA.apk`.
- applicationId: `com.tuempresa.inventariovial`; versionName **1.4.10**; versionCode **24**, conservado tras confirmación del propietario de que no hubo distribución externa de una versión igual/superior.
- SHA-256: `b1e57c250153703548a44f45ff32de1713547deef5786bf18421d81a66c8fae0`.
- Certificado SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. Firma v2/v3 válida; mismo certificado que la APK local 1.4.9 (23) verificada. APK completo, no debug/test, alineación ZIP y 16 KiB correctas; minSdk 26, targetSdk 37.
- Room conserva versión 20 y su identityHash está presente en ambas APK. No se alteraron entidades, migraciones ni contrato remoto.
- La regla R8 existente conserva Sic20FormState. El mapping confirma el nombre de clase; la lectura directa del DEX confirma que todos sus campos declarados, incluidos classCode, typeCode y wallLengthMeters, coinciden con los de la APK local 1.4.9. R8 omite del mapping los campos conservados que no renombra; no se usó esa ausencia como prueba de incompatibilidad. Las observaciones pertenecen al encabezado compartido, no a Sic20FormState.
- ADB no detectó dispositivos en esta ejecución. **No se comprobó la firma instalada de una tablet concreta ni se ejecutó Android físico.** Ante rechazo del instalador: detenerse; nunca desinstalar o borrar datos.

## Compilación y pruebas realmente ejecutadas

Gradle ordinario: `validateReleaseConfiguration`, `testDebugUnitTest` con filtros focalizados, `assembleRelease` y verificador final `verifyReleaseIdentity`: **PASS**. No se usó `gboValidationBuild=true`, configuración ficticia ni configuration cache. Dependencias resueltas desde caché local offline.

**83 casos JUnit únicos PASS; 0 fallos finales y 0 omitidos**. Resultados por caso y logs redactados en `docs/cierre-candidata-final-1.4.10-20261019/`. Compilación de aplicación y fuentes de tests completa; ejecución de casos limitada al alcance necesario.

Se conservan cuatro fallos iniciales del arnés de recuperación: sus transportes ficticios anteriores a V2 no anunciaban ni reservaban nombres V2. Se actualizaron solo los fixtures `SyncRecovery10DTest`, `HotfixBusy146Test` y el helper ficticio `ReleasePhotoV2Fixture`; no se desactivó V2 ni se alteró producción. Se volvió a ejecutar la regresión con la configuración real. El primer filtro había mantenido las seis clases de la candidata anterior (27 PASS); se conservaron sus resultados y se completaron por separado los casos nuevos, sin repetir los aprobados de esa tanda.

- SIC-20: Muro/Badén, alias históricos de la misma clase, UUID, fotografía, JSON parcial/Room, reapertura, edición, finalización normal y bloqueo de identidades contradictorias. Regresiones SIC y SCAP relacionadas.
- Pavimentos: juntas 25 H/V, berma 27 en ambos bordes confirmados, persistencia y rollback, invariancia PCI/Excel/campos del daño, D24 cuatro esquinas, anclaje/parches, daño 22 y junta 26; vista previa/confirmación y cancelación.
- Sincronización: BUSY conservando revisión/preparación y ACK anterior, dos bases de dispositivo ficticias, recuperación tras límite de reintento, copia local durable sin duplicar entregas, ACK final perdido, ACK incompleto, edición durante entrega y cancelación del trabajador.
- **Simulación local:** transportes HTTP interceptados/ficticios y Robolectric. No son ensayos de rendimiento ni pruebas reales de Google. Las imágenes `27-*-ROBOLECTRIC.png` usan los dibujantes reales bajo Robolectric; no son capturas de tablet ni certificación de PdfDocument físico.

El APK contiene PdfDocument nativo y no contiene el adaptador SyntheticPdf146 de pruebas. PDF horizontal nativo y conservación real tras actualización requieren el checklist físico.

Se renderizaron y revisaron las 27 páginas de tres PDF raster de ensayo (corto, largo y multisección), todas horizontales. Se conserva la maquetación existente y las continuaciones de tabla en páginas propias; no se rediseñó el documento. Son documentos ficticios de Robolectric, no PDF producidos en una tablet física.

## Corrección mínima de esta compilación

Se confirmó una ocultación parcial del símbolo rojo 27 por el marco negro y las llamadas dibujadas después en el PDF. Se repinta únicamente la misma marca perpendicular después de esos elementos, en Canvas y PDF, **sin desplazar coordenadas, cambiar geometría, cantidad, PCI ni numeración**. Nueva regresión verifica píxeles rojos en ambos bordes y conservación de datos. Patch exacto en `AJUSTE_VISIBILIDAD_27.diff`. No se rehízo D24 ni otras funciones aprobadas.

Fuentes funcionales ajustadas: `pavement/ui/PavementScheme.kt` y `pavement/PavementRigidPdfScheme.kt`. Pruebas ajustadas: `SyncRecovery10DTest.kt` y `HotfixBusy146Test.kt`; nuevas: `PavementShoulderVisibility24Test.kt` y `ReleasePhotoV2Fixture.kt`. La configuración Gradle de la app conserva exactamente su hash inicial.

Los demás avances de SIC-20/Pavimentos/PDF se conservaron y quedaron integrados en la compilación. No se detectó pérdida/corrupción en los casos locales ejecutados; ello no equivale a una garantía universal ni sustituye la prueba física.

## Pendientes y riesgos identificados

1. Actualización y PDF nativo en dispositivo: pendientes. Compatibilidad de certificado acreditada frente al artefacto local 1.4.9, no frente a una lectura de cada instalación.
2. Incidencia real de entregas demoradas (2/17 SIC y 1/11 Pavimentos): **causa y rendimiento Google aún no acreditados**. La revisión dirigida del código conserva cola Room, snapshot, revisiones y ACK; BUSY pasa a espera/reintento y recuperación periódica. Android puede posponer WorkManager por conexión/energía. Ni “Subiendo” ni archivos procesados equivalen a entrega confirmada.
3. Borradores históricos de identidad contradictoria permanecen bloqueados y conservados; no se repararon mediante conversión automática. Curvas PCI que ya estén pendientes siguen pendientes: no se inventó cálculo.
4. Avisos de build no bloqueantes: opción experimental android.disallowKotlinSourceSets; fallback de target JDK del tooling; nulabilidad de reflexión enumConstants en InventoryDraftStore y condición redundante del preview D24; avisos JDK/Robolectric. No se modificaron áreas ajenas para silenciarlos.

Desde Diagnóstico de estabilidad → Exportar diagnóstico se obtiene el agregado seguro existente. La explicación de tiempos del servidor requiere conciliar sus ejecuciones autorizadas; no se volvió a auditar ni modificar Apps Script/GBO LAB.

## Conservación y entrega

Respaldo reversible de fuentes/configuración pública y artefactos generados anteriores en `tmp/cierre-candidata-campo-20261019/` (`FUENTES_ANTES.zip`, `ARTEFACTOS_GENERADOS_ANTES.zip`, hashes y Git inicial). No extraer sobre todo el proyecto ni usar reset/clean. Las diez APK previas permanecen byte a byte sin cambios, incluida la candidata anterior de numeración 24.

Cinco archivos privados de raíz y 15 campos efectivos de configuración Release verificados mediante hashes: sin cambios. Versión, firma, applicationId, servicios y contrato conservados. No se publicaron valores privados en informes.

No se instaló ni distribuyó, no se subieron fichas, no se modificaron servidores/propiedades privadas/datos, no se hizo commit ni push. Git conserva el trabajo local y su rama actual.

Seguir `docs/PRUEBA_FISICA_FINAL_1_4_10_24.md`: Muro, Badén, 25/27, D24/parches/junta, PDF/Excel, fotos, reapertura y cola offline. La subida a Drive requiere una autorización separada y ACK verificable antes de considerarse aprobada.
