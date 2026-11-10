# PostgreSQL Explain Plan Studio

Graphical viewer for PostgreSQL `EXPLAIN` plans (migrated from the earlier PEG / PostgreSQL Explain Graph Swing app).

## Install the latest release (recommended)

Most users should download a prebuilt package from
[Releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases)
(use the newest release).

**Windows**

- **Installer (recommended):** download `PostgreSQLExplainPlanStudio-*.exe`, run the setup wizard (per-user install). Upgrades replace the app; your plans stay in the data directory below.
- **Portable zip:** download `PostgreSQLExplainPlanStudio-*-windows-x64.zip`, unzip, run `PostgreSQLExplainPlanStudio\PostgreSQLExplainPlanStudio.exe`

**Ubuntu / Linux (x86_64 desktop)**

1. Download `PostgreSQLExplainPlanStudio-*-linux-x64.zip`
2. Unzip it (e.g. under `~/apps/`)
3. Run `PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio`

To upgrade Linux: replace the unzipped app tree with the new zip. Leave the data directory alone.

Linux releases are built on **Ubuntu 20.04** so the same zip runs on **Ubuntu 20.04+ desktop** (and other glibc x86_64 desktops at least that new). Not for headless Server without a GUI stack, and not for Alpine/musl.

### User data directory (SQLite)

Plans live in `postgresql-explain-plan-studio.sqlite` **outside** the install/unzip folder so upgrades do not overwrite them:

| OS | Default location |
|----|------------------|
| Windows | `%APPDATA%\PostgreSQLExplainPlanStudio\` |
| Linux / macOS | `~/.local/share/postgresql-explain-plan-studio/` (or `$XDG_DATA_HOME/postgresql-explain-plan-studio/`) |

Override with env `PEPS_DATA_DIR` or `-Dpeps.data.dir=<path>`. If an older DB exists in the current working directory and the data dir has none yet, PEPS copies it once into the data dir on startup. Schema/feature upgrades run automatically in the app (`Database.init`), not in the installer.

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

SQLite is opened under the [user data directory](#user-data-directory-sqlite) (not the repo CWD), unless you set `PEPS_DATA_DIR`.

To try with saved plans from the old PEG database, copy (do not commit) into the data dir — or into CWD once and let PEPS migrate on first launch:

```powershell
# Option A: place directly in the Windows data dir
Copy-Item "\\maurice\Cee\docs\bitbucket\peg\peg.sqlite" "$env:APPDATA\PostgreSQLExplainPlanStudio\postgresql-explain-plan-studio.sqlite"

# Option B: legacy CWD file (migrated once into the data dir on startup)
Copy-Item "\\maurice\Cee\docs\bitbucket\peg\peg.sqlite" ".\postgresql-explain-plan-studio.sqlite"
$env:PEPS_DATA_DIR = (Resolve-Path .).Path   # optional: keep DB in-repo while developing
```

Then run and open a plan → **Show JSON Plan Diagram** / **Show Text Plan Diagram** (or select the Plan Diagram tab).

Plan Source accepts pasted **JSON** (`FORMAT JSON`) or classic **text** `EXPLAIN` output. The field label and Show/Execute button titles switch based on a content sniff. Text plans stay stored as raw text; PEPS converts them to JSON on the fly only for diagram generation and Compare Plans.

**Shape parity:** text EXPLAIN is normalized to the same operator-tree shape as `FORMAT JSON` (Node Type, Join Type, Parallel Aware, Aggregate Strategy, ModifyTable Operation, nesting). Text may omit extra JSON-only properties; the diagram icons and layout should still match. Offline converter tests always run; live Postgres shape checks (`ExplainFormatShapeParityTest`, `SqlitePlanFormatParityHarnessTest`) skip when the DB from SQLite connection fields is unreachable.

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
- `target/PostgreSQLExplainPlanStudio-<version>.exe` (WiX installer, when WiX is available)

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
