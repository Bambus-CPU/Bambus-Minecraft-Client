package de.bambus.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Startet Minecraft + Fabric + Bambus direkt – ohne den offiziellen Launcher, aber genauso wie er:
 * Spiel, Bibliotheken, Assets und das passende Java kommen frisch von den Mojang-Servern
 * (nichts davon steckt im Bambus-Download). Schon vorhandene Dateien im .minecraft-Ordner
 * werden mitbenutzt und per Prüfsumme kontrolliert.
 *
 * Nur aktiv, wenn der Spieler mit Microsoft angemeldet ist (siehe {@link MicrosoftKonto}).
 */
final class DirektStart {

	private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
	private static final String JAVA_MANIFEST =
			"https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";
	private static final String ASSETS = "https://resources.download.minecraft.net/";

	private final Installer installer;
	private final Path mc;

	DirektStart(Installer installer) {
		this.installer = installer;
		this.mc = installer.mcOrdner;
	}

	// ============================================================ Betriebssystem

	static String os() {
		String n = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return n.contains("win") ? "windows" : n.contains("mac") ? "osx" : "linux";
	}

	static boolean arm() {
		String a = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		return a.contains("aarch64") || a.contains("arm");
	}

	/** Mojang-Plattformname für Java-Laufzeiten. */
	static String javaPlattform() {
		switch (os()) {
			case "windows": return arm() ? "windows-arm64" : "windows-x64";
			case "osx": return arm() ? "mac-os-arm64" : "mac-os";
			default: return "linux";
		}
	}

	/** Regeln aus den Versions-Dateien (Betriebssystem, Architektur, Features – Features sind bei uns aus). */
	static boolean erlaubt(Object regelnObj) {
		List<Object> regeln = Json.liste(regelnObj);
		if (regeln == null || regeln.isEmpty()) return true;
		boolean erlaubt = false;
		for (Object o : regeln) {
			Map<String, Object> r = Json.objekt(o);
			if (r == null) continue;
			boolean passt = true;
			Map<String, Object> os = Json.objekt(r.get("os"));
			if (os != null) {
				if (os.get("name") instanceof String && !os.get("name").equals(os())) passt = false;
				if (os.get("arch") instanceof String) {
					String arch = (String) os.get("arch");
					if (arch.equals("x86") && System.getProperty("os.arch", "").contains("64")) passt = false;
					if (arch.equals("arm64") && !arm()) passt = false;
				}
			}
			Map<String, Object> features = Json.objekt(r.get("features"));
			if (features != null && !features.isEmpty()) passt = false; // Demo, Auflösung, Quick Play: nicht benutzt
			if (passt) erlaubt = "allow".equals(r.get("action"));
		}
		return erlaubt;
	}

	// ============================================================ Start

	Process starte(MicrosoftKonto.Sitzung s, int ramGb, Installer.Fortschritt f) throws IOException {
		f.melde("Minecraft-Version wird geprüft …", 5);
		Map<String, Object> vanilla = vanillaVersion();
		Map<String, Object> fabric = Json.objekt(Json.parse(new String(Files.readAllBytes(
				mc.resolve("versions").resolve(Installer.VERSION_ID).resolve(Installer.VERSION_ID + ".json")), StandardCharsets.UTF_8)));
		if (fabric == null) throw new IOException("Fabric-Profil fehlt.");

		f.melde("Minecraft wird geladen …", 10);
		Path clientJar = mc.resolve("versions").resolve(Installer.MC_VERSION).resolve(Installer.MC_VERSION + ".jar");
		Map<String, Object> client = Json.objekt(Json.objekt(vanilla.get("downloads")).get("client"));
		sichere(str(client.get("url")), clientJar, str(client.get("sha1")));

		f.melde("Bibliotheken werden geprüft …", 20);
		List<Path> klassenpfad = bibliotheken(fabric, vanilla, f);
		klassenpfad.add(clientJar);

		f.melde("Spielgrafiken und Töne werden geprüft …", 45);
		String assetIndex = assets(vanilla, f);

		f.melde("Java für Minecraft wird geprüft …", 80);
		Path java = java(vanilla, f);

		Path natives = mc.resolve("bin").resolve("bambus-" + Installer.MC_VERSION);
		Files.createDirectories(natives);
		Files.createDirectories(installer.spielOrdner);

		// Platzhalter wie im offiziellen Launcher
		Map<String, String> w = new LinkedHashMap<>();
		w.put("auth_player_name", s.name);
		w.put("version_name", Installer.VERSION_ID);
		w.put("game_directory", installer.spielOrdner.toString());
		w.put("assets_root", mc.resolve("assets").toString());
		w.put("assets_index_name", assetIndex);
		w.put("auth_uuid", s.uuid);
		w.put("auth_access_token", s.token);
		w.put("clientid", "");
		w.put("auth_xuid", s.xuid == null ? "" : s.xuid);
		w.put("user_type", "msa");
		w.put("version_type", "release");
		w.put("natives_directory", natives.toString());
		w.put("launcher_name", "bambus-launcher");
		w.put("launcher_version", BambusLauncher.VERSION);
		w.put("library_directory", mc.resolve("libraries").toString());
		w.put("classpath_separator", File.pathSeparator);
		StringBuilder cp = new StringBuilder();
		for (Path p : klassenpfad) {
			if (cp.length() > 0) cp.append(File.pathSeparator);
			cp.append(p.toString());
		}
		w.put("classpath", cp.toString());

		List<String> befehl = new ArrayList<>();
		befehl.add(java.toString());
		befehl.add("-Xmx" + ramGb + "G");
		befehl.add("-Xms1G");
		for (String a : new String[]{"-XX:+UseG1GC", "-XX:+UnlockExperimentalVMOptions", "-XX:G1NewSizePercent=20",
				"-XX:G1ReservePercent=20", "-XX:MaxGCPauseMillis=50", "-XX:G1HeapRegionSize=32M"}) befehl.add(a);
		Map<String, Object> logging = Json.objekt(Json.objekt(vanilla.get("logging")) == null ? null
				: Json.objekt(vanilla.get("logging")).get("client"));
		if (logging != null) {
			Map<String, Object> datei = Json.objekt(logging.get("file"));
			Path log = mc.resolve("assets").resolve("log_configs").resolve(str(datei.get("id")));
			sichere(str(datei.get("url")), log, str(datei.get("sha1")));
			befehl.add(str(logging.get("argument")).replace("${path}", log.toString()));
		}
		argumente(vanilla, "jvm", w, befehl);
		argumente(fabric, "jvm", w, befehl);
		befehl.add(str(fabric.get("mainClass")));
		argumente(vanilla, "game", w, befehl);
		argumente(fabric, "game", w, befehl);

		f.melde("Minecraft startet …", 98);
		Path logDatei = installer.spielOrdner.resolve("logs").resolve("bambus-start.log");
		Files.createDirectories(logDatei.getParent());
		ProcessBuilder pb = new ProcessBuilder(befehl);
		pb.directory(installer.spielOrdner.toFile());
		pb.redirectErrorStream(true);
		pb.redirectOutput(logDatei.toFile());
		return pb.start();
	}

	// ============================================================ Teile

	private Map<String, Object> vanillaVersion() throws IOException {
		Path json = mc.resolve("versions").resolve(Installer.MC_VERSION).resolve(Installer.MC_VERSION + ".json");
		Map<String, Object> m = Json.objekt(Json.parse(new String(Installer.lade(MANIFEST), StandardCharsets.UTF_8)));
		for (Object o : Json.liste(m.get("versions"))) {
			Map<String, Object> v = Json.objekt(o);
			if (Installer.MC_VERSION.equals(v.get("id"))) {
				sichere(str(v.get("url")), json, str(v.get("sha1")));
				return Json.objekt(Json.parse(new String(Files.readAllBytes(json), StandardCharsets.UTF_8)));
			}
		}
		throw new IOException("Minecraft " + Installer.MC_VERSION + " nicht bei Mojang gefunden.");
	}

	private List<Path> bibliotheken(Map<String, Object> fabric, Map<String, Object> vanilla, Installer.Fortschritt f) throws IOException {
		List<Path> pfade = new ArrayList<>();
		Set<String> schon = new LinkedHashSet<>();
		List<Object> alle = new ArrayList<>();
		if (Json.liste(fabric.get("libraries")) != null) alle.addAll(Json.liste(fabric.get("libraries")));   // Fabric zuerst
		alle.addAll(Json.liste(vanilla.get("libraries")));
		int i = 0;
		for (Object o : alle) {
			i++;
			Map<String, Object> lib = Json.objekt(o);
			if (lib == null || !erlaubt(lib.get("rules"))) continue;
			String name = str(lib.get("name"));
			String[] teile = name.split(":");
			if (teile.length < 3) continue;
			String schluessel = teile[0] + ":" + teile[1] + (teile.length > 3 ? ":" + teile[3] : "");
			if (!schon.add(schluessel)) continue;            // gleiche Bibliothek: die von Fabric gewinnt

			Map<String, Object> dl = Json.objekt(lib.get("downloads"));
			Map<String, Object> artefakt = dl == null ? null : Json.objekt(dl.get("artifact"));
			String pfad, url, sha1;
			if (artefakt != null) {
				pfad = str(artefakt.get("path"));
				url = str(artefakt.get("url"));
				sha1 = str(artefakt.get("sha1"));
			} else {                                          // Fabric-Stil: Maven-Koordinaten + Repository
				pfad = mavenPfad(teile);
				String repo = lib.get("url") instanceof String ? (String) lib.get("url") : "https://libraries.minecraft.net/";
				url = (repo.endsWith("/") ? repo : repo + "/") + pfad;
				sha1 = str(lib.get("sha1"));
			}
			if (pfad.isEmpty()) continue;
			Path ziel = mc.resolve("libraries").resolve(pfad);
			if (!url.isEmpty()) sichere(url, ziel, sha1);
			pfade.add(ziel);
			if (i % 10 == 0) f.melde("Bibliotheken: " + i + " / " + alle.size(), 20 + 25 * i / Math.max(1, alle.size()));
		}
		return pfade;
	}

	static String mavenPfad(String[] t) {
		String datei = t[1] + "-" + t[2] + (t.length > 3 ? "-" + t[3] : "") + ".jar";
		return t[0].replace('.', '/') + "/" + t[1] + "/" + t[2] + "/" + datei;
	}

	private String assets(Map<String, Object> vanilla, Installer.Fortschritt f) throws IOException {
		Map<String, Object> ai = Json.objekt(vanilla.get("assetIndex"));
		String id = str(ai.get("id"));
		Path index = mc.resolve("assets").resolve("indexes").resolve(id + ".json");
		sichere(str(ai.get("url")), index, str(ai.get("sha1")));
		Map<String, Object> objekte = Json.objekt(Json.objekt(Json.parse(new String(Files.readAllBytes(index), StandardCharsets.UTF_8))).get("objects"));
		List<String[]> fehlend = new ArrayList<>();
		for (Object o : objekte.values()) {
			Map<String, Object> e = Json.objekt(o);
			String hash = str(e.get("hash"));
			Path ziel = mc.resolve("assets").resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
			long groesse = e.get("size") instanceof Number ? ((Number) e.get("size")).longValue() : -1;
			if (!Files.isRegularFile(ziel) || (groesse >= 0 && Files.size(ziel) != groesse)) {
				fehlend.add(new String[]{hash, ziel.toString()});
			}
		}
		parallel(fehlend, a -> sichere(ASSETS + a[0].substring(0, 2) + "/" + a[0], java.nio.file.Paths.get(a[1]), a[0]),
				(n, gesamt) -> f.melde("Spielgrafiken und Töne: " + n + " / " + gesamt, 45 + 33 * n / Math.max(1, gesamt)));
		return id;
	}

	private Path java(Map<String, Object> vanilla, Installer.Fortschritt f) throws IOException {
		Map<String, Object> jv = Json.objekt(vanilla.get("javaVersion"));
		String komponente = jv == null ? "java-runtime-delta" : str(jv.get("component"));
		String plattform = javaPlattform();
		// 1) Java, das der offizielle Launcher schon geladen hat
		Path offiziell = mc.resolve("runtime").resolve(komponente).resolve(plattform).resolve(komponente);
		Path gefunden = findeJava(offiziell);
		if (gefunden != null) return gefunden;
		// 2) selbst von Mojang laden
		Path ziel = installer.spielOrdner.resolve("runtime").resolve(komponente);
		gefunden = findeJava(ziel);
		if (gefunden != null && Files.isRegularFile(ziel.resolve(".bambus-fertig"))) return gefunden;
		Map<String, Object> alle = Json.objekt(Json.parse(new String(Installer.lade(JAVA_MANIFEST), StandardCharsets.UTF_8)));
		List<Object> liste = Json.liste(Json.objekt(alle.get(plattform)).get(komponente));
		if (liste == null || liste.isEmpty()) throw new IOException("Kein Java von Mojang für " + plattform + ".");
		Map<String, Object> man = Json.objekt(Json.objekt(liste.get(0)).get("manifest"));
		Map<String, Object> dateien = Json.objekt(Json.objekt(Json.parse(new String(Installer.lade(str(man.get("url"))),
				StandardCharsets.UTF_8))).get("files"));
		List<String[]> jobs = new ArrayList<>();
		List<String> ausfuehrbar = new ArrayList<>();
		for (Map.Entry<String, Object> e : dateien.entrySet()) {
			Map<String, Object> d = Json.objekt(e.getValue());
			Path p = ziel.resolve(e.getKey());
			if ("directory".equals(d.get("type"))) {
				Files.createDirectories(p);
			} else if ("file".equals(d.get("type"))) {
				Map<String, Object> raw = Json.objekt(Json.objekt(d.get("downloads")).get("raw"));
				jobs.add(new String[]{str(raw.get("url")), p.toString(), str(raw.get("sha1"))});
				if (Boolean.TRUE.equals(d.get("executable"))) ausfuehrbar.add(p.toString());
			}
		}
		parallel(jobs, j -> sichere(j[0], java.nio.file.Paths.get(j[1]), j[2]),
				(n, gesamt) -> f.melde("Java für Minecraft: " + n + " / " + gesamt, 80 + 17 * n / Math.max(1, gesamt)));
		for (Map.Entry<String, Object> e : dateien.entrySet()) {          // symbolische Links (macOS-Bundle)
			Map<String, Object> d = Json.objekt(e.getValue());
			if ("link".equals(d.get("type"))) {
				Path p = ziel.resolve(e.getKey());
				try {
					Files.deleteIfExists(p);
					Files.createSymbolicLink(p, java.nio.file.Paths.get(str(d.get("target"))));
				} catch (Exception ignoriert) {
					// unter Windows nicht nötig
				}
			}
		}
		for (String p : ausfuehrbar) new File(p).setExecutable(true);
		Files.write(ziel.resolve(".bambus-fertig"), new byte[0]);
		gefunden = findeJava(ziel);
		if (gefunden == null) throw new IOException("Java für Minecraft wurde nicht gefunden.");
		return gefunden;
	}

	private static Path findeJava(Path wurzel) {
		if (!Files.isDirectory(wurzel)) return null;
		String exe = os().equals("windows") ? "javaw.exe" : "java";
		for (String p : new String[]{"bin/" + exe, "jre.bundle/Contents/Home/bin/" + exe}) {
			Path j = wurzel.resolve(p);
			if (Files.isRegularFile(j)) return j;
		}
		return null;
	}

	/** Neue Argument-Liste (seit 1.13): Texte und Objekte mit Regeln. */
	static void argumente(Map<String, Object> version, String art, Map<String, String> w, List<String> ziel) {
		Map<String, Object> args = Json.objekt(version.get("arguments"));
		if (args == null) return;
		List<Object> liste = Json.liste(args.get(art));
		if (liste == null) return;
		for (Object o : liste) {
			if (o instanceof String) {
				ziel.add(ersetze((String) o, w));
			} else {
				Map<String, Object> a = Json.objekt(o);
				if (a == null || !erlaubt(a.get("rules"))) continue;
				Object v = a.get("value");
				if (v instanceof String) ziel.add(ersetze((String) v, w));
				else if (Json.liste(v) != null) for (Object x : Json.liste(v)) ziel.add(ersetze(String.valueOf(x), w));
			}
		}
	}

	static String ersetze(String s, Map<String, String> w) {
		for (Map.Entry<String, String> e : w.entrySet()) s = s.replace("${" + e.getKey() + "}", e.getValue());
		return s;
	}

	// ============================================================ Downloads

	/** Lädt eine Datei, wenn sie fehlt oder die Prüfsumme nicht stimmt. */
	static void sichere(String url, Path ziel, String sha1) throws IOException {
		if (Files.isRegularFile(ziel) && (sha1 == null || sha1.isEmpty() || sha1.equalsIgnoreCase(sha1(ziel)))) return;
		Files.createDirectories(ziel.getParent());
		byte[] daten = Installer.lade(url);
		if (sha1 != null && !sha1.isEmpty() && !sha1.equalsIgnoreCase(sha1(daten))) {
			throw new IOException("Prüfsumme falsch: " + ziel.getFileName());
		}
		Path tmp = ziel.resolveSibling(ziel.getFileName() + ".download");
		Files.write(tmp, daten);
		Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);
	}

	static String sha1(Path p) throws IOException {
		return sha1(Files.readAllBytes(p));
	}

	static String sha1(byte[] b) {
		try {
			StringBuilder s = new StringBuilder();
			for (byte x : MessageDigest.getInstance("SHA-1").digest(b)) s.append(String.format("%02x", x));
			return s.toString();
		} catch (Exception e) {
			return "";
		}
	}

	interface Arbeit { void tu(String[] job) throws IOException; }
	interface Stand { void melde(int fertig, int gesamt); }

	private static void parallel(List<String[]> jobs, Arbeit arbeit, Stand stand) throws IOException {
		if (jobs.isEmpty()) return;
		ExecutorService pool = Executors.newFixedThreadPool(8);
		AtomicInteger fertig = new AtomicInteger();
		List<Future<?>> fs = new ArrayList<>();
		for (String[] j : jobs) {
			fs.add(pool.submit(() -> {
				arbeit.tu(j);
				int n = fertig.incrementAndGet();
				if (n % 25 == 0 || n == jobs.size()) stand.melde(n, jobs.size());
				return null;
			}));
		}
		pool.shutdown();
		try {
			for (Future<?> fu : fs) fu.get();
			pool.awaitTermination(1, TimeUnit.MINUTES);
		} catch (Exception e) {
			pool.shutdownNow();
			Throwable t = e.getCause() != null ? e.getCause() : e;
			throw t instanceof IOException ? (IOException) t : new IOException(t.getMessage(), t);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
