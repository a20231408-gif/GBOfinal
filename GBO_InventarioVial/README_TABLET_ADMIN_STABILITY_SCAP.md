# GBO Inventario Vial — entrega TABLET_ADMIN, estabilidad y SCAP

Base: fuente más reciente recibida, paridad SIC v13. Esta iteración es **versionCode 14**, Room **15**, sin migración nueva. Se conservan applicationId, huella, política de firma y los archivos originales de las plantillas.

La entrada de revisión es [TABLET_ADMIN_IMPLEMENTATION_REPORT.md](docs/TABLET_ADMIN_IMPLEMENTATION_REPORT.md): contiene los 15 puntos solicitados, cambios de fuente, resultados y limitaciones. Los nueve documentos solicitados están en `docs/`. Los reportes de iteraciones anteriores se conservan como históricos.

Pruebas Android y compilación de validación, desde PowerShell con el JDK/SDK existentes configurados:

```powershell
.\gradlew.bat testDebugUnitTest --no-configuration-cache --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' '-Pkotlin.compiler.execution.strategy=in-process' -PgboValidationBuild=true
.\gradlew.bat assembleDebug --no-configuration-cache --rerun-tasks --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8' '-Pkotlin.compiler.execution.strategy=in-process' -PgboValidationBuild=true
node --test backend/user-service/tests/service.test.cjs
```

La suite completa conserva fallos anteriores, identificados en `docs/tablet-admin-stability-scap-evidence/baseline.json`. Comparar por identidad con `final-tests.json`: no confundir esos fallos con regresiones ni presentar la suite como totalmente verde. Las capturas nuevas son del editor real bajo Robolectric, no validación física.

El APK debug de validación omite endpoints y credenciales privadas de BuildConfig. El User Service puede configurarse mediante su flujo autorizado existente; Drive requiere un entorno configurado conforme a la política previa. La actualización del servidor la realiza Owner siguiendo [TABLET_ADMIN_SERVER_DEPLOY.md](docs/TABLET_ADMIN_SERVER_DEPLOY.md). No se realizó despliegue, commit ni push.

La firma debug se conserva respecto del APK debug v13. Una tablet con instalación firmada por el certificado corporativo necesita una actualización con su misma clave corporativa; no desinstalar ni sustituirla por debug para probar continuidad de datos. La keystore corporativa no se incluye ni se creó una nueva.

[FINAL_TABLET_CHECKLIST_THIS_ITERATION.md](docs/FINAL_TABLET_CHECKLIST_THIS_ITERATION.md) deja los 54 pasos pendientes para tablet, incluyendo repetición crítica en MODULAR. La causa de salidas físicas sigue sin atribuirse: registrar el diagnóstico real antes de concluir.

Verificadores de esta entrega: `tools/audit_tablet_iteration.py`, `tools/document_tablet_iteration.py` y `tools/package_tablet_iteration.py`. Los scripts de empaquetado anteriores corresponden a sus versiones históricas. El ZIP incluye `SOURCE_FILE_HASHES.json`; los hashes de ZIP/APK se entregan en `hashes.json` junto a los artefactos.
