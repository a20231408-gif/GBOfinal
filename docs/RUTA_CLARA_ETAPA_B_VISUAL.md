# GBO Ruta Clara — dashboard y superficies auxiliares

Trabajo local del 15 de octubre de 2026 en `D:\Downloads\GBO_PRUEBAS_8B`, rama `codex/observaciones-campo-manual-10I`. Continúa los cambios pendientes de Ruta Clara 1.4.4 (18); no restaura el checkpoint ni incorpora cambios desde la copia anterior recuperada. Sin commit, push, Release nueva o instalación automática.

El propietario confirmó que las versiones anterior y nueva funcionan con el servicio de Usuarios. Se toma esa comprobación como evidencia aportada por el propietario; esta etapa no investiga ni modifica autenticación.

## Respaldo y conservación

Antes de editar: `tmp/ruta-clara-etapa-b-20261015/fuentes-antes.zip` contiene 442 archivos de fuentes, recursos y pruebas; `debug-antes.apk` conserva la Debug previa. `baseline.json` permite comparar fuentes y hashes de los cuatro archivos privados sin copiar sus valores. Se conserva la candidata Release 1.4.4 anterior y los respaldos de recuperación.

La copia independiente `D:\Downloads\GBO_ESTABLE_PRE_RUTA_CLARA` permanece aparte. Su compilación Debug aislada del checkpoint terminó correctamente; el trabajo visual de esta etapa se realiza únicamente en GBO_PRUEBAS_8B.

## Pantallas

- Dashboard: encabezado con variantes de orientación, título y datos de sesión nativos; logo auténtico sobre blanco cuando el ancho permite una composición legible. Fondo gris claro y tarjetas blancas. En ancho suficiente se distribuyen sesión y accesos en paralelo; con texto grande pasan a columna. No se fijan alturas que recorten textos.
- Los ocho accesos originales siguen presentes: Nuevo registro, Mis registros, Pavimentos, Entregas Drive, Mapa, Borradores por completar, Configurar sesión y Herramientas y ajustes. Sin sesión activa se conserva la acción original Iniciar sesión. Continuar borrador conserva el objeto y UUID recibido.
- Drive: mismos cálculos de `DashboardDeliveryCounts.from`, mismas etiquetas y cantidades reales. Un estado SYNCED sin recibo y fecha de confirmación no se cuenta como sincronizado. No se añaden animaciones de progreso o confirmaciones ficticias.
- Herramientas y ajustes: encabezado discreto y grupos blancos. Los controles de campo, herramientas y cuenta siguen componiendo sus implementaciones originales; se conservan las acciones y verificaciones. Solo se extrajo su contenedor visual para poder comprobarlo aisladamente.
- Nuevo registro y Todos los borradores: encabezados coherentes y contenido blanco; filtros, destinos y orden intactos.
- Mapa: encabezado decorativo por encima de los controles existentes. El renderer `LiveRoadMap`, sus gestos, zoom, ejes, selección, rutas y datos no se modifican. El patrón queda fuera del área cartográfica.
- Configuración inicial y login: ya estaban conectados a sus WebP correctos, logo y tarjetas nativas. Se conservan y se incluyen en la verificación visual. No se cambian campos, mensajes, tokens ni callbacks.

No se modifican formularios SIC/SCAP/Pavimentos, lógica de navegación, guardado, autenticación, Room, WorkManager, sincronización, medios ni configuración de producción. Se mantienen versión 1.4.4 (18), applicationId y firma.

## Assets

Se verificaron todos los hashes del `ASSET_MANIFEST.json` del paquete liviano extraído. Se importaron únicamente cinco WebP nuevos, en total **321 956 bytes**:

| Original del paquete | Recurso Android | Uso |
|---|---|---|
| 05_DASHBOARD/dashboard_header_horizontal.webp | drawable-land-nodpi/gbo_dashboard_header.webp | Encabezado horizontal |
| 05_DASHBOARD/dashboard_header_vertical.webp | drawable-nodpi/gbo_dashboard_header.webp | Encabezado vertical |
| 05_DASHBOARD/dashboard_banner_suave.webp | drawable-nodpi/gbo_dashboard_banner.webp | Cabecera de sesión y pantallas auxiliares |
| 06_MAPA_Y_PATRONES/fondo_modulos_secundarios.webp | drawable-nodpi/gbo_tools_header.webp | Solo encabezado de herramientas |
| 06_MAPA_Y_PATRONES/mapa_header.webp | drawable-nodpi/gbo_map_header.webp | Solo encabezado de mapa |

Se reutilizan sin duplicar los cinco recursos existentes: fondos horizontal/vertical de login y configuración, y gbo_auth_logo.png. No se modifica el logo.

Se dejan fuera del APK los fondos completos opcionales del dashboard, fondo_cartografico_suave, textura_topografica_ligera (2,6 MB), patrones PNG, variantes de logo no aprobadas, JPG original adicional, splash fotográfico, fondos del manual, referencias y previews. Permanecen disponibles en el paquete de trabajo.

Iconos: se consultó `08_ICONOS/ICONOS_VECTORIALES_COMPOSE.csv`. Los cinco accesos ya utilizaban vectores Compose coherentes de 24 dp. Se conservan, con color y espaciado consistentes, sin dependencia nueva ni sustitución innecesaria.

Splash: se conserva el arranque actual. El splash nativo utiliza fondo de color uniforme y un icono sujeto a dimensiones/máscara; los fondos fotográficos completos horizontal/vertical no corresponden a ese mecanismo. Añadir una bienvenida para mostrarlos alteraría el arranque. No se añade Activity, dependencia, retardo ni imagen al APK. Referencia: [documentación oficial de Android](https://developer.android.com/develop/ui/views/launch/splash-screen).

No se añaden animaciones al contenido operativo; la entrada de acceso de 200 ms existente se conserva. Así no se introducen cambios temporales en navegación ni estados de entrega.

## Archivos de código de esta etapa

- dashboard/DashboardScreen.kt: presentación del inicio y encabezado de borradores; mismos datos y callbacks.
- dashboard/DashboardHome.kt: utiliza el contenedor visual de herramientas y el encabezado del selector. Conserva estados, observaciones y destinos originales.
- dashboard/RutaClaraComponents.kt: componentes visuales sin acceso a base de datos, red, autenticación o ViewModel.
- road/AssetOrderScreen.kt: sustituye únicamente la presentación del título por el encabezado.
- Dashboard6ScreenTest.kt: conserva comprobaciones y guarda nuevos renderizados en build/outputs para preservar las imágenes históricas de docs. El acceso a todos los borradores se verifica mediante scroll.
- RutaClaraDashboardTest.kt: callbacks, orientación, fuente 2×, estados Drive, herramientas y renderer offline con geometría ficticia. No usa AuthGraph, bases de datos ni red.
- InventoryElementNavigationTest.kt: fixture actualizado al selector actual y textos aprobados; corrige selectores con codificación dañada y expectativas antiguas. No modifica formularios operativos.

## Pruebas e imágenes

Debug compilada correctamente con configuración ordinaria: sin `gboValidationBuild` ni flags de protocolo. **51 pruebas focalizadas aprobadas**, contabilizando cada caso por su resultado final:

| Suite | Casos |
|---|---:|
| Dashboard6PresentationTest | 17 |
| Dashboard6PersistenceTest | 4 |
| Dashboard6ScreenTest | 6 |
| RutaClaraDashboardTest | 8 |
| Navigation10KTest | 2 |
| InventoryElementNavigationTest | 4 |
| AuthRutaClaraScreenTest | 6 |
| AuthRutaClaraRenderTest | 4 |

La primera ejecución tuvo siete fallos: dos selectores no recorrían la lista suficientemente al quedar fuera del viewport; cuatro fixtures buscaban HomeScreen/textos antiguos o con caracteres dañados; un render de login agotó la espera asíncrona de 5 s. Se corrigieron solo fixtures de navegación y scroll. Los siete pasaron al repetirlos, incluido login sin cambiar su código ni su prueba. Los XML iniciales y finales se conservan en `tmp/ruta-clara-etapa-b-20261015/pruebas-iniciales` y `pruebas-repeticion`, con los logs correspondientes. Una repetición adicional del render horizontal comprueba el frame final antes de la revisión visual.

Se comprobó mediante hashes que solo cambiaron, respecto al respaldo inmediato, las tres fuentes visuales indicadas y dos fixtures existentes; los dos archivos Kotlin nuevos contienen presentación y pruebas. Auth, lógica de Drive, DashboardPresentation, formularios, Room, WorkManager, versiones y los cuatro archivos privados conservaron sus bytes. La candidata Release anterior conserva su SHA-256 `e8172e1a1cb0a23d2a07bab1d370a933eef6136d20a65cfcadcd3c8bdf14728e`; no incluye esta nueva etapa y no se recompiló.

Tamaño de la Debug:

| Referencia | Bytes |
|---|---:|
| Debug antes de esta etapa | 41 748 005 |
| Debug con dashboard y auxiliares | 42 087 789 |
| Incremento | 339 784 (+0,81 %) |

Se compara Debug con Debug. No se atribuye a los assets la diferencia de tamaño entre una Debug y la Release optimizada. La importación suma 321 956 bytes; el incremento total incluye recursos compilados y código de presentación.

APK original del build: `app/build/outputs/apk/debug/app-debug.apk`. Se entrega además una copia identificada como Debug en `output/ruta-clara-etapa-b-visual-20261015/`, junto al informe y renderizados. Conserva versión 1.4.4 (18), applicationId y firma Debug existente. La firma Debug difiere de la Release histórica; no usar este APK como actualización de una instalación Release de campo. No se ha instalado ni distribuido.

- Archivo: `GBO_Ruta_Clara_1.4.4_18_ETAPA_B_DEBUG.apk`.
- SHA-256: `006600a930268a53787489dc50e555be141777c43cf41b743e44049934fe15ed`.
- Certificado SHA-256: `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`.
- Firma y alineación verificadas con Android Build Tools; datos completos en `debug-identidad.json` de la entrega.

Los PNG de `app/build/outputs/ruta-clara-etapa-b/renderizados-locales` proceden del árbol Android/Compose real dibujado en Robolectric. **Son renderizados locales, no capturas físicas de una tablet.** Datos, operadores, borradores y geometría son ficticios. Las vistas aisladas de herramientas usan texto de prueba en los espacios de herramientas/cuenta; las acciones principales sí componen los botones reales. El mapa utiliza el renderer real sobre geometría ficticia, no cartografía de campo ni un patrón presentado como mapa.

ADB no detectó dispositivos conectados. No se instaló ningún APK. La revisión física de teclado, gestos y presentación en tablet queda pendiente; no se infiere de los renderizados locales.
