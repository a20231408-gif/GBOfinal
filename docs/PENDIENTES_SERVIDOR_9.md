# Servidor — Prompt 9 parcial

Esta corrección de miniaturas **no requiere desplegar Apps Script**. No se modificó el backend de sincronización ni el servicio independiente de usuarios. Sigue vigente el contrato verificado en Prompt 8B: protocolo 3, rutas V4, categorías versión 1, numeración familiar estable y entrega técnica sin GPS.

## Videos: bloqueo de habilitación

Videos no implementados ni habilitados. No se auditó todavía el transporte completo y **no existe un parche de servidor de videos en esta entrega**. El soporte actual de fotos no se considera evidencia de compatibilidad con videos.

Después de cerrar el Bloque A, el Bloque C deberá:

1. Auditar límites documentados del servicio y solicitudes, consumo de memoria y almacenamiento, MIME y tipos de video reales.
2. Diseñar transporte binario adecuado/resumible o una ampliación segura por partes; evitar videos completos en JSON/Base64.
3. Definir persistencia offline, progreso, reintentos, idempotencia UUID, SHA-256, reemplazo e historial, y capacidades explícitas del servidor.
4. Preparar parche compatible en el repositorio, pruebas y actualización manual coordinada del proyecto `GBO_Inventario` existente. No mezclarlo con el servicio de usuarios.
5. Mantener la entrega real de videos bloqueada hasta que el usuario despliegue y pruebe el servidor compatible en un entorno autorizado.

Prefijos numéricos de multimedia y migraciones de carpetas: pendientes. No renombrar ni duplicar históricos, no cambiar fileId/bindings y no reorganizar Drive por un nuevo orden KMZ. Cualquier migración futura requiere diseño y autorización separados.
