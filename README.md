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

### Run from source

```powershell
.\mvnw.cmd -q compile exec:java
```

The app creates/opens `postgresql-explain-plan-studio.sqlite` in the **current working directory**.

To try with saved plans from the old PEG database, copy (do not commit):

```powershell
Copy-Item "\\maurice\Cee\docs\bitbucket\peg\peg.sqlite" ".\postgresql-explain-plan-studio.sqlite"
```

Then run from that directory and open a plan → **Show JSON Plan Diagram** (or select the Plan Diagram tab).

### Windows package with bundled runtime (jpackage)

```powershell
.\mvnw.cmd -P jpackage -DskipTests verify
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/PostgreSQLExplainPlanStudio.exe`
- `target/PostgreSQLExplainPlanStudio-<version>-windows-x64.zip`

## License

MIT — see [LICENSE](LICENSE).
