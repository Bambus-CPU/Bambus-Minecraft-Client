package de.bambus.launcher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Selbst erstellte Capes und Flügel – im gleichen Ordner und Format, das die Mod liest:
 * &lt;Spielordner&gt;/config/bambus-kosmetik/capes|fluegel/&lt;name&gt;.png (+ .json mit Name und Farbe).
 * Die Kennung im Client ist "eigen:&lt;dateiname ohne .png&gt;".
 */
final class EigeneKosmetik {

	static final String PRAEFIX = "eigen:";
	static final String CAPES = "capes", FLUEGEL = "fluegel";

	/** Eine eigene Kosmetik. */
	static final class Eintrag {
		final String id, name, farbe;   // farbe als Hex ohne #
		final BufferedImage bild;
		final Path datei;

		Eintrag(String id, String name, String farbe, BufferedImage bild, Path datei) {
			this.id = id;
			this.name = name;
			this.farbe = farbe;
			this.bild = bild;
			this.datei = datei;
		}
	}

	private final Path ordner;

	EigeneKosmetik(Path spielOrdner) {
		this.ordner = spielOrdner.resolve("config").resolve("bambus-kosmetik");
	}

	Path ordner() {
		return ordner;
	}

	static boolean istEigen(String id) {
		return id != null && id.startsWith(PRAEFIX);
	}

	List<Eintrag> liste(String art) {
		List<Eintrag> l = new ArrayList<>();
		Path dir = ordner.resolve(art);
		if (!Files.isDirectory(dir)) {
			return l;
		}
		List<Path> dateien = new ArrayList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.png")) {
			for (Path p : ds) {
				dateien.add(p);
			}
		} catch (IOException e) {
			return l;
		}
		Collections.sort(dateien);
		for (Path png : dateien) {
			try {
				BufferedImage bild = ImageIO.read(png.toFile());
				if (bild == null) {
					continue;
				}
				String datei = png.getFileName().toString();
				String stamm = datei.substring(0, datei.length() - 4);
				String name = stamm;
				String farbe = CAPES.equals(art) ? "3DFF8A" : "FF2BD6";
				Path info = dir.resolve(stamm + ".json");
				if (Files.isRegularFile(info)) {
					Map<String, Object> m = Json.objekt(Json.parse(new String(Files.readAllBytes(info), StandardCharsets.UTF_8)));
					if (m != null) {
						if (m.get("name") instanceof String) {
							name = (String) m.get("name");
						}
						if (m.get("farbe") instanceof String) {
							farbe = ((String) m.get("farbe")).replace("#", "");
						}
					}
				}
				l.add(new Eintrag(PRAEFIX + stamm, name, farbe, bild, png));
			} catch (Exception e) {
				System.err.println("[Bambus] Eigene Kosmetik nicht lesbar: " + png + " – " + e);
			}
		}
		return l;
	}

	Eintrag finde(String art, String id) {
		for (Eintrag e : liste(art)) {
			if (e.id.equals(id)) {
				return e;
			}
		}
		return null;
	}

	/** Speichert eine neue Kosmetik und gibt ihre Kennung ("eigen:…") zurück. */
	String speichern(String art, String name, String farbeHex, BufferedImage bild) throws IOException {
		Path dir = ordner.resolve(art);
		Files.createDirectories(dir);
		String basis = dateiname(name);
		String stamm = basis;
		for (int i = 2; Files.exists(dir.resolve(stamm + ".png")); i++) {
			stamm = basis + "-" + i;
		}
		ImageIO.write(bild, "png", dir.resolve(stamm + ".png").toFile());
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("name", name);
		info.put("farbe", "#" + farbeHex);
		info.put("erstellt", "Bambus Kosmetik-Studio");
		Files.write(dir.resolve(stamm + ".json"), Json.write(info).getBytes(StandardCharsets.UTF_8));
		return PRAEFIX + stamm;
	}

	void loeschen(String art, String id) throws IOException {
		if (!istEigen(id)) {
			return;
		}
		String stamm = id.substring(PRAEFIX.length());
		Path dir = ordner.resolve(art);
		Files.deleteIfExists(dir.resolve(stamm + ".png"));
		Files.deleteIfExists(dir.resolve(stamm + ".json"));
	}

	/** Dateiname, den auch die Mod sauber als Textur-Kennung nutzen kann. */
	static String dateiname(String name) {
		String t = name.trim().toLowerCase(Locale.ROOT)
				.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
				.replaceAll("[^a-z0-9_.-]+", "-")
				.replaceAll("^-+|-+$", "");
		if (t.isEmpty()) {
			t = "kosmetik";
		}
		return t.length() > 40 ? t.substring(0, 40) : t;
	}
}
