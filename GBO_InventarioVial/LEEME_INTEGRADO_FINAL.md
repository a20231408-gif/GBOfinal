# GBO_InventarioVial integrado A + B + C

La fuente final usa B como esqueleto, autenticación/backend A íntegros, CORE conectado y Pavimentos C. Room 14, migraciones 12→13→14 y esquema KSP incluidos.

Documentación vigente: `docs/FINAL_INTEGRATION_REPORT.md`, `docs/FINAL_TABLET_CHECKLIST.md`, `docs/DRIVE_V2_SERVER_CHANGE_REQUIRED.md`, `docs/SIC14A_PENDING_DECISION.md`. Los reportes originales de rama son evidencia histórica; sus instrucciones de watermark/versiones provisionales no reemplazan las decisiones finales.

Validación del 08/10/2026: 323 pruebas Android, 308 passed y exactamente 15 baseline históricos, 0 fallos nuevos; Gradle test devuelve FAILED. User Service 21 PASS; Apps Script 37 PASS. Assemble fresco: BUILD SUCCESSFUL. APK debug de validación, sin keystore nueva ni credenciales locales incrustadas. Ver hashes e inspección DEX en el informe.

Reproducción local con Android SDK/JDK y wrapper incluidos:

```powershell
.\gradlew.bat testDebugUnitTest --no-configuration-cache -PgboValidationBuild=true
.\gradlew.bat assembleDebug --no-configuration-cache --rerun-tasks -PgboValidationBuild=true
node backend/user-service/tests/service.test.cjs
node google-apps-script/Code.test.cjs
python tools/final_integration_validation.py --tests --apk app/build/outputs/apk/debug/app-debug.apk
```

Configurar `local.properties` con el SDK de la máquina. `--source` del auditor requiere los proyectos originales A/B en `tmp/final-merge/`; la evidencia de hashes protegidos de esta ejecución se entrega en `docs/final-evidence/SOURCE_AUDIT.json`.

El cambio Apps Script está versionado pero NO desplegado. No cambiar tokens, huella, User Service ni Script Properties como parte de esta integración. El build de campo requiere la keystore release original y configuración ya autorizada. Las referencias `#REF!` de SCAP, la contradicción SIC-14A 07/13 y las curvas ausentes permanecen explícitas.
