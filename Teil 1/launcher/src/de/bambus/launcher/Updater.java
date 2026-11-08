package de.bambus.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Automatische Updates über die Releases-Seite des GitHub-Projekts.
 *
 * So funktioniert's:
 *  1. Ein neuer Tag (z. B. v3.3.0) auf GitHub baut alles und legt ein Release an.
 *  2. Der Launcher fragt beim Start die öffentliche GitHub-Schnittstelle nach dem neuesten Release.
 *  3. Ist es neuer, lädt er die Client-Mod (bambus-client-….jar) leise herunter, prüft die
 *     SHA-256-Prüfsumme und spielt sie beim nächsten SPIELEN ein – ohne neue Installation.
 *  4. Hat sich auch der Launcher geändert, bietet er das neue Installationsprogramm an
 *     (Windows: läuft von selbst durch, Mac: die .dmg öffnet sich).
 *
 * Es wird nur öffentlich lesbar bei GitHub nachgefragt – kein Konto, keine persönlichen Daten.
 * Das GitHub-Projekt trägt der Build automatisch ein (Datei update-quelle.txt im Launcher).
 */
final class Updater {

	static final class Release {
		String version = "";
		String text = "";
		String seite = "";
		String modUrl, modSha;
		String installerUrl, installerName;
	}

	interface Fortschritt { void melde(String text); }

	private static final Path ORDNER = Paths.get(System.getProperty("user.home"), ".bambus-launcher", "updates");
	private static final Path STAND = ORDNER.resolve("stand.json");
	static final Path MOD = ORDNER.resolve("bambus-client.jar");

	private Updater() {}

	/** "besitzer/projekt" aus dem Build – leer, solange es nicht auf GitHub gebaut wurde. */
	static String quelle() {
		String s = System.getProperty("bambus.updatequelle");
		if (s == null) {
			try (InputStream in = Updater.class.getResourceAsStream("/update-quelle.txt")) {
				s = in == null ? "" : new String(Installer.liesAlles(in), StandardCharsets.UTF_8);
			} catch (IOException ex) {
				s = "";
			}
		}
		s = s.trim();
		return s.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+") ? s : "";
	}

	static boolean aktiv() { return !quelle().isEmpty(); }

	/** Fragt GitHub nach dem neuesten Release. */
	static Release pruefe() throws IOException {
		String q = quelle();
		if (q.isEmpty()) throw new IOException("Für diesen Launcher ist keine Update-Quelle eingetragen.");
		HttpURLConnection c = (HttpURLConnection) new URL(System.getProperty("bambus.updateapi", "https://api.github.com") + "/repos/" + q + "/releases/latest").openConnection();
		c.setRequestProperty("User-Agent", Installer.USER_AGENT);
		c.setRequestProperty("Accept", "application/vnd.github+json");
		c.setConnectTimeout(10000);
		c.setReadTimeout(20000);
		int code = c.getResponseCode();
		if (code == 404) throw new IOException("Es gibt noch kein veröffentlichtes Update.");
		if (code != 200) throw new IOException("GitHub antwortet gerade nicht (" + code + ").");
		String json;
		try (InputStream in = c.getInputStream()) {
			json = new String(Installer.liesAlles(in), StandardCharsets.UTF_8);
		}
		Map<String, Object> o = Json.objekt(Json.parse(json));
		Release r = new Release();
		r.version = text(o.get("tag_name")).replaceFirst("^[vV]", "");
		r.text = text(o.get("body"));
		r.seite = text(o.get("html_url"));
		String os = Installer.betriebssystem();
		boolean arm = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT).contains("aarch64")
				|| System.getProperty("os.arch", "").toLowerCase(Locale.ROOT).contains("arm");
		List<Object> assets = Json.liste(o.get("assets"));
		if (assets != null) {
			for (Object a : assets) {
				Map<String, Object> m = Json.objekt(a);
				String name = text(m.get("name"));
				String url = text(m.get("browser_download_url"));
				String digest = text(m.get("digest"));
				String n = name.toLowerCase(Locale.ROOT);
				if (n.startsWith("bambus-client") && n.endsWith(".jar") && !n.contains("sources")) {
					r.modUrl = url;
					r.modSha = digest.startsWith("sha256:") ? digest.substring(7) : null;
				} else if ("windows".equals(os) && n.endsWith("setup.exe")) {
					r.installerUrl = url; r.installerName = name;
				} else if ("mac".equals(os) && n.endsWith(".dmg") && (arm ? n.contains("applesilicon") : n.contains("intel"))) {
					r.installerUrl = url; r.installerName = name;
				}
			}
		}
		return r;
	}

	/** Vergleicht Versionen wie 3.2.0 und 3.10.1 (negativ = a älter). */
	static int vergleiche(String a, String b) {
		String[] x = a.split("[.-]"), y = b.split("[.-]");
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int u = i < x.length ? zahl(x[i]) : 0, v = i < y.length ? zahl(y[i]) : 0;
			if (u != v) return Integer.compare(u, v);
		}
		return 0;
	}

	static boolean neuer(Release r) { return r != null && vergleiche(r.version, BambusLauncher.VERSION) > 0; }

	/** Version der zuletzt geladenen Client-Mod (oder die eingebaute). */
	static String modVersion() {
		try {
			if (Files.isRegularFile(STAND) && Files.isRegularFile(MOD)) {
				String v = text(Json.objekt(Json.parse(new String(Files.readAllBytes(STAND), StandardCharsets.UTF_8))).get("mod"));
				if (!v.isEmpty()) return v;
			}
		} catch (Exception ignoriert) {
			// dann die eingebaute
		}
		return BambusLauncher.VERSION;
	}

	/** Die heruntergeladene Mod, falls sie neuer ist als die im Launcher eingebaute – sonst null. */
	static Path neuereMod() {
		return Files.isRegularFile(MOD) && vergleiche(modVersion(), BambusLauncher.VERSION) > 0 ? MOD : null;
	}

	/** Lädt die Client-Mod des Releases (mit Prüfsumme). Gibt true zurück, wenn etwas Neues geladen wurde. */
	static boolean ladeMod(Release r, Fortschritt f) throws IOException {
		if (r.modUrl == null || vergleiche(r.version, modVersion()) <= 0) return false;
		if (f != null) f.melde("Client-Update " + r.version + " wird geladen …");
		byte[] daten = Installer.lade(r.modUrl);
		if (r.modSha != null && !r.modSha.equalsIgnoreCase(sha256(daten))) {
			throw new IOException("Die heruntergeladene Datei ist beschädigt (Prüfsumme stimmt nicht). Bitte später noch mal versuchen.");
		}
		if (daten.length < 4 || daten[0] != 'P' || daten[1] != 'K') {
			throw new IOException("Die heruntergeladene Datei ist keine gültige Mod.");
		}
		Files.createDirectories(ORDNER);
		Path tmp = ORDNER.resolve("bambus-client.jar.download");
		Files.write(tmp, daten);
		Files.move(tmp, MOD, StandardCopyOption.REPLACE_EXISTING);
		Map<String, Object> stand = new LinkedHashMap<>();
		stand.put("mod", r.version);
		Files.write(STAND, Json.write(stand).getBytes(StandardCharsets.UTF_8));
		return true;
	}

	/** Lädt das neue Installationsprogramm und startet es. true = der Launcher soll sich beenden. */
	static boolean starteLauncherUpdate(Release r, Fortschritt f) throws IOException {
		if (r.installerUrl == null) return false;
		if (f != null) f.melde(r.installerName + " wird geladen …");
		Path ziel;
		if ("mac".equals(Installer.betriebssystem())) {
			Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
			ziel = (Files.isDirectory(downloads) ? downloads : ORDNER).resolve(r.installerName);
		} else {
			Files.createDirectories(ORDNER);
			ziel = ORDNER.resolve(r.installerName);
		}
		Path tmp = ziel.resolveSibling(r.installerName + ".download");
		Files.write(tmp, Installer.lade(r.installerUrl));
		Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);
		if ("windows".equals(Installer.betriebssystem())) {
			// Inno Setup: ohne Fragen durchlaufen, Launcher schließen und danach wieder öffnen
			new ProcessBuilder(ziel.toString(), "/SILENT", "/SP-", "/CLOSEAPPLICATIONS", "/NORESTART").start();
			return true;
		}
		new ProcessBuilder("open", ziel.toString()).start();   // Mac: .dmg öffnet sich, App in „Programme“ ziehen
		return false;
	}

	private static String sha256(byte[] d) {
		try {
			StringBuilder b = new StringBuilder();
			for (byte x : MessageDigest.getInstance("SHA-256").digest(d)) b.append(String.format("%02x", x));
			return b.toString();
		} catch (Exception ex) {
			return "";
		}
	}

	private static int zahl(String s) {
		try { return Integer.parseInt(s.replaceAll("\\D.*", "")); } catch (Exception ex) { return 0; }
	}

	private static String text(Object o) { return o == null ? "" : o.toString(); }
}
