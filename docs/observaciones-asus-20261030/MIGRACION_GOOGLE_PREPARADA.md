# Migración preparada: carpeta y cuenta propietaria

30/10/2026, Lima. El propietario confirmó que desea cambiar carpeta y cuenta propietaria. Esto define el alcance de preparación; no autoriza transferencia, copia remota, cambios de permisos, publicación, cambio de URL/raíz, borrado ni puesta en producción.

## Información que debe inventariarse antes de ejecutar

Identificar por canal privado las cuentas origen/destino, su tipo personal/Workspace, dominio/organización, raíz actual y destino previsto. No guardar correos internos, IDs reales, URLs de servicio, ScriptProperties o tokens en este documento público.

Preparar inventario de solo lectura con: IDs y propietarios de cada archivo/carpeta, padres, rutas, tipo MIME, tamaño/hash de binarios cuando exista, permisos, accesos directos y referencias; proyecto Apps Script vigente, versión/despliegue activo, propietario/ejecutor, triggers y capacidades; configuración de Usuarios separada; reservas de nombres, numeración, bindings por UUID, manifiestos, journals, revisiones, agregados SIC y ACK. Preservar también los pendientes locales en las tablets antes de cambiar su destino. Este inventario remoto aún NO fue realizado.

## Elección de estrategia pendiente del inventario

- Transferencia conservando identidades: solo si el tipo de cuentas y políticas lo permiten. La transferencia de propiedad de Drive tiene restricciones por tipo de cuenta y organización; no se debe asumir que funciona entre cualquier origen y destino. Véase [Transferencia de propiedad de Drive](https://developers.google.com/workspace/drive/api/guides/transfer-file) y [Ayuda de propiedad](https://support.google.com/drive/answer/2494892).
- Copia hacia destino: si no procede la transferencia, preparar copia aislada con tabla explícita de IDs origen→destino y referencias reconstruidas y verificadas. Conservar intacto el origen; un archivo copiado no se considerará confirmado por un recibo del original. No cambiar el destino de una revisión congelada para hacerla pasar por entregada.

Apps Script requiere un plan propio: un cambio de propietario/dominio puede afectar la web app desplegada; no basta con mover la carpeta. La [documentación de web apps](https://developers.google.com/apps-script/guides/web) advierte que las desplegadas en un dominio dejan de funcionar si cambia la propiedad hacia una unidad compartida o cuenta en otro dominio. Por ello la preparación incluye proyecto/ejecución/despliegue de destino y reversión, sin modificar V21 vigente.

## Secuencia propuesta, sin ejecutar

1. Inventario y respaldo verificado de origen: binarios, documentos Google con exportaciones apropiadas, backend, propiedades privadas, índices y permisos. Hashes/recuentos y recuperación comprobada; claves fuera de Git.
2. Crear, tras autorización separada, raíz y backend de PRUEBAS con cuenta destino; usar solo datos ficticios. No apuntar tablets de campo al ensayo.
3. Probar reservas, nombres, consolidación SIC, fotos, reintentos, pérdida de respuesta y ACK completo contra destino. Verificar identidad/UUID/revisión y origen de cada recibo. Probar que un ACK o stage de origen no permite afirmar entrega en destino.
4. Preparar conciliación de históricos y pendientes: mapping de identidades/IDs, manifiestos de ambas raíces y tratamiento explícito de revisiones ya preparadas. No reenviar automáticamente paquetes antiguos a otra cuenta.
5. Presentar acta concreta: cuentas y raíces verificadas, estrategia, conteos/hashes, permisos mínimos, proyecto y versión de destino, pruebas, ventana de corte, tablets incluidas y procedimiento de reversión. Pedir autorización explícita sobre ese acta antes de ejecutar cambios reales.
6. Solo con esa autorización: congelar envíos durante la ventana acordada, ejecutar copia/transferencia aprobada, verificar contenido y acceso con ambas cuentas, cambiar configuración de destino de manera controlada y validar una tablet de pruebas primero. Conservar origen y configuración anterior durante el periodo de verificación acordado.
7. Cierre: conciliación de todos los UUID y pendientes, comparación de documentos y medios, ACK por revisión en destino y prueba de permisos. No borrar origen, cuentas, archivos, revisiones ni respaldos como parte de este plan; cualquier eliminación necesitará otra autorización explícita.

## Condiciones de reversión

Conservar endpoint, configuración, respaldo y permisos anteriores en almacenamiento privado. Si faltan archivos, cambian identidades/reservas, no concilia un consolidado o un ACK no acredita el destino, detener el corte y conservar ambas evidencias. No mezclar escrituras de dos servidores contra la misma raíz ni prometer reversión automática de propiedad; la ruta de reversión se verificará en el ensayo antes del corte.

## Estado actual

Plan listo para completar con el inventario. Ninguna cuenta, carpeta, permiso, ScriptProperty, raíz, URL, archivo remoto o despliegue fue modificado. No se accedió al contenido real de Drive en este trabajo. La restauración de configuración privada en ASUS es distinta de esta migración de cuenta y no debe cambiar el destino productivo.
