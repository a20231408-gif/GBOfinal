# Extracción de señales MAN_6

Fuente: `MAN_6 DCT-2016.pdf`, 532 páginas PDF. Se conserva el gráfico publicado, sin generación de imágenes, reinterpretación de símbolos ni PDF dentro del APK. Las instrucciones de los manuales se trataron como contenido normativo; el alcance del trabajo proviene del pedido del usuario.

El catálogo existente contiene 207 códigos reglamentarios, preventivos e informativos. Resultado: **207 thumbnails WebP verificados, cero códigos sin imagen**. No se amplió el catálogo con señales ajenas al catálogo actual. I-2A utiliza la variante Nacional de la página impresa 118; el manual también muestra Departamental y Vecinal.

Procedimiento reproducible:

1. Instalar PyMuPDF y Pillow en el entorno de trabajo.
2. Ejecutar `python tools/extract_signal_assets.py --pdf "ruta/MAN_6 DCT-2016.pdf"`.
3. El script encuentra encabezados con códigos, identifica los objetos raster y recorta el gráfico compuesto de la página para conservar correcciones vectoriales sobre esos raster. Si no hay raster identificable utiliza el dibujo vectorial de esa sección.
4. Ocho recortes explícitos, revisados contra los gráficos originales, están en `tools/signal_crop_overrides.json`: R-5-1/2/3/4, R-45/R-45A y R-54A/R-54B. Los cuatro primeros proceden de la figura 2.14, página impresa 41.
5. Recorta márgenes blancos, conserva bordes y leyendas de la propia señal y reduce a un máximo de 320×320 píxeles. Guarda WebP sin pérdida.
6. Genera el manifest y la cuadrícula HTML. Se revisaron visualmente las 207 miniaturas mediante cuatro hojas de contacto y se corrigieron los ocho casos anteriores. P-25B conserva la flecha vectorial distinta de P-25A; I-1D se resuelve aunque su encabezado esté partido en líneas.

Trazabilidad por código en `app/src/main/assets/signs/sign_assets_manifest.json`: código, nombre, archivo, MAN_6, página impresa, página física PDF, rectángulo en puntos PDF, método, motivo de verificación, dimensiones y SHA-256. La página física es la página impresa + 1. `SIGNAL_ASSET_AUDIT.html` muestra código, nombre y página para revisión humana y puede abrirse sin conexión.

La app utiliza `TrafficSignVisualSelector` desde `SignalCodeFields`, compartido por captura y edición técnica SIC-22. La lista es LazyColumn; solo decodifica los thumbnails visibles de forma asíncrona, con caché limitada a 4 MiB. La imagen tiene descripción accesible con código y nombre. Manual y sin identificación muestran placeholders, aunque el texto coincida con un código del catálogo. I-2A mantiene la política puntual existente.

Las pruebas verifican existencia, decodificación, dimensiones, pertenencia al catálogo, hashes únicos, trazabilidad, búsquedas, cambio de preview, placeholders y límite de caché. El desplazamiento se ejercita en Compose/Robolectric. La verificación de memoria y rendimiento en tablet física queda en el checklist de aceptación del usuario.
