# Continuar con Android Studio, GitHub y editor Apps Script

No es necesario Codex, clasp ni acceso productivo para continuar Android. Esta guía no autoriza instalar aplicaciones, cargar fichas o publicar un servidor.

## A. Recibir y comprobar

1. Recibir solo el material correspondiente a tu rol. Un colaborador normal necesita GitHub y `PUBLICO`; el publicador institucional requiere además el paquete privado por un canal seguro.
2. Verificar el SHA del manifiesto entregado por un canal independiente y ejecutar `verificar-manifiesto.ps1` según CIFRADO_Y_TRASLADO.md. Si hay un faltante/diferencia, conservar originales y pedir una copia correcta.
3. Elegir una carpeta corta fuera de OneDrive/Dropbox, por ejemplo `C:\GBO_KIT`. No guardar material privado en carpetas compartidas ni Git. Mantener intactos TAR, fuentes capturadas y manifiestos.

## B. Abrir Android

1. En GitHub abrir PR #11, comprobar que siga en borrador y ver su rama. No pulsar Merge. En Android Studio, Get from VCS → repositorio y rama de #11; confirmar el SHA mediante la terminal integrada. Si no se dispone de Git, el propietario puede entregar el snapshot del commit exacto o la fuente obtenida del bundle; no usar main ni un ZIP sin procedencia.
2. Leer RECUPERACION_ANDROID.md. En SDK Manager instalar plataforma 37/Build Tools requeridos; en Settings → Build Tools → Gradle seleccionar JBR compatible. Android Studio genera un `local.properties` propio.
3. Para trabajo normal usar el perfil de validación indicado; no pedir credenciales o una tablet. La terminal integrada permite ejecutar el Wrapper, no se necesita instalar Gradle global.
4. Para configuración original, solo el custodio copia los archivos privados y selecciona la firma Debug legítima con el init-script. Verificar signingReport. Nunca sustituir la firma Release ni desinstalar una aplicación para resolver incompatibilidad.
5. Revisar cambios en Git, crear rama `codex/...`, commits pequeños sin datos/artefactos y PR con la base correcta. No fusionar automáticamente ni cambiar versiones por este traspaso. Compilar o probar solo lo relacionado con un cambio o una comprobación de recuperación necesaria.

## C. Preparar Apps Script sin herramientas adicionales

1. Sin guardar nada, identificar el proyecto original mediante el scriptId privado. Comparar deployment/URL/V21 y cuenta. El editor de código no prueba qué versión está activa.
2. El kit privado ya contiene el paquete revisado completo; no hace falta Python/Node para copiar sus archivos manualmente. Si se quiere reconstruirlo o repetir mocks, otro integrante puede usar las herramientas documentadas, sin Google.
3. Revisar PUBLICACION_V22_Y_RECUPERACION.md y el anexo privado. Comparar todos los archivos/properties y los 13 bindings protegidos. No copiar valores secretos en comentarios, PR o chat.
4. Obtener la autorización consolidada exacta antes de guardar, crear V22, cambiar propiedades o editar la implementación. Con cuenta personal preparar/revisar solamente; la identidad GBO debe publicar y acreditarse en runtime. No confiar en el texto «Yo».
5. Después de aprobación, seguir las cinco sustituciones/cuatro adiciones, conservar manifest/lectores, propiedades y DRAIN_ONLY; usar el deployment existente. No hay que crear un proyecto nuevo ni cambiar Android a otra URL.
6. Al primer bloqueo, seguir la tabla de recuperación del documento. No volver a V21 después de una marca/efecto nuevo; conservar estado durable y escalar la reparación compatible.

## D. Cerrar un hito manual

Registrar versión, SHA de código y artefacto, dispositivo, hechos comprobados y lo no ejecutado en `docs/PROGRESO_ACUMULADO_GBO.md`. Copiar evidencias sensibles fuera del repositorio. Revisar `git diff --check`, archivos staged, exclusiones y secretos antes de commit/push. Comprobar `git ls-remote` contra el commit local y PR en borrador. Crear/verificar un bundle privado y actualizar manifiesto antes de otra transferencia.

La autorización de instalación del OPPO no se hereda para otro teléfono. La aceptación offline no habilita OPEN ni distribución. Actualmente ambos registros QUEUED del OPPO deben conservarse offline, y la eliminación histórica sigue deshabilitada.
