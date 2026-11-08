package de.bambus.launcher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Holt deinen Minecraft-Skin:
 * 1. Name/ID aus dem Minecraft Launcher (launcher_accounts.json) – nur lesen, keine Passwörter.
 * 2. Skin-Bild von den Mojang-Servern.
 * Ohne Internet wird der zuletzt geladene Skin oder der Bambus-Standard-Skin benutzt.
 */
final class SkinLader {

	static final class Skin {
		final BufferedImage bild;
		final boolean schmal;
		final String name;
		final boolean standard;

		Skin(BufferedImage bild, boolean schmal, String name, boolean standard) {
			this.bild = bild;
			this.schmal = schmal;
			this.name = name;
			this.standard = standard;
		}
	}

	private final Path mcOrdner;
	private final Path cacheBild;
	private final Path cacheInfo;

	SkinLader(Path mcOrdner, Path spielOrdner) {
		this.mcOrdner = mcOrdner;
		this.cacheBild = spielOrdner.resolve("skin-cache.png");
		this.cacheInfo = spielOrdner.resolve("skin-cache.properties");
	}

	/** Standard-Skin des Bambus Clients (eigenes Design). */
	static Skin standard() {
		try (InputStream in = SkinLader.class.getResourceAsStream("/standard-skin.png")) {
			return new Skin(ImageIO.read(in), false, "Bambus-Bot", true);
		} catch (Exception e) {
			return new Skin(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), false, "?", true);
		}
	}

	/** Zuletzt geladener Skin (sofort verfügbar, ohne Internet). */
	Skin ausCache() {
		try {
			if (Files.isRegularFile(cacheBild) && Files.isRegularFile(cacheInfo)) {
				Properties p = new Properties();
				try (Reader r = Files.newBufferedReader(cacheInfo, StandardCharsets.UTF_8)) {
					p.load(r);
				}
				BufferedImage b = ImageIO.read(cacheBild.toFile());
				if (b != null) {
					return new Skin(b, Boolean.parseBoolean(p.getProperty("schmal")), p.getProperty("name", "?"), false);
				}
			}
		} catch (Exception ignoriert) {
			// dann eben nicht
		}
		return null;
	}

	/** Name des angemeldeten Minecraft-Kontos aus dem offiziellen Launcher (oder null). */
	String[] kontoAusLauncher() {
		String[] dateien = {"launcher_accounts_microsoft_store.json", "launcher_accounts.json"};
		for (String d : dateien) {
			Path p = mcOrdner.resolve(d);
			if (!Files.isRegularFile(p)) {
				continue;
			}
			try {
				Map<String, Object> wurzel = Json.objekt(Json.parse(
						new String(Files.readAllBytes(p), StandardCharsets.UTF_8)));
				Map<String, Object> konten = Json.objekt(wurzel.get("accounts"));
				if (konten == null) {
					continue;
				}
				Object aktiv = wurzel.get("activeAccountLocalId");
				Map<String, Object> konto = aktiv != null ? Json.objekt(konten.get(aktiv)) : null;
				if (konto == null && !konten.isEmpty()) {
					konto = Json.objekt(konten.values().iterator().next());
				}
				Map<String, Object> profil = konto == null ? null : Json.objekt(konto.get("minecraftProfile"));
				if (profil != null && profil.get("id") != null) {
					return new String[]{(String) profil.get("id"), (String) profil.get("name")};
				}
			} catch (Exception ignoriert) {
				// nächste Datei probieren
			}
		}
		return null;
	}

	/** Lädt den Skin zu einem Minecraft-Namen oder einer UUID aus dem Internet. */
	Skin lade(String nameOderId) throws IOException {
		String id = nameOderId.replace("-", "");
		if (!id.matches("[0-9a-fA-F]{32}")) {
			if (!nameOderId.matches("[A-Za-z0-9_]{1,16}")) {
				throw new IOException("„" + nameOderId + "“ ist kein gültiger Minecraft-Name.");
			}
			Object o = Json.parse(new String(Installer.lade(
					"https://api.mojang.com/users/profiles/minecraft/" + nameOderId), StandardCharsets.UTF_8));
			Map<String, Object> m = Json.objekt(o);
			if (m == null || m.get("id") == null) {
				throw new IOException("Kein Minecraft-Konto mit dem Namen „" + nameOderId + "“ gefunden.");
			}
			id = (String) m.get("id");
		}

		Map<String, Object> profil = Json.objekt(Json.parse(new String(Installer.lade(
				"https://sessionserver.mojang.com/session/minecraft/profile/" + id), StandardCharsets.UTF_8)));
		if (profil == null) {
			throw new IOException("Profil nicht gefunden.");
		}
		String name = (String) profil.get("name");
		List<Object> eigenschaften = Json.liste(profil.get("properties"));
		String url = null;
		boolean schmal = false;
		if (eigenschaften != null) {
			for (Object e : eigenschaften) {
				Map<String, Object> eig = Json.objekt(e);
				if (eig != null && "textures".equals(eig.get("name"))) {
					String json = new String(Base64.getDecoder().decode((String) eig.get("value")), StandardCharsets.UTF_8);
					Map<String, Object> t = Json.objekt(Json.objekt(Json.parse(json)).get("textures"));
					Map<String, Object> s = t == null ? null : Json.objekt(t.get("SKIN"));
					if (s != null) {
						url = (String) s.get("url");
						Map<String, Object> meta = Json.objekt(s.get("metadata"));
						schmal = meta != null && "slim".equals(meta.get("model"));
					}
				}
			}
		}
		if (url == null) {
			throw new IOException(name + " hat keinen eigenen Skin.");
		}
		if (url.startsWith("http://")) {
			url = "https://" + url.substring(7);
		}
		byte[] daten = Installer.lade(url);
		BufferedImage bild = ImageIO.read(new ByteArrayInputStream(daten));
		if (bild == null) {
			throw new IOException("Skin-Bild konnte nicht gelesen werden.");
		}

		// merken
		try {
			Files.createDirectories(cacheBild.getParent());
			Files.write(cacheBild, daten);
			Properties p = new Properties();
			p.setProperty("name", name == null ? nameOderId : name);
			p.setProperty("schmal", String.valueOf(schmal));
			try (Writer w = Files.newBufferedWriter(cacheInfo, StandardCharsets.UTF_8)) {
				p.store(w, "Bambus Skin-Cache");
			}
		} catch (Exception ignoriert) {
			// nicht schlimm
		}
		return new Skin(bild, schmal, name == null ? nameOderId : name, false);
	}
}
