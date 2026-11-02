# Progreso acumulado — 02/11/2026

## Candidata integrada local y sesión web (estado más reciente)

Rama codex/candidata-final-local-20261102. Merge LOCAL normal de #5 sobre la fuente que ya incluye
#6/#7: 853b455e57525ee61467e5896dd1770f87f9b612. PR publicados siguen sin fusionar; main y rama
remota de integración intactos. No confundir integración en rama candidata con aprobación productiva.
Selección **154/154 Android PASS**, 27 clases, cero fallos/errores/omitidos (1m15s); **65/65 backend
PASS**. No suite histórica completa ni aceptación física. Primer intento Android 153/154: expectativa
antigua PENDING sin Internet actualizada a WAITING_CONNECTION, manteniendo PENDING con Internet y
rechazo de confirmación falsa. XML separados en tmp/candidata-final-20261102/{regresiones,final}/.

Debug privada **1.4.26 (40)** original, fuente 853b455; compilación normal sin perfil mocks, 15s.
Package com.tuempresa.inventariovial; certificado histórico Debug D2E54C…; firma y alineación 16 KiB
PASS, Room 21 sin cambios, endpoints/token originales comprobados offline, ningún endpoint de ensayo.
V2=true; SIC17A/B=false. Compatible por identidad con actualización de Debug 1.4.25, NO instalación
probada en este cierre. No actualiza Release de otra firma. Eliminación completa siempre deshabilitada.
Ruta: C:\Users\ASUS\GBO_CANDIDATA_PRIVADA_20261102_1_4_26_40\GBO_1.4.26_40_DEBUG_ORIGINAL_CANDIDATA_NO_INSTALAR.apk
SHA256: 47492403eee0c9615742094ca5c646c5aae410591a443c1c3dbdb1e5ea34ef32.
NO INSTALADA / NO DISTRIBUIDA. Preservadas APK 1.4.25 y cuatro respaldos, sin acceso a tablet.

Consentimiento drive.readonly intentado con archivo de autenticación separado: Google mostró
«Se bloqueó esta app». Flujo detenido; credenciales anteriores intactas. No repetir ni evadir.
Conector Drive comprobado solo por perfil: corresponde a identidad distinta de la personal clasp;
no se usó para consultar datos GBO. Los 889 metadatos siguen siendo lectura parcial, no ACK conciliados.

Por instrucción posterior se usa @Browser, sin Chrome ni más OAuth clasp. Pestaña inicial mostraba
GBO; después el propietario abrió su cuenta personal y se confirmó allí el proyecto original.
Inspección web de SOLO LECTURA: V21 activa y deploymentID/URL coincidentes con Android; el campo
visible desde personal dice «Ejecutar como: Yo (cuenta personal)». No inferir solo de esa etiqueta
identidad efectiva/propiedad de futuras subidas: falta confirmar desplegador y ejecución autorizada.
Descripción general: cuenta personal es Editor; propietario del proyecto distinto de GBO; última
modificación visible por GBO el 31/10. Scopes visibles: auth/drive COMPLETO y script.external_request.
Un auditor cuyo cuerpo lee no limita el consentimiento del proyecto a drive.readonly.
No se abrió propiedades secretas, guardó, editó, ejecutó, publicó ni cambió permisos/ROOT.

Siguiente punto de aprobación: cualquier edición del lector remoto, ejecución de función o publicación
debe presentarse separadamente con alcance y efectos. No usar cuenta GBO ni su sesión para ejecutar
sin aprobación. Mientras no exista lectura de contenido legítima, siguen bloqueados journals/ACK,
colisión SCAP e inventario exhaustivo; Users operativo/cerca en todos los escritores aún pendientes.
Candidata revisable para aceptación OFFLINE autorizada, **NO-GO online/general/eliminación real**.
No afirmar que las cuatro pruebas físicas reportadas acreditan todos los casos de esta nueva versión.

Publicación futura: revisión del diff contra V21 exacta y catálogo SCAP preservado; respaldo de fuente,
configuración y entrega; confirmar al desplegador/ejecutor legítimo y permisos de raíz; autorización
explícita para editar fuente y actualizar MISMO deployment conservando URL. No publicar como personal
suponiendo propiedad GBO. Luego una sola ficha ficticia autorizada con ACK/IDs/hashes y propietario
comprobados. Ante fallo, pausar escritores y conservar journals/cerca: rollback V21 sin guarda puede
resucitar registros. La candidata no habilita ninguna de esas operaciones.

Autorización vigente: GET-only personal en proyecto original/raíz antigua, desarrollo local y PR
sin merge automático. Escrituras Google, OAuth/scopes, producción y tablets requieren aprobación.

## Auditoría real iniciada

Se leyó identidad, metadatos de raíz e inventario paginado del registro técnico: **889 entradas**:
207 nombres journal_, 207 record_, 219 binding_, 213 photo_names_v2_ y 43 otros. No son conteos
acreditados por estado ni una prueba de que todos los JSON sean válidos.
La cuenta personal tiene canEdit/canAddChildren/canListChildren en raíz; canTrash=false.
El propietario visible de raíz no es la cuenta institucional GBO. No extrapolar a todos los archivos.
Lectura alt=media de JSON denegada con 403 appNotAuthorizedToFile. Diagnóstico de permiso guardado
privadamente; el lector se detuvo de forma controlada, sin escrituras remotas. No se completó segunda
lectura ni conciliación COMPLETE/PENDING/ACK/colisión SCAP. Esas conclusiones siguen pendientes.

Evidencia sensible fuera de Git, en la carpeta privada clasp y subcarpeta inventario-personal-*.
No publicar IDs/propietarios/capturas/configuración. Lector v2 preparado, no ejecutado: checkpoint de
cada JSON y parada inmediata ante 401/403 para evitar repetir rechazos. Sintaxis node --check PASS.

## Backend terminado localmente, NO activo

- Preflight de todos los IDs antes de primer efecto; revalidación en cada paso. Archivo compartido
  posterior en el conjunto bloquea sin retirar el primero.
- TRASH tardío tras restauración se rechaza; conserva tombstone y evita borrar de nuevo por replay.
- IndividualAuthority: exige proof.verifyAndDecode confiable, emisor/audiencia, operación/digest/UUID/
  revisión/finalidad, expiración, revocación explícitamente vigente y permisos autoritativos de Users.
  Separa actor lógico de propietario Drive verificado. Sin puertos reales no hay autoridad habilitada.
- WriteFence: hook servidor previo a v3Store/recuperación y dentro de ScriptLock; resuelve claves
  mediante índice autoritativo, protege padre SCAP y rechaza identidades legacy ambiguas. No acepta
  claves resueltas por el cliente. Aún NO cableado a V21: no afirmar protección productiva.
- Candidato privado ensamblado desde V21, baseline conservada byte por byte y módulos aislados
  añadidos; sin .clasp.json, handlers ni activación. ScapPhotoPanelV2 conservado. No APK nueva.

Selección final **65/65 PASS**, 0 fallos/omitidos: 35 backend nuevo, 10 lector/conciliación y 20 modelo
previo. Node --test backend/admin-v21/*.test.cjs backend/admin-removal-adapter/*.test.cjs
backend/admin-removal-proposal/simulation.test.cjs. Evidencia tmp/autonomo-20261102/backend-tests.log.
No sumar repeticiones ni atribuir mocks a permisos reales. GAS persistente/papelera solo probado con
APIs mock. Verificador criptográfico, Users vivo, índice inverso completo y backup real siguen faltando.

## Integración y aceptación

PR #5 revisado por diff desde merge-base: doce archivos, seis de presentación productiva; no altera
motor, ACK ni endpoint. No confundir diff de ramas divergentes con retirada de guardas de PR #4.
No fusionado ni probado integrado aquí; exige revisión e integración deliberada. No soluciona por sí
solo permisos/raíces/conflicto del servidor. PR #6 también sin fusionar. No se generó APK instalable.
Propietario informa cuatro pruebas físicas PASS, sin identificar aquí los casos: no acreditar A–E
completas ni P11–P15. Validación física restante, firma/respaldos y autorización de instalación necesarios.

## Único bloqueo inmediato que necesita intervención

Autorizar consentimiento OAuth adicional Drive de solo lectura (drive.readonly), manteniendo cuenta
personal y restringiendo el lector por código a la raíz antigua. appNotAuthorizedToFile indica que
editar por navegador no autoriza a esta aplicación OAuth a leer contenido de esos archivos. Un nuevo
grant puede resolverlo; debe comprobarse tras consentimiento, sin prometer permisos de archivo.
No usar picker/cambios de permisos para sortear el bloqueo ni tokens de subida como autorización.
Sin ese consentimiento no puede finalizar inventario/conciliación real. Con él, siguiente fase automática:
JSON + doble lectura + estados/ACK + referencias y propietarios dentro del alcance, sin ejecutar GAS.
Los permisos de papelera, autoridad Users y corte V21 serán autorizaciones posteriores, nunca implícitas.

**NO-GO productivo, online general y eliminación real.** Sin cambios Google, ROOT, permisos, APK o tablet.
