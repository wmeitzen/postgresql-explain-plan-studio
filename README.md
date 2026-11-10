# PostgreSQL Explain Plan Studio

Graphical viewer for PostgreSQL `EXPLAIN` plans.

## Download

- Windows installer: [PostgreSQLExplainPlanStudio-0.0.16-windows-installer.exe](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.16-windows-installer.exe)
- Windows zip: [PostgreSQLExplainPlanStudio-0.0.16-windows-x64.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.16-windows-x64.zip)
- Linux zip: [PostgreSQLExplainPlanStudio-0.0.16-linux-x64.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-0.0.16-linux-x64.zip)

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

**Windows** — two terminals from the repo root:

```powershell
# Terminal 1 — keep running while developing
.\scripts\dev-watch.ps1

# Terminal 2 — launch when you want to test
.\scripts\dev-run.ps1

# Optional: trigger a rebuild by hand
# New-Item -ItemType File -Path RECOMPILENOW -Force
```

**Ubuntu / Linux**

```bash
sudo apt install -y openjdk-17-jdk rsync
# Shared folders must be enabled in VMware (Windows host path = this repo).
# Confirm the share is visible:
#   ls /mnt/hgfs
#   vmware-hgfsclient

# Terminal 1 — sync Windows → ~/peps and recompile whenever host sources change
# (first run can be started from the share until ~/peps has the script)
bash /mnt/hgfs/<share>/.../postgresql-explain-plan-studio/scripts/dev-sync-watch.sh
# later, after the first sync:
#   ~/peps/scripts/dev-sync-watch.sh

# Terminal 2 — launch when you want to test
cd ~/peps
./scripts/dev-run.sh
```

If auto-detect fails, set the host path explicitly:

```bash
export PEPS_HOST_REPO=/mnt/hgfs/peps
~/peps/scripts/dev-sync-watch.sh
```

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

For a checkout that already lives only on the VM disk (no Windows share), use `./scripts/dev-watch.sh` instead — it polls for `RECOMPILENOW` and rebuilds the same way as the Windows watcher.

### Package with bundled runtime (jpackage)

Requires JDK 17+ with `jpackage` on `PATH`.

**Windows**

For the setup `.exe` installer, also install [WiX Toolset](https://wixtoolset.org/) so `candle` / `light` are on `PATH`. Without WiX, the portable zip still builds; the `.exe` step warns and is skipped. Force-skip the installer with `-Djpackage.skip.exe=true`.

```powershell
.\mvnw.cmd -P jpackage -DskipTests verify
```

Or prompt for a version, build **Windows + Linux** packages, tag, and upload to GitHub (private repo OK if `gh auth login` is done). The Linux jpackage step SSHs to the **Ubuntu 20.04** release VM (default `administrator@192.168.163.156`), syncs from `/mnt/hgfs/peps`, and refuses hosts that are not 20.04 / launchers needing glibc newer than 2.31. Day-to-day `RECOMPILENOW` sync can stay on a newer Ubuntu VM (e.g. 154).

```powershell
# optional local default (gitignored):
#   scripts\dev-release.local.ps1  →  $PepsUbuntuSsh = "administrator@192.168.163.156"
.\scripts\dev-release.ps1
# optional: .\scripts\dev-release.ps1 -Version 0.0.9 -UbuntuSsh administrator@192.168.163.156
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/PostgreSQLExplainPlanStudio.exe` (portable app image)
- `target/PostgreSQLExplainPlanStudio-<version>-windows-x64.zip`
- `target/PostgreSQLExplainPlanStudio-<version>-windows-installer.exe` (WiX installer, when WiX is available)

**Ubuntu / Linux**

Build the published `linux-x64` zip on **Ubuntu 20.04** (not a newer host) so it stays runnable on 20.04+:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk rsync
jpackage --version

# from a sync of the repo (e.g. ~/peps after rsync from /mnt/hgfs/peps)
./mvnw -P jpackage -DskipTests verify
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio`
- `target/PostgreSQLExplainPlanStudio-<version>-linux-x64.zip`

Run the app:

```bash
./target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio
```

Optional glibc check (should be ≤ 2.31):

```bash
objdump -T target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio \
  | grep -oE 'GLIBC_[0-9.]+' | sort -Vu
```

## License

MIT — see [LICENSE](LICENSE).
