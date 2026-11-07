# PostgreSQL Explain Plan Studio

Graphical viewer for PostgreSQL `EXPLAIN` plans (migrated from the earlier PEG / PostgreSQL Explain Graph Swing app).

## Install the latest release (recommended)

Most users should download a prebuilt Windows package from
[Releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases)
(use the newest release).

1. Download `PostgreSQLExplainPlanStudio-*-windows-x64.zip`
2. Unzip it
3. Run `PostgreSQLExplainPlanStudio\PostgreSQLExplainPlanStudio.exe`

The app creates/opens `postgresql-explain-plan-studio.sqlite` in the **current working directory**.

### Windows SmartScreen

Unsigned downloads often show **Windows protected your PC**. Click **More info** → **Run anyway**, or right‑click the exe/zip → **Properties** → **Unblock** when present.

## Build from source

### Requirements

- JDK 17 or newer (`java`, `javac`, and `jpackage` on `PATH`)
- Apache Maven 3.9+ (or the Maven Wrapper: `mvnw` / `mvnw.cmd`)

### Run from source (one-shot)

```powershell
.\mvnw.cmd -q compile exec:java
```

The app creates/opens `postgresql-explain-plan-studio.sqlite` in the **current working directory**.

To try with saved plans from the old PEG database, copy (do not commit):

```powershell
Copy-Item "\\maurice\Cee\docs\bitbucket\peg\peg.sqlite" ".\postgresql-explain-plan-studio.sqlite"
```

Then run from that directory and open a plan → **Show JSON Plan Diagram** (or select the Plan Diagram tab).

### Dev watch + run (recommended while coding)

Rebuilds the shaded jar when the repo-root `RECOMPILENOW` semaphore file appears (Cursor creates it after a change batch). On Windows the watcher builds but leaves the flag so the Ubuntu VM can see it; `dev-sync-watch.sh` (systemd) syncs, builds in `~/peps`, then deletes `RECOMPILENOW`. Uses a lock file so launch waits if a build is in progress. Does **not** auto-restart a running PEPS window. jpackage remains the release path (below).

**Windows** — two terminals from the repo root:

```powershell
# Terminal 1 — keep running while developing
.\scripts\dev-watch.ps1

# Terminal 2 — launch when you want to test
.\scripts\dev-run.ps1

# Optional: trigger a rebuild by hand
# New-Item -ItemType File -Path RECOMPILENOW -Force
```

**Ubuntu / Linux (VMware guest)** — edit on Windows, build on a **local disk copy** (`~/peps`). Shared-folder watches under `/mnt/hgfs` are unreliable, so use sync+build:

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

```powershell
.\mvnw.cmd -P jpackage -DskipTests verify
```

Or prompt for a version, build **Windows + Ubuntu** zips, tag, and upload to GitHub (private repo OK if `gh auth login` is done). Ubuntu build runs over SSH against the VMware share:

```powershell
# one-time: SSH key auth to the VM (BatchMode), and optionally:
#   Set-Content scripts\dev-release.local.ps1 '$PepsUbuntuSsh = "administrator@192.168.x.x"'
.\scripts\dev-release.ps1
# optional: .\scripts\dev-release.ps1 -Version 0.0.9 -UbuntuSsh administrator@192.168.x.x
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/PostgreSQLExplainPlanStudio.exe`
- `target/PostgreSQLExplainPlanStudio-<version>-windows-x64.zip`

**Ubuntu / Linux**

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
# confirm jpackage is available (ships with the JDK)
jpackage --version

git clone https://github.com/wmeitzen/postgresql-explain-plan-studio.git
cd postgresql-explain-plan-studio
./mvnw -P jpackage -DskipTests verify
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio`
- `target/PostgreSQLExplainPlanStudio-<version>-linux-x64.zip`

Run the app:

```bash
./target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio
```

## License

MIT — see [LICENSE](LICENSE).
