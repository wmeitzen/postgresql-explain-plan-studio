#Requires -Version 5.1
<#
.SYNOPSIS
  Build the shaded PEPS jar on Windows and zip it as a portable deliverable.

.DESCRIPTION
  Portable JAR for any OS with JDK/JRE 17+: shaded jar + packaging/mac/peps.sh helper.
  No Mac host, no Intel/Apple/universal native build, no jpackage .app.

  Output: target/PostgreSQLExplainPlanStudio-<version>-jar.zip
#>
[CmdletBinding()]
param(
	[switch] $SkipBuild
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

function Write-Status([string] $Message) {
	Write-Host ("[{0}] {1}" -f (Get-Date -Format 'HH:mm:ss'), $Message)
}

$pomPath = Join-Path $RepoRoot 'pom.xml'
$pom = [xml](Get-Content -LiteralPath $pomPath -Raw)
$version = $pom.project.version
if ([string]::IsNullOrWhiteSpace($version)) {
	throw "Could not read <version> from pom.xml"
}

$mvnw = Join-Path $RepoRoot 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $mvnw)) {
	throw "mvnw.cmd not found at $mvnw"
}

if (-not $SkipBuild) {
	Write-Status "Building shaded jar (mvnw -DskipTests package)…"
	& $mvnw -DskipTests package
	if ($LASTEXITCODE -ne 0) {
		throw "mvnw package failed with exit $LASTEXITCODE"
	}
}

$jarName = "postgresql-explain-plan-studio-$version.jar"
$jarPath = Join-Path $RepoRoot "target\$jarName"
if (-not (Test-Path -LiteralPath $jarPath)) {
	throw "Missing shaded jar: $jarPath (run without -SkipBuild, or package first)"
}

$macDir = Join-Path $RepoRoot 'packaging\mac'
$launcher = Join-Path $macDir 'peps.sh'
$macReadme = Join-Path $macDir 'README.txt'
foreach ($required in @($launcher, $macReadme)) {
	if (-not (Test-Path -LiteralPath $required)) {
		throw "Missing packaging file: $required"
	}
}

$stage = Join-Path $RepoRoot 'target\jar-stage'
$zipName = "PostgreSQLExplainPlanStudio-$version-jar.zip"
$zipPath = Join-Path $RepoRoot "target\$zipName"

Write-Status "Staging portable JAR zip…"
if (Test-Path -LiteralPath $stage) {
	Remove-Item -LiteralPath $stage -Recurse -Force
}
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $stage $jarName)
Copy-Item -LiteralPath $launcher -Destination (Join-Path $stage 'peps.sh')
Copy-Item -LiteralPath $macReadme -Destination (Join-Path $stage 'README.txt')

# Zip entries should use LF and executable bit is lost on Windows; README notes chmod +x.
if (Test-Path -LiteralPath $zipPath) {
	Remove-Item -LiteralPath $zipPath -Force
}
Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $zipPath -Force

Write-Status "Wrote $zipPath"
Write-Host "Any OS: unzip, install JDK 17+, then java -jar postgresql-explain-plan-studio-$version.jar"
Write-Host "macOS/Unix helper: chmod +x peps.sh && ./peps.sh"
