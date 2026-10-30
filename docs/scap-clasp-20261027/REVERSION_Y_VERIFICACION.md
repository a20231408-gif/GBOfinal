# Reversión disponible y verificación posterior

V21 fue publicada con autorización expresa. La versión anterior V20 permanece inmutable y respaldada. El despliegue, su URL y configuración son los mismos.

La reversión preparada consiste en actualizar ESE despliegue nuevamente a V20, usando su deployment ID guardado privadamente y conservando su descripción. No crear otro despliegue, no cambiar la URL ni restaurar ScriptProperties o numeraciones. No se ejecutó ninguna reversión.

Si una prueba falla: conservar el mensaje, revisiones, preparación y evidencias. No borrar ni renombrar archivos, no recrear UUID, no limpiar Android y no repetir manualmente muchas veces. Una transacción parcial requiere examinar su journal; no borrar staging para forzar el intento. Comunicar el fallo para decidir si corresponde revertir.

Revertir código no elimina ni revierte entregas ya confirmadas. El HEAD del editor puede permanecer en V21 mientras /exec sirve V20; restaurar también el editor sería una operación independiente. V20 volvería a anunciar ausencia de panel V2 y las nuevas categorías deberían esperar, conservándose localmente.

La verificación técnica del despliegue V21 ya comprobó GET real, panel=2 y equivalencia de todos los campos previos. Sigue pendiente el reintento del operador y comprobar ACK, archivos, carpetas y Excel recibidos. No se declaró una entrega correcta basándose solo en GET o porcentajes de subida.
