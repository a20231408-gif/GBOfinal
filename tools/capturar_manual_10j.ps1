param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-z0-9][a-z0-9-]{1,60}$')][string]$Nombre,
    [string]$Descripcion='Pantalla de GBO abierta manualmente por el propietario',
    [string]$Adb='D:\Android\Sdk\platform-tools\adb.exe',
    [string]$Serial='523bc083'
)
$ErrorActionPreference='Stop'
$workspace=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$captureRoot=Join-Path $workspace 'docs\manual-10i\capturas-reales'
$pendingRoot=Join-Path $captureRoot 'pendientes-privadas'
$env:ANDROID_USER_HOME=Join-Path $env:USERPROFILE '.android'
$env:ANDROID_SDK_HOME=$env:USERPROFILE
function Invoke-Adb([string[]]$Arguments) {
    $result=& $Adb -s $Serial @Arguments 2>&1
    if($LASTEXITCODE -ne 0) {throw ('ADB no completó '+$Arguments[0]+'. Comprueba conexión y autorización; no se modifica la aplicación.')}
    return ($result | Out-String)
}
if(!(Test-Path -LiteralPath $Adb)) {throw 'No se encuentra adb.exe.'}
$state=(Invoke-Adb -Arguments @('get-state')).Trim()
if($state -ne 'device') {throw 'La tablet no está conectada y autorizada.'}
$foreground=Invoke-Adb -Arguments @('shell','dumpsys','activity','activities')
if(!($foreground -split "`n" | Where-Object {$_ -match '(mResumedActivity|topResumedActivity)' -and $_ -match 'com\.tuempresa\.inventariovial/'})) {
    throw 'Abre manualmente GBO antes de capturar. No se navega ni se inicia sesión automáticamente.'
}
# No taps, form submission, package installation, app-data export or remote requests.
# The owner must first hide credentials, personal data, notifications and real records.
New-Item -ItemType Directory -Path $pendingRoot -Force | Out-Null
$stamp=Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$remoteName="/sdcard/Download/GBO_MANUAL_10J_${Nombre}_${stamp}.png"
$localFile=Join-Path $pendingRoot "${Nombre}_${stamp}.png"
Invoke-Adb -Arguments @('shell','screencap','-p',$remoteName) | Out-Null
Invoke-Adb -Arguments @('pull',$remoteName,$localFile) | Out-Null
$package=Invoke-Adb -Arguments @('shell','dumpsys','package','com.tuempresa.inventariovial')
$version=[regex]::Match($package,'versionName=([^\r\n]+)').Groups[1].Value.Trim()
$metadata=[ordered]@{name=$Nombre;description=$Descripcion;device=$Serial;versionName=$version;method='adb shell screencap -p and adb pull';sha256=(Get-FileHash -LiteralPath $localFile -Algorithm SHA256).Hash.ToLowerInvariant();privacyReviewed=$false;source=$remoteName;capturedAtLocal=Get-Date -Format o}
$metadata | ConvertTo-Json | Set-Content -LiteralPath ($localFile+'.json') -Encoding utf8
Write-Output $localFile
Write-Output 'Captura privada pendiente de revisión visual. No se incorpora al manual ni a Git automáticamente. El PNG remoto queda en Download; no se elimina ningún archivo.'
