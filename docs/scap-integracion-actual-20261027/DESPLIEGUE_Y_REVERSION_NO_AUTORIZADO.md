# Propuesta posterior al ensayo — no ejecutar todavía

**No hay autorización para modificar o desplegar producción.** Requisitos previos: PASS real Google del procedimiento, revisar evidencia y confirmar que el export actual del editor aún coincide con los SHA de entrada. Si cambió un módulo, comparar de nuevo ese módulo antes de sustituirlo. No sustituir funciones no revisadas.

## Respaldo privado del propietario

Guardar los ocho módulos y manifiesto actuales, número de versión desplegada y fecha. Conservar privadamente configuración y permisos existentes mediante el procedimiento administrativo del propietario, sin copiarlos al ZIP ni enviarlos al asistente. Registrar la versión del despliegue existente que conserva `/exec`. Este trabajo respaldó las fuentes entregadas, pero **no** obtuvo ScriptProperties ni certifica una copia completa de configuración remota.

## Archivos que se aplicarían, solo tras autorización

1. Crear `ScapPhotoPanelV2.gs` desde PARCHE_PRODUCCION_NO_PUBLICAR.
2. Actualizar el módulo existente **PhotoNameV2Core.gs**, singular. No añadir una segunda versión plural.
3. Actualizar **SyncV3Core.gs** únicamente si conserva el hash de entrada revisado.
4. Actualizar **VideoNamesV2Core.gs** con la misma condición.
5. En el doGet existente de **Código.gs**, insertar una vez la propiedad incluida en Código_CAPACIDAD_FRAGMENTO.txt. No pegar el Código completo de ensayo ni reemplazar la raíz/configuración.
6. Conservar intactos SyncV3.gs, VideoV1.gs, VideoV1Core.gs, SicObservationsV1Core.gs, manifiesto productivo, propiedades, permisos, raíz, URL y Servicio de Usuarios. No copiar TestIsolation/TestHttp/TestVariant/ScapEnsayo al servidor original. No integrar los parches generales de velocidad.

Después de revisar errores de sintaxis y dependencias, crear una versión del mismo proyecto y editar **el despliegue existente**, seleccionando esa versión. No crear otro despliegue ni publicar una URL nueva. Verificar por lectura que su GET conserva el resto de capacidades y anuncia panel 2. Esta acción sigue pendiente de autorización expresa.

La APK actual con panel 20 puede reconocer la capacidad sin recompilación. La comprobación se realiza cuando arranca el procesamiento y consulta de nuevo capacidades; respetar condiciones y periodicidad de la cola. No prometer que cualquier botón de sincronización reencole inmediatamente un PENDING_VALIDATION. No crear otra ficha ni alterar UUID, revisiones o archivos para forzar el envío. La prueba sobre una ficha real requiere autorización separada y ACK verificable.

## Reversión

Si el control posterior falla, detener el ensayo de campo, conservar archivos/ACK/journals y devolver el mismo despliegue a la versión anterior registrada, conservando `/exec`. Si además se quiere restaurar el código del editor, reponer únicamente los tres módulos cambiados, retirar el módulo nuevo y revertir la propiedad añadida usando el respaldo. No borrar ni renombrar datos Drive. No restaurar indiscriminadamente ScriptProperties ni contadores.

El backend anterior no admite categorías nuevas: al volver a la versión previa, el GET deja de anunciar panel 2 y las entregas nuevas deben esperar compatibilidad. No borrar las evidencias ni convertir categorías para que sean aceptadas. Una revisión parcialmente iniciada requiere revisar su journal antes de otro intento; no hacer limpieza automática. La reversión de código no revierte datos ya confirmados y no debe intentar hacerlo.
