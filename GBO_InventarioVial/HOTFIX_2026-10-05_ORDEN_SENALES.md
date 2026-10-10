# Hotfix MAPA 2026-10-05 · Orden de elementos + catálogo de señales

Versión: `1.3.1-final-mapa-hotfix-order-signs` (`versionCode 6`).

## 1. Cierre al abrir “Orden de elementos”

Se endureció la pantalla para que una inconsistencia de datos legacy de fotografía/entrega no cierre toda la aplicación:

- `AssetOrderScreen` ya no deja que una excepción al construir la previsualización de Drive derribe la pantalla.
- Si falla la previsualización, se conserva y muestra el orden geométrico y se presenta un mensaje de error visible.
- Se valida que exista una ruta antes de intentar recalcular/exportar.
- `LiveRoadMap` descarta puntos inválidos antes de construir el mapa.
- `DeliveryReviewPanel` captura errores del `Flow` de lotes locales en vez de propagar la excepción a la composición.

Esto protege especialmente registros previos/legacy que podrían tener metadatos no compatibles con las reglas actuales de entrega.

## 2. SIC-22 · Código de señal

El campo libre `Código de señal` se reemplazó por un desplegable dependiente de `Tipo`:

- Reglamento: 84 códigos `R-*`.
- Preventiva: 83 códigos `P-*`.
- Informativa: 40 códigos `I-*`.

Total: 207 códigos tomados del **Manual de Dispositivos de Control del Tránsito Automotor para Calles y Carreteras, MTC 2016** adjuntado al proyecto.

El valor persistido continúa siendo únicamente el código (por ejemplo `P-2B`, `R-1`, `I-2A`), por lo que no se cambia el esquema Room ni el formato de exportación SIC-22.

Al cambiar el tipo de señal, el código anterior solo se conserva si pertenece al nuevo catálogo; en caso contrario se limpia para evitar combinaciones inválidas como tipo reglamentaria + código preventivo.

## Prueba recomendada en tablet

1. Compilar e instalar con la misma keystore release utilizada para el APK de campo.
2. Ver registros → Orden de elementos: la pantalla debe abrir sin cierre.
3. Recorrer las seis rutas y verificar mapa/listado.
4. SIC-22 → Tipo Reglamento: verificar lista `R-*`.
5. SIC-22 → Tipo Preventiva: verificar lista `P-*`.
6. SIC-22 → Tipo Informativa: verificar lista `I-*`, incluyendo `I-2A - POSTES DE KILOMETRAJE`.
7. Guardar una señal y revisar historial/exportación para comprobar que se guarda solo el código.
