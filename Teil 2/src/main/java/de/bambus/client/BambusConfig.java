package de.bambus.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Alle Einstellungen des Bambus Clients. Gespeichert in .minecraft/config/bambus.json */
public class BambusConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static class ModulPos {
		public boolean an = true;
		public int x;
		public int y;
		/** Größe im HUD (1 = 100 %), im HUD-Editor mit Ecke oder Mausrad änderbar. */
		public float groesse = 1f;

		public ModulPos() {}

		public ModulPos(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	// HUD
	public boolean hudAn = true;
	public Map<String, ModulPos> module = new LinkedHashMap<>();

	// Funktionen
	public boolean toggleSprint = false;
	public boolean toggleSneak = false;
	public boolean scrollZoom = true;
	public boolean freelook = true;
	public boolean fullbright = false;
	public boolean capeAn = true;
	public boolean bambusHauptmenue = true;

	// Anzeigen anpassen (Scoreboard, Bossbar, Tabliste, Chat): an/aus und Größe in Prozent
	public boolean scoreboardAn = true;
	public int scoreboardGroesse = 100;
	public boolean bossbarAn = true;
	public int bossbarGroesse = 100;
	public int tablisteGroesse = 100;
	public int chatGroesse = 100;

	// Client-Oberfläche
	public int thema = 0;                 // Farben.THEMEN
	public boolean verlauf = true;        // Farbverlauf an HUD-Kästen
	public boolean menueAnimation = true;
	public boolean hinweise = true;        // Neon-Benachrichtigungen
	public boolean ladebildschirm = true;  // eigener Ladebildschirm
	public String letzteVersion = "";      // für „Bambus wurde aktualisiert“
	public java.util.List<String> favoriten = new java.util.ArrayList<>();

	// Extras
	public java.util.List<String> freunde = new java.util.ArrayList<>();
	public boolean autoScreenshot = false;
	public boolean discordAn = false;
	public String discordAppId = "";

	// PvP
	/** Genaue Lebenspunkte des Gegners im Target-HUD (auf manchen Servern verboten, daher aus). */
	public boolean gegnerLebenZahl = false;

	// Visuals
	public int zeitModus = 0;      // 0 normal, 1 Morgen, 2 Mittag, 3 Abend, 4 Nacht
	public int wetterModus = 0;    // 0 normal, 1 klar, 2 Regen, 3 Gewitter
	public boolean trefferPartikel = true;
	public boolean lowEnd = false;
	public int[] lowEndAlt = null; // gemerkte Grafikwerte vor dem Low-End-Modus

	// Kosmetik ("aus" = nichts ausgewählt)
	public String capeDesign = "bambus";
	public String partikel = "aus";
	public String fluegel = "aus";
	public String hut = "aus";
	public String killEffekt = "aus";
	public String spur = "aus";

	// Fadenkreuz
	public boolean fadenkreuzAn = false;
	public int fadenkreuzForm = 0;
	public int fadenkreuzFarbe = 0;
	public int fadenkreuzGroesse = 4;

	public ModulPos modul(String id, int stdX, int stdY) {
		return modul(id, stdX, stdY, true);
	}

	/** Neue Module können standardmäßig aus sein, damit das HUD nicht überladen wirkt. */
	public ModulPos modul(String id, int stdX, int stdY, boolean stdAn) {
		return module.computeIfAbsent(id, k -> {
			ModulPos p = new ModulPos(stdX, stdY);
			p.an = stdAn;
			return p;
		});
	}

	/** Liest Einstellungen aus einem JSON-Text (Import / Profile). Gibt null zurück, wenn ungültig. */
	public static BambusConfig ausJson(String json) {
		try {
			BambusConfig c = GSON.fromJson(json, BambusConfig.class);
			if (c != null && c.module == null) {
				c.module = new LinkedHashMap<>();
			}
			if (c != null && c.favoriten == null) c.favoriten = new java.util.ArrayList<>();
			if (c != null && c.freunde == null) c.freunde = new java.util.ArrayList<>();
			return c;
		} catch (Exception e) {
			return null;
		}
	}

	public String alsJson() {
		return GSON.toJson(this);
	}

	private static Path datei() {
		return FabricLoader.getInstance().getConfigDir().resolve("bambus.json");
	}

	public static BambusConfig laden() {
		Path p = datei();
		if (Files.exists(p)) {
			try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				BambusConfig c = GSON.fromJson(r, BambusConfig.class);
				if (c != null) {
					if (c.module == null) {
						c.module = new LinkedHashMap<>();
					}
					if (c.favoriten == null) c.favoriten = new java.util.ArrayList<>();
					if (c.freunde == null) c.freunde = new java.util.ArrayList<>();
					return c;
				}
			} catch (Exception e) {
				System.err.println("[Bambus] Einstellungen konnten nicht gelesen werden: " + e);
			}
		}
		return new BambusConfig();
	}

	public void speichern() {
		try {
			Files.createDirectories(datei().getParent());
			try (Writer w = Files.newBufferedWriter(datei(), StandardCharsets.UTF_8)) {
				GSON.toJson(this, w);
			}
		} catch (Exception e) {
			System.err.println("[Bambus] Einstellungen konnten nicht gespeichert werden: " + e);
		}
	}
}
