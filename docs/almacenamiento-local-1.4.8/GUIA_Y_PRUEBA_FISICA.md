# Copias accesibles — guía 1.4.8 (22)

## Para los ingenieros

1. En **Herramientas y ajustes → Copias locales**, activa **Guardar copias locales automáticamente** y confirma el aviso. Consume espacio y hace accesibles los documentos, fotos y videos copiados a otras aplicaciones/usuarios de la tablet. Empieza desactivado; no exporta fichas antiguas por sí solo.
2. Antes del primer cierre de una ficha, revisa **Destino de esta ficha**. **Drive y copia local (si está activada)** conserva el comportamiento habitual de Drive. **Solo dispositivo** prepara la copia local al finalizar, incluso con el ajuste general apagado; no programa Drive. Confirma la elección antes de finalizar.
3. Completa y finaliza normalmente. **Ficha guardada dentro de GBO** significa persistencia privada. Espera **Copia local completada** para considerar escritos y verificados todos los archivos previstos. Una copia parcial enumera faltantes; un Excel contractual incompleto no se presenta como entrega completa. El ACK de Drive se informa por separado.
4. En Android 10 o posterior, abre **Archivos → Almacenamiento interno → Download/Descargas → GBO_Ruta_Clara**. Encontrarás SIC, SCAP y PAVIMENTOS, separados por carretera, identidad y revisión. Las fotografías sueltas están en FOTOGRAFIAS/INDIVIDUALES. No se crea adicionalmente una copia en Galería.
5. En el panel de entrega de la ficha y en exportaciones puedes usar **Guardar en Descargas**, **Exportar fotografías** o **Guardar paquete ZIP**. Cada archivo verificado ofrece **Abrir archivo**, **Compartir**, **Ver ubicación** y **Guardar como…**. PDF/XLSX necesitan un lector compatible. Ver ubicación puede abrir Descargas general; no garantiza navegar a la subcarpeta exacta.
6. Desde la vista de una fotografía usa guardar copia o compartir; el original privado conserva sus bytes. Los videos SCAP también tienen copia individual. Un PDF/Excel/ZIP es un documento independiente: no restaura Room ni sustituye un respaldo integral de GBO.
7. Para fichas anteriores: **Exportar registros existentes al dispositivo**, revisa el recuento y confirma. Se programan hasta veinte revisiones por lote y se procesan tres fichas por tanda. Repite explícitamente para el siguiente lote. Los faltantes no se inventan ni cambian el estado Drive.

**Android 8/9:** primero elige una carpeta del almacenamiento de la tablet en Copias locales y concede acceso. GBO creará allí GBO_Ruta_Clara; no se afirma que sea Descargas si elegiste otro destino. No selecciones un proveedor en Internet.

**Si algo falla:** conserva la ficha; comprueba espacio, permiso y lector. Pulsa **Reintentar copia local**. Si moviste/eliminaste un archivo y Abrir detecta su ausencia, usa **Recuperar copia en Descargas**. No borres datos de GBO para resolverlo. Apagar el ajuste evita nuevas solicitudes automáticas, pero no cancela las ya encoladas. El envío posterior de Solo dispositivo exige la confirmación de un ADMIN del inicio de sesión existente.

## Antes de instalar la candidata

No se ha instalado automáticamente. Validar primero en una tablet de prueba autorizada y con respaldo recuperable. La firma histórica permite actualizar una instalación Release compatible; no permite actualizar una Debug de firma diferente. No desinstalar ni borrar datos para forzar la actualización.

La actualización migra Room 19→20 añadiendo tablas. **Volver a instalar la APK 1.4.7 no es una reversión soportada de la base migrada.** Ante un problema, conservar datos y aplicar una corrección hacia adelante o una restauración integral expresamente autorizada; nunca limpiar almacenamiento ni forzar downgrade. Conservar APK y respaldo anteriores.

## Prueba física pendiente — solo datos ficticios

Las pruebas JVM/Robolectric están documentadas aparte. Esta lista todavía NO está ejecutada en una tablet. Registrar modelo, Android, administrador de archivos, lector y resultado de cada paso. Desconectar Internet; elegir Solo dispositivo para evitar escrituras productivas.

| Paso | Procedimiento | Resultado verificable |
|---|---|---|
| 1. Actualización | En entorno de prueba autorizado, anotar UUID, cantidades, fotos, pendientes y configuración antes/después de actualizar una Release 1.4.7 con firma igual. | Mismas fichas, medios, configuración, identidad y colas; sin borrado. |
| 2. SIC offline | Crear TEST_LOCAL_148_SIC, foto ficticia, Solo dispositivo; finalizar sin conexión. | Copia verificada en Descargas, XLSX y fotos; UUID igual, sin nueva entrega Drive. |
| 3. SCAP offline | Cerrar un puente ficticio validado, Solo dispositivo, con foto, croquis y video corto. | XLSX/medios accesibles; faltantes contractuales identificados si existen; sesión de video intacta. |
| 4. Pavimentos offline | Muestra ficticia rígida con referencias 22/26 y fotografía; cerrar Solo dispositivo. | PDF nativo legible, Excel/JSON/fotos; esquinas/juntas visibles, cantidades/PCI coherentes. |
| 5. Abrir/compartir | Abrir PDF y XLSX con lectores Android; compartir un documento y foto a un destino local autorizado. | Abren desde URI real, permisos temporales; sin depender de Drive. |
| 6. Persistencia | Cerrar GBO y reabrir el documento; reiniciar tablet y repetir. | Mismos archivos/URI o indicación clara si el proveedor revocó acceso; sin copias nuevas accidentales. |
| 7. Revisión | Editar la ficha después de completada su copia y cerrar. Reintentar la misma revisión. | Nueva carpeta de revisión; anterior intacta; reintento sin duplicado. Si editaste antes de preparar la anterior, se informa revisión cambiada y debes exportar la actual. |
| 8. Ajuste | Apagar copias; cerrar nueva ficha Drive sin reconectar. Activar y cerrar otra. | No se crea solicitud local con ajuste apagado para Drive; sí con ajuste encendido. Solo dispositivo siempre solicita copia. No reconectar estas fichas Drive a producción sin autorización. |
| 9. Recuperación | En dispositivo ficticio con almacenamiento de prueba, interrumpir una copia, abrir GBO/reintentar; revocar permiso SAF o retirar solo un archivo de prueba. | Originales intactos; error/reintento claro; no READY prematuro ni duplicación. No llenar ni alterar almacenamiento de campo para ensayar falta de espacio. |
| 10. Compatibilidad | Repetir apertura y ubicación en otra versión Android; en 8/9 autorizar carpeta local SAF. | Ruta real comprensible y acceso después del reinicio. |
| 11. Drive | Solo con autorización adicional y entorno de prueba adecuado, cerrar Drive y copia local y comprobar ACK; autorizar después una ficha Solo dispositivo mediante ADMIN. | Un UUID/revisión, una entrega, ACK correcto y copia local independiente. No escribir en producción para completar esta lista sin autorización. |

La aceptación física requiere encontrar y abrir los archivos en el administrador real. Hasta entonces es una candidata validada localmente, no una distribución general aprobada.
