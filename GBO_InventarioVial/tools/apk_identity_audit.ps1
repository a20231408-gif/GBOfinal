param(
    [Parameter(Mandatory=$true,Position=0)][string[]]$Apk,
    [string]$SdkPath=$env:ANDROID_HOME,
    [string]$JavaHome=$env:JAVA_HOME,
    [string]$JsonOutput,
    [string]$ExpectedPackage,
    [int]$ExpectedVersionCode=0,
    [string]$ExpectedCertificateSha256,
    [switch]$RequireProduction
)
$ErrorActionPreference='Stop'
if(!$SdkPath -and (Test-Path -LiteralPath (Join-Path $PSScriptRoot '../local.properties'))) {
    $sdkLine=Get-Content -LiteralPath (Join-Path $PSScriptRoot '../local.properties') | Where-Object {$_ -match '^sdk.dir='} | Select-Object -First 1
    if($sdkLine) {$SdkPath=$sdkLine.Substring(8).Replace('\:',':').Replace('\\','\')}
}
if(!$SdkPath) {throw 'Indica -SdkPath o ANDROID_HOME.'}
$buildTools=Get-ChildItem -LiteralPath (Join-Path $SdkPath 'build-tools') -Directory |
    Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'lib/apksigner.jar')} |
    Sort-Object {[version]($_.Name -replace '-.*$','')} -Descending | Select-Object -First 1
if(!$buildTools) {throw 'Faltan Android SDK Build Tools con apksigner.'}
$aapt=Join-Path $buildTools.FullName 'aapt.exe'
$zipalign=Join-Path $buildTools.FullName 'zipalign.exe'
$signer=Join-Path $buildTools.FullName 'lib/apksigner.jar'
$java=if($JavaHome) {Join-Path $JavaHome 'bin/java.exe'} else {'java'}
function Invoke-SdkTool([string]$Tool,[string[]]$ToolArguments) {
    # Windows PowerShell 5 treats redirected native stderr as error records.
    # SDK/JDK warnings must not replace the actual process exit-code check.
    $savedPreference=$ErrorActionPreference
    $ErrorActionPreference='Continue'
    try {
        $output=(& $Tool @ToolArguments 2>&1 | Out-String)
        $exitCode=$LASTEXITCODE
    } finally { $ErrorActionPreference=$savedPreference }
    return @{Output=$output;ExitCode=$exitCode}
}
$rows=@()
if($RequireProduction -and (!$ExpectedPackage -or $ExpectedVersionCode -lt 1 -or !$ExpectedCertificateSha256)) {
    throw 'La verificación de producción exige package, versionCode y certificado esperados.'
}
foreach($path in $Apk) {
    $file=(Get-Item -LiteralPath $path).FullName
    $result=Invoke-SdkTool $aapt @('dump','badging',$file)
    $badging=$result.Output
    if($result.ExitCode -ne 0) {throw "Manifest APK inválido: $file"}
    $package=[regex]::Match($badging,"package: name='([^']+)'")
    if(!$package.Success) {throw "No se pudo leer la identidad: $file"}
    $versionMatch=[regex]::Match($badging,"versionCode='([0-9]+)'")
    $versionCode=if($versionMatch.Success) {[int]$versionMatch.Groups[1].Value} else {0}
    $versionName=[regex]::Match($badging,"versionName='([^']*)'").Groups[1].Value
    $result=Invoke-SdkTool $java @('-jar',$signer,'verify','--verbose','--print-certs',$file)
    $verify=$result.Output
    $verified=$result.ExitCode -eq 0
    $certificates=@([regex]::Matches($verify,'Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]+)') | ForEach-Object {$_.Groups[1].Value.ToLowerInvariant()})
    $sha1=@([regex]::Matches($verify,'Signer #\d+ certificate SHA-1 digest: ([a-fA-F0-9]+)') | ForEach-Object {$_.Groups[1].Value.ToLowerInvariant()})
    $dn=[regex]::Match($verify,'Signer #1 certificate DN: (.+)').Groups[1].Value.Trim()
    $schemes=@{}
    foreach($m in [regex]::Matches($verify,'Verified using (v[\d.]+) scheme[^\r\n]*: (true|false)')) {$schemes[$m.Groups[1].Value]=$m.Groups[2].Value -eq 'true'}
    & $zipalign -c 4 $file *> $null
    $aligned=$LASTEXITCODE -eq 0
    & $zipalign -c -P 16 4 $file *> $null
    $pageAligned=$LASTEXITCODE -eq 0
    $manifest=(Invoke-SdkTool $aapt @('dump','xmltree',$file,'AndroidManifest.xml')).Output
    $isSplit=$badging -match "package:[^\r\n]*split='" -or $manifest -match 'E: uses-split' -or
        $manifest -match 'isSplitRequired[^\r\n]*(0xffffffff|true)'
    $isTest=$package.Groups[1].Value.EndsWith('.test') -or $badging -match 'testOnly' -or $manifest -match 'E: instrumentation'
    $isDebug=$badging -match 'application-debuggable'
    $hashStream=[IO.File]::OpenRead($file)
    $hasher=[Security.Cryptography.SHA256]::Create()
    try { $apkHash=[BitConverter]::ToString($hasher.ComputeHash($hashStream)).Replace('-','').ToLowerInvariant() }
    finally { $hashStream.Dispose();$hasher.Dispose() }
    $row=[ordered]@{filename=[IO.Path]::GetFileName($file);path=$file;package=$package.Groups[1].Value;versionCode=$versionCode;versionName=$versionName;
        minSdk=[regex]::Match($badging,"sdkVersion:'([^']+)'").Groups[1].Value;targetSdk=[regex]::Match($badging,"targetSdkVersion:'([^']+)'").Groups[1].Value;
        certificateSha256=$certificates;certificateSha1=$sha1;certificateSubject=$dn;signatureSchemes=$schemes;verified=$verified;zipAligned=$aligned;pageAligned16KiB=$pageAligned;
        isSplit=$isSplit;isTest=$isTest;debuggable=$isDebug;bytes=(Get-Item -LiteralPath $file).Length;apkSha256=$apkHash;deviceGroupKnown='UNKNOWN'}
    $rows+=[pscustomobject]$row
    $gating=$RequireProduction -or $ExpectedPackage -or $ExpectedCertificateSha256 -or $ExpectedVersionCode -gt 0
    if($gating -and (!$verified -or $certificates.Count -ne 1)) {throw "Firma ausente, inválida o múltiples firmantes: $file"}
    if($ExpectedPackage -and $row.package -ne $ExpectedPackage) {throw 'applicationId no coincide.'}
    if($ExpectedVersionCode -gt 0 -and $row.versionCode -ne $ExpectedVersionCode) {throw 'versionCode no coincide.'}
    if($ExpectedCertificateSha256 -and $certificates[0] -ne ($ExpectedCertificateSha256 -replace '[:\s]','').ToLowerInvariant()) {throw 'Certificate SHA-256 no coincide.'}
    if($RequireProduction -and (!$aligned -or !$pageAligned -or $isSplit -or $isTest -or $isDebug -or $dn -match 'Android Debug' -or !($schemes['v2'] -or $schemes['v3']))) {throw 'APK incompleto, debug/test, firma sin v2/v3 o no alineado; distribución bloqueada.'}
}
if($JsonOutput) {
    $json=ConvertTo-Json -InputObject @($rows) -Depth 8
    [IO.File]::WriteAllText([IO.Path]::GetFullPath($JsonOutput),$json,[Text.UTF8Encoding]::new($false))
}
$rows | Select-Object filename,package,versionCode,versionName,@{N='Certificate SHA-256';E={$_.certificateSha256 -join ','}},apkSha256,verified,zipAligned | Format-List
