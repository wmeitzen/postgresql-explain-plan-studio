#Requires -Version 5.1
<#
.SYNOPSIS
  Launch PEPS from a *copy* of the shaded jar, waiting if a dev-watch build is in progress.

.DESCRIPTION
  Maven shade rewrites target\postgresql-explain-plan-studio-*.jar in place. If PEPS is
  already running from that jar, Batik SVG loads fail with ZipException (icons go black)
  and class loading can break. We copy to target\run\peps-dev.jar and launch that instead.
#>
$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

$LockFile = Join-Path $RepoRoot "target\dev-build.lock"
$RunDir = Join-Path $RepoRoot "target\run"
$RunJar = Join-Path $RunDir "peps-dev.jar"
$WaitTimeoutSec = 300
$PollMs = 500

function Write-Status([string]$Message) {
	$ts = Get-Date -Format "HH:mm:ss"
	Write-Host "[$ts] $Message"
}

function Find-PepsJar {
	$candidates = Get-ChildItem -Path (Join-Path $RepoRoot "target") -Filter "postgresql-explain-plan-studio-*.jar" -ErrorAction SilentlyContinue |
		Where-Object { $_.Name -notmatch '-shaded\.jar$' -and $_.Name -notmatch '-sources\.jar$' -and $_.Name -notmatch '-javadoc\.jar$' } |
		Sort-Object LastWriteTime -Descending
	if ($candidates -and $candidates.Count -gt 0) {
		return $candidates[0].FullName
	}
	return $null
}

$java = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $java) {
	Write-Error "java not found on PATH. Install JDK 17+ and retry."
	exit 1
}

$announced = $false
$deadline = (Get-Date).AddSeconds($WaitTimeoutSec)
while (Test-Path $LockFile) {
	if ((Get-Date) -gt $deadline) {
		Write-Error "Timed out after ${WaitTimeoutSec}s waiting for recompile to finish ($LockFile still present)."
		exit 1
	}
	if (-not $announced) {
		Write-Status "PEPS is recompiling — waiting…"
		$announced = $true
	}
	Start-Sleep -Milliseconds $PollMs
}

$jar = Find-PepsJar
if ($null -eq $jar) {
	Write-Error "No runnable jar in target\. Start scripts\dev-watch.ps1 first (or run: .\mvnw.cmd -DskipTests package)."
	exit 1
}

if (-not (Test-Path -LiteralPath $RunDir)) {
	New-Item -ItemType Directory -Path $RunDir | Out-Null
}
Copy-Item -LiteralPath $jar -Destination $RunJar -Force
Write-Status "Starting peps-dev.jar (copy of $($jar | Split-Path -Leaf); safe while RECOMPILENOW rebuilds)"
& java -jar $RunJar
exit $LASTEXITCODE
