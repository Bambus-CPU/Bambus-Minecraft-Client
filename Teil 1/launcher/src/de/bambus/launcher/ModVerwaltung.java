package de.bambus.launcher;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mods-Verwaltung: empfohlene Mods (FPS-Boost, Mod Menu), Mods von Modrinth und eigene .jar-Dateien.
 *
 * Alles kommt direkt von Modrinth (offizielle Download-Adressen der Mod-Autoren) – der Launcher
 * liefert keine fremden Mods mit. Einstellungen und die Liste der von Bambus verwalteten Dateien
 * liegen in config/bambus-mods.json (eigene Datei, damit die Mod-Konfiguration bambus.json unberührt bleibt).
 */
final class ModVerwaltung {

	/** Modrinth-Projekt-ID der Fabric API – wird vom Installer selbst verwaltet. */
	private static final String FABRIC_API_ID = "P7dR8mSH";
	private static final String API = "https://api.modrinth.com/v2/";

	/** Empfohlene Mods: {slug, Name, Beschreibung, Gruppe}. Gruppe "FPS" gehört zum FPS-Boost. */
	static final String[][] EMPFOHLEN = {
			{"sodium", "Sodium", "Neue Grafik-Engine – oft doppelt so viele FPS", "FPS"},
			{"lithium", "Lithium", "Schnellere Spiel-Logik: Physik, Tiere, Chunks", "FPS"},
			{"ferrite-core", "FerriteCore", "Braucht deutlich weniger Arbeitsspeicher", "FPS"},
			{"immediatelyfast", "ImmediatelyFast", "Zeichnet HUD, Text und Schilder schneller", "FPS"},
			{"entityculling", "Entity Culling", "Verdeckte Tiere und Truhen werden nicht gezeichnet", "FPS"},
			{"dynamic-fps", "Dynamic FPS", "Spart Leistung, wenn Minecraft im Hintergrund ist", "FPS"},
			{"modmenu", "Mod Menu", "„Mods“-Knopf im Spiel: alle Mods ansehen und einstellen", "MENÜ"},
	};

	/** Ein Suchergebnis oder eine zusätzlich gewählte Mod von Modrinth. */
	static final class Projekt {
		final String slug, titel, beschreibung;
		final long downloads;

		Projekt(String slug, String titel, String beschreibung, long downloads) {
			this.slug = slug;
			this.titel = titel;
			this.beschreibung = beschreibung;
			this.downloads = downloads;
		}
	}

	/** Eine .jar im Mods-Ordner, die der Spieler selbst hineingelegt hat. */
	static final class EigeneMod {
		final Path datei;
		final boolean an;

		EigeneMod(Path datei, boolean an) {
			this.datei = datei;
			this.an = an;
		}

		String name() {
			String n = datei.getFileName().toString();
			return n.replaceAll("\\.disabled$", "").replaceAll("\\.jar$", "");
		}
	}

	private final Installer installer;
	private final Path datei;
	private Map<String, Object> daten = new LinkedHashMap<>();

	ModVerwaltung(Installer installer) {
		this.installer = installer;
		this.datei = installer.spielOrdner.resolve("config").resolve("bambus-mods.json");
		laden();
	}

	// ================================================================ Einstellungen

	private void laden() {
		try {
			if (Files.isRegularFile(datei)) {
				Map<String, Object> m = Json.objekt(Json.parse(new String(Files.readAllBytes(datei), StandardCharsets.UTF_8)));
				if (m != null) {
					daten = m;
				}
			}
		} catch (Exception e) {
			System.err.println("[Bambus] bambus-mods.json nicht lesbar, Standard wird benutzt: " + e);
		}
	}

	synchronized void speichern() {
		try {
			Files.createDirectories(datei.getParent());
			Path tmp = datei.resolveSibling("bambus-mods.json.tmp");
			Files.write(tmp, Json.write(daten).getBytes(StandardCharsets.UTF_8));
			Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.err.println("[Bambus] bambus-mods.json nicht speicherbar: " + e);
		}
	}

	private Map<String, Object> map(String schluessel) {
		Map<String, Object> m = Json.objekt(daten.get(schluessel));
		if (m == null) {
			m = new LinkedHashMap<>();
			daten.put(schluessel, m);
		}
		return m;
	}

	boolean istAn(String slug) {
		Object v = map("an").get(slug);
		if (v instanceof Boolean) {
			return (Boolean) v;
		}
		return istEmpfohlen(slug); // empfohlene Mods sind standardmäßig an
	}

	void setzeAn(String slug, boolean an) {
		map("an").put(slug, an);
		speichern();
	}

	/** FPS-Boost = alle Mods der Gruppe "FPS" gemeinsam. */
	boolean fpsBoost() {
		for (String[] m : EMPFOHLEN) {
			if ("FPS".equals(m[3]) && istAn(m[0])) {
				return true;
			}
		}
		return false;
	}

	void setzeFpsBoost(boolean an) {
		for (String[] m : EMPFOHLEN) {
			if ("FPS".equals(m[3])) {
				map("an").put(m[0], an);
			}
		}
		speichern();
	}

	static boolean istEmpfohlen(String slug) {
		for (String[] m : EMPFOHLEN) {
			if (m[0].equals(slug)) {
				return true;
			}
		}
		return false;
	}

	/** Von Modrinth zusätzlich gewählte Mods (nicht die empfohlenen). */
	List<Projekt> extras() {
		List<Projekt> l = new ArrayList<>();
		for (Map.Entry<String, Object> e : map("extra").entrySet()) {
			Map<String, Object> p = Json.objekt(e.getValue());
			if (p != null) {
				l.add(new Projekt(e.getKey(), str(p.get("titel"), e.getKey()), str(p.get("beschreibung"), ""), 0));
			}
		}
		return l;
	}

	void fuegeHinzu(Projekt p) {
		Map<String, Object> eintrag = new LinkedHashMap<>();
		eintrag.put("titel", p.titel);
		eintrag.put("beschreibung", p.beschreibung);
		map("extra").put(p.slug, eintrag);
		map("an").put(p.slug, true);
		speichern();
	}

	void entferne(String slug) {
		map("extra").remove(slug);
		map("an").remove(slug);
		speichern();
	}

	boolean istExtra(String slug) {
		return map("extra").containsKey(slug);
	}

	// ================================================================ Eigene .jar-Dateien

	/** Dateinamen, die Bambus selbst verwaltet (nicht als "eigene Mod" anzeigen). */
	private Set<String> verwalteteDateien() {
		Set<String> s = new LinkedHashSet<>();
		for (Object v : map("installiert").values()) {
			if (v instanceof String) {
				s.add((String) v);
			}
		}
		return s;
	}

	List<EigeneMod> eigeneMods() {
		List<EigeneMod> l = new ArrayList<>();
		if (!Files.isDirectory(installer.modsOrdner)) {
			return l;
		}
		Set<String> verwaltet = verwalteteDateien();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(installer.modsOrdner)) {
			for (Path p : ds) {
				String n = p.getFileName().toString();
				boolean jar = n.endsWith(".jar"), aus = n.endsWith(".jar.disabled");
				if (!jar && !aus) continue;
				if (n.equals(Installer.MOD_DATEI) || n.startsWith("fabric-api-") || verwaltet.contains(n)) continue;
				l.add(new EigeneMod(p, jar));
			}
		} catch (IOException e) {
			return l;
		}
		l.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		return l;
	}

	void schalteEigene(EigeneMod m, boolean an) throws IOException {
		String n = m.datei.getFileName().toString();
		String neu = an ? n.replaceAll("\\.disabled$", "") : (n.endsWith(".disabled") ? n : n + ".disabled");
		if (!neu.equals(n)) {
			Files.move(m.datei, m.datei.resolveSibling(neu), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	void loescheEigene(EigeneMod m) throws IOException {
		Files.deleteIfExists(m.datei);
	}

	void kopiereEigene(Path jar) throws IOException {
		Files.createDirectories(installer.modsOrdner);
		Files.copy(jar, installer.modsOrdner.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
	}

	// ================================================================ Modrinth

	/** Sucht Fabric-Mods für die Minecraft-Version des Clients. */
	List<Projekt> suche(String text) throws IOException {
		String facets = "[[\"categories:fabric\"],[\"versions:" + Installer.MC_VERSION + "\"],[\"project_type:mod\"]]";
		String url = API + "search?limit=12&index=relevance&query=" + enc(text) + "&facets=" + enc(facets);
		Map<String, Object> antwort = Json.objekt(Json.parse(new String(Installer.lade(url), StandardCharsets.UTF_8)));
		List<Projekt> l = new ArrayList<>();
		if (antwort == null) return l;
		List<Object> treffer = Json.liste(antwort.get("hits"));
		if (treffer == null) return l;
		for (Object o : treffer) {
			Map<String, Object> h = Json.objekt(o);
			if (h == null) continue;
			Object dl = h.get("downloads");
			l.add(new Projekt(str(h.get("slug"), ""), str(h.get("title"), "?"), str(h.get("description"), ""),
					dl instanceof Number ? ((Number) dl).longValue() : 0));
		}
		return l;
	}

	/**
	 * Bringt den Mods-Ordner auf den gewünschten Stand: lädt eingeschaltete Mods (samt benötigter
	 * Abhängigkeiten) passend zu Minecraft {@link Installer#MC_VERSION}, entfernt abgeschaltete.
	 * Einzelne Fehler stoppen den Start nicht – sie werden als Hinweise zurückgegeben.
	 */
	List<String> installiere(Installer.Fortschritt f, int von, int bis) {
		List<String> hinweise = new ArrayList<>();
		List<String> gewuenscht = new ArrayList<>();
		for (String[] m : EMPFOHLEN) if (istAn(m[0])) gewuenscht.add(m[0]);
		for (Projekt p : extras()) if (istAn(p.slug)) gewuenscht.add(p.slug);

		Map<String, Object> installiert = map("installiert");
		Map<String, String> neu = new LinkedHashMap<>();
		Set<String> gesehen = new LinkedHashSet<>();
		List<String> warteschlange = new ArrayList<>(gewuenscht);
		int nr = 0;
		while (!warteschlange.isEmpty()) {
			String projekt = warteschlange.remove(0);
			if (!gesehen.add(projekt)) continue;
			nr++;
			f.melde("Mods: " + projekt + " …", von + (bis - von) * Math.min(nr, 10) / 10);
			try {
				Map<String, Object> version = passendeVersion(projekt);
				if (version == null) {
					hinweise.add(projekt + " gibt es noch nicht für Minecraft " + Installer.MC_VERSION);
					continue;
				}
				String slug = projekt;
				Map<String, Object> datei = primaerDatei(version);
				String name = str(datei.get("filename"), projekt + ".jar");
				Path ziel = installer.modsOrdner.resolve(name);
				if (!Files.isRegularFile(ziel)) {
					Path tmp = installer.modsOrdner.resolve(name + ".download");
					Files.write(tmp, Installer.lade(str(datei.get("url"), "")));
					Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);
				}
				neu.put(slug, name);
				// benötigte Abhängigkeiten mitnehmen (Fabric API verwaltet der Installer selbst)
				List<Object> abh = Json.liste(version.get("dependencies"));
				if (abh != null) {
					for (Object o : abh) {
						Map<String, Object> a = Json.objekt(o);
						if (a == null || !"required".equals(a.get("dependency_type"))) continue;
						Object id = a.get("project_id");
						if (id instanceof String && !FABRIC_API_ID.equals(id)) warteschlange.add((String) id);
					}
				}
			} catch (Exception e) {
				hinweise.add(projekt + ": " + e.getMessage());
				// alte Datei behalten, wenn der Download gerade nicht klappt
				Object alt = installiert.get(projekt);
				if (alt instanceof String) neu.put(projekt, (String) alt);
			}
		}
		// Was Bambus früher installiert hat und jetzt nicht mehr gebraucht wird: löschen
		for (Map.Entry<String, Object> e : installiert.entrySet()) {
			Object alt = e.getValue();
			if (alt instanceof String && !neu.containsValue(alt)) {
				try {
					Files.deleteIfExists(installer.modsOrdner.resolve((String) alt));
				} catch (IOException ignoriert) {
					// nächster Start versucht es erneut
				}
			}
		}
		installiert.clear();
		installiert.putAll(neu);
		speichern();
		return hinweise;
	}

	/** Neueste stabile Version (sonst neueste Beta) für Fabric + MC_VERSION. */
	private Map<String, Object> passendeVersion(String projekt) throws IOException {
		String url = API + "project/" + enc(projekt) + "/version?loaders=" + enc("[\"fabric\"]")
				+ "&game_versions=" + enc("[\"" + Installer.MC_VERSION + "\"]");
		List<Object> versionen = Json.liste(Json.parse(new String(Installer.lade(url), StandardCharsets.UTF_8)));
		if (versionen == null || versionen.isEmpty()) return null;
		for (Object o : versionen) {
			Map<String, Object> v = Json.objekt(o);
			if (v != null && "release".equals(v.get("version_type"))) return v;
		}
		return Json.objekt(versionen.get(0));
	}

	private static Map<String, Object> primaerDatei(Map<String, Object> version) throws IOException {
		List<Object> dateien = Json.liste(version.get("files"));
		if (dateien == null || dateien.isEmpty()) throw new IOException("keine Datei");
		for (Object o : dateien) {
			Map<String, Object> d = Json.objekt(o);
			if (d != null && Boolean.TRUE.equals(d.get("primary"))) return d;
		}
		return Json.objekt(dateien.get(0));
	}

	private static String enc(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
		} catch (Exception e) {
			return s;
		}
	}

	private static String str(Object o, String standard) {
		return o instanceof String ? (String) o : standard;
	}

	static List<String[]> fpsMods() {
		List<String[]> l = new ArrayList<>();
		for (String[] m : EMPFOHLEN) if ("FPS".equals(m[3])) l.add(m);
		return Collections.unmodifiableList(l);
	}
}
