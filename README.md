# PostgreSQL Explain Plan Studio

Private bootstrap repository for a graphical PostgreSQL `EXPLAIN` plan tool.

Right now this repo contains a **Hello World** Maven Java app so we can practice building, running, and pushing with GitHub. The real Plan Studio application will land here later.

## Requirements

- JDK 17 or newer (`java`, `javac`, and `jpackage` on `PATH`)
- Apache Maven 3.9+ (or the Maven Wrapper: `mvnw` / `mvnw.cmd`)

## Build and run

```bash
mvn -q compile exec:java
```

Or package a jar and run it:

```bash
mvn -q package
java -jar target/postgresql-explain-plan-studio-0.0.2-SNAPSHOT.jar
```

Expected output:

```text
Hello, PostgreSQL Explain Plan Studio!
```

## Windows package with bundled runtime (jpackage)

End users should not need to install Java. Use the `jpackage` Maven profile to build a Windows **app-image** (folder with `PostgreSQLExplainPlanStudio.exe` plus a bundled runtime) and a zip for GitHub Releases:

```powershell
.\mvnw.cmd -P jpackage -DskipTests verify
```

Outputs:

- `target/jpackage/PostgreSQLExplainPlanStudio/PostgreSQLExplainPlanStudio.exe` — run locally
- `target/PostgreSQLExplainPlanStudio-0.0.2-windows-x64.zip` — attach to a GitHub Release

```powershell
.\target\jpackage\PostgreSQLExplainPlanStudio\PostgreSQLExplainPlanStudio.exe
```

### Windows SmartScreen

Unsigned downloads often show **“Windows protected your PC”**. That is normal for a new private app that is not code-signed. Click **More info** → **Run anyway** (only if you trust the build you downloaded from your own repo). A paid code-signing certificate is what removes that warning for other people later.

### MSI note

A Windows `.msi` from `jpackage` needs **WiX Toolset 3.x** (`candle.exe` / `light.exe`) on `PATH`. That installer requires administrator rights (and .NET Framework 3.5 features). Until WiX 3 is available, the **app-image zip** is the release artifact.

## Open in Cursor

`File → Open Folder` → `F:\docs\github\wmeitzen\postgresql-explain-plan-studio`

Optional: install the **Extension Pack for Java** in Cursor for Run/Debug buttons.

## Source vs releases

- **Source:** clone this repo or use GitHub’s “Download ZIP” (developers need a JDK)
- **Release:** download the `PostgreSQLExplainPlanStudio-*-windows-x64.zip` asset from [Releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases) — includes a bundled Java runtime

## License

MIT — see [LICENSE](LICENSE).
