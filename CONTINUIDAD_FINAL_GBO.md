# GBO — Continuidad final 1.4.26 (40)

Último ajuste de fuente, 03/11/2026: rama local `codex/consolidados-sic-principales-20261102`.
EXCEL_SIC es la publicación principal de SIC ordinarios verificados; filas/revisiones individuales
privadas y excepciones conservadas. 65 pruebas únicas PASS. Ver actualización en
`docs/CORRECCION_DOCUMENTOS_1_4_27.md`. El propietario confirmó cinco pruebas físicas PASS de la
APK 1.4.27 previa; esa APK no contiene este último ajuste. Sin nueva APK, push o cambios V21.

## Actualización posterior: corrección documental 1.4.27 (41), solo local

Ver `docs/CORRECCION_DOCUMENTOS_1_4_27.md`: nueva rama local
`codex/documentos-publicos-estables-20261102`, fuente funcional `72a100b7ceda38b4001f7a615c81ffcdbe188d98`.
138 casos únicos PASS; APK privada con firma histórica, aún no instalada/publicada. Documentos nuevos
en carpeta estable por carretera/familia/elemento/PR, historial recuperable, sin mover históricos.
La candidata 1.4.26 y PR #8 de las secciones siguientes quedan preservados. V21/ROOT/ACK intactos.
Esta corrección NO está subida a GitHub por instrucción de no publicar. NO-GO online/general vigente.

Fecha: 02/11/2026, America/Lima. Este documento prevalece sobre estados históricos que dicen
«no instalada», «tablet 1.4.25» o «pruebas físicas no ejecutadas».

**GO para las comprobaciones locales A–D ya realizadas. NO-GO para sincronización general,
publicación y distribución.** Una entrega nueva real, su ACK y su propietario institucional
todavía NO están acreditados. No generar otra versión ni repetir selecciones aprobadas.

## Fuente recuperable y revisión

- Repositorio: https://github.com/a20231408-gif/GBOfinal
- Rama final: `codex/candidata-final-local-20261102`.
- PR #8: https://github.com/a20231408-gif/GBOfinal/pull/8 (abierto, borrador).
- Fuente compilada: `853b455e57525ee61467e5896dd1770f87f9b612`. Los commits posteriores
  hasta este cierre solo actualizan documentación; Android/backend no cambiaron.
- PR #5: `65d0c794729bdbca73803eb29af1508c5ee7f002`, presentación de sincronización.
- PR #6: `4b6ab0ea00e550615874db4b499a7b493cb56e21`, previsualización de eliminación bloqueada.
- PR #7: `9c696084253b0ed34f29949ee1e1e04f9212707d`, backend administrativo local.
- Los tres SHA son ancestros de la candidata. #8 tiene base #7, #7 tiene base #6;
  #5 está incluido mediante merge local normal. No fusionar automáticamente estos PR.
- Antes del cierre, GitHub #8 estaba en `26bb923e7192fc74e34c7aa093a98b429354bae7`.
  Pendían tres commits documentales: `93be9878bd4cbb2bee4dc7d8373803b6f4dd3d6f`,
  `cc4e62350ef1fa5a1312e9ba2133949f80482cc1`, `02df0bc452ce8c703efbaddcc7f5f25cb0eeb7d6`.
  Este cierre publica esos commits y este documento tras revisión. El SHA definitivo es
  el HEAD de la rama/PR #8, verificable con los comandos siguientes, y se comunica al propietario.
  Un documento no puede contener el SHA de su propio commit.

```powershell
git clone --single-branch --branch codex/candidata-final-local-20261102 https://github.com/a20231408-gif/GBOfinal.git C:\GBO
Set-Location C:\GBO
git rev-parse HEAD
git ls-remote origin refs/heads/codex/candidata-final-local-20261102
git switch -c codex/cierre-sincronizacion-controlada
```

No cambiar main ni la rama histórica de integración. La candidata contiene las dependencias;
no hace falta aplicar de nuevo sus commits. No confundir PR abierto con despliegue productivo.

## APK conservada e instalación física

Ruta exacta privada:
`C:\Users\ASUS\GBO_CANDIDATA_PRIVADA_20261102_1_4_26_40\GBO_1.4.26_40_DEBUG_ORIGINAL_CANDIDATA_NO_INSTALAR.apk`

SHA-256: `47492403eee0c9615742094ca5c646c5aae410591a443c1c3dbdb1e5ea34ef32`.
Recalculado en este cierre, coincide. Package `com.tuempresa.inventariovial`, versión 1.4.26,
versionCode 40; Debug, certificado histórico SHA-256
`d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`.
No es Release ni puede asumirse que actualiza una instalación firmada con Release.
El nombre NO_INSTALAR conserva una prohibición anterior: la instalación específica en Xiaomi
fue autorizada y realizada; no constituye autorización para otros dispositivos.

Xiaomi actualizada sin borrar datos. Evidencia privada de instalación confirma 43 tablas principales
idénticas antes/después de abrir, 797 archivos internos conservados, tres bases SQLite con
integrity_check=ok y foreign_key_check=0, outbox/ACK sin cambios en esa comprobación.
El propietario confirmó **A búsqueda/filtros, B abrir ficha/documentos, C guardar borrador ficticio
Solo dispositivo, D descartar/restaurar: PASS** en 1.4.26. Es evidencia manual del propietario,
no automatización ni aprobación de toda la matriz histórica. P11–P15/aceptación online no acreditadas.

La Xiaomi conserva dos SIC históricos ERROR/QUEUED y debe permanecer offline. No tocar su outbox,
no reenviar, limpiar ni restaurar datos. El worker procesa la cola global: activar Internet para
una ficha nueva no garantiza aislamiento. El emulador fue dejado fuera del trabajo por el propietario;
no iniciarlo ni asumir que se completó una prueba allí.

## Reconstrucción y pruebas acreditadas

Fuente Android, wrapper Gradle 9.6.0, catálogo de dependencias, schemas Room 21, plantillas SCAP,
mapas y pruebas están versionados. Sin diferencias funcionales respecto al SHA compilado.
Los árboles Git al cierre son: app `3c080a06f4768ac337819930ef27416b7c034d57`,
backend `5fe8199a7ece0ff961c612a85fadecc9576e38b6`,
google-apps-script `3cccc71ab5d26ed53c766e824e1ec512acc2788d`.
Esto conserva fuentes; no promete una APK bit a bit idéntica al recompilar.

En otra laptop: instalar Git, Android Studio/JBR compatible (ASUS Java 25), SDK 37 y Node.
Crear local.properties para el SDK propio usando `docs/config-ejemplos/`.
Para desarrollo sin Google ni secretos, usar el perfil de validación:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat '-PgboValidationBuild=true' :app:assembleDebug
# Solo si hay un cambio que justifique repetir pruebas:
.\gradlew.bat '-PgboValidationBuild=true' :app:testDebugUnitTest --init-script tools/migracion/pruebas.init.gradle --init-script tools/validacion/v2-mocks.init.gradle --tests '*SyncRecovery10DTest'
node --test backend/admin-v21/*.test.cjs backend/admin-removal-adapter/*.test.cjs backend/admin-removal-proposal/simulation.test.cjs
```

Estos comandos son instrucciones futuras, NO ejecutadas de nuevo en este cierre. El Debug de
validación no es la APK oficial y no lleva la configuración productiva. Para reconstruir una
candidata original hacen falta transferencia privada de configuración y firma, nunca GitHub.

Resultados previos: **154/154 Android (27 clases), 65/65 backend**, más **15/15** contra fuente
V21 exacta en harness privado y **1/1** de capacidades en VM. Selecciones, no suite histórica
completa ni prueba Google. XML Android: `C:\GBO\tmp\candidata-final-20261102\final\`;
backend: `C:\GBO\tmp\autonomo-20261102\backend-tests.log`. No publicar logs/datos reales.

## Backend y endpoint vigente

**Producción sigue en V21 ORIGINAL. No se actualizó fuente, versión, despliegue ni ROOT en este cierre.**
La APK utiliza el endpoint original, no el entorno de ensayos. Por seguridad no se reproduce la URL
operativa en el repositorio público: su referencia exacta está en
`C:\GBO\drive.local.properties` (DRIVE_WEB_APP_URL), contrastable con
`deployment-android-privado.json` y `deployments-privados.json` de la captura privada indicada abajo.
El deployment conocido termina en `B6r66Q`. No sustituirlo ni copiar tokens a informes.
El servicio de Usuarios es independiente; configuración en server.local.properties.
Flags verificados de candidata: nombres V2 habilitados, SIC17A/B deshabilitados.

V21 admite altas/archivos/consolidación/ACK en las pruebas locales. GET real acreditó capacidades
3/4, nombres V2 y panel SCAP v2; no demuestra una entrega ni propietario. La etiqueta web
«Ejecutar como Yo» cambia según visor: NO prueba que el ejecutor sea institucional GBO.
Lectura personal previa enumeró 889 metadatos; los JSON dieron 403 appNotAuthorizedToFile.
No hay inventario completo de estados/ACK conciliados. OAuth adicional bloqueado por Google:
no repetir ni evadir; usar sesión web legítima y permisos existentes.

Backend administrativo reanudable/autoridad/tombstones/adaptadores está preservado en módulos
locales revisables, probado con mocks, **sin cableado ni activación en V21**. Faltan verificadores,
Users e inventario autoritativos reales. Eliminación definitiva continúa deshabilitada y fuera
del cierre de altas nuevas. No fusionar #6/#7 como si acreditaran eliminación operativa.

## Material privado indispensable (solo transferencia segura entre autorizados)

| Material | Ubicación en ASUS / limitación |
|---|---|
| Fuente original del editor y V21 publicada, independientes e inalteradas | `C:\Users\ASUS\GBO_CLASP_PRIVADO_20261102T222357Z\editor-original` y `version-publicada-21` |
| Identidad/despliegue y comparación privada | Mismo directorio: `deployment-android-privado.json`, `deployments-privados.json`, `comparacion-verificada-privada.json` |
| Harness V21 exacto y propuesta administrativa privada | Mismo directorio: `validacion-ficha-nueva-v21` y `candidato-local-v21-admin` |
| Configuración original | `C:\GBO\server.local.properties`, `drive.local.properties`, `keystore.properties`; local.properties debe adaptarse al SDK nuevo |
| Firma Debug e histórica Release (distintas) | `C:\GBO\.private-signing\huawei-debug.keystore`; `C:\Users\ASUS\.android\debug.keystore`; Release `C:\GBO\gbo-inventario-vial-release`, referenciada por keystore.properties |
| Evidencia instalación 1.4.26 | `C:\Users\ASUS\GBO_INSTALACION_126_PRIVADA_20261102T234624Z\INSTALACION_RESULTADO.json` y archivos contiguos |
| Quinto respaldo verificado, previo a actualización | `C:\Users\ASUS\GBO_QUINTO_RESPALDO_PRIVADO_20261102T232523887Z` |
| Respaldos anteriores inalterados | Primer/segundo/tercero/cuarto en directorios privados GBO_TABLET_* bajo `C:\Users\ASUS`; no sobrescribir ninguno |

Quinto TAR: `originales\datos-internos-originales.tar`, SHA-256
`4e3ca0855d2ddf55970abc937efc0eeab54b1cab748b75e55c45a5a6724812a7`, 826 archivos,
tres SQLite íntegras. No representa las ediciones manuales A–D posteriores; no restaurarlo
para «aislar» la prueba ni asumir que sigue siendo copia exacta del estado actual.
SAF tiene limitaciones de permisos y Android Keystore no es exportable; restauración no ensayada.
La fuente V21 exacta no se sustituye por google-apps-script local: existen diferencias documentadas.
Backend público reconstruible para mocks; reproducción exacta del candidato contra V21 requiere
las capturas privadas. Credenciales OAuth no son necesarias para pruebas locales ni deben compartirse:
cada desarrollador usa su identidad y consentimiento legítimos cuando corresponda.

## Único siguiente paso: una prueba real aislada, con condiciones previas obligatorias

Retomar únicamente la aceptación de UNA ficha nueva desde otro dispositivo limpio autorizado,
con esta misma APK verificada. Sin Xiaomi, emulador ni datos históricos copiados. Antes del POST:

1. Acreditar desplegador/ejecutor institucional y permisos efectivos mediante lectura legítima;
   registrar evidencia privada, no inferirlos de «Yo» ni del almacenamiento contratado.
2. Verificar que el dispositivo limpio tiene outbox/recuperaciones vacíos y autorización para
   instalar/iniciar sesión. No habilitar red en Xiaomi. No copiar sus preferencias ni credenciales.
3. Identificar destino ficticio único admitido por el catálogo y demostrar que no modifica
   ningún consolidado existente. **Hoy esta condición no está demostrada**: las seis carreteras
   del catálogo ya existen y V21 comparte EXCEL_SIC por carretera. Una carpeta de elemento nueva
   no basta. La autorización anterior PE-22A permitía actualizar su consolidado con respaldo;
   no satisface el nuevo alcance de dejar consolidados existentes intactos. No reutilizarla
   para afirmar aislamiento. Si no existe flujo sin ese efecto, detener el POST y presentar
   al propietario la excepción mínima exacta para aprobación; no inventar una carretera.
4. Confirmar que el flujo no recuperará entregas ajenas: v3Store puede recuperar PENDING
   globalmente. No ejecutarlo como lector; una cola Android vacía no aísla por sí sola el servidor.
5. Solo con esas condiciones acreditadas y autorización vigente del alcance exacto: capturar una
   ficha PRUEBA, fotos sintéticas, revisión inicial; enviar una vez por V21. Comprobar archivos,
   nombres/rutas, hashes, UUID/revisión/ACK persistente, propietario GBO, ausencia de duplicados
   y ausencia de cambios ajenos. Guardar evidencia privada. No reintentar indiscriminadamente.

**Bloqueo mínimo actual:** falta acreditar ejecutor institucional y un destino/flujo sin efectos
sobre consolidados o recuperaciones ajenas. No se realizó POST. ACK real y propietario: NO VERIFICADOS.
No hace falta desplegar eliminación administrativa ni resolver la colisión histórica para avanzar,
pero tampoco puede ignorarse un efecto global del servidor durante la prueba nueva.
Tras PASS real, el propietario decide publicación/distribución separadamente. Hasta entonces
mantener NO-GO online/general. En este cierre: sin tests repetidos, nuevos respaldos, APK,
emuladores, merge, despliegues, cambios Drive o borrados.
