# Candidata Release 1.4.3 / 17

## Identidad y actualización

- applicationId histórico: `com.tuempresa.inventariovial`.
- El propietario confirmó que el mayor versionCode Release distribuido es 16 / 1.4.2. La APK histórica local coincide con esa identidad/versión y firma.
- Próxima versión autorizada: 17 / 1.4.3, aplicada al único módulo app.
- `:app:validateReleaseConfiguration` **PASÓ antes de compilar la candidata**: clave privada local disponible, contraseñas/alias válidos, certificado histórico coincidente, configuración ordinaria de servicios presente. No se regenera ninguna clave ni se recurre a firma Debug.
- SHA-256 del certificado Release: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
- La Debug instalada tiene otra firma. **Release no puede actualizar esa Debug**. La candidata se probará en una instalación Release histórica compatible; no se desinstala, borra ni fuerza ninguna instalación. La actualización con el mismo certificado es condición necesaria; la conservación real tras instalar requiere la prueba física indicada abajo.

## Datos y configuración

Room principal sigue en versión 19, con migraciones explícitas 1→19 y sin fallback destructivo. VideoDatabase sigue en versión 1; no se modifica esquema ni JSON de Room. UUID, asociaciones, originales, colas y preferencias de tablet/usuarios no cambian. El nombre remoto de video es una etiqueta guardada en nuevas preferencias privadas, independiente de su UUID.

La identidad de tablet se deriva de ANDROID_ID con el esquema ya existente. Actualizar bajo el mismo usuario Android y certificado histórico conserva ese ámbito de identidad. No transferir datos de la Debug a la Release por borrado/reinstalación: difieren la firma, el ámbito de ANDROID_ID y las claves AndroidKeystore.

Los archivos locales privados ordinarios conservan fotografías V2=true, videos remotos=true y ACCESS_CONTROL_ENABLED=false. Se verificó por GET el servicio Inventario real: V3/V4, fotografías V2 y videos SCAP V1; aún falta publicar manualmente nombres de video V2. La raíz publicada difiere de la referencia de Code.gs: actualizar solo su campo GET, conservando configuración de producción.

El guard de Release verifica el SERVER_BASE_URL auxiliar, configuración Drive y firma. **Ese guard no valida la provisión del servicio de usuarios en cada tablet.** El login utiliza la URL /exec y token de tablet provisionados en la pantalla existente y cifrados con AndroidKeystore. Mantenerlos en tablets actualizadas; en una tablet nueva, provisionar el mismo servicio autorizado y sincronizar antes del primer login. No guardar el PIN ni el token en Git ni compartirlos por chat.

## Login existente: decisión de distribución

Se conserva la pantalla de usuario/PIN y se considera aprobada para esta primera entrega controlada, apoyada en la prueba funcional del propietario y en el flujo real revisado. No se crea otro login ni se integra un proveedor de demostración en sustitución.

1. **Verificación del PIN:** AuthRepository compara PBKDF2-HMAC-SHA256, salt individual y al menos 210000 iteraciones, contra verificadores recibidos del servicio y cifrados en Room. Solo usuarios activos, caché no revocada y de hasta 72 horas pueden iniciar sesión. No hay ADMIN de emergencia generado localmente. Cinco fallos bloquean cinco minutos y persisten tras recrear el repositorio. El PIN se limpia al finalizar.
2. **Consulta real:** AuthGraph crea HttpsUserService; UserSyncRepository llama a `syncUsers` sobre el /exec privado provisionado con tabletToken y huella persistente. No realiza un POST de PIN en cada login: la comparación local permite el offline aprobado. Administración de usuarios revalida PIN en `adminAuthenticate` online; tablets usa su contrato/token específico. HTTPS solo acepta script.google.com /exec y recoge la respuesta redirigida de Google por GET, sin reenviar el POST secreto.
3. **Bandera false:** omite únicamente el gate adicional de DeviceAccessManager (lista de demostración/proveedor servidor stub). MainActivity mantiene AuthShell delante del inventario, SIC, SCAP y Pavimentos. Null exige LoginScreen; sesión bloqueada exige nuevamente PIN. La bandera no crea una sesión, no deshabilita UserSyncRepository ni elimina tabletToken del servicio de usuarios.
4. **Límite de la revisión:** no se obtuvo ni cambió el código publicado de `GBO - Servicio de Usuarios`, ni se descifraron credenciales de la tablet para identificar su proyecto por nombre. Se verificó la implementación cliente que consulta el /exec provisionado, junto con el login real confirmado por el propietario. No se certifica desde aquí toda la política interna del servidor.
5. **Riesgos concretos para el grupo inicial:** no se detectó una ruta de pantalla que elimine el login por ACCESS_CONTROL_ENABLED=false. El retraso de revocación en trabajo offline y la caché de 72 horas son parte del diseño existente; mantener provisión y sincronización de usuarios. La APK contiene configuración de conexión del Inventario y debe entregarse al grupo controlado previsto, fuera de Git. No declarar validada autorización avanzada de anulación ni autorización adicional de dispositivo: quedan fuera de esta versión y no justifican reemplazar el login.

Fuentes revisadas: MainActivity.kt, auth/AuthGraph.kt, LoginScreen.kt, AuthRepository.kt, AuthSessionManager.kt, UserSyncRepository.kt, UserServiceClient.kt, PasswordHasher.kt, CredentialProtector.kt, AdminUsersRepository.kt, TabletAdminRepository.kt y access/DeviceAccessManager.kt. No se reejecutó la batería de usuarios 8B–10F ni se activaron banderas.

## Compilación e inspección

**PASÓ** la única ejecución de `:app:assembleDebug :app:assembleRelease`, con archivos locales reales, sin gboValidationBuild ni overrides de fotografías. La candidata fue firmada con la clave histórica; verifyReleaseIdentity pasó y verificó alineación ZIP. La inspección independiente confirmó versión 17/1.4.3, ausencia de DEBUGGABLE/TEST_ONLY y componentes de pruebas, configuración ordinaria y presencia de todos los módulos. Resultado, hashes y rutas en CIERRE_GBO_10H.md y campo-10h/evidencia.

El buildType release ya usa isDebuggable=false, minificación y reducción de recursos. Compose tooling y el manifiesto de pruebas son debugImplementation; no se agregan al conjunto Release. La inspección de la APK confirmó no DEBUGGABLE ni TEST_ONLY, ausencia de TestActivity y ComposeViewAdapter de Debug, backup=false y tráfico claro deshabilitado. FileProvider/cámara/servicio siguen privados en el manifiesto de la aplicación.

## Matriz breve de distribución

| Condición | Estado / acción |
| --- | --- |
| Login usuario/PIN existente | Conservado y aprobado para primera entrega controlada; no activar el gate adicional |
| Clave/certificado histórico | Verificado; guard pasó |
| Versión de actualización | 17 / 1.4.3, autorizada; 16 confirmado por propietario |
| APK Release candidata | Compilada y firma histórica verificada; fuera de Git y no instalada |
| Nombres de videos V2 en producción | Bloqueado externamente hasta actualización manual de los tres módulos y campo GET |
| Instalar Release sobre la tablet Debug actual | Incompatible por firma; no instalar ni desinstalar |
| Actualización física de una Release anterior con datos pendientes | Pendiente en tablet compatible, sin borrar datos |
| Seguridad avanzada / anulación remota | Pospuesta; no forma parte de la aprobación del login actual |

## Prueba física concreta

1. En una tablet que ya tenga Release histórica, anotar versión, código de tablet, fichas y pendientes. Confirmar respaldo operativo según el procedimiento del propietario, sin borrar datos.
2. Comparar la firma de su APK con el certificado histórico anterior. Si coincide, instalar la candidata como actualización normal; si Android rechaza la firma, detenerse sin desinstalar.
3. Iniciar sesión con el usuario/PIN existentes. Comparar identidad de tablet, fichas, UUID visibles en detalles y pendientes; comprobar originales reproducibles. No sustituir la provisión de usuarios.
4. Tras la actualización manual de nombres V2, hacer solo la prueba de videos del puente ficticio autorizada descrita en NOMBRES_VIDEOS_10H.md: dos del mismo apartado, uno de otro, pérdida de conexión/ACK, nombres estables y ausencia de duplicados. No repetir el formulario completo ni modificar fichas de ingenieros.
5. Si se desea comprobar el código nuevo en la Debug actual, preparar Debug ordinaria con su firma histórica Debug y realizar su actualización solo cuando el propietario lo decida. Esta ejecución no instala ninguna APK en la tablet.
