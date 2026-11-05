# Verificar, cifrar y trasladar el kit privado

El kit local completo contiene credenciales, firmas, APK configurada y datos del propietario. **No es publicable.** La copia ASUS tiene ACL para propietario/SYSTEM, pero ACL no equivale a cifrado. No se creó un contenedor cifrado ni se generó/guardó una contraseña en esta entrega; se proporciona este procedimiento verificable, porque no estaba instalado 7-Zip y la contraseña debe quedar bajo control del propietario.

## Verificación antes de cifrar

Recibir del mantenedor el SHA-256 de `MANIFIESTOS/ARCHIVOS_SHA256.json` por un canal independiente del paquete. No basta con confiar en un hash incluido junto al archivo que protege. El manifiesto inventaría rutas relativas, tamaños y SHA de todo el material; excluye únicamente él mismo y su archivo `.sha256`. Es privado porque revela nombres de evidencias y datos.

```powershell
$gboKit='C:\GBO_KIT'
$gboManifestSha='<SHA recibido del propietario>'
& "$gboKit\PUBLICO\verificar-manifiesto.ps1" -Root $gboKit -ExpectedManifestSha256 $gboManifestSha
git bundle verify "$gboKit\PRIVADO\git\GBO_CONTINUIDAD_FINAL.bundle"
git bundle list-heads "$gboKit\PRIVADO\git\GBO_CONTINUIDAD_FINAL.bundle"
```

El auxiliar es de solo lectura: verifica hash del manifiesto, contención de rutas, cada tamaño/hash, ausencia de archivos faltantes/extra y utiliza rutas largas de Windows. No ejecuta archivos recibidos, no restaura, no conecta Google ni imprime nombres privados. Revisar primero el script y su procedencia; no desactivar globalmente controles de PowerShell para ejecutarlo.

## Crear un respaldo cifrado bajo control del propietario

1. Con una instalación legítima de [7-Zip](https://www.7-zip.org/), abrir su File Manager y seleccionar **la carpeta completa** GBO_ENTREGA_TECNICA_CONTINUIDAD. Usar Add/Añadir, formato **7z**, método de cifrado **AES-256** y **Cifrar nombres de archivo / Encrypt file names**. El formato 7z admite AES-256 según su [documentación oficial](https://www.7-zip.org/7z.html).
2. Elegir una contraseña larga y única, introducirla personalmente en la ventana y conservarla fuera del kit, de Git, de registros de terminal y del canal que transportará el archivo. No escribirla en un comando ni enviarla al chat.
3. Guardar el `.7z` **fuera** de la carpeta de origen, en almacenamiento privado. No usar ZIP sin cifrado ni poner el contenedor dentro del kit inventariado.
4. Cerrar el archivo, volver a abrirlo y comprobar que los nombres requieren contraseña. Usar Test/Probar e introducirla personalmente. Exigir cero errores.
5. Extraer mediante Extract/Extraer a otra carpeta corta y privada, por ejemplo `C:\GBO_KIT_COMPROBACION`, sin sobrescribir el origen. Evitar arrastrar a Explorador, porque puede usar temporales; el [FAQ oficial](https://www.7-zip.org/faq.html) explica esta diferencia. No extraer dentro de Git, OneDrive o Descargas compartidas.
6. Ejecutar el auxiliar anterior contra la copia extraída y el SHA independiente, y verificar el bundle allí. Exigir coincidencia completa. Esto prueba traslado de bytes; NO restauración del OPPO.
7. Calcular `Get-FileHash -Algorithm SHA256 -LiteralPath 'RUTA_PRIVADA_DEL_ARCHIVO.7z'`, registrar el hash fuera del archivo y compararlo al recibirlo en otra laptop. Enviar la contraseña por otro canal al receptor autorizado, después de comprobar su identidad.

No borrar los originales al terminar ni retirar el único respaldo en ASUS. Si falta algún archivo, el descifrado falla o los hashes difieren, declarar transferencia INSUFICIENTE y conservar todo; no improvisar una restauración. La extracción puede contener rutas largas: preferir un destino corto y herramientas que las admitan. El TAR canónico conserva la estructura interna aunque Explorador no pueda desplegar todos sus nombres.

## En la computadora receptora

Aplicar ACL privada para su usuario y SYSTEM a la carpeta recibida. No reutilizar la identidad Windows de ASUS ni asumir que las ACL viajan con el archivo. Verificar todo, conservar el contenedor y extraer solo lo necesario para el rol. Iniciar sesión Google oficialmente; no importar cookies/OAuth. Guardar configuración y firmas únicamente en rutas ignoradas del clon y verificar `git check-ignore`/`git status` antes de publicar.

El desarrollador de Android con fixtures no necesita el bloque PRIVADO. El publicador institucional necesita el anexo/fuentes/configuración y acceso real GBO; poseer el paquete no concede permisos ni autorización de corte. No restaurar datos del OPPO en otro teléfono sin un encargo y ensayo de recuperación separados.
