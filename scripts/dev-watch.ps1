#Requires -Version 5.1
<#
.SYNOPSIS
  Watch PEPS sources and rebuild the shaded jar on change (fast path; not jpackage).

.DESCRIPTION
  Keep this running in one terminal while developing. Launch the app with scripts\dev-run.ps1.
#>
$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

$LockFile = Join-Path $RepoRoot "target\dev-build.lock"
$DebounceMs = 500
$Mvnw = Join-Path $RepoRoot "mvnw.cmd"

# Shared with FileSystemWatcher event runspaces
$State = [hashtable]::Synchronized(@{
	Pending = $false
	LastChange = [datetime]::MinValue
	BuildRunning = $false
	RebuildQueued = $false
})

function Write-Status([string]$Message) {
	$ts = Get-Date -Format "HH:mm:ss"
	Write-Host "[$ts] $Message"
}

function Test-JavaAvailable {
	$java = Get-Command java -ErrorAction SilentlyContinue
	return $null -ne $java
}

function Invoke-PepsBuild {
	if ($State.BuildRunning) {
		$State.RebuildQueued = $true
		Write-Status "Build already running — queued another rebuild."
		return
	}

	$State.BuildRunning = $true
	try {
		do {
			$State.RebuildQueued = $false
			$targetDir = Join-Path $RepoRoot "target"
			if (-not (Test-Path $targetDir)) {
				New-Item -ItemType Directory -Path $targetDir | Out-Null
			}
			$lockBody = "pid=$PID`r`nstarted=$(Get-Date -Format o)`r`n"
			Set-Content -Path $LockFile -Value $lockBody -Encoding ascii

			Write-Status "Building…"
			& $Mvnw -DskipTests package
			$exit = $LASTEXITCODE
			if ($exit -eq 0) {
				Write-Status "Build OK"
			} else {
				Write-Status "Build FAILED (exit $exit)"
			}
		} while ($State.RebuildQueued)
	} finally {
		if (Test-Path $LockFile) {
			Remove-Item -Force $LockFile -ErrorAction SilentlyContinue
		}
		$State.BuildRunning = $false
	}
}

function Register-WatchHandlers {
	param(
		[System.IO.FileSystemWatcher]$Watcher,
		[hashtable]$MessageData
	)
	$action = {
		$name = $EventArgs.Name
		if ($null -ne $name -and $name -notmatch '\.(java|xml)$') {
			return
		}
		$data = $Event.MessageData
		$data.Pending = $true
		$data.LastChange = Get-Date
	}
	@(
		(Register-ObjectEvent -InputObject $Watcher -EventName Changed -Action $action -MessageData $MessageData),
		(Register-ObjectEvent -InputObject $Watcher -EventName Created -Action $action -MessageData $MessageData),
		(Register-ObjectEvent -InputObject $Watcher -EventName Deleted -Action $action -MessageData $MessageData),
		(Register-ObjectEvent -InputObject $Watcher -EventName Renamed -Action $action -MessageData $MessageData)
	)
}

if (-not (Test-JavaAvailable)) {
	Write-Error "java not found on PATH. Install JDK 17+ and retry."
	exit 1
}
if (-not (Test-Path $Mvnw)) {
	Write-Error "mvnw.cmd not found at $Mvnw"
	exit 1
}

Write-Status "Watching $RepoRoot (src + pom.xml). Ctrl+C to stop."
Invoke-PepsBuild

$srcWatcher = New-Object System.IO.FileSystemWatcher
$srcWatcher.Path = (Join-Path $RepoRoot "src")
$srcWatcher.IncludeSubdirectories = $true
$srcWatcher.Filter = "*.*"
$srcWatcher.NotifyFilter = [IO.NotifyFilters]::FileName -bor
	[IO.NotifyFilters]::DirectoryName -bor
	[IO.NotifyFilters]::LastWrite

$pomWatcher = New-Object System.IO.FileSystemWatcher
$pomWatcher.Path = "$RepoRoot"
$pomWatcher.Filter = "pom.xml"
$pomWatcher.IncludeSubdirectories = $false
$pomWatcher.NotifyFilter = [IO.NotifyFilters]::LastWrite -bor [IO.NotifyFilters]::FileName

$handlers = @()
$handlers += Register-WatchHandlers -Watcher $srcWatcher -MessageData $State
$pomAction = {
	$data = $Event.MessageData
	$data.Pending = $true
	$data.LastChange = Get-Date
}
$handlers += Register-ObjectEvent -InputObject $pomWatcher -EventName Changed -Action $pomAction -MessageData $State
$handlers += Register-ObjectEvent -InputObject $pomWatcher -EventName Created -Action $pomAction -MessageData $State

$srcWatcher.EnableRaisingEvents = $true
$pomWatcher.EnableRaisingEvents = $true

try {
	while ($true) {
		Start-Sleep -Milliseconds 200
		if (-not $State.Pending) {
			continue
		}
		$elapsed = ((Get-Date) - $State.LastChange).TotalMilliseconds
		if ($elapsed -lt $DebounceMs) {
			continue
		}
		$State.Pending = $false
		Invoke-PepsBuild
	}
} finally {
	$srcWatcher.EnableRaisingEvents = $false
	$pomWatcher.EnableRaisingEvents = $false
	foreach ($h in $handlers) {
		Unregister-Event -SourceIdentifier $h.Name -ErrorAction SilentlyContinue
		Remove-Job -Id $h.Id -Force -ErrorAction SilentlyContinue
	}
	$srcWatcher.Dispose()
	$pomWatcher.Dispose()
	if (Test-Path $LockFile) {
		Remove-Item -Force $LockFile -ErrorAction SilentlyContinue
	}
	Write-Status "Watcher stopped."
}
