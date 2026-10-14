# GBO Inventario Vial

Guía de campo visual · Edición 10J · 14 de octubre de 2026

Para registrar, revisar y entregar inventario desde una tablet Android. Siga las acciones en orden y compruebe el resultado antes de continuar. Las capturas reales pendientes tienen una posición identificada; no se representan mediante interfaces inventadas.

## 1 Empezar la jornada

1. Cargue la tablet y compruebe espacio libre, fecha y hora.
2. Abra GBO e introduzca personalmente su usuario y PIN. No comparta el PIN ni lo incluya en capturas.
3. En Inicio, compruebe operador, proyecto y ruta. Entre al módulo que corresponda: SIC, Pavimentos o Puentes.

[CAPTURA:01-inicio.png|Inicio de GBO con los accesos reales a los módulos]

**Antes de salir sin Internet:** sincronice usuarios, confirme la ruta y compruebe que puede abrir sus evidencias. No cambie credenciales ni identidad de tablet.

## 2 Registrar un elemento SIC

1. Entre en Nuevo registro y elija el tipo correcto; para una alcantarilla, use el formulario SIC-18.
2. Complete ruta, calzada, PR, distancia, ubicación y datos contractuales. Respete unidades y opciones del catálogo.
3. Si falta una medición, guarde un borrador. No invente un valor para superar la validación.

[CAPTURA:02-sic-formulario.png|Cabecera y campos del SIC 18 con datos ficticios]

4. Incorpore las fotos del elemento y escriba Observaciones al final del formulario.
5. Guarde, revise los avisos y finalice cuando los datos estén completos. Conserve el UUID del mismo elemento al editarlo después.

[CAPTURA:03-sic-observaciones.png|Observaciones y acciones de revisión en el formulario real]

**Resultado esperado:** la ficha aparece en Mis registros. En la candidata corregida, el Excel local añade OBSERVACIONES al extremo derecho y copia solo su texto, por ejemplo PINTADO OK 57 E2. La consolidación en Drive requiere el parche de backend aprobado; no dé esa actualización por publicada.

## 3 Incorporar y revisar fotografías

1. Abra Fotografías de la ficha correcta y pulse Tomar foto o Incorporar fotos.
2. Confirme la toma/selección y compruebe la miniatura y categoría. Revise orientación, nitidez y pertenencia al elemento.
3. Antes de finalizar, abra la foto para confirmar que la evidencia local está disponible.

[CAPTURA:04-fotografias.png|Botones y vista previa de fotografías en una ficha ficticia]

**Si corrige evidencia:** use la acción existente de reemplazo/retiro; no mueva fotos entre carpetas para cambiar su asociación. Cambiar una extensión a .jpg no convierte los bytes de la imagen. Conserve originales ante NOT_JPEG.

## 4 Trabajar en Pavimentos

1. Entre en Pavimentos y elija Asfáltico, Rígido o No pavimentado.
2. Abra o cree la muestra correspondiente. Revise ruta, tramo, calzada, carril, número y dimensiones antes de añadir daños.
3. Para Rígido, confirme filas y columnas. No cambie la grilla después de registrar daños.

[CAPTURA:05-pavimentos-cabecera.png|Cabecera de muestra con superficie y dimensiones]

### Ubicar una falla rígida

1. Elija tipo y severidad. Mantenga activa Localizar daño dentro de una losa.
2. Toque una posición para una falla puntual o dos extremos dentro de la losa para una línea/zona parcial.
3. En el editor confirme X1/Y1, X2/Y2 cuando corresponda y Losas afectadas. Guarde y compruebe la posición en el esquema.

[CAPTURA:06-rigido-esquema.png|Esquema real con dos daños independientes dentro de una losa ficticia]

**Medición:** el croquis localiza; no calcula automáticamente el área contractual. La cantidad rígida sigue en losas según el catálogo. Dos marcas no son necesariamente dos losas para PCI. El origen está abajo a la izquierda; X aumenta a la derecha e Y hacia arriba, en metros de la muestra completa. Los registros antiguos sin coordenadas conservan su símbolo por losa.

### Registrar ahuellamiento

1. En Asfáltico, seleccione Ahuellamiento.
2. Registre área afectada en m², profundidad media en mm y severidad según el catálogo del proyecto.
3. La versión corregida no solicita Ancho de fisura para este daño. Si aún aparece, anote la versión instalada; no traslade una medida de grieta a la profundidad de huella.

[CAPTURA:07-ahuellamiento.png|Editor corregido de ahuellamiento en una APK que incluya 10I]

### Fotografiar el daño y cerrar

1. Pulse la cámara o Ver fotos en la fila del daño correcto. Tome o incorpore una imagen y compruébela.
2. Para sustituirla, use Reemplazar; para retirarla, use Retirar y confirme. El original/historial se conserva.
3. Guarde, salga y reabra; revise también después de girar la tablet. Pulse Revisar y finalizar y cierre la muestra cuando esté lista.

[CAPTURA:08-foto-dano.png|Panel de fotos del daño con acciones reales de reemplazo y retiro]

**Importante:** Pavimentos no ofrece captura de video. Solo el inspector creador puede editar su muestra; una muestra cerrada requiere Reabrir para corregir cuando la acción esté disponible y volver a cerrar para entregar la revisión nueva.

## 5 Inspeccionar Puentes SCAP

1. Abra Puentes y seleccione la inspección correcta. Compruebe puente y tipo de plantilla LOSA/MODULAR.
2. Complete las secciones existentes y revise avisos. Guarde el avance; no cree otra inspección para corregir la misma.
3. Confirme si un elemento existe o aplica antes de evaluarlo. Finalice después de revisar el resumen y las evidencias.

[CAPTURA:09-puentes.png|Navegación real de una inspección ficticia ya existente]

### Fotos y videos por categoría

1. Abra la categoría real del puente: accesos, rodadura, aguas arriba/abajo u otro apartado que corresponda.
2. Use FOTO para imagen y VIDEO para grabar/importar video de Puentes SCAP. Reproduzca el video localmente para comprobarlo.
3. Conserve la misma evidencia al reintentar. El video espera que su ficha padre esté finalizada y confirmada; tiene su propio ACK.

[CAPTURA:10-video-scap.png|Tarjeta y estado real de un video de prueba por categoría]

**Límites:** MP4, WebM o 3GP, hasta 32 MiB por archivo. Los videos remotos dependen de videoProtocol=1 y SCAP_ONLY. Los nombres legibles V2 requieren su capacidad específica; el protocolo de video V1 no basta. No vuelva a importar el archivo para solucionar una subida pendiente.

## 6 Comprobar Drive y exportaciones

1. Tras finalizar, abra Detalles de entrega Drive de la misma ficha. Compare revisión local y revisión confirmada.
2. Conserve el registro cuando esté Pendiente, Subiendo o Reintentando. Use el reintento existente cuando corresponda; no duplique la ficha.
3. Dé la entrega por terminada cuando la revisión actual esté confirmada. La presencia de un archivo o un stage no equivale al ACK final de todos los documentos.

[CAPTURA:11-drive.png|Estado y revisiones reales de entrega de una ficha ficticia]

### Exportar Pavimentos sin confundir las copias

1. Al cerrar, la entrega automática prepara PDF de muestra, Excel de esa muestra, JSON y fotos; no exige pulsar Exportar.
2. Reporte PDF guarda una copia donde usted elige. Excel de superficie guarda un libro local con las muestras de esa superficie presentes en la tablet, incluso de otras rutas/tramos o borradores.
3. Ese libro de varias muestras **no se sube automáticamente como consolidado**. Compartirlo es una acción manual distinta. Subir muestra a Drive reintenta la entrega por muestra.

[CAPTURA:12-exportaciones.png|Reporte PDF Excel de superficie y Subir muestra a Drive en el panel real]

**SIC de Pavimentos:** complete Datos para SIC por daño. Solo los formatos completos se envían como filas a EXCEL_SIC: SIC-14/14A en Asfáltico, SIC-15 en Rígido y SIC-16 en No pavimentado. Una muestra confirmada no demuestra que un formato omitido por incompleto esté consolidado.

## 7 Resolver errores frecuentes

| Pantalla o mensaje | Acción |
| --- | --- |
| Tu sesión venció | Introduzca personalmente el PIN del mismo usuario; conserve la ficha abierta |
| Faltan datos o cálculo pendiente | Revise el campo indicado y las curvas disponibles; no interprete pendiente como índice cero |
| Sin conexión o Reintentando | Conserve evidencia y mismo UUID; revise de nuevo al recuperar Internet |
| NOT_JPEG | Conserve original y entrega bloqueada; comunique UUID y revisiones. No renombre extensiones ni desactive validación |
| Video espera a la ficha | Finalice/revise el ACK del padre y después el del video, sin reimportar |
| Observaciones no aparecen en Drive | Compare Excel local, revisión y publicación del parche con el responsable |
| No se puede crear usuario | Compruebe ADMIN, conexión y autorización online vigente; guarde el error exacto sin PIN |
| APK no se instala | Comuníquelo al responsable; no desinstale ni borre datos para resolver firma/versionCode |

**Para soporte:** indique módulo, versión, UUID/identidad, revisiones, estado, mensaje y hora aproximada. No incluya PIN, tokens, URLs privadas ni fichas de otros ingenieros.

## 8 Notas de operación y protección

**Acceso.** La primera provisión requiere el servicio de Usuarios y conexión. El login normal verifica la caché cifrada para uso offline durante hasta 72 horas desde una sincronización correcta. Cinco intentos fallidos producen bloqueo temporal de cinco minutos. Una sesión puede bloquearse tras cinco minutos en segundo plano. Conserve el mismo usuario Android y la identidad de la tablet.

**Administración.** Solo ADMIN autorizado entra a Administración. Autorizar cambios online requiere el PIN personal y dura cinco minutos; el permiso se pierde al salir o pasar a segundo plano. Nuevo usuario pide identificador de 3 a 64 caracteres permitidos, nombre hasta 120 y PIN numérico de 6 a 12 dígitos confirmado. No se ha demostrado un límite real de altas del servidor. Si el alta quedó guardada pero falta actualizar caché, sincronice usuarios antes de repetirla.

**Actualización.** La última Release distribuida registrada es 1.4.2/16. El proyecto prepara 1.4.3/17 con la firma histórica y el mismo applicationId. Una Release no actualiza una Debug firmada con otra clave. Esta guía no instala ni distribuye APK. La identidad y las bases Room deben conservarse; no use reinstalación como recuperación.

**Revisiones.** Editar la misma ficha mantiene UUID y numeración estable; confirme una nueva revisión. Un ACK anterior no confirma la edición actual. Las evidencias confirmadas tienen identificadores propios: use sus acciones normales y conserve originales. Un PDF/Excel guardado en Descargas puede ser una copia local sin ACK por archivo.

**Ubicación y unidades.** PR contractual y posición GNSS no son equivalentes. Respete calzada, sentido, distancias y unidades; una estimación de tablet no acredita precisión submétrica. No cambie grillas para desplazar daños ni cantidades PCI/URCI a partir de un croquis aproximado. Los formatos complementarios reutilizan su cabecera padre; no cree otro elemento para completar uno.

**Anulación.** La anulación y restauración remotas de fichas sincronizadas no se incluyen en esta primera Release. Sus botones permanecen ocultos. Solo use el descarte seguro de borradores exclusivamente locales cuando la aplicación lo permita; conserva originales e identidad y no elimina una entrega remota.

**Cierre de jornada.** Guarde borradores, compruebe pendientes/fotos/videos y conserve suficiente batería. No borre datos, bases, carpetas internas o colas. El reinicio de pruebas previo al campo es un mantenimiento coordinado por el propietario, no una acción del ingeniero ni un borrado masivo desde esta guía.

## 9 Referencia técnica conservada

El manual 10I completo se conserva en MANUAL_USUARIO_GBO_INVENTARIO_VIAL.docx y .pdf, con respaldo verificable separado. Amplía los catálogos, navegación, formatos y cuidados de campo. Para Pavimentos, prevalece la distinción de esta guía entre Excel local de superficie y entrega automática por muestra.

El responsable dispone de DIAGNOSTICO_EXPORTACION_PAVIMENTOS_10J.md para rutas/ACK y PLAN_REINICIO_PRODUCCION_10J.md para preparar una campaña limpia. Ninguno de esos documentos ejecuta operaciones de producción. No se envían registros ficticios de aceptación al Drive real sin autorización.
