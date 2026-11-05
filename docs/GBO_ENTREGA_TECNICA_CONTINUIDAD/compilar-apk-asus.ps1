param(
  [Parameter(Mandatory)][string]$SdkRoot,
  [Parameter(Mandatory)][string]$HistoricalDebugKeystore,
  [Parameter(Mandatory)][string]$PrivateOutputDirectory
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Set-Location $repo
if (git status --porcelain) { throw 'El clon debe estar limpio.' }
git fetch origin codex/scap-pavimentos-cierre-20261103 | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Falló git fetch.' }
git switch codex/scap-pavimentos-cierre-20261103 | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Falta la rama original.' }
git pull --ff-only origin codex/scap-pavimentos-cierre-20261103 | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'No se pudo avanzar sin merge local.' }
git merge-base --is-ancestor 32d852fe70a9662d619f1dc4ac7fffe46ad27733 HEAD
if ($LASTEXITCODE -ne 0) { throw 'No se encontró PR #12 integrado.' }
$build = Get-Content (Join-Path $repo 'app/build.gradle.kts') -Raw
if ($build -notmatch 'productionVersionCode\s*=\s*47' -or $build -notmatch 'versionName\s*=\s*"1\.4\.33"') { throw 'Versión inesperada.' }
foreach ($name in @('server.local.properties','drive.local.properties')) {
  if (-not (Test-Path (Join-Path $repo $name))) { throw "Falta $name privado." }
}
if (-not (Test-Path -LiteralPath $HistoricalDebugKeystore)) { throw 'Falta firma Debug histórica.' }
if ([IO.Path]::GetFullPath($PrivateOutputDirectory).StartsWith($repo, [StringComparison]::OrdinalIgnoreCase)) { throw 'El destino debe quedar fuera de Git.' }
if ($env:ORG_GRADLE_PROJECT_gboValidationBuild -eq 'true') { throw 'El perfil de validación está activo.' }
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot
$env:GBO_DEBUG_KEYSTORE_PATH = (Resolve-Path -LiteralPath $HistoricalDebugKeystore).Path
$init = Join-Path $repo 'docs/GBO_ENTREGA_TECNICA_CONTINUIDAD/debug-signing.init.gradle'
& (Join-Path $repo 'gradlew.bat') --init-script $init :app:assembleDebug --console=plain *> $null
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación. Revisar el entorno ASUS.' }
$apk = Join-Path $repo 'app/build/outputs/apk/debug/app-debug.apk'
if (-not (Test-Path $apk)) { throw 'Falta APK generada.' }
$bt = Get-ChildItem (Join-Path $SdkRoot 'build-tools') -Directory | Sort-Object Name -Descending | Select-Object -First 1
$signer = Join-Path $bt.FullName 'apksigner.bat'
$aapt = Join-Path $bt.FullName 'aapt.exe'
$verification = & $signer verify --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'Firma inválida.' }
$cert = ([regex]::Match(($verification -join ' '), 'Signer #1 certificate SHA-256 digest:\s*([0-9a-f:]+)')).Groups[1].Value.Replace(':','').ToLowerInvariant()
if ($cert -ne 'd2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872') { throw 'Certificado Debug distinto del histórico.' }
$identity = (& $aapt dump badging $apk | Where-Object { $_ -like 'package:*' } | Select-Object -First 1)
if ($identity -notmatch "name='com\.tuempresa\.inventariovial'" -or $identity -notmatch "versionCode='47'" -or $identity -notmatch "versionName='1\.4\.33'") { throw 'Identidad/versiones inesperadas.' }
$configFile = Join-Path $repo 'app/build/generated/source/buildConfig/debug/com/tuempresa/inventariovial/BuildConfig.java'
$config = Get-Content -LiteralPath $configFile -Raw
foreach ($field in @('SERVER_BASE_URL','DRIVE_WEB_APP_URL','DRIVE_API_TOKEN')) {
  if ($config -notmatch ('\b' + $field + '\s*=\s*".+";')) { throw 'Falta configuración de producción.' }
}
New-Item -ItemType Directory -Force -Path $PrivateOutputDirectory | Out-Null
$destination = Join-Path $PrivateOutputDirectory 'GBO_1.4.33_47_DEBUG_HISTORICA_NO_INSTALAR.apk'
if (Test-Path $destination) { throw 'La candidata ya existe.' }
Copy-Item -LiteralPath $apk -Destination $destination
$hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Output "Candidata privada creada; SHA-256: $hash"
