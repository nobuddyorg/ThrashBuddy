# Self-Hosted GitHub Runner

Diese README beschreibt die Installation und Konfiguration eines self-hosted GitHub Runners in unserer Entwicklungsinfrastruktur.

## Installation und Konfiguration

Zugriff auf die VM erfolgt ausschließlich über das Proxmox-Webinterface im lokalen Firmennetzwerk.

1. Melde dich im Proxmox-Webinterface an (Zugangsdaten siehe [Confluence](https://qytera.atlassian.net/wiki/x/AYC4xQ)).

2. Öffne die Konsole der entsprechenden VM (GithubRunner).

3. Führe darauf das vorbereitende Script aus:
   ```bash
   ./infrastructure/github-runner/prepare-runner.sh
   ```

Das Script installiert alle notwendigen Abhängigkeiten, wie `curl`, `unzip` und systemd-Komponenten. Gleichzeitig setzt es auch Rechte für den User um beispielsweise Docker ohne `sudo` starten zu können. Der Github Runner sollte so konfiguriert sein, dass er nach einem Neustart alle benötigten Services startet.

## Hinzufügen des Runners zu Github

1. Öffne die GitHub-Seite des entsprechenden Repositories oder der Organisation.

2. Navigiere zu **Settings > Actions > Runners** und klicke auf **New self-hosted runner**.

3. Folge den Anweisungen, um die Konfigurationsbefehle für den Runner zu generieren.

4. Kopiere die generierten Befehle und führe sie in der Proxmox-VM-Konsole aus:
   - Das Runner-Tool wird heruntergeladen.
   - Der Runner wird konfiguriert.
   - Der Runner wird als Systemdienst gestartet.

5. Überprüfe in GitHub, dass der Runner als aktiv angezeigt wird.

## Hinweise zur Umgebung

- Die VM ist nur über das Proxmox-Webinterface im lokalen Firmennetzwerk zugänglich.
- Details zur Proxmox-Konfiguration und Zugangsdaten findest du in unserem Confluence: [Confluence-Link](https://qytera.atlassian.net/wiki/x/AYC4xQ).
