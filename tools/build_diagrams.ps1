$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$sourceDirectory = Join-Path $root "docs\diagrams"
$outputDirectory = Join-Path $sourceDirectory "rendered"
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$sources = Get-ChildItem -Path $sourceDirectory -Filter "*.mmd" -File | Sort-Object Name
if ($sources.Count -eq 0) {
    throw "No Mermaid diagram sources found in $sourceDirectory"
}

foreach ($source in $sources) {
    foreach ($extension in @("svg", "png")) {
        $output = Join-Path $outputDirectory ($source.BaseName + "." + $extension)
        & npx --yes @mermaid-js/mermaid-cli@11.4.2 `
            --input $source.FullName `
            --output $output `
            --backgroundColor white `
            --theme neutral
        if ($LASTEXITCODE -ne 0) {
            throw "Mermaid rendering failed for $($source.Name) ($extension)"
        }
    }
}

Write-Output "Rendered $($sources.Count) Mermaid sources to $outputDirectory"
