package de.bambus.launcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Richtet den Bambus Client im offiziellen Minecraft Launcher ein:
 * Fabric, Fabric API, Bambus-Mod und ein eigenes Profil mit eigenem Spielordner.
 */
final class Installer {

	static final String MC_VERSION = "1.21.11";
	static final String LOADER_VERSION = "0.18.1";
	static final String VERSION_ID = "fabric-loader-" + LOADER_VERSION + "-" + MC_VERSION;
	static final String PROFIL_ID = "bambus-client";
	static final String PROFIL_NAME = "Bambus Client";
	static final String MOD_DATEI = "bambus-client.jar";

	private static final String FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader/"
			+ MC_VERSION + "/" + LOADER_VERSION + "/profile/json";
	private static final String MODRINTH_FABRIC_API = "https://api.modrinth.com/v2/project/fabric-api/version"
			+ "?game_versions=%5B%22" + MC_VERSION + "%22%5D&loaders=%5B%22fabric%22%5D";
	static final String USER_AGENT = "BambusLauncher/" + BambusLauncher.VERSION;

	interface Fortschritt {
		void melde(String text, int prozent);
	}

	final Path mcOrdner;
	final Path spielOrdner;
	final Path modsOrdner;
	/** Mods-Verwaltung (FPS-Boost, Mod Menu, Modrinth-Mods) – wird vom Launcher gesetzt. */
	ModVerwaltung mods;
	/** Hinweise vom letzten Einrichten (z. B. Mod gibt es noch nicht für diese Version). */
	List<String> letzteHinweise = new ArrayList<>();

	Installer(Path mcOrdner) {
		this.mcOrdner = mcOrdner;
		this.spielOrdner = mcOrdner.resolve("bambus-client");
		this.modsOrdner = spielOrdner.resolve("mods");
	}

	// ------------------------------------------------------------ Ordner finden

	static String betriebssystem() {
		String test = System.getProperty("bambus.os");
		if (test != null) {
			return test;
		}
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win")) return "windows";
		if (os.contains("mac")) return "mac";
		return "linux";
	}

	static Path findeMinecraftOrdner() {
		String test = System.getProperty("bambus.mcdir");
		if (test != null) {
			return Paths.get(test);
		}
		String home = System.getProperty("user.home");
		switch (betriebssystem()) {
			case "windows": {
				String appdata = System.getenv("APPDATA");
				return Paths.get(appdata != null ? appdata : home, ".minecraft");
			}
			case "mac":
				return Paths.get(home, "Library", "Application Support", "minecraft");
			default:
				return Paths.get(home, ".minecraft");
		}
	}

	// ----------------------------------------------------------- Bambus-Mod

	/** Ist die Mod im Launcher eingebaut (nach „gradlew build“)? */
	static boolean modEingebaut() {
		return Installer.class.getResource("/" + MOD_DATEI) != null;
	}

	/** Sucht eine bambus-client-*.jar im selben Ordner wie der Launcher. */
	static Path modNebenLauncher() {
		try {
			Path launcher = Paths.get(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			Path ordner = Files.isDirectory(launcher) ? launcher : launcher.getParent();
			if (ordner == null) {
				return null;
			}
			try (DirectoryStream<Path> dateien = Files.newDirectoryStream(ordner, "bambus-client*.jar")) {
				for (Path p : dateien) {
					String n = p.getFileName().toString();
					if (!n.contains("sources") && !n.contains("-dev")) {
						return p;
					}
				}
			}
		} catch (Exception ignoriert) {
			// kein Problem – dann eben nicht
		}
		return null;
	}

	boolean modSchonInstalliert() {
		return Files.isRegularFile(modsOrdner.resolve(MOD_DATEI));
	}

	/** Braucht der Launcher noch eine Mod-Datei vom Nutzer? */
	boolean brauchtModDatei() {
		return Updater.neuereMod() == null && !modEingebaut() && modNebenLauncher() == null && !modSchonInstalliert();
	}

	// ---------------------------------------------------------- Installation

	void installiere(int ramGb, Path modVomNutzer, Fortschritt f) throws IOException {
		Files.createDirectories(modsOrdner);

		f.melde("Fabric " + LOADER_VERSION + " wird eingerichtet …", 10);
		fabricProfil();

		f.melde("Fabric API wird geprüft …", 35);
		fabricApi(f);

		letzteHinweise = new ArrayList<>();
		if (mods != null) {
			letzteHinweise = mods.installiere(f, 50, 72);
		}

		f.melde("Bambus Client wird kopiert …", 75);
		bambusMod(modVomNutzer);

		f.melde("Profil „" + PROFIL_NAME + "“ wird angelegt …", 90);
		launcherProfil(ramGb);

		f.melde("Fertig! Viel Spaß mit dem Bambus Client.", 100);
	}

	void fabricProfil() throws IOException {
		Path ordner = mcOrdner.resolve("versions").resolve(VERSION_ID);
		Path json = ordner.resolve(VERSION_ID + ".json");
		Files.createDirectories(ordner);
		if (!Files.isRegularFile(json) || Files.size(json) == 0) {
			String text = new String(lade(FABRIC_META), StandardCharsets.UTF_8);
			Json.parse(text); // prüfen, dass es gültiges JSON ist
			Files.write(json, text.getBytes(StandardCharsets.UTF_8));
		}
		Path jar = ordner.resolve(VERSION_ID + ".jar");
		if (!Files.exists(jar)) {
			Files.createFile(jar); // leere Datei, genau wie beim offiziellen Fabric-Installer
		}
	}

	void fabricApi(Fortschritt f) throws IOException {
		List<Path> alte = new ArrayList<>();
		try (DirectoryStream<Path> dateien = Files.newDirectoryStream(modsOrdner, "fabric-api-*.jar")) {
			for (Path p : dateien) {
				if (p.getFileName().toString().contains("+" + MC_VERSION + ".jar")) {
					return; // passende Version ist schon da
				}
				alte.add(p);
			}
		}

		f.melde("Fabric API wird heruntergeladen …", 45);
		Object antwort = Json.parse(new String(lade(MODRINTH_FABRIC_API), StandardCharsets.UTF_8));
		List<Object> versionen = Json.liste(antwort);
		if (versionen == null || versionen.isEmpty()) {
			throw new IOException("Keine Fabric API für Minecraft " + MC_VERSION + " gefunden.");
		}
		List<Object> dateien = Json.liste(Json.objekt(versionen.get(0)).get("files"));
		Map<String, Object> datei = Json.objekt(dateien.get(0));
		for (Object o : dateien) {
			if (Boolean.TRUE.equals(Json.objekt(o).get("primary"))) {
				datei = Json.objekt(o);
			}
		}
		String url = (String) datei.get("url");
		String name = (String) datei.get("filename");

		Path tmp = modsOrdner.resolve(name + ".download");
		Files.write(tmp, lade(url));
		Files.move(tmp, modsOrdner.resolve(name), StandardCopyOption.REPLACE_EXISTING);
		for (Path p : alte) {
			Files.deleteIfExists(p);
		}
	}

	void bambusMod(Path vomNutzer) throws IOException {
		Path ziel = modsOrdner.resolve(MOD_DATEI);
		Path tmp = modsOrdner.resolve(MOD_DATEI + ".download");
		Path update = Updater.neuereMod();
		if (update != null) {
			// automatisch geladenes Client-Update hat Vorrang vor der eingebauten Version
			Files.copy(update, tmp, StandardCopyOption.REPLACE_EXISTING);
		} else if (modEingebaut()) {
			try (InputStream in = Installer.class.getResourceAsStream("/" + MOD_DATEI)) {
				Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
			}
		} else {
			Path quelle = vomNutzer != null ? vomNutzer : modNebenLauncher();
			if (quelle == null) {
				if (modSchonInstalliert()) {
					return;
				}
				throw new IOException("Die Bambus-Client-Mod (bambus-client-….jar) wurde nicht gefunden.");
			}
			Files.copy(quelle, tmp, StandardCopyOption.REPLACE_EXISTING);
		}
		Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);

		// ältere Kopien mit Versionsnummer im Namen entfernen
		try (DirectoryStream<Path> dateien = Files.newDirectoryStream(modsOrdner, "bambus-client*.jar")) {
			for (Path p : dateien) {
				if (!p.getFileName().toString().equals(MOD_DATEI)) {
					Files.deleteIfExists(p);
				}
			}
		}
	}

	void launcherProfil(int ramGb) throws IOException {
		String[] namen = {"launcher_profiles.json", "launcher_profiles_microsoft_store.json"};
		boolean geschrieben = false;
		for (String n : namen) {
			Path p = mcOrdner.resolve(n);
			if (Files.isRegularFile(p)) {
				schreibeProfil(p, ramGb);
				geschrieben = true;
			}
		}
		if (!geschrieben) {
			schreibeProfil(mcOrdner.resolve(namen[0]), ramGb);
		}
	}

	private void schreibeProfil(Path datei, int ramGb) throws IOException {
		Map<String, Object> wurzel;
		if (Files.isRegularFile(datei) && Files.size(datei) > 0) {
			String text = new String(Files.readAllBytes(datei), StandardCharsets.UTF_8);
			try {
				wurzel = Json.objekt(Json.parse(text));
			} catch (RuntimeException e) {
				throw new IOException("Die Datei " + datei.getFileName() + " ist beschädigt: " + e.getMessage());
			}
			if (wurzel == null) {
				throw new IOException("Unerwarteter Inhalt in " + datei.getFileName());
			}
			Path sicherung = datei.resolveSibling(datei.getFileName() + ".vor-bambus");
			if (!Files.exists(sicherung)) {
				Files.copy(datei, sicherung);
			}
		} else {
			wurzel = new LinkedHashMap<>();
			wurzel.put("settings", new LinkedHashMap<String, Object>());
			wurzel.put("version", 3L);
		}

		Map<String, Object> profile = Json.objekt(wurzel.get("profiles"));
		if (profile == null) {
			profile = new LinkedHashMap<>();
			wurzel.put("profiles", profile);
		}

		Map<String, Object> alt = Json.objekt(profile.get(PROFIL_ID));
		String jetzt = Instant.now().toString();

		Map<String, Object> profil = new LinkedHashMap<>();
		profil.put("created", alt != null && alt.get("created") != null ? alt.get("created") : jetzt);
		profil.put("gameDir", spielOrdner.toAbsolutePath().toString());
		String icon = iconDataUrl();
		if (icon != null) {
			profil.put("icon", icon);
		}
		profil.put("javaArgs", "-Xmx" + ramGb + "G -Xms1G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions"
				+ " -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M");
		profil.put("lastUsed", jetzt);
		profil.put("lastVersionId", VERSION_ID);
		profil.put("name", PROFIL_NAME);
		profil.put("type", "custom");
		profile.put(PROFIL_ID, profil);

		Files.createDirectories(datei.getParent());
		Path tmp = datei.resolveSibling(datei.getFileName() + ".tmp");
		Files.write(tmp, Json.write(wurzel).getBytes(StandardCharsets.UTF_8));
		Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING);
	}

	private static String iconDataUrl() {
		try (InputStream in = Installer.class.getResourceAsStream("/bambus-icon.png")) {
			if (in == null) {
				return null;
			}
			return "data:image/png;base64," + Base64.getEncoder().encodeToString(liesAlles(in));
		} catch (IOException e) {
			return null;
		}
	}

	// ------------------------------------------------- Minecraft Launcher starten

	/** Versucht, den offiziellen Minecraft Launcher zu öffnen. */
	static boolean starteMinecraftLauncher() {
		try {
			switch (betriebssystem()) {
				case "windows": {
					String[] orte = {
							System.getenv("ProgramFiles(x86)") + "\\Minecraft Launcher\\MinecraftLauncher.exe",
							System.getenv("ProgramFiles") + "\\Minecraft Launcher\\MinecraftLauncher.exe",
							System.getenv("LOCALAPPDATA") + "\\Programs\\Minecraft Launcher\\MinecraftLauncher.exe"
					};
					for (String ort : orte) {
						if (new File(ort).isFile()) {
							new ProcessBuilder(ort).start();
							return true;
						}
					}
					// Minecraft Launcher aus dem Microsoft Store / der Xbox-App
					new ProcessBuilder("explorer.exe",
							"shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft").start();
					return true;
				}
				case "mac": {
					Process p = new ProcessBuilder("open", "-a", "Minecraft").start();
					return p.waitFor() == 0;
				}
				default: {
					new ProcessBuilder("minecraft-launcher").start();
					return true;
				}
			}
		} catch (Exception e) {
			return false;
		}
	}

	// ------------------------------------------------ Windows-Verknüpfungen

	/** Pfad zum laufenden Programm: die .exe (mit eingebautem Java) oder die .jar. */
	static Path eigenesProgramm() {
		String exe = System.getProperty("jpackage.app-path");
		if (exe != null) {
			return Paths.get(exe);
		}
		try {
			Path p = Paths.get(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			return Files.isRegularFile(p) ? p : null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Legt unter Windows eine Verknüpfung an.
	 * @param ordner "Desktop" oder "Programs" (= Startmenü)
	 */
	static void erstelleVerknuepfung(String ordner) throws IOException {
		if (!"windows".equals(betriebssystem())) {
			throw new IOException("Verknüpfungen gibt es nur unter Windows.");
		}
		Path programm = eigenesProgramm();
		if (programm == null) {
			throw new IOException("Das Launcher-Programm wurde nicht gefunden.");
		}
		String ziel;
		String argumente = "";
		String symbol;
		if (programm.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
			ziel = Paths.get(System.getProperty("java.home"), "bin", "javaw.exe").toString();
			argumente = "-jar \"" + programm + "\"";
			symbol = ziel;
		} else {
			ziel = programm.toString();
			symbol = ziel;
		}
		String befehl = "$o=[Environment]::GetFolderPath('" + ordner + "');"
				+ "$s=(New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $o 'Bambus Launcher.lnk'));"
				+ "$s.TargetPath=" + ps(ziel) + ";"
				+ "$s.Arguments=" + ps(argumente) + ";"
				+ "$s.WorkingDirectory=" + ps(programm.getParent().toString()) + ";"
				+ "$s.IconLocation=" + ps(symbol + ",0") + ";"
				+ "$s.Description='Bambus Client starten';"
				+ "$s.Save()";
		try {
			Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
					"-Command", befehl).redirectErrorStream(true).start();
			String ausgabe = new String(liesAlles(p.getInputStream()), StandardCharsets.UTF_8).trim();
			if (p.waitFor() != 0) {
				throw new IOException("PowerShell-Fehler: " + ausgabe);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Abgebrochen.");
		}
	}

	/** Text für PowerShell in einfache Anführungszeichen setzen. */
	private static String ps(String text) {
		return "'" + text.replace("'", "''") + "'";
	}

	// ------------------------------------------------------------- Download

	static byte[] lade(String adresse) throws IOException {
		String url = adresse;
		for (int umleitung = 0; umleitung < 5; umleitung++) {
			HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
			c.setRequestProperty("User-Agent", USER_AGENT);
			c.setConnectTimeout(15000);
			c.setReadTimeout(60000);
			c.setInstanceFollowRedirects(false);
			int code = c.getResponseCode();
			if (code >= 300 && code < 400 && c.getHeaderField("Location") != null) {
				url = URI.create(url).resolve(c.getHeaderField("Location")).toString();
				continue;
			}
			if (code != 200) {
				throw new IOException("Download fehlgeschlagen (" + code + "): " + url);
			}
			try (InputStream in = c.getInputStream()) {
				return liesAlles(in);
			}
		}
		throw new IOException("Zu viele Weiterleitungen: " + adresse);
	}

	static byte[] liesAlles(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] puffer = new byte[16384];
		int n;
		while ((n = in.read(puffer)) > 0) {
			out.write(puffer, 0, n);
		}
		return out.toByteArray();
	}
}
