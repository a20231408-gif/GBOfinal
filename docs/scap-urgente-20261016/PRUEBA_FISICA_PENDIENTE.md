# Verificación física pendiente — no ejecutar sobre la ficha del ingeniero

No se ha generado ni instalado una nueva APK. Primero deben identificarse las versiones reales y aprobarse una candidata compatible, con respaldo integral recuperable y firma igual. No actualizar automáticamente, hacer downgrade, desinstalar ni borrar datos. La candidata 1.4.8 ya entregada no contiene estas correcciones.

## Antes del ensayo

- Anotar versión/versionCode y Android/modelo de cada equipo afectado. Conservar las capturas existentes.
- Revisar la evidencia local y aprobar qué base instalar: hotfix mínimo sin migración nueva o continuación desde 1.4.8. Room 20 no debe reducirse a 19.
- Usar un equipo de pruebas autorizado y fichas ficticias identificadas `TEST_SCAP_URGENT`; nunca editar la ficha real para intentar reproducir pérdida.
- Para cualquier prueba de Drive se requiere autorización de escritura y un destino de prueba acordado. Sin ella, probar solo operaciones locales; no inventar PR para conseguir un envío.

## Persistencia y medios

1. Sin Internet, crear puente ficticio. Introducir valores identificables en A, B, C, D, E, F y G; añadir 19 fotografías ficticias y cinco croquis —dos Elevación, dos Planta, uno Sección—. Anotar UUID y valores de prueba antes de salir de cada sección.
2. Pulsar Guardar borrador y esperar fin de operaciones, sin error. Verificar cantidades y datos al alternar secciones e inspecciones. No asumir guardado por ver un valor todavía en pantalla.
3. Cerrar/reabrir GBO offline y comparar cada campo, identidad, fotos y croquis. Reiniciar Android offline y repetir. **Esto no ha sido sustituido por la prueba de reapertura SQLite.**
4. Volver a capturar texto con Internet y retirar la conexión durante guardados. Esperar confirmación local; restaurar conexión y comprobar que no revierte valores. Si hay error, conservar la pantalla, reintentar y comprobar el resultado; no forzar cierre para resolverlo.
5. Bloquear/reanudar sesión con el mismo usuario y PIN introducido solo en el equipo. Confirmar que el borrador no se sustituye. Distinguir expiración de caché de usuarios de pérdida de datos.

## Croquis, provisional y gabinete

6. Cerrar con pendientes mediante confirmación. Con cinco croquis, la falta de principal de Elevación/Planta debe ser WARNING. En exportación SCAP dejar sus espacios sin dibujo arbitrario; el ZIP debe conservar los cinco anexos.
7. Probar elección principal después de reabrir: nueva revisión y mismo UUID. Comprobar que se usa solo ese dibujo en el espacio original, sin alterar la plantilla ni retirar anexos/históricos.
8. Exportar los tres paquetes SIC provisionales. Encontrarlos en Descargas, abrir el ZIP y `ABRIR_PROVISIONAL.html`. Identidad, revisión, fecha/autor y pendientes visibles; valores desconocidos vacíos y marcación PROVISIONAL inequívoca. No debe aparecer un XLSX contractual ficticio.
9. Completar en gabinete los campos SIC, exportar los tres XLSX finales y abrirlos con un lector real. El provisional anterior debe conservarse. Un SCAP cerrado puede seguir incompleto técnicamente aunque los SIC ya estén completos.
10. En una ficha ficticia sin PR, confirmar que el respaldo local funciona y Drive explica la ubicación técnica pendiente. No escribir ceros o datos estimados para forzar una carpeta.

## Drive — solo con aprobación separada

11. Con ruta y ubicación técnica válidas de prueba, enviar la revisión confirmada incompleta. Conciliar UUID, revisión ACK, 19 fotos, cinco croquis, manifest y estado de completitud. Los SIC incompletos no deben incorporarse al consolidado contractual.
12. Interrumpir/reanudar la conexión durante un envío de prueba y conciliar después sin duplicaciones. No alterar ni bloquear Apps Script real para simular BUSY; ese caso ya se ensaya con servidor ficticio.
13. Reabrir y editar gabinete, enviar nueva revisión y verificar identidad/carpeta estable. Un ACK anterior no debe ocultar la edición nueva. Comprobar videos SCAP existentes sin reconstruir ni eliminar sesiones.

## Diagnóstico si reaparece el problema

En la pantalla existente **Diagnóstico de estabilidad → Exportar diagnóstico**, compartir el JSON agregado mediante Android. Los contadores `scapLocalSave` existen solo en fuentes con este hotfix; las versiones anteriores no los generan. Anotar si falló al teclear, guardar borrador, cerrar, agregar imagen, cambiar sección, entrar de nuevo o sincronizar. No enviar PIN, token, URL privada ni fotografías reales para el diagnóstico agregado.

Registrar resultado, Android, versión, momento y operación. Una prueba simulada no identifica por sí sola la causa real ni demuestra capacidad/rendimiento del Apps Script publicado. No aprobar uso general antes de resolver la aceptación física y la estrategia de actualización.
