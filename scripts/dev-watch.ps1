#Requires -Version 5.1
<#
.SYNOPSIS
  Rebuild the shaded jar when the RECOMPILENOW semaphore appears (fast path; not jpackage).

.DESCRIPTION
  Keep this running in one terminal while developing. On start, packages once so a
  runnable jar exists, then waits for Cursor (or you) to create RECOMPILENOW in the
  repo root when a change batch is finished.

  This Windows watcher builds when it sees a *new* RECOMPILENOW (by LastWriteTime) but does
  **not** delete the file — so Ubuntu's shared-folder sync watcher can still see it.
  scripts/dev-sync-watch.sh (systemd on the VM) clears RECOMPILENOW after its build.

  Launch the app with scripts\dev-run.ps1.
#>
$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

$LockFile = Join-Path $RepoRoot "target\dev-build.lock"
$SemaphoreFile = Join-Path $RepoRoot "RECOMPILENOW"
$PollMs = 500
$Mvnw = Join-Path $RepoRoot "mvnw.cmd"

$BuildRunning = $false
$RebuildQueued = $false
# Last RECOMPILENOW stamp we already built for (do not delete the shared flag).
$LastBuiltStamp = $null

function Write-Status([string]$Message) {
	$ts = Get-Date -Format "HH:mm:ss"
	Write-Host "[$ts] $Message"
}

function Test-JavaAvailable {
	$java = Get-Command java -ErrorAction SilentlyContinue
	return $null -ne $java
}

function Get-RecompileStamp {
	if (-not (Test-Path -LiteralPath $SemaphoreFile)) {
		return $null
	}
	$item = Get-Item -LiteralPath $SemaphoreFile
	# String form so we can compare across polls; includes length in case of touch-only races.
	return "{0:o}|{1}" -f $item.LastWriteTimeUtc, $item.Length
}

function Invoke-MavenPackage([string]$Reason) {
	$targetDir = Join-Path $RepoRoot "target"
	if (-not (Test-Path $targetDir)) {
		New-Item -ItemType Directory -Path $targetDir | Out-Null
	}
	$lockBody = "pid=$PID`r`nstarted=$(Get-Date -Format o)`r`n"
	Set-Content -Path $LockFile -Value $lockBody -Encoding ascii

	Write-Status "$Reason"
	& $Mvnw -DskipTests package
	$exit = $LASTEXITCODE
	if ($exit -eq 0) {
		Write-Status "Build OK"
	} else {
		Write-Status "Build FAILED (exit $exit)"
	}
	return $exit
}

function Invoke-PepsBuild {
	if ($BuildRunning) {
		$script:RebuildQueued = $true
		Write-Status "Build already running — queued another rebuild."
		return
	}

	$script:BuildRunning = $true
	try {
		do {
			$script:RebuildQueued = $false
			$stamp = Get-RecompileStamp
			if ($null -eq $stamp) {
				break
			}
			if ($stamp -eq $LastBuiltStamp) {
				break
			}

			Invoke-MavenPackage "RECOMPILENOW seen — building (leaving flag for Ubuntu)…" | Out-Null
			# Remember this stamp so we do not rebuild in a loop; do NOT delete RECOMPILENOW.
			$script:LastBuiltStamp = Get-RecompileStamp
			if ($null -eq $LastBuiltStamp) {
				$script:LastBuiltStamp = $stamp
			}
		} while ($RebuildQueued)
	} finally {
		if (Test-Path $LockFile) {
			Remove-Item -Force $LockFile -ErrorAction SilentlyContinue
		}
		$script:BuildRunning = $false
	}
}

if (-not (Test-JavaAvailable)) {
	Write-Error "java not found on PATH. Install JDK 17+ and retry."
	exit 1
}
if (-not (Test-Path $Mvnw)) {
	Write-Error "mvnw.cmd not found at $Mvnw"
	exit 1
}

# Always compile once before waiting on the semaphore.
$script:BuildRunning = $true
try {
	Invoke-MavenPackage "Initial build (before watching RECOMPILENOW)…" | Out-Null
} finally {
	if (Test-Path $LockFile) {
		Remove-Item -Force $LockFile -ErrorAction SilentlyContinue
	}
	$script:BuildRunning = $false
}

Write-Status "Watching for $SemaphoreFile (poll ${PollMs}ms); will not delete it. Ctrl+C to stop."

try {
	while ($true) {
		$stamp = Get-RecompileStamp
		if ($null -eq $stamp) {
			# Cleared by Ubuntu (or manually) — allow the next create to rebuild.
			$LastBuiltStamp = $null
		} elseif ($stamp -ne $LastBuiltStamp) {
			Invoke-PepsBuild
		}
		Start-Sleep -Milliseconds $PollMs
	}
} finally {
	if (Test-Path $LockFile) {
		Remove-Item -Force $LockFile -ErrorAction SilentlyContinue
	}
	Write-Status "Watcher stopped."
}
