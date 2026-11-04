# GBO — entrega técnica y continuidad independiente

Estado al 03/11/2026, hora de Lima. Este kit complementa, no reemplaza, los originales privados. **NO-GO productivo y distribución**. La instalación offline de 1.4.32 está verificada; la aceptación SCAP corresponde al propietario. No hay autorización de corte V22.

## Empezar aquí

1. Leer este README, [estado y pruebas](ESTADO_Y_RECURSOS.md) y [recuperación Android](RECUPERACION_ANDROID.md).
2. Verificar el manifiesto del kit recibido y recuperar la rama exacta. No fusionar PR ni main.
3. Para preparar el servidor: [publicación y recuperación V22](PUBLICACION_V22_Y_RECUPERACION.md). Es un procedimiento condicionado a una autorización futura, no una instrucción para ejecutar ahora.
4. Sin Codex: [guía manual](GUIA_MANUAL.md). Para otra sesión: [prompt autónomo](PROMPT_CONTINUIDAD.md).
5. Antes de trasladar material privado: [cifrado y traslado](CIFRADO_Y_TRASLADO.md).

En ASUS, el kit completo está en `%USERPROFILE%\GBO_ENTREGA_TECNICA_CONTINUIDAD`, fuera de Git y con ACL privada. `PUBLICO` contiene estas instrucciones; `PRIVADO` contiene material sensible; `VERIFICACION` contiene evidencias locales y un paquete reconstruido que también es privado. Solo estos documentos y los dos auxiliares sin credenciales se publican en GitHub. El manifiesto externo fija el commit documental final; este README fija la fuente de la candidata.

## Fuente recuperable y dependencias

Repositorio: [a20231408-gif/GBOfinal](https://github.com/a20231408-gif/GBOfinal).
Rama vigente: `codex/scap-pavimentos-cierre-20261103`.
Fuente exacta de la APK: `ecd7f01f8ba037999a81c6d0b9a30a1c9913f138`.
Checkpoint de partida comprobado por clon limpio: `f7ff3e0062899602cc919cebb94739e972cd1732`.
Hito de instalación: `cbc859994f635b67230f2ecb8050b30023cb6f5a`.
Los commits posteriores de esta entrega son documentales; comprobar su tip con `git ls-remote` y el manifiesto privado antes de continuar.

| PR | Rama | SHA de referencia | Base | Estado observado |
|---|---|---|---|---|
| [#8](https://github.com/a20231408-gif/GBOfinal/pull/8) | codex/candidata-final-local-20261102 | b79456ed0fd30430517228290013582cd0b5e56c | codex/backend-administrativo-v21-20261102 | Abierto, borrador |
| [#9](https://github.com/a20231408-gif/GBOfinal/pull/9) | codex/candidata-1-4-28-20261103 | 4a21ddef2da670e2581eb0cac80a03396e3a1da5 | Rama de #8 | Abierto, revisable |
| [#10](https://github.com/a20231408-gif/GBOfinal/pull/10) | codex/cierre-backend-drive-institucional-20261103 | f8e3ed53af488b2f81319b09c6c1289dca91c353 | Rama de #9 | Abierto, borrador |
| [#11](https://github.com/a20231408-gif/GBOfinal/pull/11) | codex/scap-pavimentos-cierre-20261103 | f7ff3e0062899602cc919cebb94739e972cd1732, antes de documentación de instalación | Rama de #10 | Abierto, borrador; no fusionar |

Los cuatro SHA de referencia son ancestros del checkpoint fuente de #11: el clon de #11 ya contiene ese trabajo. No aplicar de nuevo cherry-picks o merges de #8–#10. La secuencia de revisión sigue siendo #8→#9→#10→#11; no cambiar bases ni integrar automáticamente. Main se conserva intacta.

## Arquitectura y contratos

- **Android:** Kotlin/Compose, Room, captura offline SIC/SCAP/Pavimentos, fotos y documentos privados, publicación local MediaStore/SAF y WorkManager. Datos reales no forman parte de Git. La base conserva identidad, revisión, estado de captura, outbox y ACK.
- **Documentos:** `LocalExportBuilder`, `LocalSicConsolidates`, `LocalCopyDrain`, `StableLocalPublication` y `RecordDocuments` separan fuentes/revisiones privadas de copias públicas. SIC principal por carretera/formato en EXCEL_SIC. SCAP público por UUID reservado y nombre persistido: `CARRETERA/PUENTES/Puente (NOMBRE)/SCAP - CARRETERA - NOMBRE.xlsx`. Históricos ambiguos se bloquean y conservan; no se migran para hacer pasar pruebas.
- **Exportación SCAP 1.4.32:** cabecera y botón principal guardan directamente en carpeta propia usando permiso de base SAF persistente verificado. Nombre editable en GBO. Permiso ausente/revocado solicita base. Guardar como usa el selector Android, que puede ignorar la ubicación sugerida. Borrador no acredita entrega final. Ver archivos omite provisionales técnicos; los históricos públicos siguen en Android Archivos.
- **Sincronización:** cierre/contrato de entrega, UUID/key/revisión/hash, reservas V2, preparación, staging, commit y ACK estricto/persistente. Exportar localmente no acredita ACK remoto. No fabricar identidad, revisión, reserva ni ACK para resolver un bloqueo.
- **Apps Script original:** GBO_Inventario, deployment productivo aún V21 y URL original `/exec`. Los identificadores/URL exactos y capturas están en el anexo privado. El editor no equivale a la versión desplegada; los lectores añadidos no publicaron V22.
- **V22 candidato:** módulos reanudables, guardas institucionales, enrutamiento dual con admisión por época/fecha original, pin durable, raíz histórica de solo lectura y modo DRAIN_ONLY. No activado ni publicado. La eliminación administrativa definitiva queda deshabilitada/fuera del corte.
- **Drive:** destino institucional nuevo y raíz histórica son entidades distintas. No se cambia el literal ROOT V21 ni se trasladan bindings. Identidad/propiedad/permisos requieren acceso institucional legítimo y revalidación al corte.
- **Usuarios:** servicio independiente, conserva endpoint y autenticación; no es autoridad administrativa de borrado por un token compartido. No se despliega ni cambia en esta entrega.

## Comprobación de recuperación efectivamente realizada

Clon nuevo desde GitHub, sin copiar `C:\GBO\app\build` ni su `.gradle`: checkout f7ff3e0, configuración privada legítima copiada con exclusiones comprobadas, SDK instalado y cache de dependencias ya disponible. Compilación Kotlin + Java **PASS**, 13 tareas ejecutadas; no se empaquetó otra APK ni se ejecutaron pruebas automatizadas. No acredita una instalación de herramientas desde cero ni independencia del SDK/cache de ASUS.

El empaquetador del clon más la captura V21 privada reconstruyó los 14 módulos: 12 idénticos en bytes; `AuthorizedRootGuard.gs` y `NewRootAdmission.gs` difieren solo LF/CRLF y son idénticos en texto. Para una publicación autorizada usar el paquete privado revisado y sus hashes, no normalizarlo silenciosamente. No se ejecutó código remoto. El kit conserva la evidencia de estas diferencias.

Se verificó copia privada íntegra del respaldo OPPO, con soporte de rutas largas. **No se ensayó restauración en otro dispositivo**. TAR íntegro no garantiza claves Android Keystore, sesiones cifradas, grants SAF o catálogo MediaStore portables. No ejecutar los scripts forenses de instalación/backup copiados sin revisar rutas, serial, versión, autorización y estado actual.

## Orden para continuar

Recibir aceptación SCAP del propietario → registrar resultado y conservar checkpoint → revisar expediente V22 con el responsable institucional → solicitar una autorización consolidada y condicionada para código/propiedades/deployment DRAIN_ONLY → comprobar ejecutor/capacidades/permisos → solo con dispositivo limpio, clientes pausados y autorización específica, habilitar ensayo OPEN. El OPPO tiene **dos filas QUEUED** preservadas: no conectarlo para este ensayo ni alterar su cola para obtener aislamiento.
