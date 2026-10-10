# Hotfix V2 — Orden de elementos / mapa estable

Fecha: 2026-10-05

## Problema observado
La rama FINAL_INGENIEROS_MAPA podía cerrarse al entrar a **Orden de elementos**.

## Corrección aplicada
1. Se restituyó **únicamente** `road/LiveRoadMap.kt` desde la rama `GBO_InventarioVial_VIDEO_SHOWCASE`, cuyo mapa fue probado visualmente y mostrado correctamente.
2. Se mantuvo el resto de la rama FINAL_INGENIEROS_MAPA actualizada, incluido el catálogo desplegable de códigos de señal MTC 2016.
3. La lógica de orden (`RoadOrdering` / `recalculateOrder`) NO fue sustituida por la rama showcase; se conserva la versión actual porque es idéntica en ambas ramas.
4. El panel de Drive ya no se compone automáticamente al abrir Orden de elementos. Se abre solo al pulsar **Preparar / sincronizar Drive**, aislando el mapa/orden de cualquier incidencia del flujo de entregas.
5. Se conserva el manejo defensivo de errores al calcular orden/plan de carpetas.

## Versión
- versionCode: 7
- versionName: 1.3.2-final-mapa-hotfix-map-stable

## Prueba mínima en tablet
1. Instalar desde Android Studio o APK firmado.
2. Abrir Historial/Mapa, orden y Drive -> Orden de elementos.
3. Debe aparecer el mapa offline sin cierre.
4. Cambiar entre las 6 rutas.
5. Probar zoom +, zoom -, pellizco y arrastre.
6. Registrar un elemento con GNSS y confirmar que aparece y conserva el código geométrico.
7. Pulsar Recalcular orden y verificar que la pantalla permanece abierta.
8. Solo después, abrir Preparar / sincronizar Drive.
