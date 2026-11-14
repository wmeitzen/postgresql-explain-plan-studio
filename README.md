# PostgreSQL Explain Plan Studio

Graphical viewer for PostgreSQL `EXPLAIN` plans.

## Download

- <img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" /> Windows installer: [PostgreSQLExplainPlanStudio-0.0.23-windows-x64-installer.exe](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.23-windows-x64-installer.exe)
- <img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" /> Windows zip: [PostgreSQLExplainPlanStudio-0.0.23-windows-x64.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.23-windows-x64.zip)
- <img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" /> Linux zip: [PostgreSQLExplainPlanStudio-0.0.23-linux-x64.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.23-linux-x64.zip)
- <img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" /> Linux `.deb`: [PostgreSQLExplainPlanStudio-0.0.23-linux-x64.deb](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.23-linux-x64.deb)

  To install the `.deb` on Linux: `sudo apt install ./PostgreSQLExplainPlanStudio-0.0.23-linux-x64.deb`
- <img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS app: [PostgreSQLExplainPlanStudio-0.0.23-mac-x64.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.23-mac-x64.zip)
  
  To install the `x64.zip` on macOS:
  - Unzip file
  - Drag `PostgreSQLExplainPlanStudio.app` to Applications
- <img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS (JAR): [PostgreSQLExplainPlanStudio-0.0.23-mac-jar.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.23-mac-jar.zip)

  To install the `jar.zip` on macOS:
  - Unzip file
  - Open a terminal
  - Navigate to the unzipped file's new directory
  - `chmod +x peps.sh`
  - Install JDK 17 or later
  - To launch: `./peps.sh`

All [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases).

## Build from source

### Requirements

- JDK 17 or newer (`java`, `javac`, and `jpackage` on `PATH`)
- Apache Maven 3.9+ (or the Maven Wrapper: `mvnw` / `mvnw.cmd`)

### Run from source (one-shot)

```powershell
.\mvnw.cmd -q compile exec:java
```

### A Few Recommendations About Development

I develop on Windows, and continuously cascade changes to a linux VM.

**Windows** Ã¢â‚¬â€ two terminals from the repo root:

```powershell
# Terminal 1 Ã¢â‚¬â€ keep running while developing
.\scripts\dev-watch.ps1

# Terminal 2 Ã¢â‚¬â€ launch when you want to test
.\scripts\dev-run.ps1

# Optional: trigger a rebuild by hand
# New-Item -ItemType File -Path RECOMPILENOW -Force
```

**Ubuntu / Linux**

One command to remember (after the first sync has created `~/peps`):

```bash
sudo apt install -y openjdk-17-jdk rsync   # once
# VMware Shared Folders on; share this repo (often named "peps")
bash ~/peps/scripts/dev-watch.sh
```

That mounts HGFS if `/mnt/hgfs` is empty, refreshes from the Windows share when `~/peps` is stale, syncs Windows Ã¢â€ â€™ `~/peps`, does an **initial compile**, then watches host changes / `RECOMPILENOW`. First boot only (no `~/peps` yet):

```bash
sudo mkdir -p /mnt/hgfs
sudo vmhgfs-fuse .host:/ /mnt/hgfs -o subtype=vmhgfs-fuse,allow_other
bash /mnt/hgfs/peps/scripts/dev-watch.sh
```

In a second terminal:

```bash
cd ~/peps
./scripts/dev-run.sh
```

If auto-detect fails:

```bash
export PEPS_HOST_REPO=/mnt/hgfs/peps
bash /mnt/hgfs/peps/scripts/dev-watch.sh
```

(`ubuntu-dev-up.sh` is a thin alias for the same script.)

**Start sync+build when the VM boots / you log in** (one-time install):

```bash
# Recommended: systemd user service (restarts if the watcher dies)
bash /mnt/hgfs/peps/scripts/install-dev-sync-autostart.sh

# Or classic cron @reboot:
bash /mnt/hgfs/peps/scripts/install-dev-sync-autostart.sh --cron
```

Useful checks:

```bash
systemctl --user status peps-dev-sync-watch.service
journalctl --user -u peps-dev-sync-watch.service -f
```

For a checkout that already lives only on the VM disk (no Windows share), the same `./scripts/dev-watch.sh` runs in local-only mode: initial package, then `RECOMPILENOW` polls.

### Package with bundled runtime (jpackage)

Requires JDK 17+ with `jpackage` on `PATH`.

**Windows**

For the setup `.exe` installer, also install [WiX Toolset](https://wixtoolset.org/) so `candle` / `light` are on `PATH`. Without WiX, the portable zip still builds; the `.exe` step warns and is skipped. Force-skip the installer with `-Djpackage.skip.exe=true`.

```powershell
.\mvnw.cmd -P jpackage -DskipTests verify
```

Or prompt for a version, build **Windows + Linux** packages, tag, and upload to GitHub (private repo OK if `gh auth login` is done). The Linux jpackage step SSHs to the **Ubuntu 20.04** release VM (default `administrator@192.168.163.156`), syncs from `/mnt/hgfs/peps`, builds the `linux-x64` zip **and** a `.deb` installer (`fakeroot` required), and refuses hosts that are not 20.04 / launchers needing glibc newer than 2.31. Day-to-day `RECOMPILENOW` sync can stay on a newer Ubuntu VM (e.g. 154).

```powershell
# optional local default (gitignored):
#   scripts\dev-release.local.ps1  Ã¢â€ â€™  $PepsUbuntuSsh = "administrator@192.168.163.156"
.\scripts\dev-release.ps1
# optional: .\scripts\dev-release.ps1 -Version 0.0.9 -UbuntuSsh administrator@192.168.163.156
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/PostgreSQLExplainPlanStudio.exe` (portable app image)
- `target/PostgreSQLExplainPlanStudio-<version>-windows-x64.zip`
- `target/PostgreSQLExplainPlanStudio-<version>-windows-x64-installer.exe` (WiX installer, when WiX is available)

**Ubuntu / Linux**

Build the published `linux-x64` zip on **Ubuntu 20.04** (not a newer host) so it stays runnable on 20.04+:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk rsync fakeroot
jpackage --version

# from a sync of the repo (e.g. ~/peps after rsync from /mnt/hgfs/peps)
./mvnw -P jpackage -DskipTests verify

# .deb from the verified app-image (dev-release.ps1 does this automatically)
jpackage --type deb \
  --app-image target/jpackage/PostgreSQLExplainPlanStudio \
  --dest target \
  --name PostgreSQLExplainPlanStudio \
  --app-version "$(grep -oP '(?<=<jpackage.appVersion>)[^<]+' pom.xml | head -1)" \
  --linux-package-name postgresql-explain-plan-studio \
  --linux-shortcut
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio`
- `target/PostgreSQLExplainPlanStudio-<version>-linux-x64.zip`
- `target/*.deb` (renamed by `dev-release.ps1` to `PostgreSQLExplainPlanStudio-<version>-linux-x64.deb`)

Run the app:

```bash
./target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio
# or after installing the .deb:
# sudo apt install ./PostgreSQLExplainPlanStudio-<version>-linux-x64.deb
```

Optional glibc check (should be Ã¢â€°Â¤ 2.31):

```bash
objdump -T target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio \
  | grep -oE 'GLIBC_[0-9.]+' | sort -Vu
```

**macOS â€” JAR zip (any Mac + JDK 17+)**

One JAR works on Intel and Apple Silicon when a JDK is installed. Build on Windows (no Mac required):

```powershell
.\scripts\pack-mac-jar.ps1
```

On the Mac: unzip `target/PostgreSQLExplainPlanStudio-<version>-mac-jar.zip`, then `chmod +x peps.sh && ./peps.sh`.

**macOS â€” `.app` (drag into Applications; bundled JRE)**

Must be built **on a Mac** with JDK 17+ (`jpackage`). Prefer a **local** checkout (e.g. `~/peps`), not a VMware HGFS share.

`.\scripts\dev-release.ps1` does this over SSH to the Mac VM (default `macosx@192.168.163.164`, override with `-MacSsh` / `$env:PEPS_MAC_SSH` / `scripts/dev-release.local.ps1`). Manual build:

```bash
chmod +x scripts/pack-mac-app.sh
./scripts/pack-mac-app.sh
```

Outputs:

- `target/jpackage-mac/PostgreSQLExplainPlanStudio.app`
- `target/PostgreSQLExplainPlanStudio-<version>-mac-x64.zip` (or `mac-arm64`) â€” unzip and drag the `.app` to Applications
- matching `.dmg` when built interactively (skipped over SSH to avoid the SetFile / Xcode CLT dialog)

Unsigned builds may need right-click â†’ Open the first time. Signing/notarization is still deferred.

## License

MIT Ã¢â‚¬â€ see [LICENSE](LICENSE).
