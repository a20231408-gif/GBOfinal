# GBO Inventario Vial — Prompt 7 para colaborador

## Punto de partida obligatorio

Clonar `https://github.com/a20231408-gif/GBOfinal`, rama `codex/dashboard-sesion-6`, y verificar que HEAD coincide con el hash de publicación indicado en `ENTREGA_COLABORADOR.md` y en el informe de transferencia. No partir de `main` ni de un checkpoint anterior. No rehacer los Prompts 1–6.

## Objetivo

Completar el flujo posterior a guardar un elemento para que el ingeniero pueda confirmar el registro y acceder directamente a su ficha, fotografías y Excel SIC, manteniendo identidad, consolidación y revisiones entre tablets.

## Flujo Nuevo registro → Guardar → Detalle

Al guardar o cerrar un registro desde cualquiera de los formularios SIC, mostrar el detalle del mismo UUID guardado. No abrir una ficha nueva ni depender de la progresiva como identidad.

El detalle debe distinguir borrador, registro validado pendiente, sincronizado confirmado, actualización pendiente, conflicto y error. La georreferenciación externa pendiente no es un error de entrega técnica.

Debe permitir volver al dashboard o a Mis registros sin perder campos, observaciones, fotografías, fecha de sesión, revisión ni estado de sincronización.

## Acciones directas

En el detalle ofrecer acciones reales, accesibles y compatibles con tablet:

1. **Ver ficha:** abrir la ficha del elemento en modo consulta y permitir entrar al flujo de edición existente cuando corresponda.
2. **Ver fotografías:** abrir la galería del registro con categorías, originales, reemplazos e historial del Prompt 5. Conservar UUID, fileId y metadata.
3. **Ver Excel SIC:** mostrar el archivo correspondiente y su estado local/remoto.
4. **Abrir XLSX:** usar el archivo XLSX real mediante intent seguro y URI autorizada.
5. **Compartir XLSX:** compartir el mismo archivo real sin exponer rutas privadas internas.

No crear controles decorativos ni duplicar pantallas/rutas cuando ya exista una función equivalente.

## Vista previa y consolidación Excel

Crear una vista previa comprensible de los datos que se escribirán o ya están escritos en Excel. Puede ser una tabla Compose derivada del mismo modelo de exportación; no simular el contenido con una segunda fuente de datos.

El Excel consolidado debe agrupar por SIC y ruta según la infraestructura V4 existente. Una edición debe actualizar únicamente la fila del UUID correspondiente. No reemplazar el consolidado remoto con datos parciales de una tablet, no alterar otras filas y no crear duplicados por reintento.

Después de editar condición, progresiva, observaciones o fotografías, regenerar/actualizar la salida necesaria para la revisión nueva. Mantener el mismo elemento remoto, número de carpeta e identidad fotográfica. Si cambia la progresiva, actualizar orden y Excel sin usarla como clave.

## Drive y concurrencia 2B

- Reutilizar el outbox, WorkManager, payload V3/pathVersion 4 y ACK actuales.
- Enviar `revision` y `baseRevision` correctos.
- Actualizar por fileId cuando exista.
- Detectar revisiones remotas más nuevas y conservar la edición local en conflicto.
- Mantener reintentos idempotentes y ausencia de carpetas/archivos duplicados.
- Preservar originales e historial de fotografías.
- Mantener separados entrega técnica y georreferenciación externa.
- No crear un backend o sistema de consolidación paralelo.
- No modificar ni desplegar producción.

## Integridad entre tablets

Simular al menos dos operadores/tablets sobre la misma ruta y SIC. Verificar que cada UUID conserva su fila; que editar un elemento no elimina ni reemplaza otros; que dos ediciones del mismo UUID generan conflicto cuando corresponde; y que el reintento de la revisión ganadora no duplica archivos ni carpetas.

No asumir que los datos locales de una tablet representan todo el inventario remoto. El consolidado debe construirse con el estado remoto confirmado y las operaciones de revisión existentes.

## Pruebas focalizadas

Cubrir como mínimo:

1. Guardar registro nuevo y abrir exactamente su detalle.
2. Volver al dashboard/Mis registros y reabrir el mismo UUID.
3. Ver ficha con todos los datos conservados.
4. Ver fotografías categorizadas e historial.
5. Ver vista previa del Excel desde el mismo modelo de exportación.
6. Abrir y compartir el XLSX real.
7. Dos registros del mismo SIC/ruta permanecen en el consolidado.
8. Editar uno actualiza solo su fila.
9. Cambio de progresiva actualiza orden sin cambiar identidad.
10. Edición fotográfica conserva originales/fileIds/historial.
11. Reintento no crea duplicados.
12. Dos operadores sobre el mismo UUID producen conflicto sin pérdida.
13. Sin Internet conserva la revisión local y la cola.
14. Solo mostrar sincronizado después del ACK de la revisión actual.
15. Navegación y accesibilidad en tablet vertical/horizontal.

Ejecutar pruebas Compose/Robolectric y repositorio focalizadas, más `:app:assembleDebug -PgboValidationBuild=true`. No ejecutar la suite completa salvo una regresión transversal justificada. Para una prueba Drive real usar únicamente un despliegue aislado autorizado compatible con syncProtocol 3/pathVersion 4/PhotoCategoryVersion 1.

## Restricciones

No cambiar Room 18/photo_history, firma histórica, applicationId, versionCode, SIC-08/09, GPS opcional, observaciones, longitudes, numeración V4 sin SIB, SCAP, pavimentos, mapa/KMZ, autenticación/TABLET_ADMIN ni servicios de usuarios. No generar Release, instalar en tablet de campo, publicar Apps Script ni iniciar una auditoría general.

Actualizar `docs/CODEX_HANDOFF.md`, documentar matriz requisito → archivo → prueba, conservar resultados fallidos como tales y crear un checkpoint local derivado de `codex/dashboard-sesion-6`.
