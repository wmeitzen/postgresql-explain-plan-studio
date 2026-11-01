# PostgreSQL Explain Plan Studio

Graphical viewer for PostgreSQL `EXPLAIN` plans (migrated from the earlier PEG / PostgreSQL Explain Graph Swing app).

## Requirements

- JDK 17 or newer (`java`, `javac`, and `jpackage` on `PATH`)
- Apache Maven 3.9+ (or the Maven Wrapper: `mvnw` / `mvnw.cmd`)

## Build and run

```powershell
.\mvnw.cmd -q compile exec:java
```

The app creates/opens `postgresql-explain-plan-studio.sqlite` in the **current working directory**.

To try with saved plans from the old PEG database, copy (do not commit):

```powershell
Copy-Item "\\maurice\Cee\docs\bitbucket\peg\peg.sqlite" ".\postgresql-explain-plan-studio.sqlite"
```

Then run from that directory and open a plan → **Show JSON Plan Diagram** (or select the Plan Diagram tab).

## Windows package with bundled runtime (jpackage)

```powershell
.\mvnw.cmd -P jpackage -DskipTests verify
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/PostgreSQLExplainPlanStudio.exe`
- `target/PostgreSQLExplainPlanStudio-0.0.2-windows-x64.zip`

### Windows SmartScreen

Unsigned downloads often show **Windows protected your PC**. Click **More info** → **Run anyway**, or right‑click the exe/zip → **Properties** → **Unblock** when present.

## Source vs releases

- **Source:** clone this repo (developers need a JDK)
- **Release:** download the Windows zip from [Releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases) — includes a bundled Java runtime

## License

MIT — see [LICENSE](LICENSE).
