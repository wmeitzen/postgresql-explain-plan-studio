# PostgreSQL Explain Plan Studio

Graphical viewer for PostgreSQL `EXPLAIN` plans.

## Download

- <img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" /> Windows installer: [PostgreSQLExplainPlanStudio-1.0.0-windows-x64-installer.exe](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-windows-x64-installer.exe)
- <img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" /> Windows zip: [PostgreSQLExplainPlanStudio-1.0.0-windows-x64.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-windows-x64.zip)
- <img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" /> Linux zip: [PostgreSQLExplainPlanStudio-1.0.0-linux-x64.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-linux-x64.zip)

  To install the `linux-x64.zip` on Linux:
  - Unzip file
  - Open a terminal
  - Navigate to the unzipped `PostgreSQLExplainPlanStudio/bin` directory
  - `chmod +x PostgreSQLExplainPlanStudio`
  - To launch: `./PostgreSQLExplainPlanStudio`

- <img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" /> Linux `.deb`: [PostgreSQLExplainPlanStudio-1.0.0-linux-x64.deb](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-linux-x64.deb)

  To install the `.deb` on Linux: `sudo apt install ./PostgreSQLExplainPlanStudio-1.0.0-linux-x64.deb`
- <img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS disk image: [PostgreSQLExplainPlanStudio-1.0.0-mac-x64.dmg](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-mac-x64.dmg)

  To install the `.dmg` on macOS:
  - Open the `.dmg`
  - Drag `PostgreSQL Explain Plan Studio.app` to Applications
  - Eject the disk image when finished
  - First launch of an unsigned build: right-click the app / Open / Open
- <img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS app zip: [PostgreSQLExplainPlanStudio-1.0.0-mac-x64.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-mac-x64.zip)

  To install the `x64.zip` on macOS:
  - Unzip file
  - Drag `PostgreSQL Explain Plan Studio.app` to Applications
  - No separate Java install needed (JRE is bundled)
  - First launch of an unsigned build: right-click the app / Open / Open
- <img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS (JAR): [PostgreSQLExplainPlanStudio-1.0.0-mac-jar.zip](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-mac-jar.zip)

  To install the `jar.zip` on macOS:
  - Unzip file
  - Open a terminal
  - Navigate to the unzipped file's new directory
  - `chmod +x peps.sh`
  - Install [Java 17 or later](https://adoptium.net/temurin/releases/?version=17) (Temurin or any JRE/JDK)
  - To launch: `./peps.sh`

All [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases).

**Stable** is the GitHub **Latest** release (links above). **Pre-release** builds are marked Pre-release on the [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases) page and do not replace Latest.

## Using PEPS

Step-by-step guides (with screenshot placeholders): **[docs/user/](docs/user/README.md)**.

## Build from source

See **[BUILDING.md](BUILDING.md)** for requirements, running from source, packaging (Windows / Linux / macOS), and maintainer release notes.

## License

MIT — see [LICENSE](LICENSE).
