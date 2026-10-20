# GBO Ruta Clara — etapa A y compatibilidad de la tablet

Fecha: 2026-10-14. Cambios locales para revisión; sin commit, push, Release ni instalación.
Base: `5de1242581dc460ea38d45006020d022f8d4da78`, rama `codex/observaciones-campo-manual-10I`.
Checkpoint conservado: `checkpoint-pre-redisenio-visual-2026-10-14`.

## Comprobación de instalación solicitada por el propietario

Consulta ADB de solo lectura al dispositivo `523bc083`, con comprobación de los APK mediante `aapt` y `apksigner`.

| Dato | GBO instalado | Debug nueva |
|---|---|---|
| applicationId | `com.tuempresa.inventariovial` | `com.tuempresa.inventariovial` |
| versionName | `1.4.2` | `1.4.3` |
| versionCode | `16` | `17` |
| Depurable | No | Sí |
| SHA-256 del certificado | `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315` | `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872` |

**La nueva Debug no es compatible como actualización del APK instalado: las firmas son distintas.** No se ha intentado instalar, desinstalar, cambiar firma, limpiar almacenamiento ni cambiar identidad. No se propone desinstalar para resolverlo.

Para una actualización de esta instalación se necesitará una futura candidata con la firma histórica, previa autorización independiente del propietario. Esta etapa no genera esa Release.

### Registros y entregas pendientes

**No comprobados.** Android rechazó `run-as` con `package not debuggable`. No se pudieron leer Room, archivos privados de fotografías/videos ni la base WorkManager. No se puede afirmar que estén vacíos o que todo esté sincronizado. Tampoco se extrajo una copia de los datos de campo.

Se copió únicamente el APK instalado al temporal ignorado para verificar su certificado. Eso no constituye respaldo de registros, fotografías, videos ni credenciales.

Para completar el inventario de pendientes, el propietario debe consultar en GBO los registros/borradores y los detalles de Entregas Drive, incluyendo medios pendientes y errores. Una consulta visual no equivale a un respaldo completo de los datos privados.

### Configuración y servicios

- El rediseño no cambia `AuthGraph`, `AuthRepository`, `UserServiceClient`, WorkManager, Room, protocolos Drive ni los archivos privados de configuración.
- Las acciones existentes de aprovisionamiento, rotación de token, login y sincronización conservan sus callbacks y validaciones.
- Una comparación local sin imprimir valores confirmó que la URL y token de Drive y la URL base del servidor de la configuración local aparecen tanto en el APK instalado como en la Debug nueva.
- Esa coincidencia de constantes no permite descifrar ni verificar la configuración efectiva del servicio de usuarios almacenada en la tablet. No se cambió esa configuración y no se hicieron solicitudes de prueba al backend.
- No se puede garantizar el resultado de una actualización que está bloqueada por firma. No se ha realizado ninguna instalación.

## Implementación visual local

Solo acceso y configuración inicial:

- `auth/LoginScreen.kt`: conexión con la presentación adaptable; mismos controles, mensajes y acciones. `PinField`, compartido por otras pantallas, no se modifica.
- `auth/AuthPresentation.kt`: fondo recortado proporcionalmente, logotipo sobre tarjeta blanca, composición lateral con ancho suficiente y columna en vertical o con texto grande; scroll e insets de teclado/sistema.
- `auth/LoginFormInputs.kt`: entradas retenidas exclusivamente en memoria mediante ViewModel durante la recreación de Activity. No se usan SavedStateHandle, Bundle, preferencias, archivos ni logs para PIN/token. Al salir de acceso se limpian las referencias; tras muerte del proceso no se recuperan secretos.
- Transición de opacidad de 200 ms mediante animación Compose que utiliza la escala de animación Android. Los controles se componen inmediatamente; no hay espera funcional ni duplicación de llamadas para animar.

Cinco recursos importados, exactamente los seleccionados (1.391.195 bytes):

| Destino en `app/src/main/res` | Origen del paquete |
|---|---|
| `drawable-nodpi/gbo_login_background.webp` | `02_LOGIN/login_fondo_vertical.webp` |
| `drawable-land-nodpi/gbo_login_background.webp` | `02_LOGIN/login_fondo_horizontal.webp` |
| `drawable-nodpi/gbo_setup_background.webp` | `03_CONFIG_INICIAL/config_inicial_vertical.webp` |
| `drawable-land-nodpi/gbo_setup_background.webp` | `03_CONFIG_INICIAL/config_inicial_horizontal.webp` |
| `drawable-nodpi/gbo_auth_logo.png` | `01_LOGO/logo_gbo_principal_transparente.png` |

No se modifica el logo ni se usa sobre azul oscuro. El PNG suministrado se conserva sobre blanco, con proporciones originales. No se importan los recursos de dashboard, splash, manual, mapa ni patrones. Sin dependencias nuevas ni cambios en Gradle, versionCode, applicationId o firmas.

## Verificación

- `:app:assembleDebug`: PASS, compilación ordinaria con configuración local.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, 41.731.621 bytes. SHA-256: `65331a63a7fbdfec443cd45572e2c6e67de93f2ec7990e415560f73a59f496a3`.
- APK Debug previo a esta etapa: 40.329.890 bytes; incremento: 1.401.731 bytes, aproximadamente 3,48 %.
- `UserAuthenticationTest`: 23/24 PASS. Un fallo en `fieldSessionOperatorComesFromAuthenticatedUser`: WorkManager no inicializado en el entorno de prueba. El stack termina en la construcción de FieldController, fuera de las pantallas modificadas. No se cambia producción ni ese flujo para resolver una configuración del test.
- `AuthRutaClaraScreenTest`: 5/5 PASS. Configuración inicial con validación real de entrada inválida, login horizontal y filtro PIN, texto al 200 % con viewport de 300 dp, recreación de Activity conservando entradas solo en memoria y limpieza al salir, login bloqueado y rotación de token deshabilitada.
- `AuthRutaClaraRenderTest`: 4/4 PASS. Selección de recursos de orientación y renderizado Android de login/configuración en horizontal y vertical, con identidad ficticia y sin credenciales reales.
- `git diff --check`: PASS. Ningún archivo de autenticación de negocio, base de datos, sincronización, configuración o versión cambió.

### Capturas y límites de validación

No se ha instalado la Debug ni se han obtenido capturas físicas del diseño nuevo. No hay emulador configurado. La firma incompatible y la instrucción expresa de no instalar impiden capturarlo en esta tablet.

La configuración inicial no se expondrá borrando el aprovisionamiento existente. Necesita un dispositivo/emulador aislado sin datos reales para su comprobación física.

Los cuatro renderizados locales se revisaron visualmente. Proceden del árbol Android/Compose en Robolectric con identidad ficticia. Se identifican como renderizados locales; no son capturas de la tablet ni prueban su teclado, GNSS, servicios o rendimiento físico. El logo se ve sobre blanco a 280 dp de ancho, sin deformación; no fue necesario modificar su transparencia.

Archivos en `app/build/outputs/ruta-clara/renderizados-locales/`:

- `login-horizontal.png`
- `login-vertical.png`
- `configuracion-horizontal.png`
- `configuracion-vertical.png`

Reproducción local: `gradlew.bat :app:testDebugUnitTest --tests com.tuempresa.inventariovial.AuthRutaClaraScreenTest --tests com.tuempresa.inventariovial.AuthRutaClaraRenderTest`. Los fixtures son locales de Robolectric, sin aprovisionar ni escribir en la tablet o Drive.

Pendientes de validación física: ambas orientaciones, teclado real, fuentes grandes, reducción de movimiento, retorno de segundo plano, aspecto del logo a tamaño real y login/sincronización contra los servicios autorizados. No se avanza al dashboard antes de revisión del propietario.
