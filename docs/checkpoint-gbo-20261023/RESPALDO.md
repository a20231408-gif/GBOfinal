# Respaldo del código GBO Ruta Clara — 23/10/2026

Rama de respaldo: `checkpoint/gbo-2026-10-23`.
Base anterior: `aab9cf95f4f049418bbf1ebf2563f33373e843a5`.
Repositorio autorizado por el propietario: `a20231408-gif/GBOfinal` (público).

## Contenido conservado

Código Android actual 1.4.12 (26), applicationId `com.tuempresa.inventariovial`:
SIC-20 (Muro, Túnel y Badén), recuperación/persistencia de borradores,
Pavimentos rígidos y PDF, fotografías y archivos, sincronización y recuperación,
nombres descriptivos de copias locales, identificación de fichas y secciones
«En la carga» / «Mis entregas». Incluye las pruebas nuevas y documentación técnica.
Las correcciones de Pavimentos/PDF y sincronización anteriores ya forman parte
de la base y se conservan mediante su historial.

Se conservan también dos pruebas Node locales, la extensión existente de su
harness y los adaptadores de propuesta 1.4.6 que necesitan como entradas. Están
bajo `docs/hotfix-1.4.6/backend-propuesto`, explícitamente NO desplegados y NO
aprobados para producción. Guardarlos en Git no publica Apps Script.
El resto de paquetes independientes de optimización y ensayos Google permanece local.

## Seguridad y límites

Se revisaron el historial completo accesible, los archivos rastreados actuales,
los nuevos archivos seleccionados y el diff preparado. Gitleaks 8.30.1 detectó
un único falso positivo en documentación histórica: el nombre público de acción
`stageRecordV3`, junto a la palabra «token». No se detectaron credenciales reales.
Se comprobó además que los valores sensibles no vacíos de la configuración privada
local no aparezcan en los objetos del historial ni en la selección actual.
No se incorporan nuevas URL privadas ni identificadores de servicios.
La referencia raíz Drive de las fuentes históricas ya publicadas no se ha cambiado;
no constituye una credencial ni se ha usado para acceder a Drive en esta tarea.

Excluidos: propiedades privadas, keystores, APK/AAB, bases de datos, ZIP,
logs, temporales, salidas de compilación, manuales binarios, previews generados,
corridas y evidencias Google y paquetes pendientes de revisión. Los originales
se conservan localmente; la rama no pretende respaldar secretos ni datos de campo.
Algunos informes históricos enlazan evidencias locales excluidas; esos enlaces
no representan archivos disponibles en este respaldo público.

No se ha modificado código funcional durante este respaldo. Solo se añaden reglas
preventivas de Git y este documento. No se han recompilado ni instalado APK,
repetido pruebas Android, cambiado servicios, usuarios, Drive o datos.
Los resultados de pruebas de los informes previos son evidencia histórica,
no pruebas nuevas ejecutadas el 23/10/2026.

## Recuperación

Clonar el repositorio en una carpeta nueva y seleccionar esta rama o el SHA del
commit publicado. No reemplazar una carpeta de trabajo existente ni forzar un
retroceso de datos Android. Para compilar con servicios y firma legítimos se
requieren las configuraciones privadas y el keystore custodiados fuera de Git;
no se promete reproducibilidad binaria idéntica de una APK solo desde el commit.
La rama anterior y `main` se conservan sin modificaciones.
