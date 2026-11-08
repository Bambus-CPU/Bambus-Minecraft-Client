package de.bambus.client;

import de.bambus.client.screen.Anim;
import de.bambus.client.screen.Neon;
import de.bambus.client.screen.Symbole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Kleine Neon-Karten unten rechts: fahren ein, bleiben kurz und gleiten wieder hinaus.
 * Z. B. „Bambus aktualisiert“, „Freund online“, „Screenshot gespeichert“, „Profil geladen“.
 */
public final class Hinweise {

	private static final int BREITE = 160;
	private static final int HOEHE = 30;
	private static final long DAUER = 4500;
	private static final long REIN = 320;
	private static final long RAUS = 260;

	private static final class Hinweis {
		final String symbol, titel, text;
		final int farbe;
		long seit = -1;           // startet erst, wenn die Karte zum ersten Mal sichtbar ist
		float y = Float.NaN;      // gleitet nach, wenn eine Karte darüber verschwindet

		Hinweis(String symbol, String titel, String text, int farbe) {
			this.symbol = symbol;
			this.titel = titel;
			this.text = text;
			this.farbe = farbe;
		}
	}

	private static final List<Hinweis> LISTE = new ArrayList<>();
	private static final Set<String> FREUNDE_ONLINE = new HashSet<>();
	private static int freundTakt;

	private Hinweise() {}

	public static void zeige(String symbol, String titel, String text, int farbe) {
		if (!BambusClient.config.hinweise) return;
		synchronized (LISTE) {
			// gleiche Meldung nicht doppelt stapeln
			for (Hinweis h : LISTE) {
				if (h.titel.equals(titel) && h.text.equals(text)) { h.seit = h.seit < 0 ? -1 : System.currentTimeMillis() - REIN; return; }
			}
			LISTE.add(new Hinweis(symbol, titel, text, farbe));
			while (LISTE.size() > 5) LISTE.remove(0);
		}
	}

	/** Beim Start: Willkommen bzw. „aktualisiert“ nach einem automatischen Update. */
	public static void versionPruefen() {
		BambusConfig c = BambusClient.config;
		if (c.letzteVersion == null || c.letzteVersion.isEmpty()) {
			zeige("stern", "Willkommen bei Bambus!", "RECHTS-SHIFT öffnet das Menü", Farben.NEON);
		} else if (!c.letzteVersion.equals(BambusClient.VERSION)) {
			zeige("update", "Bambus aktualisiert", "Jetzt auf Version " + BambusClient.VERSION, Farben.NEON);
		}
		if (!BambusClient.VERSION.equals(c.letzteVersion)) {
			c.letzteVersion = BambusClient.VERSION;
			c.speichern();
		}
	}

	/** Alle 2 Sekunden schauen, ob ein Freund in der Spielerliste aufgetaucht ist. */
	public static void tick(Minecraft mc) {
		ClientPacketListener verbindung = mc.getConnection();
		if (verbindung == null || mc.player == null) {
			FREUNDE_ONLINE.clear();
			return;
		}
		if (++freundTakt % 40 != 0) return;
		for (String f : BambusClient.config.freunde) {
			boolean online = verbindung.getPlayerInfo(f) != null;
			String k = f.toLowerCase(java.util.Locale.ROOT);
			if (online && FREUNDE_ONLINE.add(k)) {
				if (!f.equalsIgnoreCase(mc.player.getName().getString())) {
					zeige("person", "Freund online", f, Farben.NEON_CYAN);
				}
			} else if (!online) {
				FREUNDE_ONLINE.remove(k);
			}
		}
	}

	public static void zeichne(GuiGraphics g, Minecraft mc) {
		synchronized (LISTE) {
			if (LISTE.isEmpty()) return;
			Font font = mc.font;
			long jetzt = System.currentTimeMillis();
			int sb = mc.getWindow().getGuiScaledWidth();
			int sh = mc.getWindow().getGuiScaledHeight();
			float zielY = sh - 8 - HOEHE - (mc.player != null && mc.screen == null ? 34 : 0);   // über der Hotbar-Höhe
			Iterator<Hinweis> it = LISTE.iterator();
			List<Hinweis> sichtbar = new ArrayList<>();
			while (it.hasNext()) {
				Hinweis h = it.next();
				if (h.seit < 0) h.seit = jetzt;
				if (jetzt - h.seit > DAUER + RAUS) it.remove(); else sichtbar.add(h);
			}
			// neueste unten, ältere schieben sich nach oben
			for (int i = sichtbar.size() - 1; i >= 0; i--) {
				Hinweis h = sichtbar.get(i);
				long alter = jetzt - h.seit;
				float rein = Anim.zurueck(alter / (float) REIN);
				float raus = alter > DAUER ? Anim.aus((alter - DAUER) / (float) RAUS) : 0f;
				if (Float.isNaN(h.y)) h.y = zielY;
				h.y += (zielY - h.y) * 0.25f;
				int x = sb - 8 - BREITE + Math.round((1 - rein) * (BREITE + 12) + raus * (BREITE + 12));
				int y = Math.round(h.y);
				karte(g, font, h, x, y, 1f - Math.min(1f, (jetzt - h.seit) / (float) DAUER));
				zielY -= HOEHE + 5;
			}
		}
	}

	private static void karte(GuiGraphics g, Font font, Hinweis h, int x, int y, float rest) {
		g.fill(x, y, x + BREITE, y + HOEHE, 0xE6060B12);
		Neon.leuchtRahmen(g, x, y, BREITE, HOEHE, Farben.alpha(h.farbe, 170), 0.6f);
		g.fill(x, y, x + 2, y + HOEHE, h.farbe);
		// Symbol in einem kleinen leuchtenden Feld
		g.fill(x + 6, y + 6, x + 24, y + 24, Farben.alpha(h.farbe, 36));
		Symbole.zeichne(g, h.symbol, x + 7, y + 7, 2, h.farbe);
		g.drawString(font, font.plainSubstrByWidth(h.titel, BREITE - 34), x + 29, y + 6, Farben.WEISS, false);
		Neon.text(g, font, font.plainSubstrByWidth(h.text, (int) ((BREITE - 34) / 0.75f)), x + 29, y + 18, 0.75f, Farben.GRAU, false);
		// Restzeit als dünner Balken
		g.fill(x + 2, y + HOEHE - 1, x + 2 + Math.round((BREITE - 2) * Math.max(0, rest)), y + HOEHE, Farben.alpha(h.farbe, 160));
	}
}
