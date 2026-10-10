# Puesta en marcha — Inventario Vial + Pavimentos

## 1. Android

Crear `drive.local.properties` en la raíz local:

```properties
DRIVE_API_TOKEN=<token del Apps Script desplegado>
DRIVE_WEB_APP_URL=https://script.google.com/macros/s/<ID>/exec
```

La URL se incorpora a `BuildConfig` al compilar. Si cambia el deployment `/exec`, hay que recompilar/reinstalar la APK.

Compilar en Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain
```

## 2. Apps Script

Desplegar `google-apps-script/Code.gs` como **Aplicación web**:

- Ejecutar como: propietario.
- Acceso: Cualquier persona.
- La URL `/exec` debe abrir en incógnito sin solicitar login.
- `doGet()` debe responder `v6.5 · Drive + pavimentos + entrega por lotes` y `pavementProtocol: 1`.

Antes de un despliegue real, sustituir/rotar `API_TOKEN` y actualizar `DRIVE_API_TOKEN` localmente.

## 3. SIC de inventario vial

Hay dos flujos diferentes:

1. **Ver registros → Orden de elementos → Preparar entrega → Sincronizar**: genera los Excel SIC de la ruta automáticamente y los incluye en el mismo lote junto con fotos/metadata. Quedan dentro de `RUTA/SIB/SIC-XX.xlsx`.
2. **Exportar SIC a Excel**: genera manualmente Excel o ZIP para guardar en tablet. Solo se sube a Drive si se pulsa expresamente **Subir Excel al Drive**; este flujo usa `EXPORTACIONES_SIC`.

## 4. Pavimentos

Dashboard → **Fallas en pavimentos** → crear/editar muestra → cerrar → **Subir muestra a Drive**.

La subida de pavimentos es independiente del lote SIC-17…23 y de SCAP para no mezclar contratos ni carpetas.

## 5. SCAP D.4

`D.4 Perfil longitudinal` queda fuera de navegación, validación y exportación. El bloque de la plantilla fuente se conserva oculto para no alterar el workbook canónico.
