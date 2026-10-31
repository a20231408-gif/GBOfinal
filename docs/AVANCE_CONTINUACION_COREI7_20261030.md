# Continuación desde la copia Core i7 — 30/10/2026

La rama de trabajo es `codex/observaciones-campo-asus-20261030`. El historial y los archivos de esta carpeta se contrastaron con el HEAD publicado antes de editar. El bundle recibido es completo, pero termina un commit documental antes de la rama remota.

## Cambio de la app

«Preparar nuevo tramo» abre ahora el diálogo de nueva sesión de campo y exige seleccionar una ruta del proyecto. El tramo asociado se toma del catálogo de rutas existente. Al confirmar, se usa `FieldController.startSession`, que cierra la sesión previa y crea otra en una transacción; el listado restablece sus filtros solo después de que la operación termina correctamente. Las fichas, los originales, el historial y las entregas pendientes no se eliminan. Si se cancela o falla, la sesión y los filtros actuales permanecen.

Este cambio requiere compilación y validación en tablet antes de considerarse acreditado. Gradle no pudo iniciar en esta computadora: la JVM falló al establecer la conexión local del proceso auxiliar (`Unable to establish loopback connection`), incluso en modo sin daemon. No se generó ni instaló una APK nueva.

## Estado operativo comprobado aquí

- La conexión de Drive disponible pertenece a una cuenta personal con acceso a elementos GBO; no expone el proyecto Apps Script original ni sustituye el consentimiento de ejecución pendiente. No se ejecutó `gboPilotInventoryReadonly` ni se obtuvo inventario real de journals.
- ADB está instalado, pero no detecta ninguna tablet conectada. No hubo respaldo, instalación ni lectura de datos de campo.
- El ZIP `GBO.zip` permitió recuperar en esta carpeta, fuera de Git, 172 archivos del respaldo privado anterior y `keystore.properties`. Sus 42 entradas con SHA-256 en el manifiesto privado coinciden. Los archivos locales de configuración de Usuarios y Drive y el keystore ya presentes coincidían byte a byte con los del ZIP. `local.properties` se recreó con el SDK de esta computadora.
- De los 1 430 archivos de fuente/documentación revisados en el ZIP, solo faltaban dos ejemplos ficticios de Excel; se copiaron localmente en una ruta excluida de Git. La fuente actual de la rama se conservó: el ZIP corresponde a una etapa anterior y no contiene la APK 1.4.20 (34), el snapshot final del original ni el inventario operativo completo.
- El navegador de esta sesión no tiene acceso autenticado al editor Apps Script original. El ZIP contiene referencias privadas antiguas, pero no permite acreditar el estado actual del proyecto o ejecutar el auditor allí.
- El despliegue V21, la raíz operativa, el Servicio de Usuarios y los permisos de Drive no se modificaron en esta continuación.

La secuencia pendiente sigue siendo: obtener acceso verificable al proyecto original y al respaldo privado; resolver el consentimiento del auditor y completar su inventario paginado; revisar cualquier journal PENDING o ilegible; preparar y revisar el cambio del mismo despliegue; validar un ACK controlado; respaldar y probar una tablet; solo entonces decidir la distribución.
