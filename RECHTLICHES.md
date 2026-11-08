# Rechtliches & Sicherheit

**Kein offizielles Minecraft-Produkt. Nicht von Mojang oder Microsoft genehmigt oder mit ihnen verbunden.**
Minecraft ist eine Marke von Mojang AB / Microsoft.

Diese Seite erklärt, wie der Bambus Client die Regeln von Mojang einhält und warum er sicher ist.
(Keine Rechtsberatung – im Zweifel die Originaltexte lesen, Links unten.)

## 1. Anmeldung: nur über Microsoft – nie über Bambus

- Der Bambus Launcher fragt **niemals** nach E-Mail-Adresse oder Passwort.
- Es gibt **keinen** Offline- oder „gecrackten“ Modus. Spielen geht nur mit einem gekauften Minecraft: Java Edition.
- **Standard:** Bambus richtet das Profil „Bambus Client“ ein und öffnet den **offiziellen Minecraft Launcher**.
  Angemeldet wird dort.
- **Optional – eigene Microsoft-Anmeldung (oben rechts „MICROSOFT · ANMELDEN“):** Der Spieler meldet sich auf der
  offiziellen Microsoft-Seite an (microsoft.com/link, Gerätecode). Bambus bekommt nur Zugangs-Tokens
  (Microsoft → Xbox Live → Minecraft), prüft, dass das Konto Minecraft: Java Edition besitzt, und startet das Spiel
  dann direkt. Die Tokens liegen nur auf diesem Computer im Benutzerordner (`.bambus-launcher/konto.json`, nur für den
  eigenen Benutzer lesbar) – nicht im Spielordner. „ABMELDEN“ löscht sie.

### So wird die eigene Anmeldung freigeschaltet

Mojang erlaubt den Zugriff auf die Minecraft-Dienste nur für **freigegebene Apps**. Bis dahin ist die Funktion aus.

1. Auf <https://portal.azure.com> → *App-Registrierungen* → *Neue Registrierung*.
   Kontotyp: **„Nur persönliche Microsoft-Konten“** (oder „… und persönliche Microsoft-Konten“).
2. In der App unter *Authentifizierung* → **„Öffentliche Clientflows zulassen“ = Ja** (für die Gerätecode-Anmeldung).
3. Die **Anwendungs-ID (Client-ID)** notieren und bei Mojang die Freigabe beantragen:
   <https://aka.ms/mce-reviewappid>
4. Nach der Zusage im Launcher oben rechts auf **MICROSOFT · ANMELDEN** → App-ID eintragen → **Mit Microsoft anmelden**.

Danach startet **SPIELEN** Minecraft direkt aus Bambus. Spiel, Bibliotheken, Grafiken/Töne und das passende Java lädt
Bambus dann genau wie der offizielle Launcher **frisch von den Mojang-Servern** (schon vorhandene Dateien im
`.minecraft`-Ordner werden mitbenutzt und per Prüfsumme kontrolliert). Abschaltbar im selben Fenster
(„Direkt starten“).

## 2. Minecraft-EULA und Markenrichtlinien

- **Keine Mojang-Dateien:** Weder der Launcher noch die Installer enthalten Spieldateien von Minecraft
  (keine `minecraft.jar`, keine Texturen, keine Sounds von Mojang). Das Spiel kommt immer frisch von den
  Mojang-Servern – über den offiziellen Launcher oder (mit eigener Anmeldung) direkt von dort.
- **Was Bambus herunterlädt:** nur Fabric (von `meta.fabricmc.net`), Fabric API (von `modrinth.com`) und auf Wunsch
  deinen Skin von der öffentlichen Mojang-Schnittstelle zur Anzeige in der 3D-Vorschau.
- **Eigene Grafiken:** Logo, Capes, Flügel, Partikel, Standard-Skin und Musik sind selbst erstellt.
  Kein Minecraft-Logo, keine kopierten Inhalte anderer Clients.
- **Name:** „Bambus Client“ – der Name enthält nicht „Minecraft“, und überall steht der Hinweis
  „Kein offizielles Minecraft-Produkt“ (Launcher-Seitenleiste, Bambus-Hauptmenü, diese Datei).
- **Nichts verkaufen, was Vorteile bringt:** Bambus ist kostenlos. Spielvorteile dürfen nie verkauft werden.
  Falls Bambus einmal etwas verkauft, dann nur rein kosmetische Dinge im Rahmen der Mojang-Regeln.
- **Server-Regeln beachten:** Freelook und Fullbright sind auf manchen Servern (z. B. Hypixel) verboten.
  Dort ausschalten.

## 3. Sicherheit, Code-Signierung und Virenwarnungen

- **Open Source:** Der komplette Quellcode liegt offen (MIT-Lizenz, siehe `LICENSE`). Jede Datei kann geprüft
  werden; die fertigen Programme baut GitHub automatisch aus genau diesem Code.
- **Windows SmartScreen / macOS Gatekeeper:** Unsignierte Programme zeigen „Unbekannter Herausgeber“ bzw.
  „nicht verifizierter Entwickler“. Das ist kein Virus, sondern fehlende Signatur.
- **Signieren ist vorbereitet:** Der GitHub-Ablauf (`.github/workflows/build.yml`) signiert automatisch,
  sobald Zertifikate als *Secrets* hinterlegt sind (Repository → Settings → Secrets and variables → Actions).
  Ohne Secrets wird – wie bisher – unsigniert gebaut.

| Secret | Wofür |
|---|---|
| `WINDOWS_CERT_PFX_BASE64` | Code-Signing-Zertifikat (.pfx) als Base64 |
| `WINDOWS_CERT_PASSWORD` | Passwort des .pfx |
| `MAC_CERT_P12_BASE64` | Apple „Developer ID Application“-Zertifikat (.p12) als Base64 |
| `MAC_CERT_PASSWORD` | Passwort des .p12 |
| `MAC_SIGN_NAME` | Name im Zertifikat, z. B. `Vorname Nachname (TEAMID1234)` |
| `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_PASSWORD` | für die Beglaubigung (Notarisierung) bei Apple |

Ein Windows-Zertifikat gibt es bei offiziellen Zertifizierungsstellen (kostet Geld, jährlich).
Für den Mac braucht man eine Mitgliedschaft im Apple Developer Program (99 $/Jahr).
Base64 erzeugen: Mac/Linux `base64 -i datei.pfx | pbcopy`, Windows `[Convert]::ToBase64String([IO.File]::ReadAllBytes("datei.pfx"))`.

## 4. Aufbau auf bestehenden Systemen

Bambus baut – wie Feather und viele andere Clients – auf **Fabric** auf:
der Client ist eine Fabric-Mod, der Launcher richtet Fabric, Fabric API und die Mod ein und startet das Spiel
über den offiziellen Minecraft Launcher.

## Fremde Software

| Software | Lizenz | Wie genutzt |
|---|---|---|
| Fabric Loader | Apache-2.0 | wird beim Spielen heruntergeladen |
| Fabric API | Apache-2.0 | wird beim Spielen heruntergeladen |
| Fabric Loom (nur zum Bauen) | MIT | Gradle-Plugin |
| Eclipse Temurin / OpenJDK | GPLv2 mit Classpath Exception | im Mac-/Windows-Installer eingebaut |
| Mods aus dem MODS-Tab (Sodium, Lithium, Mod Menu, …) | Lizenz der jeweiligen Autoren | nur auf Wunsch direkt von modrinth.com heruntergeladen, nicht mitgeliefert |

## Links

- Minecraft-EULA: https://www.minecraft.net/en-us/eula
- Nutzungsrichtlinien (Usage Guidelines): https://www.minecraft.net/en-us/usage-guidelines
- Fabric: https://fabricmc.net
