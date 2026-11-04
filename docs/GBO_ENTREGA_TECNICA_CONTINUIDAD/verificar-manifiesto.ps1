param(
    [Parameter(Mandatory=$true)][string]$Root,
    [Parameter(Mandatory=$true)][string]$ExpectedManifestSha256
)
$ErrorActionPreference='Stop'
$base=[IO.Path]::GetFullPath($Root).TrimEnd('\')
if(!$ExpectedManifestSha256 -or $ExpectedManifestSha256 -notmatch '^[a-fA-F0-9]{64}$'){throw 'Invalid trusted manifest hash.'}
$manifestRelative='MANIFIESTOS\ARCHIVOS_SHA256.json'
$checksumRelative='MANIFIESTOS\ARCHIVOS_SHA256.sha256'
function Read-Digest([string]$path) {
    $stream=[IO.File]::OpenRead('\\?\'+$path)
    $algorithm=[Security.Cryptography.SHA256]::Create()
    try {
        $size=$stream.Length
        $hash=([BitConverter]::ToString($algorithm.ComputeHash($stream))).Replace('-','').ToLowerInvariant()
        return @{size=$size;sha256=$hash}
    } finally {$stream.Dispose();$algorithm.Dispose()}
}
$manifestPath=[IO.Path]::Combine($base,$manifestRelative)
if((Read-Digest $manifestPath).sha256 -ne $ExpectedManifestSha256.ToLowerInvariant()){throw 'Trusted manifest hash mismatch.'}
$manifest=Get-Content -LiteralPath ('\\?\'+$manifestPath) -Raw -Encoding UTF8 | ConvertFrom-Json
$seen=[Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
$count=0
foreach($row in $manifest.files) {
    $relative=([string]$row.path).Replace('/','\')
    if([IO.Path]::IsPathRooted($relative) -or $relative.Split('\') -contains '..'){throw 'Unsafe manifest path.'}
    $path=[IO.Path]::GetFullPath([IO.Path]::Combine($base,$relative))
    if(!$path.StartsWith($base+'\',[StringComparison]::OrdinalIgnoreCase) -or !$seen.Add($relative)){throw 'Duplicate or escaping manifest path.'}
    $actual=Read-Digest $path
    if($actual.size -ne [long]$row.size -or $actual.sha256 -ne $row.sha256){throw 'Transfer file integrity mismatch.'}
    $count++
}
$actualCount=0
foreach($path in [IO.Directory]::EnumerateFiles('\\?\'+$base,'*',[IO.SearchOption]::AllDirectories)) {
    $relative=$path.Substring(('\\?\'+$base+'\').Length)
    if($relative -in @($manifestRelative,$checksumRelative)){continue}
    if(!$seen.Contains($relative)){throw 'Unexpected transfer file.'}
    $actualCount++
}
if($count -eq 0 -or $count -ne $actualCount){throw 'Incomplete transfer manifest.'}
Write-Output "TRANSFER VERIFIED: $count files; trusted manifest $ExpectedManifestSha256"
