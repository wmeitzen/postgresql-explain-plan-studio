#Requires -Version 5.1
<#
.SYNOPSIS
  Prompt for a release version, build Windows + Ubuntu jpackage zips, and publish a GitHub release.

.DESCRIPTION
  1) Bumps version, commits/tags/pushes (unless -SkipGit)
  2) Builds Windows zip on this machine
  3) SSHs to the Ubuntu VM, syncs from the VMware share into ~/peps, runs jpackage,
     and copies the linux-x64 zip back
  4) Creates/uploads the GitHub release (private repos OK when `gh auth login` is done)

  Default Ubuntu SSH target: administrator@192.168.163.154
  Override with -UbuntuSsh, $env:PEPS_UBUNTU_SSH, or scripts/dev-release.local.ps1
  ($PepsUbuntuSsh / $PepsLastVersion). After each confirmed run, the chosen SSH
  target and release version are saved to scripts/dev-release.local.ps1 (gitignored).
  The version prompt defaults to the next patch after pom.xml (e.g. 0.0.13 → 0.0.14);
  the SSH target still defaults to the last saved / env value.

  Existing git tags / GitHub releases for the same version are overwritten.

.PARAMETER Version
  Release version like 0.0.9 (without a leading v). Prompted if omitted.

.PARAMETER UbuntuSsh
  SSH target for the Ubuntu VM (default: administrator@192.168.163.154)

.PARAMETER UbuntuLocalRepo
  Path on the VM for the local build tree (default: ~/peps)

.PARAMETER UbuntuHostRepo
  Path on the VM to the Windows share checkout (default: /mnt/hgfs/peps)

.PARAMETER SkipBuild
  Skip both OS builds; upload existing zips already in target\

.PARAMETER SkipUbuntu
  Build/upload Windows only

.PARAMETER SkipGit
  Do not commit, tag, or push

.EXAMPLE
  .\scripts\dev-release.ps1
  .\scripts\dev-release.ps1 -Version 0.0.9 -UbuntuSsh administrator@192.168.56.101
#>
param(
	[string]$Version,
	[string]$UbuntuSsh,
	[string]$UbuntuLocalRepo = "~/peps",
	[string]$UbuntuHostRepo = "/mnt/hgfs/peps",
	[switch]$SkipBuild,
	[switch]$SkipUbuntu,
	[switch]$SkipGit
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

# Optional: scripts/dev-release.local.ps1 (gitignored) remembers last choices:
#   $PepsUbuntuSsh = "user@host"
#   $PepsLastVersion = "0.0.10"
$PepsUbuntuSsh = $null
$PepsLastVersion = $null
$localConfig = Join-Path $PSScriptRoot "dev-release.local.ps1"
if (Test-Path $localConfig) {
	. $localConfig
}

function Write-Status([string]$Message) {
	$ts = Get-Date -Format "HH:mm:ss"
	Write-Host "[$ts] $Message"
}

function Get-CurrentPomVersion {
	[xml]$pom = Get-Content -Raw (Join-Path $RepoRoot "pom.xml")
	return [string]$pom.project.version
}

function Set-ProjectVersion([string]$NewVersion) {
	$pomPath = Join-Path $RepoRoot "pom.xml"
	$mainPath = Join-Path $RepoRoot "src\main\java\com\williammeitzen\peps\gui\MainFrame.java"

	$pomLines = Get-Content $pomPath
	$projectVersionUpdated = $false
	$jpackageVersionUpdated = $false
	for ($i = 0; $i -lt $pomLines.Count; $i++) {
		$line = $pomLines[$i]
		if (-not $projectVersionUpdated -and $line -match '^\s*<version>[^<]+</version>\s*$') {
			$pomLines[$i] = $line -replace '<version>[^<]+</version>', "<version>$NewVersion</version>"
			$projectVersionUpdated = $true
			continue
		}
		if ($line -match '<jpackage\.appVersion>') {
			$pomLines[$i] = $line -replace '<jpackage\.appVersion>[^<]+</jpackage\.appVersion>', "<jpackage.appVersion>$NewVersion</jpackage.appVersion>"
			$jpackageVersionUpdated = $true
		}
	}
	if (-not $projectVersionUpdated -or -not $jpackageVersionUpdated) {
		throw "Failed to update version fields in pom.xml"
	}
	$pomLines | Set-Content -Path $pomPath

	$mainLines = Get-Content $mainPath
	$mainUpdated = $false
	for ($i = 0; $i -lt $mainLines.Count; $i++) {
		if ($mainLines[$i] -match 'APPLICATION_VERSION\s*=') {
			$mainLines[$i] = $mainLines[$i] -replace 'APPLICATION_VERSION = "[^"]+"', "APPLICATION_VERSION = `"$NewVersion`""
			$mainUpdated = $true
			break
		}
	}
	if (-not $mainUpdated) {
		throw "Failed to update APPLICATION_VERSION in MainFrame.java"
	}
	$mainLines | Set-Content -Path $mainPath
}

function Test-GhAuth {
	$gh = Get-Command gh -ErrorAction SilentlyContinue
	if ($null -eq $gh) {
		throw "gh (GitHub CLI) not found on PATH. Install it and run: gh auth login"
	}
	gh auth status 2>&1 | Out-Null
	if ($LASTEXITCODE -ne 0) {
		throw "GitHub CLI is not authenticated. For a private repo run: gh auth login"
	}
}

function Get-DefaultUbuntuSsh {
	if (-not [string]::IsNullOrWhiteSpace($env:PEPS_UBUNTU_SSH)) {
		return $env:PEPS_UBUNTU_SSH.Trim()
	}
	if (-not [string]::IsNullOrWhiteSpace($PepsUbuntuSsh)) {
		return $PepsUbuntuSsh.Trim()
	}
	return "administrator@192.168.163.154"
}

function Get-NextPatchVersion {
	param([string]$Version)
	if ([string]::IsNullOrWhiteSpace($Version)) {
		return $Version
	}
	# 0.0.13 or 0.0.13-SNAPSHOT → 0.0.14 (bump the third numeric component).
	if ($Version.Trim() -match '^(\d+)\.(\d+)\.(\d+)') {
		$major = [int]$Matches[1]
		$minor = [int]$Matches[2]
		$patch = [int]$Matches[3]
		return "{0}.{1}.{2}" -f $major, $minor, ($patch + 1)
	}
	return $Version.Trim()
}

function Get-DefaultReleaseVersion {
	param([string]$PomVersion)
	return Get-NextPatchVersion -Version $PomVersion
}

function Save-DevReleaseLocalConfig {
	param(
		[string]$UbuntuSshTarget,
		[string]$LastVersion
	)
	$ssh = if (-not [string]::IsNullOrWhiteSpace($UbuntuSshTarget)) {
		$UbuntuSshTarget.Trim()
	} else {
		Get-DefaultUbuntuSsh
	}
	$ver = if (-not [string]::IsNullOrWhiteSpace($LastVersion)) {
		$LastVersion.Trim()
	} else {
		""
	}
	$lines = @(
		"# Local defaults for scripts/dev-release.ps1 (gitignored).",
		"# Written automatically when you confirm a release run.",
		"`$PepsUbuntuSsh = `"$ssh`""
	)
	if (-not [string]::IsNullOrWhiteSpace($ver)) {
		$lines += "`$PepsLastVersion = `"$ver`""
	}
	Set-Content -Path $localConfig -Value $lines -Encoding UTF8
}

function Resolve-UbuntuSshTarget {
	param([string]$Explicit)
	if (-not [string]::IsNullOrWhiteSpace($Explicit)) {
		return $Explicit.Trim()
	}
	$defaultTarget = Get-DefaultUbuntuSsh
	$entered = Read-Host "Ubuntu SSH target (user@host) [$defaultTarget]"
	if ([string]::IsNullOrWhiteSpace($entered)) {
		return $defaultTarget
	}
	return $entered.Trim()
}

function Resolve-ReleaseVersion {
	param(
		[string]$Explicit,
		[string]$PomVersion
	)
	if (-not [string]::IsNullOrWhiteSpace($Explicit)) {
		return $Explicit.Trim()
	}
	$defaultVersion = Get-DefaultReleaseVersion -PomVersion $PomVersion
	$entered = Read-Host "Release version (pom is $PomVersion; Enter = next) [$defaultVersion]"
	if ([string]::IsNullOrWhiteSpace($entered)) {
		return $defaultVersion
	}
	return $entered.Trim()
}

# git/ssh write progress to stderr. In Windows PowerShell that becomes ErrorRecords;
# stringify so Out-Host never re-surfaces them as NativeCommandError.
function Invoke-Native {
	param(
		[Parameter(Mandatory = $true)][scriptblock]$Command,
		[string]$Stdin
	)
	$prev = $ErrorActionPreference
	$ErrorActionPreference = "SilentlyContinue"
	try {
		if ($null -ne $Stdin) {
			$raw = $Stdin | & $Command 2>&1
		} else {
			$raw = & $Command 2>&1
		}
		$code = $LASTEXITCODE
		$lines = @(
			$raw | ForEach-Object {
				if ($_ -is [System.Management.Automation.ErrorRecord]) {
					$_.Exception.Message
				} else {
					"$_"
				}
			}
		)
	} finally {
		$ErrorActionPreference = $prev
	}
	return [pscustomobject]@{
		ExitCode = $code
		Output   = $lines
	}
}

# Direct console ssh/scp via Start-Process so:
#  - stdout stays on the console (password prompts work; no 2>&1)
#  - stdout is NOT collected as PowerShell function output (which would
#    pollute the returned exit code, e.g. "KEY_INSTALLED 0")
# Do not use ControlMaster on Windows OpenSSH — "getsockname failed: Not a socket".
function Invoke-SshDirect {
	param([Parameter(Mandatory = $true)][string[]]$ArgumentList)
	$p = Start-Process -FilePath "ssh.exe" -ArgumentList $ArgumentList -NoNewWindow -Wait -PassThru
	if ($null -eq $p) { return 1 }
	return [int]$p.ExitCode
}

function Invoke-ScpDirect {
	param([Parameter(Mandatory = $true)][string[]]$ArgumentList)
	$p = Start-Process -FilePath "scp.exe" -ArgumentList $ArgumentList -NoNewWindow -Wait -PassThru
	if ($null -eq $p) { return 1 }
	return [int]$p.ExitCode
}

function Ensure-PepsSshKey {
	$sshDir = Join-Path $env:USERPROFILE ".ssh"
	$key = Join-Path $sshDir "id_ed25519"
	$pub = "$key.pub"
	if (-not (Test-Path $sshDir)) {
		New-Item -ItemType Directory -Force -Path $sshDir | Out-Null
	}
	if (-not (Test-Path $key)) {
		Write-Status "Creating Windows SSH key $key (empty passphrase)…"
		$prev = $ErrorActionPreference
		$ErrorActionPreference = "SilentlyContinue"
		try {
			& ssh-keygen.exe -t ed25519 -N '""' -f $key -q
			$gen = [int]$LASTEXITCODE
		} finally {
			$ErrorActionPreference = $prev
		}
		if ($gen -ne 0 -or -not (Test-Path $pub)) {
			throw "ssh-keygen failed; create a key manually: ssh-keygen -t ed25519"
		}
	}
	return $key
}

function Install-PepsSshKeyOnRemote {
	param(
		[string]$SshTarget,
		[string]$KeyPath,
		[string[]]$PasswordSshOpts
	)
	$pubPath = "$KeyPath.pub"
	$pubLine = (Get-Content -Path $pubPath -Raw).Trim()
	if ([string]::IsNullOrWhiteSpace($pubLine)) {
		throw "Public key file is empty: $pubPath"
	}
	if ($pubLine -match "'") {
		throw "Public key contains a single quote; refuse to embed in remote shell command"
	}
	# Put the key in the remote argv so stdin stays on the console for the password prompt.
	$remote = "mkdir -p ~/.ssh && chmod 700 ~/.ssh && touch ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && (grep -qxF '$pubLine' ~/.ssh/authorized_keys || echo '$pubLine' >> ~/.ssh/authorized_keys) && echo KEY_INSTALLED"
	Write-Status "Installing public key on $SshTarget (enter VM password once)…"
	$code = Invoke-SshDirect -ArgumentList ($PasswordSshOpts + @($SshTarget, $remote))
	if ($code -ne 0) {
		throw @"
Could not install SSH key on $SshTarget (exit $code).
Try manually:
  ssh $SshTarget
If password login works, install the key with:
  type `$env:USERPROFILE\.ssh\id_ed25519.pub | ssh $SshTarget "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
"@
	}
}

function Build-UbuntuPackage {
	param(
		[string]$SshTarget,
		[string]$HostRepo,
		[string]$LocalRepo,
		[string]$ZipName,
		[string]$DestZip
	)

	if (-not (Get-Command ssh.exe -ErrorAction SilentlyContinue) -or
		-not (Get-Command scp.exe -ErrorAction SilentlyContinue)) {
		throw "OpenSSH client (ssh/scp) not found. Install 'OpenSSH Client' optional feature on Windows."
	}

	Write-Status "Building Ubuntu jpackage on $SshTarget …"

	$keyPath = Ensure-PepsSshKey
	$sshKeyOpts = @(
		"-i", $keyPath,
		"-o", "IdentitiesOnly=yes",
		"-o", "ConnectTimeout=20",
		"-o", "StrictHostKeyChecking=accept-new"
	)
	$sshBatch = $sshKeyOpts + @("-o", "BatchMode=yes", "-o", "PreferredAuthentications=publickey")
	$sshPassword = @(
		"-o", "ConnectTimeout=20",
		"-o", "StrictHostKeyChecking=accept-new",
		"-o", "PubkeyAuthentication=no",
		"-o", "PreferredAuthentications=password,keyboard-interactive",
		"-o", "NumberOfPasswordPrompts=3"
	)

	Write-Status "Checking key-based SSH to $SshTarget…"
	$probe = Invoke-SshDirect -ArgumentList ($sshBatch + @($SshTarget, "echo SSH_OK"))
	if ($probe -ne 0) {
		Install-PepsSshKeyOnRemote -SshTarget $SshTarget -KeyPath $keyPath -PasswordSshOpts $sshPassword
		$probe = Invoke-SshDirect -ArgumentList ($sshBatch + @($SshTarget, "echo SSH_OK"))
		if ($probe -ne 0) {
			throw "SSH key installed but key login still fails for $SshTarget (exit $probe). On the VM check ~/.ssh/authorized_keys permissions (700/600) and that sshd allows PubkeyAuthentication."
		}
	}
	Write-Status "SSH key auth OK — no more password prompts for this release"

	$remoteScriptPath = Join-Path $env:TEMP "peps-ubuntu-jpackage.sh"
	$remotePathFile = "/tmp/peps-linux-zip-path.txt"
	# Note: quoted "~/peps" does NOT expand in bash — expand leading ~ to $HOME explicitly.
	$remoteScript = @"
set -euo pipefail
HOST_REPO='$HostRepo'
LOCAL_REPO='$LocalRepo'
ZIP_NAME='$ZipName'
PATH_FILE='$remotePathFile'
case "`$LOCAL_REPO" in
  "~") LOCAL_REPO="`$HOME" ;;
  "~/"*) LOCAL_REPO="`$HOME/`${LOCAL_REPO#~/}" ;;
esac
if [[ ! -f "`$HOST_REPO/pom.xml" ]]; then
  echo "Host share missing pom.xml at `$HOST_REPO" >&2
  echo "Check VMware shared folders / mount (/mnt/hgfs)." >&2
  exit 1
fi
mkdir -p "`$LOCAL_REPO"
rsync -a --delete \
  --exclude '.git/' \
  --exclude 'target/' \
  --exclude '*.sqlite' \
  --exclude '.idea/' \
  "`$HOST_REPO/" "`$LOCAL_REPO/"
cd "`$LOCAL_REPO"
chmod +x mvnw scripts/*.sh 2>/dev/null || true
./mvnw -P jpackage -DskipTests verify
test -f "target/`$ZIP_NAME"
ZIP_ABS="`$(pwd)/target/`$ZIP_NAME"
printf '%s\n' "`$ZIP_ABS" > "`$PATH_FILE"
echo "Built `$ZIP_ABS"
"@
	[System.IO.File]::WriteAllText($remoteScriptPath, ($remoteScript -replace "`r`n", "`n"))

	try {
		Write-Status "Uploading build script and running jpackage on Ubuntu…"
		$remoteTmp = "/tmp/peps-ubuntu-jpackage.sh"
		$up = Invoke-ScpDirect -ArgumentList ($sshBatch + @($remoteScriptPath, "${SshTarget}:$remoteTmp"))
		if ($up -ne 0) { throw "scp of build script failed (exit $up)" }

		$build = Invoke-SshDirect -ArgumentList ($sshBatch + @($SshTarget, "bash $remoteTmp"))
		if ($build -ne 0) {
			throw "Ubuntu remote jpackage failed (exit $build). Check that /mnt/hgfs/peps is mounted and JDK 17 + rsync are installed."
		}

		$pathOutFile = Join-Path $env:TEMP "peps-remote-zip-path.txt"
		Remove-Item -Force $pathOutFile -ErrorAction SilentlyContinue
		$prev = $ErrorActionPreference
		$ErrorActionPreference = "SilentlyContinue"
		try {
			& ssh.exe @sshBatch $SshTarget "cat $remotePathFile" | Set-Content -Path $pathOutFile
			$pathCode = $LASTEXITCODE
		} finally {
			$ErrorActionPreference = $prev
		}
		if ($pathCode -ne 0 -or -not (Test-Path $pathOutFile) -or -not (Get-Content $pathOutFile -Raw).Trim()) {
			throw "Could not read remote zip path from $remotePathFile"
		}
		$resolved = (Get-Content $pathOutFile -Raw).Trim() -replace "`r", "" -replace "`n", ""

		Write-Status "Copying ${SshTarget}:$resolved → $DestZip"
		New-Item -ItemType Directory -Force -Path (Split-Path $DestZip) | Out-Null
		$copy = Invoke-ScpDirect -ArgumentList ($sshBatch + @("${SshTarget}:$resolved", $DestZip))
		if ($copy -ne 0) {
			throw "scp of Linux zip failed (exit $copy)"
		}
		if (-not (Test-Path $DestZip)) {
			throw "Linux zip was not copied to $DestZip"
		}
	} finally {
		Remove-Item -Force $remoteScriptPath -ErrorAction SilentlyContinue
	}
}

# --- version ---
$current = Get-CurrentPomVersion
$Version = Resolve-ReleaseVersion -Explicit $Version -PomVersion $current
$Version = $Version.Trim()
if ($Version.StartsWith("v") -or $Version.StartsWith("V")) {
	$Version = $Version.Substring(1)
}
if ($Version -notmatch '^\d+\.\d+\.\d+([.-][A-Za-z0-9.-]+)?$') {
	throw "Invalid version '$Version'. Use forms like 0.0.9"
}
$Tag = "v$Version"
$WindowsZipName = "PostgreSQLExplainPlanStudio-$Version-windows-x64.zip"
$LinuxZipName = "PostgreSQLExplainPlanStudio-$Version-linux-x64.zip"
$WindowsZip = Join-Path $RepoRoot "target\$WindowsZipName"
$LinuxZip = Join-Path $RepoRoot "target\$LinuxZipName"

Write-Status "Preparing release $Tag"
Test-GhAuth

if ($current -ne $Version) {
	Write-Status "Updating project version $current -> $Version"
	Set-ProjectVersion $Version
} else {
	Write-Status "pom.xml already at $Version"
}

if (-not $SkipUbuntu -and -not $SkipBuild) {
	$UbuntuSsh = Resolve-UbuntuSshTarget -Explicit $UbuntuSsh
	Write-Status "Ubuntu SSH target: $UbuntuSsh"
} elseif (-not [string]::IsNullOrWhiteSpace($UbuntuSsh)) {
	$UbuntuSsh = $UbuntuSsh.Trim()
} else {
	$UbuntuSsh = Get-DefaultUbuntuSsh
}

Write-Host ""
Write-Host "This will:"
Write-Host "  - $(if ($SkipGit) { 'Skip git commit/tag/push' } else { "Commit/tag/push $Tag" })"
Write-Host "  - $(if ($SkipBuild) { 'Skip Windows build' } else { 'Build Windows jpackage zip' })"
Write-Host "  - $(if ($SkipBuild -or $SkipUbuntu) { 'Skip Ubuntu build' } else { "Build Ubuntu jpackage on $UbuntuSsh" })"
Write-Host "  - Upload available zips to GitHub release $Tag"
$confirm = Read-Host "Continue? [Y/n]"
if ($confirm -match '^[Nn]') {
	Write-Status "Aborted."
	exit 1
}

Save-DevReleaseLocalConfig -UbuntuSshTarget $UbuntuSsh -LastVersion $Version

# Push first so the Windows share (and any git consumers) see the release commit before Ubuntu syncs.
if (-not $SkipGit) {
	Write-Status "Committing version bump (if any) and tagging $Tag"
	git add pom.xml "src/main/java/com/williammeitzen/peps/gui/MainFrame.java"
	$status = git status --porcelain -- pom.xml "src/main/java/com/williammeitzen/peps/gui/MainFrame.java"
	if ($status) {
		$commit = Invoke-Native -Command { git commit -m "Release $Version" }
		if ($commit.Output) { $commit.Output | ForEach-Object { Write-Host $_ } }
		if ($commit.ExitCode -ne 0) { throw "git commit failed (exit $($commit.ExitCode))" }
	}
	$existingTag = git tag -l $Tag
	if ($existingTag) {
		Write-Status "Tag $Tag already exists — overwriting"
		Invoke-Native -Command { git tag -d $Tag } | Out-Null
		# Remote tag may or may not exist; ignore delete failures.
		$del = Invoke-Native -Command { git push origin "refs/tags/${Tag}" --delete }
		if ($del.Output) { $del.Output | ForEach-Object { Write-Host $_ } }
	}
	$tagResult = Invoke-Native -Command { git tag -a $Tag -m $Tag }
	if ($tagResult.ExitCode -ne 0) {
		if ($tagResult.Output) { $tagResult.Output | ForEach-Object { Write-Host $_ } }
		throw "git tag failed (exit $($tagResult.ExitCode))"
	}
	Write-Status "Pushing main and $Tag"
	$pushMain = Invoke-Native -Command { git push origin HEAD }
	if ($pushMain.Output) { $pushMain.Output | ForEach-Object { Write-Host $_ } }
	if ($pushMain.ExitCode -ne 0) { throw "git push failed (exit $($pushMain.ExitCode))" }
	$pushTag = Invoke-Native -Command { git push origin $Tag --force }
	if ($pushTag.Output) { $pushTag.Output | ForEach-Object { Write-Host $_ } }
	if ($pushTag.ExitCode -ne 0) { throw "git push tag failed (exit $($pushTag.ExitCode))" }
}

if (-not $SkipBuild) {
	$mvnw = Join-Path $RepoRoot "mvnw.cmd"
	Write-Status "Building Windows jpackage zip…"
	& $mvnw -P jpackage -DskipTests verify
	if ($LASTEXITCODE -ne 0) {
		throw "Windows mvnw jpackage failed with exit $LASTEXITCODE"
	}
	if (-not (Test-Path $WindowsZip)) {
		throw "Missing Windows zip: $WindowsZip"
	}

	if (-not $SkipUbuntu) {
		Build-UbuntuPackage -SshTarget $UbuntuSsh -HostRepo $UbuntuHostRepo `
			-LocalRepo $UbuntuLocalRepo -ZipName $LinuxZipName -DestZip $LinuxZip
	}
}

$assets = @()
if (Test-Path $WindowsZip) { $assets += $WindowsZip }
if (Test-Path $LinuxZip) { $assets += $LinuxZip }
if ($assets.Count -eq 0) {
	throw "No release zips found in target\ for version $Version"
}
if (-not (Test-Path $WindowsZip)) {
	Write-Warning "Windows zip missing: $WindowsZipName"
}
if (-not (Test-Path $LinuxZip) -and -not $SkipUbuntu) {
	throw "Linux zip missing after Ubuntu build: $LinuxZip"
}

Write-Host "Assets to publish:"
$assets | ForEach-Object { Write-Host "  $_" }

$title = "v$Version"
$notes = @"
## PostgreSQL Explain Plan Studio v$Version

Bundled Java runtime included.

### Windows
- Asset: ``$WindowsZipName``
- Unzip and run ``PostgreSQLExplainPlanStudio\PostgreSQLExplainPlanStudio.exe``

### Ubuntu / Linux
- Asset: ``$LinuxZipName``
- Unzip and run ``PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio``
"@

$prevEa = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$existing = gh release view $Tag 2>$null
$releaseExists = ($LASTEXITCODE -eq 0)
$ErrorActionPreference = $prevEa
if ($releaseExists) {
	Write-Status "Release $Tag already exists — replacing assets"
	gh release upload $Tag @assets --clobber
	if ($LASTEXITCODE -ne 0) { throw "gh release upload failed" }
	gh release edit $Tag --title $title --notes $notes | Out-Null
} else {
	Write-Status "Creating release $Tag"
	gh release create $Tag @assets --title $title --notes $notes
	if ($LASTEXITCODE -ne 0) { throw "gh release create failed" }
}

Write-Status "Done: $(gh release view $Tag --json url -q .url)"
