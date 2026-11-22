# PostgreSQL Explain Plan Studio

Graphical viewer for PostgreSQL `EXPLAIN` plans.

## Download

I've tested on Windows 10, Windows 11, Ubuntu 20, 22, 24, and 26 LTS, and an older macOSX version 10.15. I've also tested on several versions between PostgreSQL 12 through 19.<br />
I haven't tested every combination of the above, so please submit an issue if you see something wrong or out of place. Thanks!

<ul>
<li>
<img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" /> Windows installer: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-windows-x64-installer.exe">PostgreSQLExplainPlanStudio-1.0.0-windows-x64-installer.exe</a>
</li>
</ul>

<ul>
<li>
<img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" /> Windows zip: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-windows-x64.zip">PostgreSQLExplainPlanStudio-1.0.0-windows-x64.zip</a>
</li>
</ul>

<ul>
<li>
<img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" /> Linux zip: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-linux-x64.zip">PostgreSQLExplainPlanStudio-1.0.0-linux-x64.zip</a>
<br/>
To install the <code>linux-x64.zip</code> on Linux:
<ul>
<li>Unzip file</li>
<li>Open a terminal</li>
<li>Navigate to the unzipped <code>PostgreSQLExplainPlanStudio/bin</code> directory</li>
<li><code>chmod +x PostgreSQLExplainPlanStudio</code></li>
<li>To launch: <code>./PostgreSQLExplainPlanStudio</code></li>
</ul>
</li>
</ul>

<ul>
<li>
<img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" /> Linux <code>.deb</code>: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-linux-x64.deb">PostgreSQLExplainPlanStudio-1.0.0-linux-x64.deb</a>
<br/>
To install the <code>.deb</code> on Linux:
<ul>
<li>Navigate to the directory with the downloaded <code>.deb</code> file</li>
<li><code>sudo apt install ./PostgreSQLExplainPlanStudio-1.0.0-linux-x64.deb</code></li>
<li>If apt prints an <code>N:</code> note about <code>_apt</code> / Permission denied while installing from your home folder (for example <code>Downloads</code>), you can ignore it when the package finishes with <code>Setting up postgresql-explain-plan-studio</code></li>
<li>To launch: open <strong>PostgreSQL Explain Plan Studio</strong> from the applications menu, or run <code>PostgreSQLExplainPlanStudio</code></li>
</ul>
</li>
</ul>

### macOS support is experimental.

If it works or fails for you, I'd be grateful if you could tell me your MacOS version. Thanks!


<ul>
<li>
<img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS disk image: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-mac-x64.dmg">PostgreSQLExplainPlanStudio-1.0.0-mac-x64.dmg</a>
<br/>
To install the <code>.dmg</code> on macOS:
<ul>
<li>Open the <code>.dmg</code></li>
<li>Drag <code>PostgreSQL Explain Plan Studio.app</code> to Applications</li>
<li>Eject the disk image when finished</li>
<li>First launch of an unsigned build: right-click the app in Applications / Open / Open</li>
<li>If macOS says the app is <strong>damaged</strong> (Gatekeeper quarantine on unsigned downloads), run:<br/><code>xattr -dr com.apple.quarantine "/Applications/PostgreSQL Explain Plan Studio.app"</code><br/>then open the app again</li>
</ul>
</li>
</ul>

<ul>
<li>
<img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS app zip: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-mac-x64.zip">PostgreSQLExplainPlanStudio-1.0.0-mac-x64.zip</a>
<br/>
To install the <code>x64.zip</code> on macOS:
<ul>
<li>Unzip file</li>
<li>Drag <code>PostgreSQL Explain Plan Studio.app</code> to Applications</li>
<li>No separate Java install needed (JRE is bundled)</li>
<li>First launch of an unsigned build: right-click the app in Applications / Open / Open</li>
<li>If macOS says the app is <strong>damaged</strong> (Gatekeeper quarantine on unsigned downloads), run:<br/><code>xattr -dr com.apple.quarantine "/Applications/PostgreSQL Explain Plan Studio.app"</code><br/>then open the app again</li>
</ul>
</li>
</ul>

<ul>
<li>
<img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS (JAR): <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.0-mac-jar.zip">PostgreSQLExplainPlanStudio-1.0.0-mac-jar.zip</a>
<br/>
To install the <code>jar.zip</code> on macOS:
<ul>
<li>Install <a href="https://adoptium.net/temurin/releases/?version=17">Java 17 or later</a> (Temurin or any JRE/JDK)</li>
<li>Unzip file</li>
<li>Open a terminal</li>
<li>Navigate to the unzipped file's new directory</li>
<li><code>chmod +x peps.sh</code></li>
<li>To launch: <code>./peps.sh</code></li>
</ul>
</li>
</ul>

All [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases).

**Stable** is the GitHub **Latest** release (links above). **Pre-release** builds are marked Pre-release on the [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases) page and do not replace Latest.

## Using PEPS

Step-by-step guides (with screenshot placeholders): **[docs/user/](docs/user/README.md)**.

## Build from source

See **[BUILDING.md](BUILDING.md)** for requirements, running from source, packaging (Windows / Linux / macOS), and maintainer release notes.

## License

MIT — see [LICENSE](LICENSE).
