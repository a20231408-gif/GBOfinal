# Progreso acumulado — 02/11/2026

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
