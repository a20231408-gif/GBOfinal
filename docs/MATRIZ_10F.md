# Matriz breve 10F

| Punto | Estado / bloqueo |
|---|---|
| Cliente SCAP ordinario y reanudación | **20/20 focalizadas aprobadas** (17 Android + 3 Node); dos regresiones corregidas; Debug normal compilada |
| Servidor real de videos | **Bloqueado para entrega remota**: GET videoProtocol=0, scope SCAP_ONLY; no se cambió producción |
| Activación cliente | Bandera remota false; habilitar en archivo privado y Run normal únicamente tras capacidad 1 validada |
| Captura, reproducción y recuperación en tablet | **Pendiente**: prueba de Puente ficticio; no hay tablet ADB conectada |
| Firma/identidad y datos | 4 casos locales de identidad/Room aprobados; applicationId/16/1.4.2 y certificado Debug igual a 10E; firma instalada pendiente sin tablet conectada |
| Control de acceso | **Pendiente para distribución protegida**: false; proveedor local de demostración y proveedor servidor sin integración validada |
| Versión/firma de futura distribución | Propuesta 17/1.4.3 pendiente de autorización e inventario de versiones; Debug y Release histórico tienen firmas distintas |
| Secretos y Release | Configuración privada fuera de Git; APK Release preexistente inspeccionada; validación de su clave privada pendiente para futura Release; app/release intacto |

Activación, reversión, límites y cinco pasos de prueba física en `ENTREGA_10F.md`. Las seis pruebas físicas aprobadas y la batería general 8B–10E no se repiten.
