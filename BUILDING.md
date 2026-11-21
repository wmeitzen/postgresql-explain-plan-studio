# Building PostgreSQL Explain Plan Studio

## Requirements

- JDK 17 or newer (`java`, `javac`, and `jpackage` on `PATH` for packaged builds)
- Apache Maven 3.9+ (or the Maven Wrapper: `mvnw` / `mvnw.cmd`)

## Run from source

```powershell
.\mvnw.cmd -q compile exec:java
```

```bash
./mvnw -q compile exec:java
```

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

## Day-to-day development watch (optional)

On Windows, from the repo root:

```powershell
# Terminal 1 — keep running while developing
.\scripts\dev-watch.ps1

# Terminal 2 — launch when you want to test
.\scripts\dev-run.ps1

# Optional: trigger a rebuild by hand
# New-Item -ItemType File -Path RECOMPILENOW -Force
```

On Linux, after JDK 17 and `rsync` are installed, `./scripts/dev-watch.sh` syncs from a host share when present (for example VMware HGFS) into `~/peps`, does an initial compile, then watches for changes / `RECOMPILENOW`. In a second terminal:

```bash
cd ~/peps
./scripts/dev-run.sh
```

For a checkout that already lives only on the VM disk (no Windows share), the same `./scripts/dev-watch.sh` runs in local-only mode.

Optional boot autostart helpers live under `scripts/install-dev-sync-autostart.sh`.

## Package with bundled runtime (jpackage)

Requires JDK 17+ with `jpackage` on `PATH`.

### Windows

For the setup `.exe` installer, also install [WiX Toolset](https://wixtoolset.org/) so `candle` / `light` are on `PATH`. Without WiX, the portable zip still builds; the `.exe` step warns and is skipped. Force-skip the installer with `-Djpackage.skip.exe=true`.

```powershell
.\mvnw.cmd -P jpackage -DskipTests verify
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/PostgreSQLExplainPlanStudio.exe` (portable app image)
- `target/PostgreSQLExplainPlanStudio-<version>-windows-x64.zip`
- `target/PostgreSQLExplainPlanStudio-<version>-windows-x64-installer.exe` (WiX installer, when WiX is available)

### Ubuntu / Linux

Build the published `linux-x64` zip on **Ubuntu 20.04** (not a newer host) so it stays runnable on 20.04+:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk rsync fakeroot
jpackage --version

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

The `.deb` is built on **Ubuntu 20.04** (glibc ≤ 2.31) so the bundled launcher runs on 20.04+. After jpackage, `scripts/linux-deb-widen-depends.sh` rewrites `Depends` so **one** `.deb` installs with `apt` on Ubuntu 20.04 through current.

```bash
./target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio
# sudo apt install ./PostgreSQLExplainPlanStudio-<version>-linux-x64.deb
```

Optional glibc check (should be ≤ 2.31):

```bash
objdump -T target/jpackage/PostgreSQLExplainPlanStudio/bin/PostgreSQLExplainPlanStudio \
  | grep -oE 'GLIBC_[0-9.]+' | sort -Vu
```

### macOS — JAR zip (any Mac + JDK 17+)

One JAR works on Intel and Apple Silicon when a JDK is installed. Build on Windows (no Mac required):

```powershell
.\scripts\pack-mac-jar.ps1
```

On the Mac: unzip `target/PostgreSQLExplainPlanStudio-<version>-mac-jar.zip`, then `chmod +x peps.sh && ./peps.sh`.

### macOS — `.app` / `.dmg` (bundled JRE)

Must be built **on a Mac** with JDK 17+ (`jpackage`). Prefer a **local** checkout (for example `~/peps`), not a network/HGFS share.

```bash
chmod +x scripts/pack-mac-app.sh
./scripts/pack-mac-app.sh
```

Outputs:

- `target/jpackage-mac/PostgreSQL Explain Plan Studio.app`
- `target/PostgreSQLExplainPlanStudio-<version>-mac-x64.zip` (or `mac-arm64`) — unzip and drag the `.app` to Applications
- matching `.dmg` (hdiutil + Applications symlink; works over SSH, no Xcode CLT)

Unsigned builds may need right-click → Open the first time. If macOS says the app is **damaged**, clear quarantine after installing to Applications:

```bash
xattr -dr com.apple.quarantine "/Applications/PostgreSQL Explain Plan Studio.app"
```

Signing/notarization is still deferred.

## Maintainers: publishing a GitHub release

`scripts/dev-release.ps1` bumps version when needed, builds Windows + Linux + macOS assets (Linux/macOS steps SSH to your release VMs), tags, and uploads a GitHub release.

```powershell
.\scripts\dev-release.ps1
```

Interactive menu:

1. **Publish Pre-release** — GitHub Pre-release; does **not** rewrite README Download links (Latest stays on the last Stable).
2. **Promote to Stable** — full Latest release; rewrites README Download links; then auto-bumps the working tree to the next patch version.
3. **Publish to Stable** — full Latest release; rewrites README Download links; **keeps** the working version (default prompt `1.0.0`, no bump).

Non-interactive: `-Action PreRelease`, `-Action PromoteStable`, or `-Action Stable`, plus `-Version`, `-Yes`, and the usual `-Skip*` switches.

SSH targets and last version default from gitignored `scripts/dev-release.local.ps1` (`$PepsUbuntuSsh`, `$PepsMacSsh`, `$PepsLastVersion`), or `-UbuntuSsh` / `-MacSsh` / environment variables `PEPS_UBUNTU_SSH` / `PEPS_MAC_SSH`. Linux jpackage for published zips/`.deb` should run on **Ubuntu 20.04**.
