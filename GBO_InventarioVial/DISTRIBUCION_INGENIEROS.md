# Distribución a tablets de ingenieros

Versión de aplicación: **1.3.0-final-mapa-offline**.

## Qué contiene esta rama

- Captura SIC-17 a SIC-23 y complementarios.
- SCAP y exportaciones existentes.
- Fallas de pavimentos.
- Orden geométrico por eje de las seis rutas del proyecto.
- Mapa de orden en vivo y **100 % offline** usando `road_reference.json` empaquetado.
- Zoom con gesto focal, paneo y códigos que se renumeran automáticamente según posición geométrica.
- KMZ, Drive, metadata y fotos.
- Servicio central de usuarios con huella estable.
- No incluye dataset QA, presets de coordenadas ni paneles rojos de simulación.

## Instalación

No es obligatorio usar una laptop. Un APK firmado puede copiarse o descargarse en cada tablet y abrirse directamente si Android permite instalar aplicaciones desde esa fuente.

Para una entrega controlada se recomienda:

1. Android Studio → **Build > Generate Signed App Bundle / APK > APK**.
2. Seleccionar **release**, no debug.
3. Firmar siempre con el mismo keystore de GBO y guardar ese keystore fuera del repositorio.
4. Distribuir el APK por un canal privado o MDM.
5. En cada tablet, abrir la app, copiar la huella estable y provisionar un token **único por tablet** en el servicio de usuarios.
6. No enviar el token de tablet junto con el APK por el mismo canal.

Actualizar futuras versiones instalando el APK firmado con el mismo `applicationId` y el mismo certificado. No es necesario desinstalar.

## Seguridad frente a copia del APK

Android permite copiar un APK instalado/distribuido y ningún APK cliente puede impedir por completo la ingeniería inversa. La seguridad del sistema no debe depender de ocultar el APK.

Esta rama aplica defensa por capas:

- autenticación de usuario y PIN;
- token secreto diferente por tablet;
- token cifrado localmente con Android Keystore AES-GCM;
- huella estable de dispositivo;
- validación de huella + token en el servicio central;
- revocación remota de tablet;
- compilación `release` con R8/minificación y reducción de recursos;
- eliminación de logs en release;
- el rebind automático por solo conocer el token queda deshabilitado en el servicio **users-v2.2-stable-device-locked**.

Copiar solo el APK a otra tablet no autoriza el acceso. Para recuperación excepcional de una tablet cuya huella cambió, el propietario debe autorizar durante 1–30 minutos el nuevo fingerprint exacto.

## Apps Script de usuarios recomendado antes de distribuir

Desplegar los archivos de `backend/user-service` como nueva versión de la implementación existente. `doGet()` debe mostrar:

`users-v2.2-stable-device-locked`

La tablet que ya fue migrada a huella estable sigue funcionando sin reprovisionar.
