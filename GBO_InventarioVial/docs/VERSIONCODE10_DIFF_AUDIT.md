# Auditoría VERSIONCODE10

Comparación binaria completa con el ZIP integrado que precedió a los nuevos SIC. VERSIONCODE10 se usa como referencia; no se copia sobre la base Room 15.

Modificados: ['app/build.gradle.kts']

Añadidos: []

Eliminados: []

```diff
--- integrado/app/build.gradle.kts
+++ VERSIONCODE10/app/build.gradle.kts
@@ -54,7 +54,7 @@
         applicationId = "com.tuempresa.inventariovial"
         minSdk = 26
         targetSdk = 37
-        versionCode = 8
+        versionCode = 10
         versionName = "1.3.3-final-mapa-hotfix-map-compat"
 
         testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```

Comparado con la versión nuevos SIC, 246 fuentes/assets/schemas protegidos difieren o están ausentes en VERSIONCODE10. Se conserva íntegramente la implementación SIC.
