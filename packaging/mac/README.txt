PostgreSQL Explain Plan Studio — portable JAR + macOS app packaging
===================================================================

1) Portable JAR zip (any OS with JDK/JRE 17+)
   Built on Windows: .\scripts\pack-jar.ps1
   → target/PostgreSQLExplainPlanStudio-<ver>-jar.zip

   Contents: shaded postgresql-explain-plan-studio-<ver>.jar + peps.sh + this README.

   Launch (any OS):
     java -jar postgresql-explain-plan-studio-<ver>.jar

   On macOS / Unix you can also use the helper script:
     chmod +x peps.sh
     ./peps.sh

   Catalina 10.15: current Temurin 17 builds break the GUI (JavaRuntimeSupport).
   Install Temurin 17.0.8 Intel instead:
     https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%2B7/OpenJDK17U-jdk_x64_mac_hotspot_17.0.8_7.pkg
   Then put it on PATH (macOS may still point java at a stub):
     export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
     export PATH="$JAVA_HOME/bin:$PATH"
   Confirm: java -version shows 17.0.8

2) Native .app / DMG (drag into Applications; bundled JRE)
   Must be built ON a Mac (Intel → mac-x64, Apple Silicon → mac-arm64):

     # Prefer a local disk copy (e.g. ~/peps), not a VMware HGFS share:
     ./scripts/pack-mac-app.sh

   Outputs:
     target/jpackage-mac/PostgreSQL Explain Plan Studio.app
     target/PostgreSQLExplainPlanStudio-<ver>-mac-<arch>.zip  (always; zip name stays unspaced)
     target/PostgreSQLExplainPlanStudio-<ver>-mac-<arch>.dmg  (skipped over SSH)

   Install: unzip the .zip (or open the .dmg) and drag
   "PostgreSQL Explain Plan Studio.app" into Applications.

   Or from Windows: .\scripts\dev-release.ps1 SSHs to the Mac VM, runs pack-mac-app.sh,
   and uploads the .app zip (+ DMG if present) with the GitHub release.

   First launch of an unsigned build: right-click the app / Open / Open.
   Signing/notarization can remove that Gatekeeper step later.

peps.icns is generated from src/main/resources/images/app-icon.png by pack-mac-app.sh.
