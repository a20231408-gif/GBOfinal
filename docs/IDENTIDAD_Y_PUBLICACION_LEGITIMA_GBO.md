# Identidad y publicación futura — guía de lectura, 02/11/2026

No se accedió a Google. Ningún ID, propietario, versión o identidad ejecutora se considera
verificado por este documento. Tener edición en cuatro proyectos no identifica al productivo.

## Hoja privada de comprobación manual (sin ejecutar funciones)

| Dato | Dónde comprobar | Criterio |
|---|---|---|
| scriptId | Configuración del proyecto de cada candidato | Copiar solo a hoja privada; el nombre GBO_Inventario no basta. |
| deploymentId/URL | Implementar → Gestionar implementaciones, abrir detalles sin guardar | Comparar deploymentId de la URL /macros/s/.../exec con configuración legítima Android. No confundir con scriptId ni /dev. |
| Android vigente | Revisión privada de DRIVE_WEB_APP_URL y auditoría offline DEX de APK acreditada | Comparar URLs sin copiar propiedades/token al informe. No ejecutar doGet/doPost. |
| Versión publicada | Detalles de la implementación y versión seleccionada | Registrar número y descripción: etiqueta V21 no sustituye número real. La fuente del editor puede ser posterior. |
| Ejecutar como / acceso | Detalles de la MISMA implementación | Registrar modo y cuenta desplegadora/propietaria si se muestran. No guardar cambios. Si identidad no aparece, queda pendiente del propietario. |
| Ejecutor real | Evidencia del despliegue original y propietario que lo creó | No inferir del usuario que edita, ni de ejecución manual del editor. Ejecutar como usuario cambia la conclusión. |
| Propiedad histórica | Drive: detalles de archivos existentes elegidos por IDs conocidos, permisos existentes | Propietario, ubicación y unidad; compartidos pueden tener otro régimen. Propiedad histórica no prueba identidad de futuras subidas. |
| Raíz antigua | Fuente de la versión publicada y registro privado existente | Comparar ROOT real; no editar ScriptProperties ni sustituirlo por la carpeta nueva. |
| Cuenta personal | Compartir del proyecto y carpetas/ancestros, sin editar permisos | Editor de Script no implica acceso a Drive, Users, despliegues o almacenamiento de GBO. |

Si la UI no permite ver un dato, registrar NO VERIFICADO; no buscar tokens ni ejecutar funciones
para deducirlo. No usar v3Store(), inicializadores, recuperación o preparación de raíces.

## Lecturas que SÍ requieren ejecución y quedan pendientes

- gboPilotInventoryReadonly y gboPilotDestinationReadonly (PreflightReadonly.gs): requieren
  autorización de lectura y consentimiento oficial de la cuenta prevista. El segundo exige
  configuración privada legítima de ancestros. No ejecutados en este encargo.
- gboAdminRegistryReadonly(config): candidato local, aún no instalado. Requiere revisión/autorización
  para añadir fuente al original y ejecución manual de lectura con permisos reales.
- Drive.About identifica a quien ejecuta ESA lectura, no al ejecutor del endpoint productivo.
  Si falta prueba del despliegue, debe confirmarla su propietario; una lectura como personal no basta.

Procedimiento detallado, riesgos y checklist de ficha aislada en BACKEND_ELIMINACION_PREPARACION_20261102.md.

## Avance local y límites

Conservado fac6e0cfa6a6120d9a5037ef4bbac46ab45493d9 en rama padre. Nueva rama separada
codex/backend-identidad-publicacion-20261102. Añadida conciliación offline de la salida real del
lector: estados de journals, JSON ilegible, ACK por metadatos, autoridades duplicadas entre raíces
y selección explícita de UUID de prueba. No clasifica por nombres como prueba de autorización.
No accede a Drive ni escribe datos. usableForDeletion permanece false.

Autoridad individual operativa, tombstones persistentes, cerca en todos los escritores, papelera por
IDs exclusivos y recuperación duradera siguen pendientes del adaptador real descrito en la matriz.
El modelo de 20 pruebas no acredita estas funciones en producción. Sin autoridad administrativa ni
inventario real no es seguro conectar ejecutores; su falta no impide desarrollar lectores, contratos,
verificadores y pruebas locales. PR #5/#6 no fusionados; sin APK ni implementación Google nueva.

## Publicación legítima posterior (no ejecutada ni autorizada aquí)

1. Desarrollar en Git con cuenta personal y mocks; no ejecutar pruebas en Google ni crear proyectos.
2. Revisar diff contra fuente exacta publicada del original; aprobar versión, cerca, pruebas y plan
   de recuperación. Respaldar fuente/configuración privada por canal legítimo sin llevar secretos a Git.
3. Propietario confirma scriptId/deploymentId/ejecutor. Si no es GBO, detener y resolver expresamente
   la identidad, sin asumir transferencia por edición o por cambiar cuenta del navegador.
4. Solicitar autorización GBO para consentimiento/permisos que falten y para editar/publicar el original.
   No ampliar scopes, cambiar raíz, acceso del endpoint o permisos de Drive tácitamente.
5. La cuenta responsable autorizada debe crear la versión y actualizar la MISMA implementación cuando
   sea técnicamente posible, con URL y modo de ejecución verificados. No publicar como personal
   suponiendo que conservará ejecutor GBO. Si el propietario/desplegador no permite actualizarla,
   detener y acordar el procedimiento legítimo; no crear otra implementación automáticamente.
6. Solo con permiso separado: una entrega ficticia controlada, respaldo y IDs exactos, verificar
   propietario/unidad y consumo atribuible a GBO, rutas, hashes y ACK estricto Android. Tener 2 TB
   no demuestra que los bytes se hayan cargado a esa cuenta. No cargar ahora para comprobarlo.
7. Recuperación: conservar cerca/tombstones, journal y respaldo; pausar escritores ante incertidumbre.
   Volver a V21 sin cerca puede resucitar registros. Verificar versión de recuperación compatible,
   conciliar por operationId/fileId y no borrar journals ni repetir operaciones a ciegas.

Pruebas locales: 6 lector + 20 modelo previas reejecutadas PASS; cuatro nuevas de conciliación
offline. No Android ni HTTP real. NO-GO administrativo/productivo hasta verificaciones y aprobación.
