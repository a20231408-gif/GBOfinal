# Etapa 1 — Inspección de pavimentos

Se agregó una opción **Condición del pavimento** al Inicio existente de GBO. Reutiliza el login, usuario activo, tema visual y base Room de la app.

## Implementado

- Catálogo tipado de daños asfálticos 1–20, rígidos 21–39 y no pavimentados 81–87.
- Muestras locales ligadas al usuario autenticado, con superficie, tramo, carril, progresivas, ancho y largo.
- Validación visual de la pantalla de captura y estado Borrador/Cerrada.
- Esquema táctil a escala con origen inferior izquierdo, cuadrícula de un metro y coordenadas en metros.
- Registro por toque de daños lineales, de área, huecos, losas y globales; severidad B/M/A según catálogo disponible.
- Edición de datos de muestra, eliminación de daños y captura de fotografías enlazadas al daño.
- Migración Room 10→11 para las tablas de muestras, daños y fotografías.

## Cómo probar

1. Iniciar sesión en la app.
2. En Inicio pulsar **Condición del pavimento**.
3. Elegir Asfáltico, Rígido o No pavimentado y pulsar **Nueva muestra**.
4. Completar tramo, carril, progresivas, ancho y largo; pulsar Guardar.
5. Elegir daño y severidad; tocar el esquema. El daño aparece en la tabla con coordenadas.
6. Seleccionar un daño y pulsar Foto para abrir la cámara. Cerrar la muestra con Cierre.

## Pendientes antes de cálculo y reportes

- `// TODO: validar con manual`: descripciones completas de severidad del capítulo 5.
- `// TODO: validar con manual`: reglas de conteo especiales de losas y exclusiones.
- `// TODO: digitalizar`: curvas VD y CDV del capítulo 6; la app debe mostrar “PCI no calculable” mientras estén vacías.
- `// TODO: validar con manual`: advertencias de áreas 140–320 m², 20±8 losas y longitud de losa <8 m.
- `// TODO: confirmar`: separación de Calzada/Faja y columnas extra X2/Y2, Tramo, Muestra y Fecha en la exportación.
- El motor PCI/URCI, PDF y Excel se implementarán en las etapas siguientes.

La compilación automática quedó pendiente porque el entorno no tenía la distribución Gradle descargada y la revisión automática bloqueó el acceso de red solicitado.
