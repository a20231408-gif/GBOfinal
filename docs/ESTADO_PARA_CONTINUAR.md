# GBO — Estado para continuar, 30/10/2026

> Actualización posterior: el usuario autorizó retomar el cierre funcional y preparar la Release original. Consultar **`GBO_CIERRE_FINAL_CAMPO.md`**, que sustituye el estado operativo de este documento histórico: candidata 1.4.20 (34), código `2bad57c`, selección Android 77/77 y backend local 14/14 + 3/3; producción sigue en V21 y no se distribuyó APK. Persisten el permiso de escritura de GBO sobre la raíz actual, el preflight completo de journals y las autorizaciones de corte/piloto. No retomar ensayos masivos ni asumir que Flavio quedó validado.

## Cierre y publicación bloqueada

Desarrollo detenido por solicitud del usuario. No iniciar funcionalidades, HTTP, Google ni compilaciones adicionales en esta intervención.

- Rama local: `codex/observaciones-campo-asus-20261030`.
- Último commit de código/pruebas al redactar: `e441881e947664eb7e4208449ec72a14a00f33ba`.
- Rama publicada: **ninguna de esta intervención**. Push **NO realizado**.
- Bloqueo comprobado por conector GitHub y API oficial: `a20231408-gif/GBOfinal` tiene `private=false`, `visibility=public` (30/10/2026). El usuario exige repositorio privado. No se cambió su visibilidad.
- Obtener el SHA exacto del cierre documental: `git log -1 --format=%H -- docs/ESTADO_PARA_CONTINUAR.md`; también figura en `CIERRE_VERIFICADO.json` del respaldo independiente.
- `main`, ramas históricas y producción permanecen sin cambios. No merge, force push, instalación ni distribución.

## Conservado y cambios WIP

| Estado | Cambio / evidencia Git |
|---|---|
| Preexistente conservado | Filtros y descarte reversible (`42e8a04`), persistencia de referencia kilométrica y exportación de longitud (`4c44a9f`), carpetas locales y acceso a archivos (`19ab997`). Verificar en campo; no reimplementar sin revisar. |
| Terminado en entorno aislado | Backend reanudable, journal durable, reconciliación de IDs/hashes, BUSY, ACK solo al completar (`a9e1393`). Pruebas Android de continuación sin ACK prematuro ni reenvíos (`7e9739e`). |
| **WIP** | Limpieza de copias públicas con ZIP de respaldo leído y verificado, selección individual/masiva, protección de pendientes y archivo/restauración local; conserva originales, Room, UUID y ACK. UI y recuperación tras interrupción requieren cierre y validación física. No equivale a borrado completo de datos privados (`ce4b727`). |
| **WIP** | Pavimentos: publicación automática al cerrar la unidad y descarte de trabajos automáticos de borradores antiguos; autoguardado de fallas no eliminado. Cambios entrelazados con la limpieza (`ce4b727`). No se alteró cálculo PCI ni severidades del daño 26. |
| **WIP** | `PublicDeliveryNames.kt`: helper provisional **sin integrar** con reservas V2/transporte/backend. Revisar truncamiento Unicode y entradas numéricas; no tiene pruebas específicas. No renombra históricos (`243ea4c`). |
| Pruebas conservadas | Cuatro nuevos casos focalizados (`e441881`); alcance limitado, no aprobación integral de las funcionalidades WIP. |

Archivos del WIP: `AutomaticLocalDocuments.kt`, `LocalCopies.kt`, `SharedDocumentStore.kt`, `UnifiedRecordsScreen.kt`, `ConfirmedLocalCleanup.kt` (paquete `localfiles`), `core/PublicDeliveryNames.kt` y `FieldCleanupAndConsolidationTest.kt`. No hay nuevas modificaciones funcionales durante este cierre.

## Pruebas ejecutadas y pendientes

- Selección Android anterior al WIP: **198/198**, 34 clases, `tmp/entrega-20261030/final3/xml` y `resultado-final-consolidado.json`. Cobertura P0.2 conservada en `cobertura77/xml`. No es toda la suite: quedaron 167 clases históricas fuera de aquella selección.
- Continuación Android: **35/35**, `tmp/desbloqueo-20261030/android/xml`.
- Backend reanudable Node: **10/10**, `tmp/desbloqueo-20261030/continuation-tests-6.log`. Intentos previos fallidos conservados; no presentarlos como PASS. Selección histórica backend: 17/25 con ocho fallos de recursos ignorados ausentes; no cerrada.
- Última ejecución local terminada: **12/12**, cero fallos, errores u omitidos, **51,88 s**. `FieldCleanupAndConsolidationTest` 4; `LocalDraftDiscard10CTest` 7; `PavementJointFlow20261019Test` 1. XML y resultados: `tmp/campo-final-20261030/foco2/`. Primer intento `foco1` no compiló el fixture; se preserva separado. No se inició otra prueba.
- Pendientes: regresión completa de limpieza, permisos SAF/MediaStore, recuperación tras cierres, selección masiva/UI, compatibilidad de `AutomaticDocuments149Test` con cierre de Pavimentos (contiene expectativas antiguas de borradores), nombres e integración local/Drive; pruebas físicas Flavio–Julián y actualización sin pérdida de datos. No probar contra datos reales sin autorización.

## Ambos backends y ensayo detenido

- **Anterior/productivo:** no modificado; la candidata de campo conserva configuración legítima anterior. Capacidades consultadas, pero no hay entrega grande productiva autorizada con ACK que cierre el riesgo observado de timeout. **NO-GO para afirmar piloto de sincronización grande seguro**.
- **Nuevo:** fuente sin secretos en `backend/aislado-commit-resumible/` y referencia en `backend/referencia-v21/`. Solo copia de pruebas; guardas exigen raíz privada de pruebas. No es importación directa sobre producción. Proyecto de copia `GBO_Inventario_PRUEBAS_GBO_20261030_NO_CORTE`; implementación HTTP de pruebas autorizada previamente, autenticación de aplicación y Drive privado. No trasladar token, raíz o endpoint de ensayo a la APK de campo.
- Evidencia previa: recuperación de 59 archivos; 62/62 hashes/propietarios/rutas, journal COMPLETE 67/67; conserva los 14 IDs anteriores; ACK HTTP real, repetición idempotente sin duplicados y BUSY. Evidencia privada exacta preservada con fuentes, propiedades y journals.
- Último ensayo, en **segunda raíz ficticia**: proceso HTTP terminó con **59/59 staging**. **No se inició commit, no tiene ACK**. No repetir desde cero ni confundirlo con el caso anterior completo. No hay ensayo HTTP iniciado en este cierre. La copia queda configurada en la segunda raíz; la primera y su evidencia completa permanecen conservadas.
- Transición definitiva pendiente: adaptación revisada para destino legítimo, protección de pendientes y journals, capacidades efectivas, integración Android y autorización de corte. No desplegar V21/Servicio de Usuarios ni cambiar permisos o cuentas.

## APK exactas existentes (no corresponden al WIP actual)

| Artefacto | Identidad y estado |
|---|---|
| `C:\GBO\tmp\entrega-20261030\release-online\GBO_1.4.19_33_CANDIDATA_NO_DISTRIBUIR.apk` | `com.tuempresa.inventariovial`, **1.4.19 (33)**, firmada con certificado histórico, Room 21; backend anterior. SHA-256 `0aa733c794f097eefce06be3a2c1fb0ba555d45cd53702ade51b552165dc4254`. Construida antes del WIP; no instalar/distribuir por este cierre. |
| `C:\GBO\tmp\entrega-20261030\backend-privado\desbloqueo\GBO_1.4.19_33_ENSAYO_SCAP_NO_DISTRIBUIR.apk` | `com.tuempresa.inventariovial.ensayo`, **1.4.19-ENSAYO (33)**, firma histórica, endpoint/token exclusivos de pruebas. SHA-256 `073f005a8475a5342a0e667cc2174b8cd3a31c976fa3cc4b3f8b01e69108b2b8`. No instalada, no es APK de campo. |

Certificado histórico SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. No se compiló ni incrementó versión en este cierre; cualquier candidata con cambios posteriores a 1.4.19 requiere nueva versión y validación. Distribución general y piloto necesitan autorización correspondiente.

## Respaldo y traslado seguro

Respaldo independiente restringido al usuario local y SYSTEM: **`C:\Users\ASUS\GBO_RESPALDO_PRIVADO_20261030_1300`**.

- `repositorio/GBO_CIERRE_20261030.bundle`: historia Git, rama y todos los commits conservados; se verificará con `git bundle verify`. No subir este bundle a un repositorio público.
- `evidencias/`: copia independiente de resultados, XML, scripts de ejecución y capturas preservadas; fuera de Git, tratar como privada.
- `privado/`: configuración legítima, keystore, fuentes exactas del editor de pruebas, propiedades, journals/IDs/ACK y APK. **No subir a GitHub ni copiar al chat.** Contiene también propuestas no ejecutadas, identificadas como tales; no confundir con código validado.
- `CIERRE_VERIFICADO.json` y `MANIFIESTO_SHA256.json`: SHA final, bloqueo de push e integridad de las copias; el documento Git no contiene el hash de su propio commit para evitar autorreferencia.
- PNG locales ignorados: preservados sin agregarlos a Git. El inventario previo `imagenes-conservadas.json` y la copia independiente permiten comprobar sus hashes; no borrar originales.

Necesarios en otra laptop: `local.properties` (recrear **solo ruta SDK** local), `server.local.properties`, `drive.local.properties`, `keystore.properties`, keystore histórico `gbo-inventario-vial-release`, y propiedades privadas del Apps Script de pruebas si se retoma ese entorno (incluida `GBO_ISOLATED_ROOT_ID`). Usar respaldo privado legítimo; no inventar valores, no sustituir firma ni reutilizar token de ensayo. Los archivos `gradle.properties`, `gradle/gradle-daemon-jvm.properties` y `gradle/wrapper/gradle-wrapper.properties` son configuración de compilación sin credenciales y permanecen versionados.

Las exclusiones de configuración, APK/bases/logs y keystore se mantienen en `.gitignore`; se hizo portable la exclusión del keystore sin extensión, antes dependiente de `.git/info/exclude`. Transferir el respaldo por medio cifrado privado. Las ACL de Windows protegen esta copia local; no equivalen a cifrado para transportar archivos.

Restauración de código en otra laptop, sin tocar este checkout:

```powershell
git bundle verify 'X:\respaldo\repositorio\GBO_CIERRE_20261030.bundle'
git clone -b codex/observaciones-campo-asus-20261030 'X:\respaldo\repositorio\GBO_CIERRE_20261030.bundle' C:\GBO_RECUPERADO
git -C C:\GBO_RECUPERADO remote set-url origin https://github.com/a20231408-gif/GBOfinal.git
```

**Antes de push:** el propietario debe hacer privado el repositorio o proporcionar un destino privado autorizado; comprobar `private=true`, volver a revisar archivos/commits y publicar únicamente esta rama sin force. Este cierre no solicitó ni ejecutó un cambio de visibilidad. Evidencia de privacidad y revisión local: `tmp/cierre-preservacion-20261030/` y su copia independiente. El escaneo no encontró valores de los tokens/contraseñas locales ni endpoints Apps Script en los cambios e historia nueva revisados; dos coincidencias de cabecera de clave son aserciones literales de los empaquetadores históricos, no claves.

## Pendientes funcionales y prompt para continuar

Pendientes: cerrar limpieza y restauración con respaldo comprobable; completar nombres descriptivos y estructura equivalente local/Drive sin renombrar históricos; consolidación visible de Pavimentos conservando autoguardado, PCI y daño 26; UX de registros/archivos; pruebas de filtros/fechas/longitud/descarte/estados ya implementados; transición del backend reanudable y diagnóstico físico autorizado de Flavio/Julián. No se generó la matriz final funcional: la prioridad fue sustituida por preservar y publicar de forma segura.

> Continúa en C:\GBO, rama codex/observaciones-campo-asus-20261030. Lee docs/ESTADO_PARA_CONTINUAR.md y CIERRE_VERIFICADO.json del respaldo privado. Primero verifica SHA y privacidad real de a20231408-gif/GBOfinal: el push quedó bloqueado por repositorio público; no publiques hasta comprobar private=true y autorización del destino. Todo código está conservado, incluido WIP ce4b727/243ea4c y pruebas e441881. No descartar ni duplicar cambios. No reiniciar ensayos grandes ya acreditados. La segunda raíz ficticia solo llegó a staging 59/59; no tiene commit/ACK. No cambiar producción, main, Drive, Apps Script ni tablets; no distribuir APK sin autorización. Cuando se autorice retomar desarrollo, cerrar WIP con pruebas dirigidas, mantener separados backends y preparar nueva versión solo tras validar los cambios. No mostrar secretos.
