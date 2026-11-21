PEPS Application Icon Set
=========================

Preferred master: peps_icon_1024.png

Wired into the build:
- src/main/resources/images/app-icon.png  (window / taskbar; also source for macOS .icns)
- packaging/windows/peps.ico             (jpackage Windows --icon)

Windows
-------
- windows/peps_icon.ico  (multi-resolution: 16, 32, 48, 64, 128, 256)
  Copied to packaging/windows/peps.ico for the installer / exe.

macOS
-----
- pack-mac-app.sh regenerates packaging/mac/peps.icns from app-icon.png via iconutil.

Linux
-----
- PNG sizes under linux/ (16 → 1024) for FreeDesktop / hicolor or jpackage.

Color notes
-----------
Dark navy background, blue connectors, amber-orange warning glow on one node.
Matches the application’s plan diagram theme.
