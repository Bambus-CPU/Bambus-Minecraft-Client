# 🎋 Bambus Client 3.0 – Neon Edition

Ein PvP-/HUD-Client für **Minecraft Java 1.21.11** (Fabric) im Neon-Look – mit eigenem Launcher.

## Was du brauchst

**Nur Minecraft Java Edition** (gekauft, mit dem offiziellen Minecraft Launcher).
Java, Fabric und die Fabric API bringt der Bambus Launcher selbst mit bzw. lädt sie automatisch.

## Herunterladen

| Datei | Für wen |
|---|---|
| `Bambus-Launcher-Setup.exe` | **Windows – empfohlen.** Installieren, starten, SPIELEN. Java ist eingebaut. |
| `Bambus-Launcher-Windows.zip` | Windows ohne Installation: entpacken, `Bambus Launcher.exe` starten. |
| `Bambus-Launcher-Mac-AppleSilicon.dmg` | Mac mit M1/M2/M3/M4 |
| `Bambus-Launcher-Mac-Intel.dmg` | Mac mit Intel-Chip |
| `BambusLauncher.jar` | Alle Systeme, wenn Java schon installiert ist |

Die Dateien entstehen automatisch auf GitHub (siehe „Bauen“). **`Bambus-Client-Download.zip`** enthält alles in einem Download.
Schritt-für-Schritt (Mac & Windows) und eine Testliste: **ANLEITUNG.md**.

## Windows: installieren und in die Taskleiste

1. `Bambus-Launcher-Setup.exe` doppelklicken. Keine Administratorrechte nötig.
   (Falls Windows „Der Computer wurde durch Windows geschützt“ zeigt: *Weitere Informationen* → *Trotzdem ausführen* –
   das kommt bei selbstgebauten Programmen ohne gekaufte Signatur.)
2. Das Setup legt den Launcher ins **Startmenü** und – wenn angehakt – auf den **Desktop** und startet ihn.
3. **Taskleiste:** Rechtsklick auf das Bambus-Symbol unten in der Taskleiste → **„An Taskleiste anheften“**.
   Ab dann reicht ein Klick unten in der Leiste. (Windows erlaubt Programmen nicht, sich selbst anzuheften –
   diesen einen Klick musst du selbst machen.)

Wer die Zip-Version nutzt: Im Launcher unter **EINSTELLUNGEN → WINDOWS** gibt es Knöpfe für Desktop und Startmenü.
Deinstallieren geht wie bei jedem Programm über *Einstellungen → Apps*.

## Der Launcher

Links die Seitenleiste mit vier Tabs:

- **START** – großer SPIELEN-Knopf, Übersicht (aktive Module, Cape, Arbeitsspeicher)
- **MODULE** – jede Funktion als Karte mit Neon-Schalter: an/aus per Klick
- **KOSMETIK** – dein eigener Skin als drehbare 3D-Figur, dazu Capes, Flügel und Partikelspuren –
  und das **Kosmetik-Studio** („+ Erstellen“), mit dem du eigene Capes und Flügel baust
- **MODS** – **⚡ FPS-Boost** (Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling, Dynamic FPS),
  **Mod Menu** (Mods-Knopf im Spiel), Mods auf Modrinth suchen und hinzufügen, eigene `.jar`-Mods an/aus
- **EINSTELLUNGEN** – Arbeitsspeicher (2–16 GB), Ordner öffnen, Zurücksetzen

Beim Klick auf **SPIELEN** richtet der Launcher Fabric + Fabric API + Bambus Client in einem eigenen Spielordner
(`.minecraft/bambus-client`) ein, legt das Profil **„Bambus Client“** an und öffnet Minecraft.
Dort das Profil wählen und auf Spielen klicken. Die Anmeldung läuft wie immer sicher über dein Microsoft-Konto.

**Optional:** Mit eigener Microsoft-Anmeldung (oben rechts im Launcher) startet SPIELEN Minecraft direkt – ohne den
offiziellen Launcher. Dafür braucht es eine von Mojang freigegebene Azure-App; die Schritte stehen in
[`RECHTLICHES.md`](RECHTLICHES.md).

## Im Spiel

**Neu:** Bambus-Ladebildschirm beim Betreten einer Welt · HUD-Editor mit Einrasten (Shift = frei) und Größe per Ecke/Mausrad (Rechtsklick = 100 %) · weiche Animationen im Menü · Neon-Benachrichtigungen (Update, Freund online, Screenshot, Profil) · einheitliche Symbole für Module, Kategorien und Tabs. Alles abschaltbar unter **MEHR → DESIGN**.

| Taste | Funktion |
|---|---|
| **Rechts-Shift** | Bambus-Menü (auch über „BAMBUS“ im Hauptmenü) |
| **Tab** (im Menü) | nächster Tab · Shift+Tab = zurück |
| **C** (halten) | Zoom – Mausrad = näher/weiter |
| **Linke Alt** (halten) | Freelook |
| **H** | ganzes HUD an/aus |

Statt des normalen Minecraft-Startbildschirms erscheint das **Bambus-Hauptmenü** (Neon-Gitter, Bambus-Logo,
Einzelspieler/Mehrspieler/Bambus-Menü/Optionen). Unten rechts kommst du über „Minecraft-Menü“ jederzeit zum normalen
Bildschirm; abschalten kannst du es im Bambus-Menü unter *Funktionen → Bambus-Hauptmenü*.

Das Menü hat fünf Tabs: **MODULE** (HUD-Anzeigen mit Suche, Kategorien PvP/HUD/Info und ★-Favoriten),
**FUNKTIONEN** (inkl. Visuals, Leistung und Scoreboard/Bossbar/Tabliste/Chat anpassen), **FADENKREUZ**,
**KOSMETIK** und **MEHR** (Farbthemen, Profile, Import/Export, Tastenbelegung, Statistik, Freunde, Screenshots,
Discord-Status, empfohlene Server). „HUD BEARBEITEN“ öffnet den Editor zum Verschieben.

### Funktionen
- **PvP:** Keystrokes, CPS, Combo-Zähler, Reach-Anzeige, Target-HUD (Name, Leben, Abstand, letzter Schaden),
  W-/S-Tap-Anzeige, Kill-Zähler mit K/D, Pfeil-Zähler, Item-Cooldowns, TNT-Timer, Rüstung, Trank-Effekte, Fadenkreuz-Editor
- **HUD:** FPS, Ping, Koordinaten, Richtung, Biom, Tempo, Uhr, Arbeitsspeicher, Server-Info;
  Scoreboard/Bossbar ein-aus und Größe, Tabliste und Chat in der Größe
- **Visuals:** eigene Tageszeit und eigenes Wetter (nur bei dir), Treffer-Partikel, Zoom, Freelook, Fullbright
- **Leistung:** FPS-Boost (Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling, Dynamic FPS) und Low-End-Modus
- **Oberfläche:** 7 Farbthemen inkl. RGB, Farbverläufe, Menü-Animation, 3 Einstellungs-Profile, Import/Export über die
  Zwischenablage, Tastenbelegung
- **Extras:** Spieler-Statistik über alle Sitzungen, Freundesliste (Freunde im Target-HUD markiert), Screenshot-Manager,
  Auto-Screenshot bei Kills, Discord-Status (braucht eine Discord-App-ID), empfohlene Server
- Gegner-Leben als Zahl, Freelook und Fullbright sind schaltbar, weil manche Server sie verbieten.

### Kosmetik
Seit 3.3 im neuen HD-Look: weiche Verläufe, Glanz, Leuchten und echte 3D-Formen.
- **Umhänge (wehend):** Bambuswald, Neon-Platine, Cyber, Galaxie, Synthwave, Matrix, Polarlicht, Glut –
  aus 24 Stoff-Reihen mit Stoffdicke: Wellen laufen nach unten, beim Laufen, Sprinten und Fallen weht er
  stärker. Jeder hat eine leuchtende Animation (Glühwürmchen, Datenregen, Nordlicht, Flammen …).
  Das Design erscheint auch auf der Elytra.
- **Flügel (3D):** Engel, Seraph, Nachtschwinge, Kristall, Aurora, Sakura, Phönix, Drache, Cyber, Schmetterling, Bambus –
  24 Streifen pro Flügel biegen sich ohne Lücken, Deckfedern liegen als eigene Ebene darüber, die Vorderkante
  ist ein echter Knochen, ein Gelenkstück führt in den Rücken, ein Glanz läuft über die Federn.
  Bewegung: schlagen beim Laufen, Springen und Fliegen, gleiten im Stand, anlegen beim Schleichen.
- **Hüte:** Krone (funkelnde Steine), Zylinder (pulsierendes Neonband), Heiligenschein (kreisende Funken),
  Hörner (gebogen), Katzenohren (zucken), Partyhut (laufende Streifen), Bandana (flatternde Enden), Neon-Visier (Scan-Balken)
- **Partikel:** Neon-Funken, Sternenstaub, Herzen, Musik, Seelenfeuer, Kirschblüten, Magie, Flammen – kreisen um dich
- **Kill-Effekte:** Neon-Blitz, Feuerwerk, Seelen, Flammen, Herzen, Totem – Ring, Doppelspirale und Blitz
- **Pfeil-Spuren:** als Spirale hinter eigenen Pfeilen, Dreizacken und Perlen

Die Texturen entstehen mit `werkzeuge/fluegel_3d.py` und `werkzeuge/capes_hd.py`.

### Eigene Kosmetik erstellen
Im Launcher unter **KOSMETIK → „+ Erstellen“** öffnet sich das Kosmetik-Studio:

1. **Cape** oder **Flügel** wählen
2. Vorlage aussuchen (Capes: Verlauf, Streifen, Bambus, Sterne, Platine, Welle · Flügel: Federn, Drache,
   Schmetterling, Cyber) und drei Farben anpassen – oder **🎲 Zufall**
3. Optional: Initialen aufs Cape oder ein **eigenes Bild** laden (ein fertiges 64×32-Cape wird direkt übernommen)
4. Namen geben → **Speichern & Anziehen**

Eigene Kosmetik liegt als PNG in `.minecraft/bambus-client/config/bambus-kosmetik/capes/` bzw. `…/fluegel/`.
Du kannst dort auch selbst Bilder hineinlegen (Capes im Seitenverhältnis 2:1, Flügel quadratisch mit dem
Gelenk oben links). Im Spiel: Bambus-Menü → Kosmetik → **⟳ LADEN** liest den Ordner neu ein.
Rechtsklick auf eine eigene Kachel im Launcher löscht sie.

Kosmetik ist **nur für dich sichtbar** – andere Spieler sehen sie nicht. Für das Cape muss unter
*Optionen → Skin-Anpassung → Umhang* eingeschaltet sein.

> ⚠️ Freelook und Fullbright sind auf manchen Servern (z. B. Hypixel) verboten. Dort ausschalten.

Alle Einstellungen liegen in `.minecraft/bambus-client/config/bambus.json` – Launcher und Spiel benutzen dieselbe Datei.

## Updates – ohne neu zu verschicken

Der Launcher sucht beim Start selbst nach einer neuen Version (abschaltbar unter **Einstellungen → AUTO-UPDATE**).

- **Neue Funktionen im Spiel** (Module, Kosmetik, HUD): Die neue Client-Mod wird leise heruntergeladen, mit Prüfsumme kontrolliert und beim nächsten **SPIELEN** automatisch eingespielt. Man muss nichts tun.
- **Neuer Launcher**: Oben erscheint **▲ UPDATE**. Ein Klick zeigt „Was ist neu“, und **LAUNCHER AKTUALISIEREN** spielt die neue Version ein (Windows: läuft von selbst durch und startet neu, Mac: die neue .dmg öffnet sich).

**So veröffentlichst du ein Update (alles auf github.com):**
1. Änderungen hochladen und in `AENDERUNGEN.md` kurz aufschreiben, was neu ist – das sehen alle als „Was ist neu“.
2. Auf der Projektseite rechts **Releases → Draft a new release** → bei „Choose a tag“ eine höhere Nummer eintippen, z. B. `v3.3.0` → **Publish release**.
   (Oder einen Tag `v3.3.0` hochladen.)
3. GitHub baut alles (ca. 10–15 Minuten) und hängt die Dateien an das Release. Ab dann bekommen alle Launcher das Update.

Die Versionsnummer überall im Code übernimmt der Build automatisch aus dem Tag. Der Launcher fragt nur die öffentliche GitHub-Seite – ohne Konto und ohne persönliche Daten.

## Rechtliches

**Kein offizielles Minecraft-Produkt. Nicht von Mojang oder Microsoft genehmigt oder mit ihnen verbunden.**
Bambus fragt nie nach deinem Passwort (Anmeldung nur im offiziellen Minecraft Launcher), liefert keine
Minecraft-Spieldateien mit und ist Open Source (MIT, siehe `LICENSE`).
Alles Weitere – EULA, Code-Signierung, fremde Lizenzen – steht in [`RECHTLICHES.md`](RECHTLICHES.md).

## Bauen

### Auf GitHub (empfohlen, ohne etwas zu installieren)
1. Neues Repository auf github.com anlegen und den Inhalt dieses Ordners hochladen.
2. Unter **Actions** startet „Bambus Client bauen“ automatisch.
3. Nach ein paar Minuten liegen unten bei *Artifacts* die Downloads (Setup.exe, Zip, Dmg, Jar).
4. Für eine richtige Download-Seite: unter *Releases* → *Draft a new release* → Tag `v3.3.0` → veröffentlichen.
   Dann landen alle Dateien automatisch im Release.

### Am eigenen Computer
Du brauchst **Java 21** (z. B. „Temurin 21“ von https://adoptium.net).

```
gradlew.bat build        (Windows)
./gradlew build          (Mac/Linux)
```

Danach in `build/libs`: `BambusLauncher.jar` (Launcher mit eingebauter Mod) und `bambus-client-3.3.0.jar` (nur die Mod).

`gradlew.bat runClient` startet Minecraft direkt zum Testen.
