package de.bambus.client.screen;

import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Einheitlicher Symbol-Satz (8 × 8 Pixel) für Module, Kategorien, Tabs und Hinweise.
 * Derselbe Satz steckt im Launcher (de.bambus.launcher.Symbole) – erzeugt aus werkzeuge/symbole.txt.
 */
public final class Symbole {

	private static final Map<String, Long> BILDER = new HashMap<>();

	static {
		Object[] daten = {
			"info", 0x3C4299819999423CL,
			"tasten", 0x181800DBDB007E7EL,
			"server", 0x7E425A7E425A7E18L,
			"status", 0x0C0C3C5A18244281L,
			"ruestung", 0xC3E7FF7E7E7E7E3CL,
			"effekte", 0x183C182442BDBD7EL,
			"ziel", 0x3C4299A5A599423CL,
			"combo", 0x9048241212244890L,
			"reach", 0x002442FF42240081L,
			"tap", 0xA8A8A85001024428L,
			"kills", 0x7EFF9999FF7E5A5AL,
			"pfeile", 0x0703050810A040A0L,
			"cooldown", 0xFF42241818245AFFL,
			"richtung", 0x183C7E1818181818L,
			"biom", 0x183C7EFF7E18183CL,
			"tempo", 0x3C428385899900FFL,
			"uhr", 0x3C4289898D81423CL,
			"speicher", 0x54FF81BDBD81FF54L,
			"tnt", 0x1008FFFFA5FFFFFFL,
			"alle", 0xE7E7E70000E7E7E7L,
			"pvp", 0x03070E9CD8706884L,
			"hud", 0xFF81FF81B1818DFFL,
			"stern", 0x1818FF7E3C664200L,
			"blitz", 0x0E1C387E1C386080L,
			"fadenkreuz", 0x181800C3C3001818L,
			"kosmetik", 0x101038FE38121702L,
			"mehr", 0x000000DBDB000000L,
			"schleichen", 0x181818995A3C1800L,
			"zoom", 0x78848484780C0603L,
			"auge", 0x003C429999423C00L,
			"sonne", 0x925438EE38549200L,
			"herz", 0x66FFFFFF7E3C1800L,
			"kamera", 0x38FF99A5A599FF00L,
			"person", 0x183C3C187EFFFF00L,
			"update", 0x183C7E181818007EL
		};
		for (int i = 0; i < daten.length; i += 2) BILDER.put((String) daten[i], (Long) daten[i + 1]);
	}

	private Symbole() {}

	/** Zeichnet ein Symbol; px = Größe eines Pixels (1 → 8 × 8, 2 → 16 × 16). */
	public static void zeichne(GuiGraphics g, String name, int x, int y, int px, int farbe) {
		long bits = BILDER.getOrDefault(name, BILDER.get("hud"));
		for (int r = 0; r < 8; r++) {
			int start = -1;
			for (int c = 0; c <= 8; c++) {
				boolean an = c < 8 && ((bits >>> (63 - (r * 8 + c))) & 1L) != 0;
				if (an && start < 0) start = c;
				if (!an && start >= 0) {   // waagerechte Läufe auf einmal füllen
					g.fill(x + start * px, y + r * px, x + c * px, y + (r + 1) * px, farbe);
					start = -1;
				}
			}
		}
	}

	public static boolean gibt(String name) { return BILDER.containsKey(name); }

	/** Passendes Symbol zu einem Modul (id) oder einer Funktion (Name). */
	public static String fuer(String idOderName) {
		if (idOderName == null) return "hud";
		if (BILDER.containsKey(idOderName)) return idOderName;
		String n = idOderName.toLowerCase(Locale.ROOT);
		String[][] regeln = {
				{"sprint", "status"}, {"schleich", "schleichen"}, {"sneak", "schleichen"}, {"zoom", "zoom"},
				{"freelook", "auge"}, {"fullbright", "sonne"}, {"hell", "sonne"}, {"fadenkreuz", "fadenkreuz"},
				{"partikel", "kosmetik"}, {"cape", "kosmetik"}, {"flügel", "kosmetik"}, {"low-end", "blitz"},
				{"fps", "blitz"}, {"leben", "herz"}, {"screenshot", "kamera"}, {"discord", "person"},
				{"freund", "person"}, {"zeit", "uhr"}, {"wetter", "sonne"}, {"scoreboard", "hud"},
				{"bossbar", "hud"}, {"tab", "hud"}, {"chat", "hud"}, {"hauptmenü", "hud"}, {"menü", "mehr"},
				{"verlauf", "kosmetik"}, {"benachrichtigung", "update"}, {"lade", "update"}, {"treffer", "ziel"},
		};
		for (String[] r : regeln) if (n.contains(r[0])) return r[1];
		return "hud";
	}
}
