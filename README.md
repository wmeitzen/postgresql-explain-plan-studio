# PostgreSQL Explain Plan Studio

Private bootstrap repository for a graphical PostgreSQL `EXPLAIN` plan tool.

Right now this repo contains a **Hello World** Maven Java app so we can practice building, running, and pushing with GitHub. The real Plan Studio application will land here later.

## Requirements

- JDK 17 or newer
- Apache Maven 3.9+ (or the Maven Wrapper once generated: `mvnw` / `mvnw.cmd`)

## Build and run

```bash
mvn -q compile exec:java
```

Or package a jar and run it:

```bash
mvn -q package
java -jar target/postgresql-explain-plan-studio-0.0.1-SNAPSHOT.jar
```

Expected output:

```text
Hello, PostgreSQL Explain Plan Studio!
```

## Open in Cursor

`File → Open Folder` → `F:\docs\github\wmeitzen\postgresql-explain-plan-studio`

Optional: install the **Extension Pack for Java** in Cursor for Run/Debug buttons.

## Source vs releases (later)

- **Source:** clone this repo or use GitHub’s “Download ZIP”
- **Release:** a tagged installable build (jar/zip) — not set up yet

## License

MIT — see [LICENSE](LICENSE).
