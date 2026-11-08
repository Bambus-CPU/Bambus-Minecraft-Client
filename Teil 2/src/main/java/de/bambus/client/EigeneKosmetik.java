package de.bambus.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Selbst erstellte Capes und Flügel.
 *
 * Ordner: config/bambus-kosmetik/capes/ und config/bambus-kosmetik/fluegel/
 * Jede Kosmetik ist ein PNG (z. B. "mein-cape.png"), optional mit gleichnamiger
 * .json daneben: {"name": "Mein Cape", "farbe": "#3DFF8A"}.
 *
 * Capes: Minecraft-Cape-Layout im Seitenverhältnis 2:1 (64×32, 128×64, 256×128 …).
 * Flügel: quadratisch, Gelenk oben links, Flügel breitet sich nach rechts aus.
 *
 * Der Launcher (Kosmetik-Studio) schreibt genau dieses Format.
 */
public final class EigeneKosmetik {

	private EigeneKosmetik() {}

	public static final String PRAEFIX = "eigen:";

	/** Eine eigene Kosmetik: id ist "eigen:<dateiname>". */
	public record Eintrag(String id, String name, int farbe, Identifier bild) {}

	private static final List<Eintrag> CAPES = new ArrayList<>();
	private static final List<Eintrag> FLUEGEL = new ArrayList<>();
	private static final List<Identifier> REGISTRIERT = new ArrayList<>();
	private static boolean geladen = false;

	public static Path ordner() {
		return FabricLoader.getInstance().getConfigDir().resolve("bambus-kosmetik");
	}

	public static List<Eintrag> capes() {
		sicherGeladen();
		return Collections.unmodifiableList(CAPES);
	}

	public static List<Eintrag> fluegel() {
		sicherGeladen();
		return Collections.unmodifiableList(FLUEGEL);
	}

	public static Eintrag cape(String id) {
		return capes().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
	}

	public static Eintrag fluegel(String id) {
		return fluegel().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
	}

	private static void sicherGeladen() {
		if (!geladen) {
			neuLaden();
		}
	}

	/** Liest beide Ordner neu ein. Muss auf dem Render-Thread laufen (Texturen). */
	public static synchronized void neuLaden() {
		geladen = true;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.getTextureManager() == null) {
			geladen = false;
			return;
		}
		TextureManager tm = mc.getTextureManager();
		for (Identifier id : REGISTRIERT) {
			tm.release(id);
		}
		REGISTRIERT.clear();
		CAPES.clear();
		FLUEGEL.clear();
		try {
			Files.createDirectories(ordner().resolve("capes"));
			Files.createDirectories(ordner().resolve("fluegel"));
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Kosmetik-Ordner nicht anlegbar: {}", e.toString());
		}
		lade(tm, "capes", CAPES, 0xFF3DFF8A);
		lade(tm, "fluegel", FLUEGEL, 0xFFFF2BD6);
		BambusClient.LOGGER.info("Eigene Kosmetik: {} Capes, {} Flügel", CAPES.size(), FLUEGEL.size());
	}

	private static void lade(TextureManager tm, String art, List<Eintrag> ziel, int standardFarbe) {
		Path dir = ordner().resolve(art);
		if (!Files.isDirectory(dir)) {
			return;
		}
		List<Path> dateien = new ArrayList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.png")) {
			ds.forEach(dateien::add);
		} catch (Exception e) {
			return;
		}
		dateien.sort(null);
		for (Path png : dateien) {
			String datei = png.getFileName().toString();
			String stamm = datei.substring(0, datei.length() - 4);
			String sauber = sauber(stamm);
			if (sauber.isEmpty()) {
				continue;
			}
			try (InputStream in = Files.newInputStream(png)) {
				NativeImage bild = NativeImage.read(in);
				if (art.equals("capes") && bild.getWidth() != bild.getHeight() * 2) {
					BambusClient.LOGGER.warn("Cape {} hat nicht das Format 2:1 und wird trotzdem verwendet", datei);
				}
				Identifier id = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "eigen/" + art + "/" + sauber);
				tm.register(id, new DynamicTexture(() -> "bambus eigen " + datei, bild));
				REGISTRIERT.add(id);

				String name = stamm;
				int farbe = standardFarbe;
				Path info = dir.resolve(stamm + ".json");
				if (Files.isRegularFile(info)) {
					try {
						JsonObject o = JsonParser.parseString(Files.readString(info, StandardCharsets.UTF_8)).getAsJsonObject();
						if (o.has("name")) {
							name = o.get("name").getAsString();
						}
						if (o.has("farbe")) {
							farbe = 0xFF000000 | Integer.parseInt(o.get("farbe").getAsString().replace("#", ""), 16);
						}
					} catch (Exception ignoriert) {
						// kaputte Info-Datei: Dateiname reicht
					}
				}
				ziel.add(new Eintrag(PRAEFIX + stamm, name, farbe, id));
			} catch (Exception e) {
				BambusClient.LOGGER.warn("Eigene Kosmetik {} konnte nicht geladen werden: {}", datei, e.toString());
			}
		}
	}

	/** Identifier dürfen nur a-z, 0-9, _ - . / enthalten. */
	static String sauber(String s) {
		String t = s.toLowerCase(Locale.ROOT)
				.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
				.replaceAll("[^a-z0-9_.-]", "_");
		return t.length() > 60 ? t.substring(0, 60) : t;
	}
}
