param([string]$Docx,[string]$Pdf)
$ErrorActionPreference='Stop'
$documentPath=[IO.Path]::GetFullPath($Docx)
$pdfPath=[IO.Path]::GetFullPath($Pdf)
$wordApp=$null
$manualDocument=$null
try {
    $wordApp=New-Object -ComObject Word.Application
    $wordApp.Visible=$false
    $wordApp.DisplayAlerts=0
    $wordApp.AutomationSecurity=3
    $manualDocument=$wordApp.Documents.Open($documentPath,$false,$false)
    $manualDocument.Repaginate()
    foreach($tableOfContents in $manualDocument.TablesOfContents) {$tableOfContents.Update()}
    $manualDocument.Fields.Update() | Out-Null
    $manualDocument.Repaginate()
    foreach($tableOfContents in $manualDocument.TablesOfContents) {$tableOfContents.UpdatePageNumbers()}
    $manualDocument.Save()
    $manualDocument.ExportAsFixedFormat($pdfPath,17)
    Write-Output ('Pages: '+$manualDocument.ComputeStatistics(2))
} finally {
    if($manualDocument) {$manualDocument.Close(0)}
    if($wordApp) {$wordApp.Quit()}
    if($manualDocument) {[Runtime.InteropServices.Marshal]::FinalReleaseComObject($manualDocument) | Out-Null}
    if($wordApp) {[Runtime.InteropServices.Marshal]::FinalReleaseComObject($wordApp) | Out-Null}
}
