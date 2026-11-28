# PostgreSQL Explain Plan Studio

Graphical viewer for PostgreSQL `EXPLAIN` plans.

## Download and Install

<ul>
<li>
<img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" /> Windows installer: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.2-windows-x64-installer.exe">PostgreSQLExplainPlanStudio-1.0.2-windows-x64-installer.exe</a>
</li>
</ul>

<ul>
<li>
<img src="docs/readme-icons/windows.svg" width="16" height="16" alt="Windows" /> Windows zip: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.2-windows-x64.zip">PostgreSQLExplainPlanStudio-1.0.2-windows-x64.zip</a>
</li>
</ul>

<ul>
<li>
<img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" /> Linux zip: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.2-linux-x64.zip">PostgreSQLExplainPlanStudio-1.0.2-linux-x64.zip</a>
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
<img src="docs/readme-icons/linux.svg" width="16" height="16" alt="Linux" /> Linux <code>.deb</code>: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.2-linux-x64.deb">PostgreSQLExplainPlanStudio-1.0.2-linux-x64.deb</a>
<br/>
To install the <code>.deb</code> on Linux:
<ul>
<li>Navigate to the directory with the downloaded <code>.deb</code> file</li>
<li><code>sudo apt install ./PostgreSQLExplainPlanStudio-1.0.2-linux-x64.deb</code></li>
<li>If apt prints an <code>N:</code> note about <code>_apt</code> / Permission denied while installing from your home folder (for example <code>Downloads</code>), you can ignore it when the package finishes with <code>Setting up postgresql-explain-plan-studio</code></li>
<li>To launch: open <strong>PostgreSQL Explain Plan Studio</strong> from the applications menu, or run <code>PostgreSQLExplainPlanStudio</code></li>
</ul>
</li>
</ul>

<ul>
<li>
<img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS disk image: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.2-mac-x64.dmg">PostgreSQLExplainPlanStudio-1.0.2-mac-x64.dmg</a>
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
<img src="docs/readme-icons/apple.svg" width="16" height="16" alt="macOS" /> macOS app zip: <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.2-mac-x64.zip">PostgreSQLExplainPlanStudio-1.0.2-mac-x64.zip</a>
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
Portable JAR (any OS with Java 17+): <a href="https://github.com/wmeitzen/postgresql-explain-plan-studio/releases/latest/download/PostgreSQLExplainPlanStudio-1.0.2-jar.zip">PostgreSQLExplainPlanStudio-1.0.2-jar.zip</a>
<br/>
To install the portable <code>jar.zip</code>:
<ul>
<li><strong>macOS Catalina 10.15</strong>
<ul>
<li>Current Temurin 17 builds break the GUI (<code>JavaRuntimeSupport</code>). Use Temurin <strong>17.0.8</strong> Intel instead:
<ul>
<li>Download <a href="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%2B7/OpenJDK17U-jdk_x64_mac_hotspot_17.0.8_7.pkg">OpenJDK17U-jdk_x64_mac_hotspot_17.0.8_7.pkg</a></li>
<li>Install the <code>.pkg</code></li>
<li>In Terminal, put Temurin ahead of macOS's <code>java</code> stub:<br/>
<code>export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home</code><br/>
<code>export PATH="$JAVA_HOME/bin:$PATH"</code></li>
<li>Confirm: <code>java -version</code> shows <code>17.0.8</code></li>
</ul>
</li>
<li>Unzip the <code>jar.zip</code> file</li>
<li>Open a terminal and navigate to the unzipped directory</li>
<li><code>chmod +x peps.sh</code> then <code>./peps.sh</code> (or <code>java -jar postgresql-explain-plan-studio-1.0.2.jar</code>)</li>
</ul>
</li>
<li><strong>Other macOS</strong>
<ul>
<li>Install <a href="https://adoptium.net/temurin/releases/?version=17">Java 17 or later</a> (Intel or Apple Silicon)</li>
<li>Unzip the <code>jar.zip</code> file</li>
<li>Open a terminal and navigate to the unzipped directory</li>
<li><code>chmod +x peps.sh</code> then <code>./peps.sh</code> (or <code>java -jar postgresql-explain-plan-studio-1.0.2.jar</code>)</li>
</ul>
</li>
<li><strong>Windows</strong>
<ul>
<li>Install <a href="https://adoptium.net/temurin/releases/?version=17">Java 17 or later</a></li>
<li>Unzip the <code>jar.zip</code> file</li>
<li>Open Command Prompt or PowerShell and navigate to the unzipped directory</li>
<li><code>java -jar postgresql-explain-plan-studio-1.0.2.jar</code></li>
</ul>
</li>
<li><strong>Linux</strong>
<ul>
<li>Install <a href="https://adoptium.net/temurin/releases/?version=17">Java 17 or later</a></li>
<li>Unzip the <code>jar.zip</code> file</li>
<li>Open a terminal and navigate to the unzipped directory</li>
<li><code>java -jar postgresql-explain-plan-studio-1.0.2.jar</code> (or <code>chmod +x peps.sh</code> then <code>./peps.sh</code>)</li>
</ul>
</li>
</ul>
</li>
</ul>

All [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases).

**Stable** is the GitHub **Latest** release (links above). **Pre-release** builds are marked Pre-release on the [releases](https://github.com/wmeitzen/postgresql-explain-plan-studio/releases) page and do not replace Latest.

## Using PEPS

**[Documentation](docs/user/README.md)**.

## Build from source

See **[BUILDING.md](BUILDING.md)** for requirements, running from source, packaging (Windows / Linux / macOS), and maintainer release notes.

## License

MIT -- see [LICENSE](LICENSE).
