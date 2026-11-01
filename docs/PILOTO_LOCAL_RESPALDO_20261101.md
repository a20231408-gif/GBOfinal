# Tablet personal — preparación, no ejecución (01/11/2026)

**P01–P17 continúan SIN EJECUTAR.** Este procedimiento no autoriza extraer datos, instalar,
sincronizar, limpiar ni restaurar. El propietario navega manualmente; ADB/Logcat solo para
respaldo autorizado o un defecto reproducible. No pedir tablets de otros ingenieros.

## Respaldo previo: autorización independiente de la instalación

1. Confirmar consentimiento para copiar datos reales exclusivamente a un directorio privado
   del propietario y para detener la app mientras se obtiene una copia coherente. Modo avión,
   cerrar edición y esperar guardados locales. No pulsar preparar/sincronizar/limpiar.
2. Comprobar el dispositivo elegido, package original, versión y certificado. Última evidencia:
   tablet personal con Debug 1.4.20 (34), certificado Debug Huawei/ASUS; reconfirmar cuando
   se autorice conectar. Release tiene otro certificado y no actualiza ese Debug.
3. Si la variante sigue siendo debuggable, un operador autorizado puede detener el paquete
   sin borrar datos (`am force-stop`) y copiar íntegramente el directorio accesible mediante
   `run-as com.tuempresa.inventariovial`, incluyendo databases, WAL/SHM, files, shared_prefs y
   no_backup. Usar `adb exec-out ... tar -cf - .` y capturar stdout **binario** mediante un
   proceso que escriba bytes (por ejemplo subprocess de Python). No redirigir el TAR binario
   con PowerShell clásico. No usar adb backup como garantía de respaldo en Android moderno.
4. Si run-as no está permitido, detener el procedimiento y acordar otro respaldo legítimo;
   no rootear, desinstalar ni sustituir el paquete. Si hay originales fuera de filesDir,
   inventariarlos y copiarlos también con autorización; las capturas/importaciones actuales
   usan files/images, pero las rutas históricas pueden ser distintas.
5. En la copia privada, comprobar TAR legible sin errores, manifiesto SHA-256, presencia de
   Room y WAL correspondientes, `PRAGMA integrity_check` sobre una copia offline y cobertura
   de las fotografías/documentos referenciados. Inventariar UUID/revisiones/ACK privadamente;
   no exportar valores al chat. Un archivo ausente es bloqueo, no un respaldo aprobado.
6. Acordar restauración viable en el mismo dispositivo/paquete/firma, sin desinstalar.
   Algunas preferencias dependen del Android Keystore no exportable: el TAR no garantiza
   restauración universal en otra tablet. No ejecutar una restauración sobre datos reales
   para «probar» el respaldo. Si no hay plan recuperable, no autorizar actualización.
7. Conservar fuente, APK instalada y APK candidata separadas, con hashes y versión; los bundles
   de Git y los ZIP de limpieza de copias NO sustituyen este respaldo. Transferir datos y
   firmas solo cifrados y a responsables autorizados; no GitHub ni logs generales.

## Actualización, solo después de aprobación expresa

Con respaldo comprobado y SHA/certificado de la candidata final contrastados, mantener modo
avión y actualizar Debug sobre Debug con el mismo package usando `adb install -r -t <APK>`.
Verificar resultado y versión efectiva. Si falla firma o pide desinstalar: **DETENER**;
no ejecutar uninstall, clear ni instalación Release sobre Debug. No instalar por leer este texto.

La candidata futura de esta intervención es 1.4.22 (36); usar solo el artefacto final y hash
registrados en el informe de avance, nunca una APK intermedia ni el backend de ensayo archivado.

## Ficha breve que completa el propietario

Registrar versión efectiva, hora, esperado/observado y PASS/FAIL/BLOCKED por caso. Primero:

- P01: conservación de los 15 registros reportados y sus fotos; contrastar conteos/error, no
  interpretar «0 pendientes» como confirmación remota de todas las fichas.
- P02–P04: conflicto SCAP con nueve preparados sin reintento/ACK inventado; SIC-17 vigente,
  A/B opcionales según flags; archivo actual e historial de copias accesibles sin borrado.
- P05–P10: fechas/estados/orden; borrador ficticio local; 42+995 a 43+005 = 10 m al reabrir;
  descarte/restauración y selección/cancelar. Nunca confirmar borrado masivo de datos reales.
- P11–P13: autoguardado Pavimentos y daño 26 L/M/H; bloquear cierre ficticio si programa Drive.
- P14–P17: fotos/croquis ficticios, nombres/ubicación y apertura; inspeccionar previsualización
  de limpieza sin ejecutarla. No exige conceder ubicación ni inventar GPS.

La lista exacta P01–P17 está en `docs/ESTADO_PARA_CONTINUAR.md` y en el manual privado.
«Nuevo tramo»: comprobar diálogo de ruta y cancelar primero; iniciar sesión nueva solo cuando
se acuerde la prueba. El flujo existente cierra la sesión anterior y puede marcar sus registros
para el servicio de Usuarios; no es una operación para borrar/archivar automáticamente fichas.
Sin modo avión no probar ficticios contra producción. No se acredita Drive en una prueba offline.
