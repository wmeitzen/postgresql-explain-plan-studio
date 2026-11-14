PostgreSQL Explain Plan Studio — macOS packaging
================================================

1) JAR zip (any Mac + JDK 17+)
   Built on Windows: .\scripts\pack-mac-jar.ps1
   → target/PostgreSQLExplainPlanStudio-<ver>-mac-jar.zip
   On the Mac: unzip, chmod +x peps.sh, ./peps.sh

2) Native .app / DMG (drag into Applications; bundled JRE)
   Must be built ON a Mac (Intel → mac-x64, Apple Silicon → mac-arm64):

     # Prefer a local disk copy (e.g. ~/peps), not a VMware HGFS share:
     ./scripts/pack-mac-app.sh

   Outputs:
     target/jpackage-mac/PostgreSQLExplainPlanStudio.app
     target/PostgreSQLExplainPlanStudio-<ver>-mac-<arch>.zip  (always)
     target/PostgreSQLExplainPlanStudio-<ver>-mac-<arch>.dmg  (skipped over SSH)

   Install: unzip the .zip (or open the .dmg) and drag the .app into Applications.

   Or from Windows: .\scripts\dev-release.ps1 SSHs to the Mac VM, runs pack-mac-app.sh,
   and uploads the .app zip (+ DMG if present) with the GitHub release.

   First launch of an unsigned build: right-click the app → Open → Open.
   Signing/notarization can remove that Gatekeeper step later.

peps.icns is generated from src/main/resources/images/app-icon.png by pack-mac-app.sh.
