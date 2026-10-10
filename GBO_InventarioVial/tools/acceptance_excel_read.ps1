$ErrorActionPreference = 'Stop'
$auditRoot = Join-Path (Split-Path $PSScriptRoot -Parent) 'docs\acceptance-audit-v15-2026-10-10'
$auditApp = $null
$auditBooks = @()
$auditResults = @()
try {
    $auditApp = New-Object -ComObject Excel.Application
    $auditApp.Visible = $false
    $auditApp.DisplayAlerts = $false
    $auditApp.EnableEvents = $false
    $auditApp.AskToUpdateLinks = $false
    $auditApp.AutomationSecurity = 3
    foreach ($auditType in @('LOSA','MODULAR')) {
        $auditFolder = if ($auditType -eq 'LOSA') { '04-puente-losa' } else { '05-puente-modular' }
        $auditFile = Join-Path $auditRoot "scenarios\$auditFolder\SCAP_$auditType.xlsx"
        $auditBook = $auditApp.Workbooks.Open($auditFile, 0, $true)
        $auditBooks += $auditBook
        $auditApp.CalculateFullRebuild()
        $auditSheets = @()
        foreach ($auditSheet in $auditBook.Worksheets) {
            $auditErrors = @()
            try {
                $auditRange = $auditSheet.UsedRange.SpecialCells(-4123,16)
                foreach ($auditCell in $auditRange.Cells) {
                    if ($auditErrors.Count -lt 100) {
                        $auditErrors += @{ cell = $auditCell.Address($false,$false); text = [string]$auditCell.Text; formula = [string]$auditCell.Formula }
                    }
                }
            } catch { }
            $auditSheets += @{ name=[string]$auditSheet.Name; formulaErrors=$auditErrors }
        }
        $auditResults += @{ type=$auditType; excelVersion=[string]$auditApp.Version; readOnly=[bool]$auditBook.ReadOnly; sheets=$auditSheets; recalculated=$true; saved=$false }
        $auditBook.Close($false)
    }
} catch {
    $auditResults += @{ available=$false; errorType=$_.Exception.GetType().Name; detail='Excel COM inspection unavailable or failed; no workbook saved' }
} finally {
    if ($auditApp) {
        try { $auditApp.Quit() } catch { $auditResults += @{ cleanupConfirmed=$false; errorType=$_.Exception.GetType().Name } }
        try { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($auditApp) } catch { }
    }
}
ConvertTo-Json -InputObject @($auditResults) -Depth 10 | Set-Content -LiteralPath (Join-Path $auditRoot 'evidence\excel-read-only.json') -Encoding utf8
Write-Output 'Independent Excel read-only evidence saved; original workbooks never saved.'
