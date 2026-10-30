# Validación del panel SCAP oficial

| Requisito | Evidencia local | Pendiente real |
|---|---|---|
| Crear inspección nueva y abrir F.2 | Room y componentes Compose reales, controlador y formulario de alta del registro fotográfico | Tablet |
| Mostrar los 20 conceptos en orden | Lista independiente del test, ambos catálogos y recorrido de los 20 encabezados Compose | Confirmación visual en tablet |
| Asociar evidencias 01,03,08,15,20 | JPEG válido generado para prueba, repositorio real; captura deliberadamente en orden inverso | Cámara/importador físico |
| Guardar borrador y reabrir | Cierre completo y reapertura de base Room en disco, mismos UUID/índices/valores | Cierre/apertura de app en tablet |
| Excel SCAP | XLSX real con cinco fotos y descripciones ordenadas; plantilla, geometría y originales conservados | Abrir Excel en equipo del ingeniero |
| Exportación ZIP | Entradas reales, carpetas y nombres descriptivos, bytes JPEG originales y repetición determinista | Abrir ZIP en tablet |
| Copia automática local | Plan real del exportador con rutas oficiales, hashes y nombres sin UUID en los archivos | Publicación MediaStore/SAF y visor de archivos de tablet |
| Videos | Nombres de las 20 categorías en backend simulado; ACK/HTTP simulado, persistencia Room y plan local de video | Grabación, decodificación/reproducción y carga reanudable de MP4 real |
| ACK y Drive | Adaptador V20 aislado: carpeta estable, dos clientes/inspecciones, BUSY, respuesta perdida, rollback y reintento sin duplicados | Publicación autorizada de parche, paridad y escrituras en Google |
| Históricos | Equivalencias solo de lectura; valores ambiguos sin asignación inventada; se conservan reservas, UUID y originales | Revisar una ficha histórica real, sin editarla |
| SIC/compatibilidad | Códigos SIC-17 independientes conservados; regresiones de otros SIC, nombres V2, fotografías, progresiva SCAP→SIC y exportadores | Validación física de la futura candidata |

Los archivos `PLAN_VIDEO_FICTICIO_SOLO_BYTES.json` usan bytes sintéticos para validar persistencia/nombres/copias; **no acreditan un MP4 decodificable**. Las fotografías de prueba sí se generan mediante el codificador JPEG de Android en Robolectric. Ninguna imagen o captura de prueba se presenta como evidencia de una tablet real.

El ensayo Google se mantiene **PENDIENTE**. Un ACK simulado no acredita una entrega productiva. La consulta real de capacidades fue exclusivamente GET y confirmó que falta `scapPhotoPanelVersion=2`; los envíos nuevos quedan detenidos con evidencia conservada. No se cambió producción, configuración privada ni firma. Se autorizó la candidata 1.4.13 (27); no se instaló nada.
