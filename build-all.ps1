# Builds one jar per supported Minecraft 26.x group into dist.
# Needs JDK 25 to run Gradle (set JAVA_HOME). The original 1.21.x sources (src/main) are not touched by this script.
$ErrorActionPreference = 'Stop'

$targets = @(
    @{ mc = '26.1'; fabric = '0.145.1+26.1'; modmenu = '18.0.2'; compat = 'o1'; range = '>=26.1 <26.2' },
    @{ mc = '26.2'; fabric = '0.161.0+26.2'; modmenu = '20.0.3'; compat = 'o2'; range = '>=26.2 <26.4' }
)

$out = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Force $out | Out-Null
Get-ChildItem $out -Filter *.jar | Remove-Item

foreach ($t in $targets) {
    Write-Host "=== Minecraft $($t.mc) (compat $($t.compat))"
    & "$PSScriptRoot/gradlew.bat" clean build -Plane=official `
        "-Pofficial_minecraft_version=$($t.mc)" "-Pofficial_fabric_version=$($t.fabric)" `
        "-Pofficial_modmenu_version=$($t.modmenu)" "-Pofficial_compat=$($t.compat)" "-Pofficial_mc_range=$($t.range)"
    if ($LASTEXITCODE -ne 0) { throw "Build failed for Minecraft $($t.mc)" }
    Get-ChildItem (Join-Path $PSScriptRoot 'build/libs') -Filter *.jar |
        Where-Object { $_.Name -notlike '*-sources.jar' -and $_.Name -notlike '*-dev.jar' } |
        Copy-Item -Destination $out
}

Write-Host "Done. Jars:"
Get-ChildItem $out -Filter *.jar | ForEach-Object { Write-Host "  $($_.Name)" }
