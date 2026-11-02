# Segundo respaldo privado posterior a pruebas — preparado, NO ejecutado

El primer respaldo verificado permanece intacto. La tablet tiene cambios posteriores;
no restaurar aquella base encima del estado actual. Este documento no autoriza extracción
ni instalación. Solicitar permiso independiente para el segundo respaldo y, después de
verificarlo, otra autorización para instalar la candidata Debug.

## Propuesta de alcance para autorizar

Tablet Xiaomi personal, package `com.tuempresa.inventariovial`. Nuevo directorio privado
fuera de Git y carpetas sincronizadas:
`%USERPROFILE%\GBO_TABLET_RESPALDO_PRIVADO_20261102_POST_PRUEBAS_<UTC>`.
Crear carpeta inédita, ACL solo propietario y SYSTEM; no reutilizar ni sobrescribir el
primer respaldo. ACL local no sustituye cifrado si más adelante se traslada a otro equipo.

Tras autorización:

1. Propietario guarda toda edición, cierra GBO y confirma ausencia de operaciones locales.
   Mantener modo avión y Wi-Fi apagado; verificarlo con ADB de solo lectura. Si no coincide,
   pedir corregirlo manualmente. No abrir sincronización ni limpiar registros.
2. Identificar el único dispositivo autorizado por USB; comprobar package, versión real,
   debuggable/TEST_ONLY y certificado de su APK instalada. Guardar identificadores solo en
   evidencia privada. No asumir que la versión instalada sigue siendo la del primer respaldo.
3. Detener temporalmente GBO con `am force-stop` únicamente dentro de esta autorización;
   comprobar ausencia de proceso. Verificar `run-as com.tuempresa.inventariovial`.
   Si falla run-as, detenerse: no root, desinstalación, pm clear ni sustitución del paquete.
4. Inventariar tamaños/SHA-256 de archivos accesibles antes y después de la copia. Capturar
   íntegro el directorio privado, incluidos files, databases, WAL/SHM, shared_prefs y no_backup.
   Usar `adb exec-out run-as com.tuempresa.inventariovial tar -cf - .`, con stdout conectado
   directamente a un archivo abierto en modo **wb** mediante subprocess de Python. Separar
   stderr y comprobar código de salida; **no redirigir bytes TAR mediante PowerShell**.
5. Conservar TAR canónico inmutable. Comprobar estructura, tamaño/hash y hashes de cada
   archivo frente al inventario de origen. Extraer solo offline a ruta segura; en Windows
   usar soporte de rutas largas si hace falta. No interpretar una extracción parcial como PASS.
6. Detectar todas las bases SQLite, no solo la principal. Copiar base y WAL/SHM a un área
   offline separada y ejecutar `PRAGMA integrity_check` y `foreign_key_check` allí. No hacer
   checkpoint, reparaciones ni escrituras sobre la base de la tablet o el TAR canónico.
7. Leer referencias desde la copia offline e inventariar originales/documentos externos.
   Comprobar su cobertura real; una fuente interna con mismo SHA no acredita que la copia
   pública o su URI siga accesible. No dar por conservados los conteos del primer respaldo.
   No cambiar permisos SAF ni originales para intentar completar una copia inaccesible.
8. Crear manifiestos SHA-256 y resumen privado sin publicar fotos, tokens, UUID ni datos
   personales. Registrar faltantes y limitaciones de cada proveedor. Si hay contenido
   necesario sin cobertura, declarar RESPALDO INSUFICIENTE y detener la actualización.

Android Keystore no exporta sus claves por run-as. El TAR no garantiza restauración de
credenciales cifradas, grants SAF, catálogo MediaStore o recuperación universal en otro
dispositivo/UID. No probar restauración sobre datos reales. Dejar GBO detenido y detener
el procedimiento después del informe; instalación y reapertura requieren aprobación aparte.

## Instalación futura: condición, no ejecución autorizada

Solo después de respaldo suficiente y autorización explícita: actualización **Debug sobre
Debug** con package y certificado coincidentes, sin desinstalar ni borrar datos. Revalidar
SHA de la candidata privada registrada y versión 1.4.23 (37). Release histórica tiene otro
certificado y no sustituye a Debug. Ante incompatibilidad de firma, detenerse.

Repetir offline P01–P10 y P17 afectados; P16 solo inspección salvo autorización de limpieza;
completar P11–P15. No programar entregas ficticias, forzar conflictos ni asumir autorización
para Drive, Apps Script, cambios de ROOT o distribución general.
