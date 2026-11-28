#Requires -Version 5.1
<#
.SYNOPSIS
  Prompt for Pre-release or Stable, build packages, and publish a GitHub release.

.DESCRIPTION
  Interactive menu (unless -Action is set):
    (1) Publish Pre-release — GitHub --prerelease; README Download links unchanged
    (2) Promote Pre-release (v N.N.N) to Stable — Latest full release; rewrite README; bump working tree to next patch
    (3) Publish to Stable — Latest full release; rewrite README; keep working version (no bump)

  Then: bump version if needed, commit/tag/push (unless -SkipGit), build Windows + Ubuntu +
  macOS assets, upload the GitHub release.

  Override SSH with -UbuntuSsh / -MacSsh, $env:PEPS_UBUNTU_SSH / $env:PEPS_MAC_SSH, or
  scripts/dev-release.local.ps1 ($PepsUbuntuSsh / $PepsMacSsh / $PepsLastVersion).
  After each confirmed run, chosen SSH targets and release version are saved there
  (gitignored). The version prompt defaults to the current pom.xml version.
  Linux jpackage for published assets should run on Ubuntu 20.04.

  Existing git tags / GitHub release assets for the same version are overwritten.
  Release notes bodies are preserved on edit (Promote/re-upload); only the title heading
  is normalized for Pre-release vs Stable. New releases still start title-only.

.PARAMETER Action
  PreRelease, PromoteStable, or Stable. Prompted via menu if omitted.

.PARAMETER Version
  Release version like 1.0.0 (without a leading v). Prompted if omitted; default is current pom.

.PARAMETER UbuntuSsh
  SSH target for the Ubuntu 20.04 release VM

.PARAMETER UbuntuLocalRepo
  Path on the VM for the local build tree (default: ~/peps)

.PARAMETER UbuntuHostRepo
  Path on the VM to the Windows share checkout (default: /mnt/hgfs/peps)

.PARAMETER MacSsh
  SSH target for the macOS release VM

.PARAMETER MacLocalRepo
  Path on the Mac for the local build tree (default: ~/peps)

.PARAMETER MacHostRepo
  Optional path on the Mac to a Windows share checkout (auto-detected if empty)

.PARAMETER SkipBuild
  Skip OS builds; upload existing assets already in target\

.PARAMETER SkipUbuntu
  Skip Ubuntu zip/.deb

.PARAMETER SkipMacApp
  Skip macOS .app zip/DMG remote build (still builds portable jar.zip unless -SkipBuild)

.PARAMETER SkipGit
  Do not commit, tag, or push

.PARAMETER Yes
  Skip the interactive "Continue?" confirmation (for non-interactive shells)

.EXAMPLE
  .\scripts\dev-release.ps1
  .\scripts\dev-release.ps1 -Action Stable -Version 1.0.0 -Yes
  .\scripts\dev-release.ps1 -Action PromoteStable -Version 1.0.0 -Yes
  .\scripts\dev-release.ps1 -Action PreRelease -Version 1.0.1
#>
param(
	[ValidateSet("", "PreRelease", "PromoteStable", "Stable")]
	[string]$Action = "",
	[string]$Version,
	[string]$UbuntuSsh,
	[string]$UbuntuLocalRepo = "~/peps",
	[string]$UbuntuHostRepo = "/mnt/hgfs/peps",
	[string]$MacSsh,
	[string]$MacLocalRepo = "~/peps",
	[string]$MacHostRepo = "",
	[switch]$SkipBuild,
	[switch]$SkipUbuntu,
	[switch]$SkipMacApp,
	[switch]$SkipGit,
	[switch]$Yes
)

$ErrorActionPreference = "Stop"

# Set $true to ship Mac-only (skip Windows jpackage + Ubuntu zip/.deb) during Mac iteration.
$MacOnlyRelease = $false
if ($MacOnlyRelease) {
	$SkipUbuntu = $true
	Write-Host "[dev-release] TEMPORARY Mac-only mode: Windows jpackage + Ubuntu zip/.deb are skipped."
	Write-Host "[dev-release] Mac assets: portable JAR zip (Windows) + .app zip/DMG (SSH to Mac VM)."
}

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

# Optional: scripts/dev-release.local.ps1 (gitignored) remembers last choices:
#   $PepsUbuntuSsh = "user@host"
#   $PepsMacSsh = "user@host"
#   $PepsLastVersion = "0.0.10"
$PepsUbuntuSsh = $null
$PepsMacSsh = $null
$PepsLastVersion = $null
$localConfig = Join-Path $PSScriptRoot "dev-release.local.ps1"
if (Test-Path $localConfig) {
	. $localConfig
}

function Write-Status([string]$Message) {
	$ts = Get-Date -Format "HH:mm:ss"
	Write-Host "[$ts] $Message"
}

# Coarse overall progress (same Write-Progress UI as Compress-Archive). Not byte-accurate.
$script:ReleaseProgressId = 1
$script:ReleaseProgressStep = 0
$script:ReleaseProgressTotal = 1
$script:ReleaseProgressActivity = "PEPS release"

function Initialize-ReleaseProgress {
	param(
		[string]$Activity,
		[string[]]$Steps
	)
	$script:ReleaseProgressActivity = $Activity
	$script:ReleaseProgressStep = 0
	$script:ReleaseProgressTotal = [Math]::Max(1, $Steps.Count)
	$script:ReleaseProgressSteps = $Steps
	Write-Progress -Id $script:ReleaseProgressId -Activity $Activity -Status "Starting…" -PercentComplete 0
}

function Update-ReleaseProgress {
	param([string]$Status)
	$script:ReleaseProgressStep++
	$step = [Math]::Min($script:ReleaseProgressStep, $script:ReleaseProgressTotal)
	$pct = [int][Math]::Floor(100.0 * $step / $script:ReleaseProgressTotal)
	if ($pct -gt 100) { $pct = 100 }
	$label = if ($Status) { $Status } elseif ($script:ReleaseProgressSteps -and ($step -le $script:ReleaseProgressSteps.Count)) {
		$script:ReleaseProgressSteps[$step - 1]
	} else {
		"Working…"
	}
	Write-Progress -Id $script:ReleaseProgressId `
		-Activity $script:ReleaseProgressActivity `
		-Status ("[{0}/{1}] {2}" -f $step, $script:ReleaseProgressTotal, $label) `
		-PercentComplete $pct
	Write-Status $label
}

function Complete-ReleaseProgress {
	param([string]$Status = "Done")
	Write-Progress -Id $script:ReleaseProgressId `
		-Activity $script:ReleaseProgressActivity `
		-Status $Status `
		-PercentComplete 100 `
		-Completed
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

function Update-ReadmeDownloads([string]$NewVersion) {
	# Keep README ## Download and Install links in sync with release asset names.
	$readmePath = Join-Path $RepoRoot "README.md"
	if (-not (Test-Path -LiteralPath $readmePath)) {
		throw "Missing README.md at $readmePath"
	}
	$base = "https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download"
	$winExe = "PostgreSQLExplainPlanStudio-$NewVersion-windows-x64-installer.exe"
	$winZip = "PostgreSQLExplainPlanStudio-$NewVersion-windows-x64.zip"
	$linuxZip = "PostgreSQLExplainPlanStudio-$NewVersion-linux-x64.zip"
	$linuxDeb = "PostgreSQLExplainPlanStudio-$NewVersion-linux-x64.deb"
	$winIcon = '<img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" />'
	$linuxIcon = '<img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" />'
	$macIcon = '<img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" />'
	$macJar = "PostgreSQLExplainPlanStudio-$NewVersion-jar.zip"
	# Catalina Intel release VM produces mac-x64; arm64 can be added later if built.
	$macAppZip = "PostgreSQLExplainPlanStudio-$NewVersion-mac-x64.zip"
	$macDmg = "PostgreSQLExplainPlanStudio-$NewVersion-mac-x64.dmg"
	# HTML lists (one <ul> per download) keep consistent disc bullets and a modest gap
	# between major items. Markdown list + <br>/&nbsp; hacks break markers or spacing.
	$downloadBlock = @"
## Download and Install

<ul>
<li>
$winIcon Windows installer: <a href="$base/$winExe">$winExe</a>
</li>
</ul>

<ul>
<li>
$winIcon Windows zip: <a href="$base/$winZip">$winZip</a>
</li>
</ul>

<ul>
<li>
$linuxIcon Linux zip: <a href="$base/$linuxZip">$linuxZip</a>
<br/>
To install the <code>linux-x64.zip</code> on Linux:
<ul>
<li>Unzip file</li>
<li>Open a terminal</li>
<li>Navigate to the unzipped <code>PostgreSQLExplainPlanStudio/bin</code> directory</li>
<li><code>chmod +x PostgreSQLExplainPlanStudio</code></li>
<li>To launch: <code>./PostgreSQLExplainPlanStudio</code></li>
</ul>
</li>
</ul>

<ul>
<li>
$linuxIcon Linux <code>.deb</code>: <a href="$base/$linuxDeb">$linuxDeb</a>
<br/>
To install the <code>.deb</code> on Linux:
<ul>
<li>Navigate to the directory with the downloaded <code>.deb</code> file</li>
<li><code>sudo apt install ./$linuxDeb</code></li>
<li>If apt prints an <code>N:</code> note about <code>_apt</code> / Permission denied while installing from your home folder (for example <code>Downloads</code>), you can ignore it when the package finishes with <code>Setting up postgresql-explain-plan-studio</code></li>
<li>To launch: open <strong>PostgreSQL Explain Plan Studio</strong> from the applications menu, or run <code>PostgreSQLExplainPlanStudio</code></li>
</ul>
</li>
</ul>

<ul>
<li>
$macIcon macOS disk image: <a href="$base/$macDmg">$macDmg</a>
<br/>
To install the <code>.dmg</code> on macOS:
<ul>
<li>Open the <code>.dmg</code></li>
<li>Drag <code>PostgreSQL Explain Plan Studio.app</code> to Applications</li>
<li>Eject the disk image when finished</li>
<li>First launch of an unsigned build: right-click the app in Applications / Open / Open</li>
<li>If macOS says the app is <strong>damaged</strong> (Gatekeeper quarantine on unsigned downloads), run:<br/><code>xattr -dr com.apple.quarantine "/Applications/PostgreSQL Explain Plan Studio.app"</code><br/>then open the app again</li>
</ul>
</li>
</ul>

<ul>
<li>
$macIcon macOS app zip: <a href="$base/$macAppZip">$macAppZip</a>
<br/>
To install the <code>x64.zip</code> on macOS:
<ul>
<li>Unzip file</li>
<li>Drag <code>PostgreSQL Explain Plan Studio.app</code> to Applications</li>
<li>No separate Java install needed (JRE is bundled)</li>
<li>First launch of an unsigned build: right-click the app in Applications / Open / Open</li>
<li>If macOS says the app is <strong>damaged</strong> (Gatekeeper quarantine on unsigned downloads), run:<br/><code>xattr -dr com.apple.quarantine "/Applications/PostgreSQL Explain Plan Studio.app"</code><br/>then open the app again</li>
</ul>
</li>
</ul>

<ul>
<li>
Portable JAR (any OS with Java 17+): <a href="$base/$macJar">$macJar</a>
<br/>
To install the portable <code>jar.zip</code>:
<ul>
<li><strong>macOS Catalina 10.15</strong>
<ul>
<li>Current Temurin 17 builds break the GUI (<code>JavaRuntimeSupport</code>). Use Temurin <strong>17.0.8</strong> Intel instead:
<ul>
<li>Download <a href="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%2B7/OpenJDK17U-jdk_x64_mac_hotspot_17.0.8_7.pkg">OpenJDK17U-jdk_x64_mac_hotspot_17.0.8_7.pkg</a></li>
<li>Install the <code>.pkg</code></li>
<li>In Terminal, put Temurin ahead of macOS's <code>java</code> stub:<br/>
<code>export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home</code><br/>
<code>export PATH=`"`$JAVA_HOME/bin:`$PATH`"</code></li>
<li>Confirm: <code>java -version</code> shows <code>17.0.8</code></li>
</ul>
</li>
<li>Unzip the <code>jar.zip</code> file</li>
<li>Open a terminal and navigate to the unzipped directory</li>
<li><code>chmod +x peps.sh</code> then <code>./peps.sh</code> (or <code>java -jar postgresql-explain-plan-studio-$NewVersion.jar</code>)</li>
</ul>
</li>
<li><strong>Other macOS</strong>
<ul>
<li>Install <a href="https://adoptium.net/temurin/releases/?version=17">Java 17 or later</a> (Intel or Apple Silicon)</li>
<li>Unzip the <code>jar.zip</code> file</li>
<li>Open a terminal and navigate to the unzipped directory</li>
<li><code>chmod +x peps.sh</code> then <code>./peps.sh</code> (or <code>java -jar postgresql-explain-plan-studio-$NewVersion.jar</code>)</li>
</ul>
</li>
<li><strong>Windows</strong>
<ul>
<li>Install <a href="https://adoptium.net/temurin/releases/?version=17">Java 17 or later</a></li>
<li>Unzip the <code>jar.zip</code> file</li>
<li>Open Command Prompt or PowerShell and navigate to the unzipped directory</li>
<li><code>java -jar postgresql-explain-plan-studio-$NewVersion.jar</code></li>
</ul>
</li>
<li><strong>Linux</strong>
<ul>
<li>Install <a href="https://adoptium.net/temurin/releases/?version=17">Java 17 or later</a></li>
<li>Unzip the <code>jar.zip</code> file</li>
<li>Open a terminal and navigate to the unzipped directory</li>
<li><code>java -jar postgresql-explain-plan-studio-$NewVersion.jar</code> (or <code>chmod +x peps.sh</code> then <code>./peps.sh</code>)</li>
</ul>
</li>
</ul>
</li>
</ul>

All [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases).

**Stable** is the GitHub **Latest** release (links above). **Pre-release** builds are marked Pre-release on the [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases) page and do not replace Latest.
"@
	$raw = Get-Content -LiteralPath $readmePath -Raw
	# Normalize newlines for a reliable match, then restore Windows CRLF on write.
	$normalized = $raw -replace "`r`n", "`n"
	# Match "## Download" or "## Download and Install" through All releases, plus the
	# Stable/Pre-release blurb when present.
	$pattern = '(?s)## Download(?: and Install)?\n.*?All \[releases\]\(https://github\.com/wmeitzen/postgresql-explain-plan-studio/releases\)\.\n(?:\n\*\*Stable\*\* is the GitHub \*\*Latest\*\* release \(links above\)\. \*\*Pre-release\*\* builds are marked Pre-release on the \[releases\]\(https://github\.com/wmeitzen/postgresql-explain-plan-studio/releases\) page and do not replace Latest\.\n)?'
	$replacement = ($downloadBlock -replace "`r`n", "`n") + "`n"
	if ($normalized -notmatch $pattern) {
		throw "Failed to locate README ## Download and Install section to update for version $NewVersion"
	}
	$updated = [regex]::Replace($normalized, $pattern, [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $replacement }, 1)
	$updated = $updated -replace "`n", "`r`n"
	Set-Content -LiteralPath $readmePath -Value $updated -NoNewline
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
	return "administrator@192.168.163.156"
}

function Get-DefaultMacSsh {
	if (-not [string]::IsNullOrWhiteSpace($env:PEPS_MAC_SSH)) {
		return $env:PEPS_MAC_SSH.Trim()
	}
	if (-not [string]::IsNullOrWhiteSpace($PepsMacSsh)) {
		return $PepsMacSsh.Trim()
	}
	return "macosx@192.168.163.164"
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
	# Working tree holds the candidate version (Stable or Pre-release); do not auto-bump here.
	if ([string]::IsNullOrWhiteSpace($PomVersion)) {
		return $PomVersion
	}
	return $PomVersion.Trim()
}

function Resolve-ReleaseAction {
	param([string]$Explicit)
	if (-not [string]::IsNullOrWhiteSpace($Explicit)) {
		$normalized = $Explicit.Trim()
		if ($normalized -match '^(?i)pre-?release$') { return "PreRelease" }
		if ($normalized -match '^(?i)promote(-?stable)?$') { return "PromoteStable" }
		if ($normalized -match '^(?i)stable$') { return "Stable" }
		throw "Invalid -Action '$Explicit'. Use PreRelease, PromoteStable, or Stable."
	}
	Write-Host ""
	Write-Host "Release action:"
	Write-Host "  (1) Publish Pre-release   — build/upload as GitHub Pre-release (Latest unchanged)"
	Write-Host "  (2) Promote Pre-release (v N.N.N) to Stable — publish as official Latest; then bump to next patch"
	Write-Host "  (3) Publish to Stable     — publish as official Latest; keep working version (no bump)"
	$choice = Read-Host "Choose [1/2/3]"
	switch -Regex ($choice.Trim()) {
		'^(1|pre)$' { return "PreRelease" }
		'^(2|promote)$' { return "PromoteStable" }
		'^(3|stable)$' { return "Stable" }
		default { throw "Invalid choice '$choice'. Enter 1, 2, or 3." }
	}
}

function Save-DevReleaseLocalConfig {
	param(
		[string]$UbuntuSshTarget,
		[string]$MacSshTarget,
		[string]$LastVersion
	)
	$ssh = if (-not [string]::IsNullOrWhiteSpace($UbuntuSshTarget)) {
		$UbuntuSshTarget.Trim()
	} else {
		Get-DefaultUbuntuSsh
	}
	$mac = if (-not [string]::IsNullOrWhiteSpace($MacSshTarget)) {
		$MacSshTarget.Trim()
	} else {
		Get-DefaultMacSsh
	}
	$ver = if (-not [string]::IsNullOrWhiteSpace($LastVersion)) {
		$LastVersion.Trim()
	} else {
		""
	}
	$lines = @(
		"# Local defaults for scripts/dev-release.ps1 (gitignored).",
		"# Written automatically when you confirm a release run.",
		"`$PepsUbuntuSsh = `"$ssh`"",
		"`$PepsMacSsh = `"$mac`""
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

function Resolve-MacSshTarget {
	param([string]$Explicit)
	if (-not [string]::IsNullOrWhiteSpace($Explicit)) {
		return $Explicit.Trim()
	}
	$defaultTarget = Get-DefaultMacSsh
	$entered = Read-Host "Mac SSH target (user@host) [$defaultTarget]"
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
	$entered = Read-Host "Release version (pom is $PomVersion; Enter = current) [$defaultVersion]"
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
		[string]$AppVersion,
		[string]$ZipName,
		[string]$DebName,
		[string]$DestZip,
		[string]$DestDeb
	)

	if (-not (Get-Command ssh.exe -ErrorAction SilentlyContinue) -or
		-not (Get-Command scp.exe -ErrorAction SilentlyContinue)) {
		throw "OpenSSH client (ssh/scp) not found. Install 'OpenSSH Client' optional feature on Windows."
	}

	Write-Status "Building Ubuntu jpackage (zip + .deb) on $SshTarget …"

	$keyPath = Ensure-PepsSshKey
	$sshKeyOpts = @(
		"-i", $keyPath,
		"-o", "IdentitiesOnly=yes",
		"-o", "ConnectTimeout=20",
		"-o", "StrictHostKeyChecking=accept-new"
	)
	# ssh -n: do not read console stdin (avoids a pause after remote build until Enter).
	# Do not pass -n to scp — there it means dry-run.
	$sshBatch = $sshKeyOpts + @("-n", "-o", "BatchMode=yes", "-o", "PreferredAuthentications=publickey")
	$scpBatch = $sshKeyOpts + @("-o", "BatchMode=yes", "-o", "PreferredAuthentications=publickey")
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
	$remoteZipPathFile = "/tmp/peps-linux-zip-path.txt"
	$remoteDebPathFile = "/tmp/peps-linux-deb-path.txt"
	# Note: quoted "~/peps" does NOT expand in bash — expand leading ~ to $HOME explicitly.
	$remoteScript = @"
set -euo pipefail
HOST_REPO='$HostRepo'
LOCAL_REPO='$LocalRepo'
APP_VERSION='$AppVersion'
ZIP_NAME='$ZipName'
DEB_NAME='$DebName'
ZIP_PATH_FILE='$remoteZipPathFile'
DEB_PATH_FILE='$remoteDebPathFile'
MAX_GLIBC='2.31'
case "`$LOCAL_REPO" in
  "~") LOCAL_REPO="`$HOME" ;;
  "~/"*) LOCAL_REPO="`$HOME/`${LOCAL_REPO#~/}" ;;
esac

# Linux releases must build on Ubuntu 20.04 so the zip/.deb run on 20.04+ (glibc floor).
if [[ -r /etc/os-release ]]; then
  # shellcheck disable=SC1091
  . /etc/os-release
fi
if [[ "`${VERSION_ID:-}" != "20.04" ]]; then
  echo "Refusing Linux jpackage on this host: need Ubuntu 20.04, got '`${PRETTY_NAME:-unknown}' (VERSION_ID=`${VERSION_ID:-?})." >&2
  echo "Point -UbuntuSsh / scripts/dev-release.local.ps1 at the Ubuntu 20.04 release VM (not the newer sync VM)." >&2
  exit 1
fi

if ! command -v fakeroot >/dev/null 2>&1; then
  echo "fakeroot is required for jpackage --type deb." >&2
  echo "Install on the Ubuntu 20.04 VM: sudo apt update && sudo apt install -y fakeroot" >&2
  exit 1
fi

if [[ ! -f "`$HOST_REPO/pom.xml" ]]; then
  echo "Host share missing pom.xml at `$HOST_REPO" >&2
  echo "Check VMware shared folders / mount (/mnt/hgfs). Remount if needed:" >&2
  echo "  sudo mkdir -p /mnt/hgfs && sudo vmhgfs-fuse .host:/ /mnt/hgfs -o subtype=vmhgfs-fuse,allow_other" >&2
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

# Fail if the launcher needs a newer glibc than Ubuntu 20.04 (e.g. wrong build host).
APP_IMAGE="target/jpackage/PostgreSQLExplainPlanStudio"
LAUNCHER="`$APP_IMAGE/bin/PostgreSQLExplainPlanStudio"
if [[ ! -x "`$LAUNCHER" ]]; then
  echo "Missing jpackage launcher: `$LAUNCHER" >&2
  exit 1
fi
FLOOR=`$(objdump -T "`$LAUNCHER" 2>/dev/null | grep -oE 'GLIBC_[0-9.]+' | sed 's/GLIBC_//' | sort -uV | tail -1 || true)
if [[ -z "`$FLOOR" ]]; then
  echo "Could not determine glibc floor from `$LAUNCHER" >&2
  exit 1
fi
echo "Measured launcher glibc floor: `$FLOOR (ceiling `$MAX_GLIBC for Ubuntu 20.04)"
if [[ "`$(printf '%s\n%s\n' "`$MAX_GLIBC" "`$FLOOR" | sort -V | tail -1)" != "`$MAX_GLIBC" ]]; then
  echo "Refusing zip: launcher needs GLIBC_`$FLOOR which is newer than `$MAX_GLIBC (Ubuntu 20.04)." >&2
  exit 1
fi

# .deb from the app-image already verified above (bundled runtime + glibc floor).
rm -f target/*.deb
jpackage \
  --type deb \
  --app-image "`$APP_IMAGE" \
  --dest target \
  --name PostgreSQLExplainPlanStudio \
  --app-version "`$APP_VERSION" \
  --vendor 'William Meitzen' \
  --description 'PostgreSQL Explain Plan Studio' \
  --linux-package-name postgresql-explain-plan-studio \
  --linux-app-category database \
  --linux-shortcut \
  --linux-menu-group 'Development'
shopt -s nullglob
debs=(target/*.deb)
shopt -u nullglob
if [[ `${#debs[@]} -ne 1 ]]; then
  echo "Expected exactly one .deb in target/ after jpackage; found `${#debs[@]}." >&2
  printf '  %s\n' "`${debs[@]}" >&2
  exit 1
fi
mv -f "`${debs[0]}" "target/`$DEB_NAME"
DEB_ABS="`$(pwd)/target/`$DEB_NAME"
test -f "`$DEB_ABS"
# Drop libpcre3 / add *t64 alternatives so apt can install on Ubuntu 20.04–26+.
bash scripts/linux-deb-widen-depends.sh "`$DEB_ABS"

printf '%s\n' "`$ZIP_ABS" > "`$ZIP_PATH_FILE"
printf '%s\n' "`$DEB_ABS" > "`$DEB_PATH_FILE"
echo "Built `$ZIP_ABS"
echo "Built `$DEB_ABS"
"@
	[System.IO.File]::WriteAllText($remoteScriptPath, ($remoteScript -replace "`r`n", "`n"))

	try {
		Write-Status "Uploading build script and running jpackage on Ubuntu…"
		$remoteTmp = "/tmp/peps-ubuntu-jpackage.sh"
		$up = Invoke-ScpDirect -ArgumentList ($scpBatch + @($remoteScriptPath, "${SshTarget}:$remoteTmp"))
		if ($up -ne 0) { throw "scp of build script failed (exit $up)" }

		$build = Invoke-SshDirect -ArgumentList ($sshBatch + @($SshTarget, "bash $remoteTmp"))
		if ($build -ne 0) {
			throw "Ubuntu remote jpackage failed (exit $build). Check that /mnt/hgfs/peps is mounted and JDK 17 + rsync + fakeroot are installed."
		}

		function Get-RemotePathFromFile([string]$RemoteFile, [string]$Label) {
			$pathOutFile = Join-Path $env:TEMP ("peps-remote-" + $Label + "-path.txt")
			Remove-Item -Force $pathOutFile -ErrorAction SilentlyContinue
			$prev = $ErrorActionPreference
			$ErrorActionPreference = "SilentlyContinue"
			try {
				& ssh.exe @sshBatch $SshTarget "cat $RemoteFile" | Set-Content -Path $pathOutFile
				$pathCode = $LASTEXITCODE
			} finally {
				$ErrorActionPreference = $prev
			}
			if ($pathCode -ne 0 -or -not (Test-Path $pathOutFile) -or -not (Get-Content $pathOutFile -Raw).Trim()) {
				throw "Could not read remote $Label path from $RemoteFile"
			}
			return ((Get-Content $pathOutFile -Raw).Trim() -replace "`r", "" -replace "`n", "")
		}

		$resolvedZip = Get-RemotePathFromFile -RemoteFile $remoteZipPathFile -Label "zip"
		$resolvedDeb = Get-RemotePathFromFile -RemoteFile $remoteDebPathFile -Label "deb"

		Write-Status "Copying ${SshTarget}:$resolvedZip → $DestZip"
		New-Item -ItemType Directory -Force -Path (Split-Path $DestZip) | Out-Null
		$copyZip = Invoke-ScpDirect -ArgumentList ($scpBatch + @("${SshTarget}:$resolvedZip", $DestZip))
		if ($copyZip -ne 0) {
			throw "scp of Linux zip failed (exit $copyZip)"
		}
		if (-not (Test-Path $DestZip)) {
			throw "Linux zip was not copied to $DestZip"
		}

		Write-Status "Copying ${SshTarget}:$resolvedDeb → $DestDeb"
		$copyDeb = Invoke-ScpDirect -ArgumentList ($scpBatch + @("${SshTarget}:$resolvedDeb", $DestDeb))
		if ($copyDeb -ne 0) {
			throw "scp of Linux .deb failed (exit $copyDeb)"
		}
		if (-not (Test-Path $DestDeb)) {
			throw "Linux .deb was not copied to $DestDeb"
		}
	} finally {
		Remove-Item -Force $remoteScriptPath -ErrorAction SilentlyContinue
	}
}

function Build-MacAppPackage {
	param(
		[string]$SshTarget,
		[string]$HostRepo,
		[string]$LocalRepo,
		[string]$AppVersion,
		[string]$ReleaseTag,
		[string]$DestDir
	)

	if (-not (Get-Command ssh.exe -ErrorAction SilentlyContinue) -or
		-not (Get-Command scp.exe -ErrorAction SilentlyContinue)) {
		throw "OpenSSH client (ssh/scp) not found. Install 'OpenSSH Client' optional feature on Windows."
	}

	Write-Status "Building macOS .app on $SshTarget …"

	$keyPath = Ensure-PepsSshKey
	$sshKeyOpts = @(
		"-i", $keyPath,
		"-o", "IdentitiesOnly=yes",
		"-o", "ConnectTimeout=20",
		"-o", "StrictHostKeyChecking=accept-new"
	)
	# ssh -n: do not read console stdin (avoids a pause after remote build until Enter).
	# Do not pass -n to scp — there it means dry-run.
	$sshBatch = $sshKeyOpts + @("-n", "-o", "BatchMode=yes", "-o", "PreferredAuthentications=publickey")
	$scpBatch = $sshKeyOpts + @("-o", "BatchMode=yes", "-o", "PreferredAuthentications=publickey")
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
			throw "SSH key installed but key login still fails for $SshTarget (exit $probe). On the Mac check Remote Login (System Preferences → Sharing) and ~/.ssh/authorized_keys."
		}
	}
	Write-Status "SSH key auth OK for Mac — no more password prompts for this release"

	$remoteScriptPath = Join-Path $env:TEMP "peps-mac-jpackage.sh"
	$remoteZipPathFile = "/tmp/peps-mac-zip-path.txt"
	$remoteDmgPathFile = "/tmp/peps-mac-dmg-path.txt"
	$remoteArchFile = "/tmp/peps-mac-arch.txt"
	$hostRepoArg = if ($null -eq $HostRepo) { "" } else { $HostRepo }
	$remoteScript = @"
set -euo pipefail
HOST_REPO='$hostRepoArg'
LOCAL_REPO='$LocalRepo'
APP_VERSION='$AppVersion'
RELEASE_TAG='$ReleaseTag'
ZIP_PATH_FILE='$remoteZipPathFile'
DMG_PATH_FILE='$remoteDmgPathFile'
ARCH_FILE='$remoteArchFile'
# Expand leading ~ without bash `${var#…}` (PowerShell here-strings corrupt that form).
case "`$LOCAL_REPO" in
  "~") LOCAL_REPO="`$HOME" ;;
  "~/"*) LOCAL_REPO="`$HOME/`$(echo "`$LOCAL_REPO" | sed 's|^~/||')" ;;
esac
rm -f "`$ZIP_PATH_FILE" "`$DMG_PATH_FILE" "`$ARCH_FILE"

if [[ "`$(uname -s)" != "Darwin" ]]; then
  echo "Refusing macOS jpackage on non-Darwin host: `$(uname -s)" >&2
  exit 1
fi

if [[ -z "`${HOST_REPO}" || ! -f "`${HOST_REPO}/pom.xml" ]]; then
  for candidate in \
    "/Volumes/peps" \
    "/Volumes/VMware Shared Folders/peps" \
    "/mnt/hgfs/peps" \
    "/mnt/hgfs/PEPS"
  do
    if [[ -f "`${candidate}/pom.xml" ]]; then
      HOST_REPO="`$candidate"
      break
    fi
  done
fi

mkdir -p "`$LOCAL_REPO"
if [[ -n "`${HOST_REPO}" && -f "`${HOST_REPO}/pom.xml" ]]; then
  echo "Syncing from host share: `$HOST_REPO -> `$LOCAL_REPO"
  rsync -a --delete \
    --exclude '.git/' \
    --exclude 'target/' \
    --exclude '*.sqlite' \
    --exclude '.idea/' \
    "`$HOST_REPO/" "`$LOCAL_REPO/"
elif [[ -d "`$LOCAL_REPO/.git" ]]; then
  echo "No HGFS share with pom.xml; checking out `$RELEASE_TAG in `$LOCAL_REPO"
  cd "`$LOCAL_REPO"
  git fetch --tags origin
  git checkout -f "`$RELEASE_TAG"
else
  echo "Need a VMware share with pom.xml or a git checkout at `$LOCAL_REPO" >&2
  echo "Tried HOST_REPO='`${HOST_REPO:-}' and git at `$LOCAL_REPO" >&2
  exit 1
fi

cd "`$LOCAL_REPO"
if ! command -v java >/dev/null 2>&1 || ! command -v jpackage >/dev/null 2>&1; then
  if [[ -x /usr/libexec/java_home ]]; then
    export JAVA_HOME="`$(/usr/libexec/java_home -v 17 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)"
    if [[ -n "`${JAVA_HOME:-}" ]]; then
      export PATH="`$JAVA_HOME/bin:`$PATH"
    fi
  fi
fi
if ! command -v jpackage >/dev/null 2>&1; then
  echo "JDK 17+ with jpackage is required on the Mac (Temurin 17.0.8 recommended for Catalina)." >&2
  exit 1
fi

chmod +x mvnw scripts/pack-mac-app.sh 2>/dev/null || true
./scripts/pack-mac-app.sh

ARCH_RAW="`$(uname -m)"
case "`$ARCH_RAW" in
  x86_64) ARCH_LABEL="x64" ;;
  arm64|aarch64) ARCH_LABEL="arm64" ;;
  *) ARCH_LABEL="`$ARCH_RAW" ;;
esac
ZIP_ABS="`$(pwd)/target/PostgreSQLExplainPlanStudio-`${APP_VERSION}-mac-`${ARCH_LABEL}.zip"
DMG_ABS="`$(pwd)/target/PostgreSQLExplainPlanStudio-`${APP_VERSION}-mac-`${ARCH_LABEL}.dmg"
test -f "`$ZIP_ABS"
printf '%s\n' "`$ZIP_ABS" > "`$ZIP_PATH_FILE"
printf '%s\n' "`$ARCH_LABEL" > "`$ARCH_FILE"
if [[ -f "`$DMG_ABS" ]]; then
  printf '%s\n' "`$DMG_ABS" > "`$DMG_PATH_FILE"
  echo "Built `$DMG_ABS"
else
  : > "`$DMG_PATH_FILE"
  echo "No DMG (expected over SSH); .app zip is `$ZIP_ABS"
fi
echo "Built `$ZIP_ABS"
"@
	[System.IO.File]::WriteAllText($remoteScriptPath, ($remoteScript -replace "`r`n", "`n"))

	try {
		Write-Status "Uploading build script and running pack-mac-app.sh on Mac…"
		$remoteTmp = "/tmp/peps-mac-jpackage.sh"
		$up = Invoke-ScpDirect -ArgumentList ($scpBatch + @($remoteScriptPath, "${SshTarget}:$remoteTmp"))
		if ($up -ne 0) { throw "scp of Mac build script failed (exit $up)" }

		$build = Invoke-SshDirect -ArgumentList ($sshBatch + @($SshTarget, "bash $remoteTmp"))
		if ($build -ne 0) {
			throw "Mac remote pack-mac-app.sh failed (exit $build). Check SSH reachability, JDK 17 on the Mac, and that ~/peps is a share sync or git clone."
		}

		function Get-RemotePathFromFile([string]$RemoteFile, [string]$Label, [switch]$AllowEmpty) {
			$pathOutFile = Join-Path $env:TEMP ("peps-remote-mac-" + $Label + "-path.txt")
			Remove-Item -Force $pathOutFile -ErrorAction SilentlyContinue
			$prev = $ErrorActionPreference
			$ErrorActionPreference = "SilentlyContinue"
			try {
				& ssh.exe @sshBatch $SshTarget "cat $RemoteFile" | Set-Content -Path $pathOutFile
				$pathCode = $LASTEXITCODE
			} finally {
				$ErrorActionPreference = $prev
			}
			$raw = if (Test-Path $pathOutFile) { (Get-Content $pathOutFile -Raw) } else { "" }
			$trimmed = if ($null -eq $raw) { "" } else { $raw.Trim() -replace "`r", "" -replace "`n", "" }
			if ($pathCode -ne 0) {
				throw "Could not read remote Mac $Label path from $RemoteFile"
			}
			if (-not $AllowEmpty -and [string]::IsNullOrWhiteSpace($trimmed)) {
				throw "Empty remote Mac $Label path from $RemoteFile"
			}
			return $trimmed
		}

		$resolvedZip = Get-RemotePathFromFile -RemoteFile $remoteZipPathFile -Label "zip"
		$resolvedDmg = Get-RemotePathFromFile -RemoteFile $remoteDmgPathFile -Label "dmg" -AllowEmpty
		$archLabel = Get-RemotePathFromFile -RemoteFile $remoteArchFile -Label "arch"
		if ([string]::IsNullOrWhiteSpace($archLabel)) { $archLabel = "x64" }

		$destZipName = "PostgreSQLExplainPlanStudio-$AppVersion-mac-$archLabel.zip"
		$destDmgName = "PostgreSQLExplainPlanStudio-$AppVersion-mac-$archLabel.dmg"
		$destZip = Join-Path $DestDir $destZipName
		$destDmg = Join-Path $DestDir $destDmgName

		Write-Status "Copying ${SshTarget}:$resolvedZip → $destZip"
		New-Item -ItemType Directory -Force -Path $DestDir | Out-Null
		$copyZip = Invoke-ScpDirect -ArgumentList ($scpBatch + @("${SshTarget}:$resolvedZip", $destZip))
		if ($copyZip -ne 0) {
			throw "scp of Mac .app zip failed (exit $copyZip)"
		}
		if (-not (Test-Path $destZip)) {
			throw "Mac .app zip was not copied to $destZip"
		}

		$copiedDmg = $null
		if (-not [string]::IsNullOrWhiteSpace($resolvedDmg)) {
			Write-Status "Copying ${SshTarget}:$resolvedDmg → $destDmg"
			$copyDmg = Invoke-ScpDirect -ArgumentList ($scpBatch + @("${SshTarget}:$resolvedDmg", $destDmg))
			if ($copyDmg -ne 0) {
				Write-Warning "scp of Mac DMG failed (exit $copyDmg); continuing with .app zip only"
			} elseif (Test-Path $destDmg) {
				$copiedDmg = $destDmg
			}
		}

		return [pscustomobject]@{
			Arch    = $archLabel
			AppZip  = $destZip
			Dmg     = $copiedDmg
		}
	} finally {
		Remove-Item -Force $remoteScriptPath -ErrorAction SilentlyContinue
	}
}

# --- action + version ---
$ReleaseAction = Resolve-ReleaseAction -Explicit $Action
$IsPrerelease = ($ReleaseAction -eq "PreRelease")
$IsStableChannel = ($ReleaseAction -eq "Stable" -or $ReleaseAction -eq "PromoteStable")
$BumpAfterStable = ($ReleaseAction -eq "PromoteStable")
Write-Status "Release action: $ReleaseAction"

$current = Get-CurrentPomVersion
# (3) Publish to Stable: prefer staying on 1.0.0 for the public-launch cut.
$versionPromptDefault = if ($ReleaseAction -eq "Stable") { "1.0.0" } else { $current }
$Version = Resolve-ReleaseVersion -Explicit $Version -PomVersion $versionPromptDefault
$Version = $Version.Trim()
if ($Version.StartsWith("v") -or $Version.StartsWith("V")) {
	$Version = $Version.Substring(1)
}
if ($Version -notmatch '^\d+\.\d+\.\d+([.-][A-Za-z0-9.-]+)?$') {
	throw "Invalid version '$Version'. Use forms like 1.0.0"
}
$Tag = "v$Version"
$WindowsZipName = "PostgreSQLExplainPlanStudio-$Version-windows-x64.zip"
$WindowsExeName = "PostgreSQLExplainPlanStudio-$Version-windows-x64-installer.exe"
$LinuxZipName = "PostgreSQLExplainPlanStudio-$Version-linux-x64.zip"
$LinuxDebName = "PostgreSQLExplainPlanStudio-$Version-linux-x64.deb"
$MacJarName = "PostgreSQLExplainPlanStudio-$Version-jar.zip"
# Default Intel Catalina names; Build-MacAppPackage may produce mac-arm64 instead.
$MacAppZipName = "PostgreSQLExplainPlanStudio-$Version-mac-x64.zip"
$MacDmgName = "PostgreSQLExplainPlanStudio-$Version-mac-x64.dmg"
$WindowsZip = Join-Path $RepoRoot "target\$WindowsZipName"
$WindowsExe = Join-Path $RepoRoot "target\$WindowsExeName"
$LinuxZip = Join-Path $RepoRoot "target\$LinuxZipName"
$LinuxDeb = Join-Path $RepoRoot "target\$LinuxDebName"
$MacJar = Join-Path $RepoRoot "target\$MacJarName"
$MacAppZip = Join-Path $RepoRoot "target\$MacAppZipName"
$MacDmg = Join-Path $RepoRoot "target\$MacDmgName"
$script:MacAppBuildResult = $null

Write-Status "Preparing release $Tag"
Test-GhAuth

if ($current -ne $Version) {
	Write-Status "Updating project version $current -> $Version"
	Set-ProjectVersion $Version
} else {
	Write-Status "pom.xml already at $Version"
}
if ($IsPrerelease) {
	Write-Status "Pre-release: leaving README Download links on Latest Stable"
} elseif ($IsStableChannel) {
	Write-Status "Updating README download links for Stable $Version"
	Update-ReadmeDownloads $Version
}

if (-not $SkipUbuntu -and -not $SkipBuild) {
	$UbuntuSsh = Resolve-UbuntuSshTarget -Explicit $UbuntuSsh
	Write-Status "Ubuntu SSH target: $UbuntuSsh"
} elseif (-not [string]::IsNullOrWhiteSpace($UbuntuSsh)) {
	$UbuntuSsh = $UbuntuSsh.Trim()
} else {
	$UbuntuSsh = Get-DefaultUbuntuSsh
}

if (-not $SkipMacApp -and -not $SkipBuild) {
	$MacSsh = Resolve-MacSshTarget -Explicit $MacSsh
	Write-Status "Mac SSH target: $MacSsh"
} elseif (-not [string]::IsNullOrWhiteSpace($MacSsh)) {
	$MacSsh = $MacSsh.Trim()
} else {
	$MacSsh = Get-DefaultMacSsh
}

Write-Host ""
Write-Host "This will:"
Write-Host "  - Action: $ReleaseAction$(if ($IsPrerelease) { ' (GitHub Pre-release)' } elseif ($IsStableChannel) { ' (Stable / Latest)' } else { '' })"
Write-Host "  - $(if ($SkipGit) { 'Skip git commit/tag/push' } else { "Commit/tag/push $Tag" })"
if ($MacOnlyRelease) {
	Write-Host "  - $(if ($SkipBuild) { 'Skip portable JAR zip build' } else { 'Build portable JAR zip (pack-jar.ps1)' })  [TEMPORARY Mac-only]"
	Write-Host "  - $(if ($SkipBuild -or $SkipMacApp) { 'Skip Mac .app build' } else { "Build macOS .app zip/DMG on $MacSsh" })  [TEMPORARY Mac-only]"
	Write-Host "  - Skip Windows jpackage  [TEMPORARY]"
	Write-Host "  - Skip Ubuntu zip/.deb  [TEMPORARY]"
} else {
	Write-Host "  - $(if ($SkipBuild) { 'Skip Windows build' } else { 'Build Windows jpackage zip (+ .exe installer if WiX is available)' })"
	Write-Host "  - $(if ($SkipBuild -or $SkipUbuntu) { 'Skip Ubuntu build' } else { "Build Ubuntu jpackage zip + .deb on $UbuntuSsh" })"
	Write-Host "  - $(if ($SkipBuild) { 'Skip portable JAR zip' } else { 'Build portable JAR zip (pack-jar.ps1)' })"
	Write-Host "  - $(if ($SkipBuild -or $SkipMacApp) { 'Skip Mac .app build' } else { "Build macOS .app zip/DMG on $MacSsh" })"
}
Write-Host "  - Upload available release assets to GitHub release $Tag"
if ($BumpAfterStable) {
	Write-Host "  - After Promote to Stable succeeds: bump working tree to next patch"
} elseif ($ReleaseAction -eq "Stable") {
	Write-Host "  - After Publish to Stable succeeds: keep working version at $Version (no bump)"
}
if (-not $Yes) {
	$confirm = Read-Host "Continue? [Y/n]"
	if ($confirm -match '^[Nn]') {
		Write-Status "Aborted."
		exit 1
	}
} else {
	Write-Status "Continuing without prompt (-Yes)"
}

Save-DevReleaseLocalConfig -UbuntuSshTarget $UbuntuSsh -MacSshTarget $MacSsh -LastVersion $Version

$progressSteps = @()
if (-not $SkipGit) { $progressSteps += "Commit, tag, and push $Tag" }
if (-not $SkipBuild) {
	if ($MacOnlyRelease) {
		$progressSteps += "Build portable JAR zip"
		if (-not $SkipMacApp) { $progressSteps += "Build macOS .app zip/DMG" }
	} else {
		$progressSteps += "Build Windows jpackage packages"
		if (-not $SkipUbuntu) { $progressSteps += "Build Ubuntu zip + .deb" }
		$progressSteps += "Build portable JAR zip"
		if (-not $SkipMacApp) { $progressSteps += "Build macOS .app zip/DMG" }
	}
}
$progressSteps += "Collect release assets"
$progressSteps += "Publish GitHub release $Tag"
Initialize-ReleaseProgress -Activity ("PEPS release $Tag") -Steps $progressSteps

try {
	# Push first so the Windows share (and any git consumers) see the release commit before Ubuntu syncs.
	if (-not $SkipGit) {
		Update-ReleaseProgress "Commit, tag, and push $Tag"
		git add pom.xml README.md scripts/dev-release.ps1 "src/main/java/com/williammeitzen/peps/gui/MainFrame.java"
		$status = git status --porcelain -- pom.xml README.md scripts/dev-release.ps1 "src/main/java/com/williammeitzen/peps/gui/MainFrame.java"
		if ($status) {
			$commit = Invoke-Native -Command { git commit -m "Release $Version" }
			if ($commit.Output) { $commit.Output | ForEach-Object { Write-Host $_ } }
			if ($commit.ExitCode -ne 0) { throw "git commit failed (exit $($commit.ExitCode))" }
		}
		# Force-move the annotated tag in place. Do NOT delete the remote tag first —
		# deleting it turns an existing GitHub release into an untagged Draft, which
		# never becomes Latest until published again.
		$existingTag = git tag -l $Tag
		if ($existingTag) {
			Write-Status "Tag $Tag already exists — moving with git tag -f (no remote delete)"
			Invoke-Native -Command { git tag -d $Tag } | Out-Null
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
		if ($MacOnlyRelease) {
			Update-ReleaseProgress "Build portable JAR zip"
			$packJar = Join-Path $PSScriptRoot "pack-jar.ps1"
			& $packJar
			if ($LASTEXITCODE -ne 0) {
				throw "pack-jar.ps1 failed with exit $LASTEXITCODE"
			}
			if (-not (Test-Path $MacJar)) {
				throw "Missing portable JAR zip: $MacJar"
			}

			if (-not $SkipMacApp) {
				Update-ReleaseProgress "Build macOS .app zip/DMG"
				$script:MacAppBuildResult = Build-MacAppPackage -SshTarget $MacSsh `
					-HostRepo $MacHostRepo -LocalRepo $MacLocalRepo `
					-AppVersion $Version -ReleaseTag $Tag `
					-DestDir (Join-Path $RepoRoot "target")
				$MacAppZip = $script:MacAppBuildResult.AppZip
				$MacAppZipName = Split-Path $MacAppZip -Leaf
				if ($script:MacAppBuildResult.Dmg) {
					$MacDmg = $script:MacAppBuildResult.Dmg
					$MacDmgName = Split-Path $MacDmg -Leaf
				}
			}
		} else {
			Update-ReleaseProgress "Build Windows jpackage packages"
			$mvnw = Join-Path $RepoRoot "mvnw.cmd"
			& $mvnw -P jpackage -DskipTests verify
			if ($LASTEXITCODE -ne 0) {
				throw "Windows mvnw jpackage failed with exit $LASTEXITCODE"
			}
			if (-not (Test-Path $WindowsZip)) {
				throw "Missing Windows zip: $WindowsZip"
			}
			if (-not (Test-Path $WindowsExe)) {
				Write-Warning "Windows installer missing: $WindowsExeName (install WiX Toolset for jpackage --type exe, or ignore if zip-only is enough)"
			}

			if (-not $SkipUbuntu) {
				Update-ReleaseProgress "Build Ubuntu zip + .deb"
				Build-UbuntuPackage -SshTarget $UbuntuSsh -HostRepo $UbuntuHostRepo `
					-LocalRepo $UbuntuLocalRepo -AppVersion $Version `
					-ZipName $LinuxZipName -DebName $LinuxDebName `
					-DestZip $LinuxZip -DestDeb $LinuxDeb
			}

			Update-ReleaseProgress "Build portable JAR zip"
			$packJar = Join-Path $PSScriptRoot "pack-jar.ps1"
			& $packJar -SkipBuild
			if ($LASTEXITCODE -ne 0) {
				throw "pack-jar.ps1 failed with exit $LASTEXITCODE"
			}
			if (-not (Test-Path $MacJar)) {
				throw "Missing portable JAR zip: $MacJar"
			}

			if (-not $SkipMacApp) {
				Update-ReleaseProgress "Build macOS .app zip/DMG"
				$script:MacAppBuildResult = Build-MacAppPackage -SshTarget $MacSsh `
					-HostRepo $MacHostRepo -LocalRepo $MacLocalRepo `
					-AppVersion $Version -ReleaseTag $Tag `
					-DestDir (Join-Path $RepoRoot "target")
				$MacAppZip = $script:MacAppBuildResult.AppZip
				$MacAppZipName = Split-Path $MacAppZip -Leaf
				if ($script:MacAppBuildResult.Dmg) {
					$MacDmg = $script:MacAppBuildResult.Dmg
					$MacDmgName = Split-Path $MacDmg -Leaf
				}
			}
		}
	}

	Update-ReleaseProgress "Collect release assets"
	$assets = @()
	if (-not $MacOnlyRelease) {
		if (Test-Path $WindowsZip) { $assets += $WindowsZip }
		if (Test-Path $WindowsExe) { $assets += $WindowsExe }
		if (Test-Path $LinuxZip) { $assets += $LinuxZip }
		if (Test-Path $LinuxDeb) { $assets += $LinuxDeb }
	}
	if (Test-Path $MacJar) { $assets += $MacJar }
	if (Test-Path $MacAppZip) { $assets += $MacAppZip }
	if (Test-Path $MacDmg) { $assets += $MacDmg }
	if ($assets.Count -eq 0) {
		throw "No release assets found in target\ for version $Version"
	}
	if (-not $MacOnlyRelease) {
		if (-not (Test-Path $WindowsZip)) {
			Write-Warning "Windows zip missing: $WindowsZipName"
		}
		if (-not (Test-Path $WindowsExe)) {
			Write-Warning "Windows installer missing: $WindowsExeName"
		}
		if (-not (Test-Path $LinuxZip) -and -not $SkipUbuntu) {
			throw "Linux zip missing after Ubuntu build: $LinuxZip"
		}
		if (-not (Test-Path $LinuxDeb) -and -not $SkipUbuntu) {
			throw "Linux .deb missing after Ubuntu build: $LinuxDeb"
		}
	}
	if (-not (Test-Path $MacJar)) {
		throw "Portable JAR zip missing: $MacJar"
	}
	if (-not $SkipMacApp -and -not $SkipBuild -and -not (Test-Path $MacAppZip)) {
		throw "Mac .app zip missing: $MacAppZip"
	}

	Write-Host "Assets to publish:"
	$assets | ForEach-Object { Write-Host "  $_" }

	$title = if ($IsPrerelease) { "v$Version (Pre-release)" } else { "v$Version" }
	# Heading only by default (install steps live in README.md). When editing an
	# existing release, keep the prior body (Improvements, etc.) and only rewrite
	# the first ## heading for Pre-release vs Stable.
	$notesHeading = if ($IsPrerelease) {
		"## PostgreSQL Explain Plan Studio v$Version (Pre-release)"
	} else {
		"## PostgreSQL Explain Plan Studio v$Version"
	}

	Update-ReleaseProgress "Publish GitHub release $Tag"
	$prevEa = $ErrorActionPreference
	$ErrorActionPreference = "Continue"
	$existing = gh release view $Tag 2>$null
	$releaseExists = ($LASTEXITCODE -eq 0)
	$ErrorActionPreference = $prevEa

	$notes = "$notesHeading`n"
	if ($releaseExists) {
		$prevBody = gh release view $Tag --json body -q .body 2>$null
		if (-not [string]::IsNullOrWhiteSpace($prevBody)) {
			$prevLines = $prevBody -split "`r?`n", -1
			$restStart = 0
			if ($prevLines.Count -gt 0 -and $prevLines[0] -match '^##\s+') {
				$restStart = 1
				if ($restStart -lt $prevLines.Count -and [string]::IsNullOrWhiteSpace($prevLines[$restStart])) {
					$restStart++
				}
			}
			$rest = ""
			if ($restStart -lt $prevLines.Count) {
				$rest = (($prevLines[$restStart..($prevLines.Count - 1)]) -join "`n").TrimEnd()
			}
			if (-not [string]::IsNullOrWhiteSpace($rest)) {
				$notes = "$notesHeading`n`n$rest`n"
				Write-Status "Keeping existing release notes for $Tag (heading updated)"
			}
		}
	}

	# PowerShell passes each line of a multiline string as a separate native argv
	# entry, so `gh ... --notes $notes` becomes "accepts 1 arg(s), received N".
	# Always use --notes-file instead.
	$notesFile = Join-Path ([System.IO.Path]::GetTempPath()) ("peps-release-notes-{0}.md" -f [guid]::NewGuid().ToString("n"))
	$utf8NoBom = New-Object System.Text.UTF8Encoding $false
	[System.IO.File]::WriteAllText($notesFile, $notes, $utf8NoBom)
	try {
		# Always clear draft on edit/create follow-up. Stable must not stay Draft or Latest
		# remains on an older release (seen with v1.0.0 after remote tag delete).
		$editFlags = @("--title", $title, "--notes-file", $notesFile, "--draft=false")
		if ($IsPrerelease) {
			$editFlags += "--prerelease"
		} else {
			# Existing releases keep their prior Latest pointer unless --latest is set
			# (PromoteStable on an already-published tag left Latest on an older release).
			$editFlags += "--prerelease=false"
			$editFlags += "--latest"
		}
		$createFlags = @()
		if ($IsPrerelease) {
			$createFlags += "--prerelease"
		} else {
			$createFlags += "--latest"
		}
		if ($releaseExists) {
			Write-Status "Release $Tag already exists — replacing assets"
			gh release upload $Tag @assets --clobber
			if ($LASTEXITCODE -ne 0) { throw "gh release upload failed" }
			gh release edit $Tag @editFlags | Out-Null
			if ($LASTEXITCODE -ne 0) { throw "gh release edit failed" }
		} else {
			Write-Status "Creating release $Tag$(if ($IsPrerelease) { ' (Pre-release)' } else { '' })"
			gh release create $Tag @assets --title $title --notes-file $notesFile @createFlags
			if ($LASTEXITCODE -ne 0) { throw "gh release create failed" }
			# Belt-and-suspenders: ensure we did not land as Draft.
			gh release edit $Tag @editFlags | Out-Null
			if ($LASTEXITCODE -ne 0) { throw "gh release edit (publish flags) failed" }
		}
	} finally {
		Remove-Item -LiteralPath $notesFile -Force -ErrorAction SilentlyContinue
	}
	$isDraftNow = gh release view $Tag --json isDraft -q .isDraft 2>$null
	$isPreNow = gh release view $Tag --json isPrerelease -q .isPrerelease 2>$null
	if ($isDraftNow -eq "true") {
		throw "GitHub release $Tag is still a Draft after publish (Latest will not move)."
	}
	if ($IsPrerelease) {
		if ($isPreNow -ne "true") {
			throw "Expected Pre-release flag on $Tag, got isPrerelease=$isPreNow"
		}
	} elseif ($isPreNow -eq "true") {
		throw "Expected Stable $Tag (not Pre-release), got isPrerelease=$isPreNow"
	}

	if ($BumpAfterStable) {
		$nextVersion = Get-NextPatchVersion -Version $Version
		if ($nextVersion -ne $Version) {
			Write-Status "Promote to Stable published — bumping working tree $Version -> $nextVersion"
			Set-ProjectVersion $nextVersion
			Save-DevReleaseLocalConfig -UbuntuSshTarget $UbuntuSsh -MacSshTarget $MacSsh -LastVersion $nextVersion
			if (-not $SkipGit) {
				git add pom.xml "src/main/java/com/williammeitzen/peps/gui/MainFrame.java"
				$statusBump = git status --porcelain -- pom.xml "src/main/java/com/williammeitzen/peps/gui/MainFrame.java"
				if ($statusBump) {
					$bumpCommit = Invoke-Native -Command { git commit -m "Bump version to $nextVersion" }
					if ($bumpCommit.Output) { $bumpCommit.Output | ForEach-Object { Write-Host $_ } }
					if ($bumpCommit.ExitCode -ne 0) { throw "git commit (post-Stable bump) failed (exit $($bumpCommit.ExitCode))" }
					$pushBump = Invoke-Native -Command { git push origin HEAD }
					if ($pushBump.Output) { $pushBump.Output | ForEach-Object { Write-Host $_ } }
					if ($pushBump.ExitCode -ne 0) { throw "git push (post-Stable bump) failed (exit $($pushBump.ExitCode))" }
				}
			}
		}
	} elseif ($ReleaseAction -eq "Stable") {
		Write-Status "Publish to Stable done — leaving working version at $Version"
		Save-DevReleaseLocalConfig -UbuntuSshTarget $UbuntuSsh -MacSshTarget $MacSsh -LastVersion $Version
	}

	Complete-ReleaseProgress "Done"
	Write-Status "Done: $(gh release view $Tag --json url -q .url)"
} catch {
	Complete-ReleaseProgress "Failed"
	throw
}
