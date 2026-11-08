package de.bambus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.Anzeigen;
import de.bambus.client.DiscordStatus;
import de.bambus.client.Profile;
import de.bambus.client.Statistik;
import de.bambus.client.Visuals;
import de.bambus.client.BambusClient;
import de.bambus.client.BambusConfig;
import de.bambus.client.EigeneKosmetik;
import de.bambus.client.Fadenkreuz;
import de.bambus.client.Farben;
import de.bambus.client.Funktionen;
import de.bambus.client.Kosmetik;
import de.bambus.client.hud.HudModul;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Das Bambus-Menü im Neon-Look (Rechts-Shift).
 * Tabs: Module · Funktionen · Fadenkreuz · Kosmetik – mit der TAB-Taste wechseln.
 */
public class BambusMenuScreen extends Screen {

	private enum Tab {
		MODULE("MODULE"), FUNKTIONEN("FUNKTIONEN"), FADENKREUZ("FADENKREUZ"), KOSMETIK("KOSMETIK"), MEHR("MEHR");

		final String titel;

		Tab(String titel) {
			this.titel = titel;
		}
	}

	private record Karte(String name, String text, BooleanSupplier an, Runnable umschalten, int akzent, String fav) {
		Karte(String name, String text, BooleanSupplier an, Runnable umschalten, int akzent) {
			this(name, text, an, umschalten, akzent, null);
		}
	}

	private static final String[] KATEGORIEN = {"ALLE", "PVP", "HUD", "INFO", "★"};
	private static String kategorie = "ALLE";
	private EditBox suchFeld;
	private EditBox freundFeld;
	private String meldung = "";
	private long meldungZeit;
	private final long offenSeit = System.currentTimeMillis();
	private long tabSeit = 0;        // für den weichen Inhaltswechsel
	private long schliessSeit = 0;   // für die Schließ-Animation

	private record Bereich(int x, int y, int b, int h, Runnable aktion, boolean imInhalt) {}

	private static final Map<String, String> MODUL_TEXT = Map.of(
			"info", "FPS, Koordinaten und CPS",
			"tasten", "W A S D, Maus und Leertaste",
			"server", "Serveradresse und Ping",
			"status", "Zeigt Toggle Sprint / Sneak",
			"ruestung", "Rüstung mit Haltbarkeit",
			"effekte", "Aktive Tränke mit Restzeit"
	);

	private static Tab letzterTab = Tab.MODULE;
	private static int letzteKosmetikArt = 0; // 0 Capes, 1 Flügel, 2 Partikel

	private final Screen eltern;
	private final List<Bereich> bereiche = new ArrayList<>();

	private Tab tab = letzterTab;
	private int kosmetikArt = letzteKosmetikArt;
	private int scroll = 0;
	private int inhaltHoehe = 0;

	// Layout (wird in render berechnet)
	private int px, py, pb, ph;
	private int ix, iy, ib, ih;

	public BambusMenuScreen(Screen eltern) {
		super(Component.literal("Bambus Client"));
		this.eltern = eltern;
	}

	@Override
	protected void init() {
		suchFeld = new EditBox(font, 0, 0, 120, 14, Component.literal("Suche"));
		suchFeld.setHint(Component.literal("Modul suchen …"));
		suchFeld.setMaxLength(32);
		suchFeld.setResponder(t -> scroll = 0);
		addRenderableWidget(suchFeld);
		freundFeld = new EditBox(font, 0, 0, 120, 14, Component.literal("Freund"));
		freundFeld.setHint(Component.literal("Spielername …"));
		freundFeld.setMaxLength(16);
		freundFeld.setVisible(false);
		addRenderableWidget(freundFeld);
	}

	private void melde(String text) {
		meldung = text;
		meldungZeit = System.currentTimeMillis();
		de.bambus.client.Hinweise.zeige("mehr", "Bambus", text, Farben.NEON_CYAN);
	}

	private static BambusConfig cfg() {
		return BambusClient.config;
	}

	private void speichern() {
		cfg().speichern();
	}

	private void wechsle(Tab neu) {
		if (tab != neu) {
			tab = neu;
			letzterTab = neu;
			scroll = 0;
			tabSeit = System.currentTimeMillis();
		}
	}

	// =================================================================== Zeichnen

	@Override
	public void renderBackground(GuiGraphics g, int mausX, int mausY, float delta) {
		g.fill(0, 0, width, height, 0xC0020407);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 7));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		bereiche.clear();

		Farben.themaAnwenden(cfg().thema);
		pb = Math.min(width - 20, 500);
		ph = Math.min(height - 20, 310);
		px = (width - pb) / 2;
		py = (height - ph) / 2;
		float sichtbar = 1f;
		if (cfg().menueAnimation) {
			float k = Math.min(1f, (System.currentTimeMillis() - offenSeit) / 220f);
			py += (int) ((1 - Anim.aus(k)) * 14);   // weich von unten einschweben
			sichtbar = Anim.aus(k);
			if (schliessSeit > 0) {
				float z = (System.currentTimeMillis() - schliessSeit) / 140f;
				if (z >= 1f) {
					speichern();
					minecraft.setScreen(eltern);
					return;
				}
				py += (int) (Anim.aus(z) * 10);
				sichtbar = Math.min(sichtbar, 1 - Anim.aus(z));
			}
		}
		ix = px + 10;
		iy = py + 58;
		ib = pb - 20;
		ih = ph - 58 - 32;
		boolean suchleiste = tab == Tab.MODULE;
		suchFeld.setVisible(suchleiste);
		if (tab != Tab.MEHR) {
			freundFeld.setVisible(false);
		}
		if (suchleiste) {
			iy += 22;
			ih -= 22;
		}

		// Grundfläche mit Leuchtrahmen
		g.fill(px, py, px + pb, py + ph, Farben.PANEL);
		Neon.gitter(g, px, py, pb, ph, 16, Farben.alpha(Farben.NEON, 6));
		Neon.leuchtRahmen(g, px, py, pb, ph, Farben.NEON, 0.55f + 0.45f * Farben.puls());
		Neon.ecken(g, px - 5, py - 5, pb + 10, ph + 10, 12, Farben.NEON_CYAN);

		kopf(g, mx, my);
		tabs(g, mx, my);
		if (suchleiste) {
			suchleiste(g, mx, my);
		}

		// Inhalt (scrollbar)
		g.enableScissor(ix, iy, ix + ib, iy + ih);
		float wechsel = cfg().menueAnimation ? Anim.aus(Math.min(1f, (System.currentTimeMillis() - tabSeit) / 180f)) : 1f;
		int y0 = iy - scroll + (int) ((1 - wechsel) * 10);
		inhaltHoehe = switch (tab) {
			case MODULE -> karten(g, mx, my, moduleKarten(), ix, y0, ib);
			case FUNKTIONEN -> funktionen(g, mx, my, ix, y0, ib);
			case FADENKREUZ -> fadenkreuz(g, mx, my, ix, y0, ib);
			case KOSMETIK -> kosmetik(g, mx, my, ix, y0, ib);
			case MEHR -> mehr(g, mx, my, ix, y0, ib);
		};
		if (wechsel < 1f) {
			g.fill(ix, iy, ix + ib, iy + ih, Farben.alpha(Farben.PANEL, (int) ((1 - wechsel) * 230)));
		}
		g.disableScissor();

		int maxScroll = Math.max(0, inhaltHoehe - ih);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		if (maxScroll > 0) {
			int balkenH = Math.max(12, ih * ih / inhaltHoehe);
			int balkenY = iy + (ih - balkenH) * scroll / maxScroll;
			g.fill(px + pb - 5, iy, px + pb - 3, iy + ih, Farben.LINIE);
			g.fill(px + pb - 5, balkenY, px + pb - 3, balkenY + balkenH, Farben.NEON);
		}

		fuss(g, mx, my);
		super.render(g, mx, my, delta);
		if (sichtbar < 1f) {   // ein- und ausblenden
			g.fill(px - 6, py - 6, px + pb + 6, py + ph + 6, Farben.alpha(0xFF020407, (int) ((1 - sichtbar) * 255)));
		}
	}

	private void kopf(GuiGraphics g, int mx, int my) {
		Neon.logo(g, px + 12, py + 8, 16);
		Neon.leuchtText(g, font, "BAMBUS", px + 30, py + 9, 1.5f, Farben.NEON);
		int cx = px + 30 + (int) (font.width("BAMBUS") * 1.5f) + 5;
		Neon.text(g, font, "CLIENT", cx, py + 14, 0.75f, Farben.NEON_CYAN, false);

		String version = "v" + BambusClient.VERSION + " // 1.21.11";
		Neon.text(g, font, version, px + pb - 28 - (int) (font.width(version) * 0.75f), py + 13, 0.75f, Farben.GRAU, false);

		// Schließen
		int sx = px + pb - 20;
		int sy = py + 8;
		boolean hover = Neon.ueber(mx, my, sx, sy, 12, 12);
		g.drawString(font, "✕", sx + 2, sy + 2, hover ? Farben.ROT : Farben.GRAU, false);
		klick(sx, sy, 12, 12, this::onClose, false);

		g.fill(px + 8, py + 30, px + pb - 8, py + 31, Farben.LINIE);
	}

	private static String tabSymbol(Tab t) {
		return switch (t) {
			case MODULE -> "alle";
			case FUNKTIONEN -> "blitz";
			case FADENKREUZ -> "fadenkreuz";
			case KOSMETIK -> "kosmetik";
			case MEHR -> "mehr";
		};
	}

	private void tabs(GuiGraphics g, int mx, int my) {
		int y = py + 36;
		// erst die Positionen, damit der Unterstrich zum aktiven Tab gleiten kann
		int x = px + 10;
		int aktivX = x, aktivB = 0;
		for (Tab t : Tab.values()) {
			int b = font.width(t.titel) + 25;
			if (t == tab) { aktivX = x; aktivB = b; }
			x += b + 4;
		}
		boolean weich = cfg().menueAnimation;
		int ux = weich ? Math.round(Anim.zu("tab-x", aktivX, 18f)) : aktivX;
		int ub = weich ? Math.round(Anim.zu("tab-b", aktivB, 18f)) : aktivB;
		g.fill(ux, y, ux + ub, y + 16, Farben.alpha(Farben.NEON, 22));
		g.fill(ux, y + 15, ux + ub, y + 17, Farben.NEON);
		g.fill(ux, y + 17, ux + ub, y + 18, Farben.alpha(Farben.NEON, 60));

		x = px + 10;
		for (Tab t : Tab.values()) {
			int b = font.width(t.titel) + 25;
			boolean aktiv = t == tab;
			boolean hover = Neon.ueber(mx, my, x, y, b, 16);
			float h = weich ? Anim.zu("tab-hover:" + t, hover || aktiv ? 1f : 0f, 14f) : (hover || aktiv ? 1f : 0f);
			int farbe = aktiv ? Farben.NEON : Anim.mische(Farben.GRAU, Farben.WEISS, h);
			Symbole.zeichne(g, tabSymbol(t), x + 6, y + 4, 1, farbe);
			g.drawString(font, t.titel, x + 18, y + 4, farbe, aktiv);
			final Tab ziel = t;
			klick(x, y, b, 16, () -> wechsle(ziel), false);
			x += b + 4;
		}
		String hinweis = "[TAB] wechseln";
		Neon.text(g, font, hinweis, px + pb - 12 - (int) (font.width(hinweis) * 0.75f), y + 5, 0.75f, Farben.GRAU, false);
	}

	private void fuss(GuiGraphics g, int mx, int my) {
		int y = py + ph - 26;
		g.fill(px + 8, y - 5, px + pb - 8, y - 4, Farben.LINIE);

		boolean imSpiel = minecraft.player != null;
		knopf(g, mx, my, "HUD BEARBEITEN", px + 10, y, 110, 18, false, imSpiel,
				() -> minecraft.setScreen(new HudEditorScreen(this)));
		knopf(g, mx, my, "FERTIG", px + pb - 90, y, 80, 18, true, true, this::onClose);

		String tipp = imSpiel ? "RECHTS-SHIFT öffnet dieses Menü" : "HUD bearbeiten geht in einer Welt";
		int tb = (int) (font.width(tipp) * 0.75f);
		Neon.text(g, font, tipp, px + (pb - tb) / 2, y + 6, 0.75f, Farben.GRAU, false);
	}

	// ------------------------------------------------------------- Karten

	private List<Karte> moduleKarten() {
		BambusConfig c = cfg();
		List<Karte> k = new ArrayList<>();
		k.add(new Karte("HUD (alles)", "Alle Anzeigen ein/aus · Taste H",
				() -> c.hudAn, () -> c.hudAn = !c.hudAn, Farben.NEON_CYAN));
		String suche = suchFeld == null ? "" : suchFeld.getValue().trim().toLowerCase(java.util.Locale.ROOT);
		List<HudModul> liste = new ArrayList<>(BambusClient.MODULE);
		// Favoriten zuerst
		liste.sort((a, b2) -> Boolean.compare(!c.favoriten.contains(a.id), !c.favoriten.contains(b2.id)));
		for (HudModul m : liste) {
			String text = m.beschreibung.isEmpty() ? MODUL_TEXT.getOrDefault(m.id, "") : m.beschreibung;
			boolean passt = suche.isEmpty() || m.name.toLowerCase(java.util.Locale.ROOT).contains(suche)
					|| text.toLowerCase(java.util.Locale.ROOT).contains(suche);
			boolean kat = kategorie.equals("ALLE") || kategorie.equals(m.kategorie)
					|| (kategorie.equals("★") && c.favoriten.contains(m.id));
			if (passt && kat) {
				k.add(new Karte(m.name, text, () -> m.pos().an, () -> m.pos().an = !m.pos().an,
						m.kategorie.equals("PVP") ? Farben.NEON_PINK : m.kategorie.equals("INFO") ? Farben.NEON_CYAN : Farben.NEON, m.id));
			}
		}
		if (!suche.isEmpty() || !kategorie.equals("ALLE")) {
			k.remove(0); // "HUD (alles)" nur in der vollen Liste
		}
		return k;
	}

	private List<Karte> funktionsKarten() {
		BambusConfig c = cfg();
		List<Karte> k = new ArrayList<>();
		k.add(new Karte("Toggle Sprint", "Sprint-Taste 1× drücken = dauerhaft",
				() -> c.toggleSprint, () -> {
			c.toggleSprint = !c.toggleSprint;
			Funktionen.sprintUndSchleichenAnwenden(minecraft);
		}, Farben.NEON));
		k.add(new Karte("Toggle Sneak", "Schleich-Taste 1× drücken = dauerhaft",
				() -> c.toggleSneak, () -> {
			c.toggleSneak = !c.toggleSneak;
			Funktionen.sprintUndSchleichenAnwenden(minecraft);
		}, Farben.NEON));
		k.add(new Karte("Zoom mit Mausrad", "C halten · Mausrad = näher/weiter",
				() -> c.scrollZoom, () -> c.scrollZoom = !c.scrollZoom, Farben.NEON));
		k.add(new Karte("Freelook", "Linke Alt halten · ⚠ nicht überall erlaubt",
				() -> c.freelook, () -> c.freelook = !c.freelook, Farben.NEON_PINK));
		k.add(new Karte("Fullbright", "Alles hell · ⚠ nicht überall erlaubt",
				() -> c.fullbright, () -> c.fullbright = !c.fullbright, Farben.NEON_PINK));
		k.add(new Karte("Gegner-Leben als Zahl", "Target-HUD zeigt genaue ❤ · ⚠ nicht überall erlaubt",
				() -> c.gegnerLebenZahl, () -> c.gegnerLebenZahl = !c.gegnerLebenZahl, Farben.NEON_PINK));
		k.add(new Karte("Bambus-Hauptmenü", "Neon-Startbildschirm statt Minecraft-Menü",
				() -> c.bambusHauptmenue, () -> c.bambusHauptmenue = !c.bambusHauptmenue, Farben.NEON_CYAN));
		return k;
	}

	/** Funktionen + Anzeigen anpassen (Scoreboard, Bossbar, Tabliste, Chat). */
	private int funktionen(GuiGraphics g, int mx, int my, int x, int y, int b) {
		BambusConfig c = cfg();
		int h = karten(g, mx, my, funktionsKarten(), x, y, b);
		int zy = y + h + 6;
		Neon.text(g, font, "VISUALS & LEISTUNG", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		List<Karte> vis = List.of(
				new Karte("Treffer-Partikel", "Extra Funken, wenn du triffst", () -> c.trefferPartikel, () -> c.trefferPartikel = !c.trefferPartikel, Farben.NEON_CYAN),
				new Karte("Low-End-Modus", "Sparsame Grafik für langsame PCs", () -> c.lowEnd, () -> Visuals.lowEnd(minecraft, !c.lowEnd), Farben.NEON));
		zy += karten(g, mx, my, vis, x, zy, b, 2);
		int hb = (b - 6) / 2;
		auswahl(g, mx, my, "ZEIT", Visuals.ZEITEN[Math.floorMod(c.zeitModus, Visuals.ZEITEN.length)], x, zy, hb,
				() -> c.zeitModus = Math.floorMod(c.zeitModus - 1, Visuals.ZEITEN.length),
				() -> c.zeitModus = Math.floorMod(c.zeitModus + 1, Visuals.ZEITEN.length));
		auswahl(g, mx, my, "WETTER", Visuals.WETTER[Math.floorMod(c.wetterModus, Visuals.WETTER.length)], x + hb + 6, zy, hb,
				() -> c.wetterModus = Math.floorMod(c.wetterModus - 1, Visuals.WETTER.length),
				() -> c.wetterModus = Math.floorMod(c.wetterModus + 1, Visuals.WETTER.length));
		zy += 28;
		Neon.text(g, font, "ANZEIGEN ANPASSEN", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		List<Karte> an = List.of(
				new Karte("Scoreboard", "Rechte Seitenleiste ein/aus", () -> c.scoreboardAn, () -> c.scoreboardAn = !c.scoreboardAn, Farben.NEON_CYAN),
				new Karte("Bossbar", "Bossleiste oben ein/aus", () -> c.bossbarAn, () -> c.bossbarAn = !c.bossbarAn, Farben.NEON_CYAN));
		zy += karten(g, mx, my, an, x, zy, b, 2);
		int sb = (b - 6) / 2;
		auswahl(g, mx, my, "SCORE", c.scoreboardGroesse + " %", x, zy, sb,
				() -> c.scoreboardGroesse = Anzeigen.naechste(c.scoreboardGroesse, -1),
				() -> c.scoreboardGroesse = Anzeigen.naechste(c.scoreboardGroesse, 1));
		auswahl(g, mx, my, "BOSS", c.bossbarGroesse + " %", x + sb + 6, zy, sb,
				() -> c.bossbarGroesse = Anzeigen.naechste(c.bossbarGroesse, -1),
				() -> c.bossbarGroesse = Anzeigen.naechste(c.bossbarGroesse, 1));
		zy += 22;
		auswahl(g, mx, my, "TAB", c.tablisteGroesse + " %", x, zy, sb,
				() -> c.tablisteGroesse = Anzeigen.naechste(c.tablisteGroesse, -1),
				() -> c.tablisteGroesse = Anzeigen.naechste(c.tablisteGroesse, 1));
		auswahl(g, mx, my, "CHAT", c.chatGroesse + " %", x + sb + 6, zy, sb,
				() -> setzeChat(c, -1), () -> setzeChat(c, 1));
		zy += 22;
		return zy - y;
	}

	/** Chat-Größe über die Minecraft-Option, damit Klicks auf Chat-Links weiter passen. */
	private void setzeChat(BambusConfig c, int richtung) {
		c.chatGroesse = Math.min(100, Anzeigen.naechste(c.chatGroesse, richtung));
		minecraft.options.chatScale().set(c.chatGroesse / 100.0);
		minecraft.options.save();
	}

	/** Zeichnet Karten im Raster und gibt die benötigte Höhe zurück. */
	private int karten(GuiGraphics g, int mx, int my, List<Karte> karten, int x, int y, int b) {
		return karten(g, mx, my, karten, x, y, b, b >= 420 ? 3 : 2);
	}

	private int karten(GuiGraphics g, int mx, int my, List<Karte> karten, int x, int y, int b, int spalten) {
		int abstand = 6;
		int kb = (b - 6 - abstand * (spalten - 1)) / spalten;
		int kh = 38;

		for (int i = 0; i < karten.size(); i++) {
			Karte k = karten.get(i);
			int kx = x + (i % spalten) * (kb + abstand);
			int ky = y + (i / spalten) * (kh + abstand);
			boolean an = k.an().getAsBoolean();
			boolean hover = imInhalt(mx, my) && Neon.ueber(mx, my, kx, ky, kb, kh);

			float hk = cfg().menueAnimation ? Anim.zu("karte:" + k.name(), hover ? 1f : 0f, 16f) : (hover ? 1f : 0f);
			g.fill(kx, ky, kx + kb, ky + kh, Anim.mische(Farben.KARTE, Farben.KARTE_HOVER, hk));
			if (an) {
				g.fill(kx, ky, kx + 2, ky + kh, k.akzent());
				g.renderOutline(kx, ky, kb, kh, Farben.alpha(k.akzent(), hover ? 140 : 70));
			} else {
				g.renderOutline(kx, ky, kb, kh, hover ? 0xFF2E4058 : Farben.LINIE);
			}

			Symbole.zeichne(g, Symbole.fuer(k.fav() != null ? k.fav() : k.name()), kx + 8, ky + 7, 1, an ? k.akzent() : 0xFF5A6A7E);
			String name = font.plainSubstrByWidth(k.name(), kb - (k.fav() != null ? 74 : 52));
			g.drawString(font, name, kx + 20, ky + 7, an ? Farben.WEISS : 0xFFC8D2DE, false);
			String text = font.plainSubstrByWidth(k.text(), (int) ((kb - 14) / 0.75f));
			Neon.text(g, font, text, kx + 8, ky + 22, 0.75f, Farben.GRAU, false);
			Neon.text(g, font, an ? "AN" : "AUS", kx + kb - 44, ky + 8, 0.75f, an ? k.akzent() : Farben.GRAU, false);
			Neon.schalter(g, kx + kb - 26, ky + 7, an, k.name());

			klick(kx, ky, kb, kh, () -> {
				k.umschalten().run();
				speichern();
			}, true);
			if (k.fav() != null) {
				boolean fav = cfg().favoriten.contains(k.fav());
				int sx = kx + kb - 58;
				boolean sh = imInhalt(mx, my) && Neon.ueber(mx, my, sx - 2, ky + 4, 12, 12);
				g.drawString(font, fav ? "★" : "☆", sx, ky + 6, fav ? Farben.GELB : sh ? Farben.WEISS : 0xFF4A5A6E, false);
				klick(sx - 2, ky + 4, 12, 12, () -> {
					if (!cfg().favoriten.remove(k.fav())) {
						cfg().favoriten.add(k.fav());
					}
					speichern();
				}, true);
			}
		}
		int zeilen = (karten.size() + spalten - 1) / spalten;
		return zeilen * (kh + abstand);
	}

	private int katBreite(String kat) {
		return kat.equals("★") ? 18 : font.width(kat) + 22;
	}

	private static String katSymbol(String kat) {
		return switch (kat) {
			case "PVP" -> "pvp";
			case "HUD" -> "hud";
			case "INFO" -> "info";
			case "★" -> "stern";
			default -> "alle";
		};
	}

	/** Suche + Kategorien über der Modul-Liste. */
	private void suchleiste(GuiGraphics g, int mx, int my) {
		int y = py + 60;
		suchFeld.setX(ix);
		suchFeld.setY(y + 1);
		int x = ix + 128;
		int aktivX = x, aktivB = 0;
		for (String kat : KATEGORIEN) {
			int b = katBreite(kat);
			if (kategorie.equals(kat)) { aktivX = x; aktivB = b; }
			x += b + 4;
		}
		x = ix + 128;
		for (String kat : KATEGORIEN) {
			int b = katBreite(kat);
			boolean hover = Neon.ueber(mx, my, x, y, b, 16);
			g.fill(x, y, x + b, y + 16, hover ? Farben.KARTE_HOVER : Farben.KARTE);
			g.renderOutline(x, y, b, 16, Farben.LINIE);
			x += b + 4;
		}
		boolean weich = cfg().menueAnimation;
		int mxk = weich ? Math.round(Anim.zu("kat-x", aktivX, 18f)) : aktivX;
		int mbk = weich ? Math.round(Anim.zu("kat-b", aktivB, 18f)) : aktivB;
		g.fill(mxk, y, mxk + mbk, y + 16, Farben.alpha(Farben.NEON, 40));
		g.renderOutline(mxk, y, mbk, 16, Farben.NEON);
		x = ix + 128;
		for (String kat : KATEGORIEN) {
			int b = katBreite(kat);
			boolean aktiv = kategorie.equals(kat);
			int farbe = aktiv ? Farben.NEON : kat.equals("★") ? Farben.GELB : Farben.GRAU;
			Symbole.zeichne(g, katSymbol(kat), x + 5, y + 4, 1, farbe);
			if (!kat.equals("★")) g.drawString(font, kat, x + 16, y + 4, farbe, false);
			final String ziel = kat;
			klick(x, y, b, 16, () -> {
				if (!kategorie.equals(ziel)) tabSeit = System.currentTimeMillis();
				kategorie = ziel;
				scroll = 0;
			}, false);
			x += b + 4;
		}
	}

	/** MEHR: Design, Profile, Teilen, Tasten, Statistik und Extras. */
	private int mehr(GuiGraphics g, int mx, int my, int x, int y, int b) {
		BambusConfig c = cfg();
		int zy = y;
		int hb = (b - 6) / 2;

		Neon.text(g, font, "DESIGN", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		auswahl(g, mx, my, "THEMA", Farben.THEMEN[Math.floorMod(c.thema, Farben.THEMEN.length)], x, zy, hb,
				() -> c.thema = Math.floorMod(c.thema - 1, Farben.THEMEN.length),
				() -> c.thema = Math.floorMod(c.thema + 1, Farben.THEMEN.length));
		zy += 22;
		zy += karten(g, mx, my, List.of(
				new Karte("Farbverlauf", "HUD-Kästen mit Verlauf statt einer Farbe", () -> c.verlauf, () -> c.verlauf = !c.verlauf, Farben.NEON_CYAN),
				new Karte("Weiche Animationen", "Menü, Schalter und Tabs gleiten weich", () -> c.menueAnimation, () -> c.menueAnimation = !c.menueAnimation, Farben.NEON),
				new Karte("Benachrichtigungen", "Neon-Karten für Updates, Freunde, Screenshots", () -> c.hinweise, () -> c.hinweise = !c.hinweise, Farben.NEON_CYAN),
				new Karte("Bambus-Ladebildschirm", "Eigener Ladebildschirm beim Betreten einer Welt", () -> c.ladebildschirm, () -> c.ladebildschirm = !c.ladebildschirm, Farben.NEON_PINK)),
				x, zy, b, 2);

		Neon.text(g, font, "PROFILE", x, zy + 2, 0.75f, Farben.NEON, false);
		zy += 14;
		for (int i = 0; i < Profile.NAMEN.length; i++) {
			final int slot = i;
			int px0 = x + i * (b / 3);
			Neon.text(g, font, Profile.NAMEN[i] + (Profile.vorhanden(i) ? "" : " (leer)"), px0, zy + 4, 0.75f, Farben.WEISS, false);
			kleinKnopf(g, mx, my, "SPEICHERN", px0, zy + 13, 52, () -> melde(Profile.speichern(slot) ? "Profil „" + Profile.NAMEN[slot] + "“ gespeichert" : "Speichern fehlgeschlagen"));
			if (Profile.vorhanden(i)) {
				kleinKnopf(g, mx, my, "LADEN", px0 + 56, zy + 13, 40, () -> melde(Profile.laden(slot) ? "Profil „" + Profile.NAMEN[slot] + "“ geladen" : "Laden fehlgeschlagen"));
			}
		}
		zy += 36;

		Neon.text(g, font, "TEILEN & STEUERUNG", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		kleinKnopf(g, mx, my, "EXPORT", x, zy, 60, () -> {
			minecraft.keyboardHandler.setClipboard(c.alsJson());
			melde("Einstellungen in die Zwischenablage kopiert");
		});
		kleinKnopf(g, mx, my, "IMPORT", x + 64, zy, 60, () ->
				melde(Profile.uebernehmen(minecraft.keyboardHandler.getClipboard()) ? "Einstellungen übernommen" : "In der Zwischenablage sind keine Bambus-Einstellungen"));
		kleinKnopf(g, mx, my, "TASTENBELEGUNG", x + 128, zy, 84, () ->
				minecraft.setScreen(new KeyBindsScreen(this, minecraft.options)));
		zy += 22;
		if (!meldung.isEmpty() && System.currentTimeMillis() - meldungZeit < 4000) {
			Neon.text(g, font, meldung, x, zy, 0.75f, Farben.NEON_CYAN, false);
		}
		zy += 12;

		Neon.text(g, font, "STATISTIK", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		Statistik.Daten d = Statistik.daten;
		String[][] werte = {
				{"Kills", String.valueOf(d.kills)}, {"davon Spieler", String.valueOf(d.killsSpieler)},
				{"Tode", String.valueOf(d.tode)}, {"K/D", Statistik.kd()},
				{"Beste Combo", String.valueOf(d.besteCombo)}, {"Spielzeit", Statistik.spielzeit()}};
		for (int i = 0; i < werte.length; i++) {
			int wx = x + (i % 3) * (b / 3), wy = zy + (i / 3) * 22;
			g.fill(wx, wy, wx + b / 3 - 6, wy + 19, Farben.KARTE);
			Neon.text(g, font, werte[i][0].toUpperCase(), wx + 5, wy + 3, 0.6f, Farben.GRAU, false);
			g.drawString(font, werte[i][1], wx + 5, wy + 10, Farben.WEISS, false);
		}
		zy += 48;
		zy = extras(g, mx, my, x, zy, b);
		return zy - y;
	}

	private static final String[][] SERVER = {
			{"Hypixel", "mc.hypixel.net", "Minispiele, SkyBlock, Bedwars"},
			{"GommeHD.net", "gommehd.net", "Deutsch · Bedwars, SkyWars"},
			{"GrieferGames", "griefergames.net", "Deutsch · Citybuild"},
			{"Minemen Club", "eu.minemen.club", "PvP-Training · Practice"},
	};

	/** Freunde, Extras (Screenshots, Auto-Screenshot, Discord) und empfohlene Server. */
	private int extras(GuiGraphics g, int mx, int my, int x, int zy, int b) {
		BambusConfig c = cfg();
		Neon.text(g, font, "FREUNDE", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		// Eingabefeld scrollt mit – nur zeigen, wenn es ganz im sichtbaren Bereich ist
		freundFeld.setX(x);
		freundFeld.setY(zy);
		freundFeld.setVisible(zy >= iy && zy + 14 <= iy + ih);
		kleinKnopf(g, mx, my, "+ FREUND", x + 124, zy, 56, () -> {
			String n = freundFeld.getValue().trim();
			if (n.matches("[A-Za-z0-9_]{2,16}") && c.freunde.stream().noneMatch(f -> f.equalsIgnoreCase(n))) {
				c.freunde.add(n);
				freundFeld.setValue("");
				speichern();
			}
		});
		zy += 20;
		int fx = x;
		for (String f : new ArrayList<>(c.freunde)) {
			int fb = font.width(f) + 22;
			if (fx + fb > x + b) {
				fx = x;
				zy += 16;
			}
			g.fill(fx, zy, fx + fb, zy + 13, Farben.alpha(Farben.NEON_PINK, 40));
			g.drawString(font, f, fx + 4, zy + 3, Farben.WEISS, false);
			g.drawString(font, "✕", fx + fb - 10, zy + 3, Farben.GRAU, false);
			klick(fx + fb - 12, zy, 12, 13, () -> {
				c.freunde.remove(f);
				speichern();
			}, true);
			fx += fb + 4;
		}
		if (c.freunde.isEmpty()) {
			Neon.text(g, font, "Noch keine Freunde – Name eintippen und + FREUND.", x, zy + 3, 0.75f, Farben.GRAU, false);
		}
		zy += 20;

		Neon.text(g, font, "EXTRAS", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		String discordText = DiscordStatus.moeglich() ? "Zeigt in Discord „Spielt Bambus Client“" : "Braucht eine Discord-App-ID (siehe Anleitung)";
		zy += karten(g, mx, my, List.of(
				new Karte("Auto-Screenshot", "Screenshot bei jedem Spieler-Kill", () -> c.autoScreenshot, () -> c.autoScreenshot = !c.autoScreenshot, Farben.NEON_CYAN),
				new Karte("Discord-Status", discordText, () -> c.discordAn, () -> c.discordAn = !c.discordAn, Farben.NEON_PINK)),
				x, zy, b, 2);
		kleinKnopf(g, mx, my, "SCREENSHOTS ANSEHEN", x, zy, 110, () -> minecraft.setScreen(new ScreenshotScreen(this)));
		kleinKnopf(g, mx, my, "RESSOURCENPAKETE", x + 114, zy, 100, () ->
				minecraft.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(this, minecraft.options)));
		zy += 24;

		Neon.text(g, font, "EMPFOHLENE SERVER", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		for (String[] sv : SERVER) {
			g.fill(x, zy, x + b - 6, zy + 18, Farben.KARTE);
			g.drawString(font, sv[0], x + 6, zy + 5, Farben.WEISS, false);
			Neon.text(g, font, sv[1] + " · " + sv[2], x + 10 + font.width(sv[0]), zy + 7, 0.6f, Farben.GRAU, false);
			kleinKnopf(g, mx, my, "+ LISTE", x + b - 62, zy + 2, 54, () -> {
				net.minecraft.client.multiplayer.ServerList liste = new net.minecraft.client.multiplayer.ServerList(minecraft);
				liste.load();
				if (liste.get(sv[1]) == null) {
					liste.add(new net.minecraft.client.multiplayer.ServerData(sv[0], sv[1],
							net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);
					liste.save();
					melde(sv[0] + " steht jetzt in deiner Serverliste");
				} else {
					melde(sv[0] + " ist schon in deiner Serverliste");
				}
			});
			zy += 21;
		}
		return zy + 4;
	}

	// ----------------------------------------------------------- Fadenkreuz

	private int fadenkreuz(GuiGraphics g, int mx, int my, int x, int y, int b) {
		BambusConfig c = cfg();

		// Vorschau
		int vb = 96;
		vorschauBox(g, x, y, vb, vb);
		Fadenkreuz.zeichneBei(g, x + vb / 2, y + vb / 2);
		Neon.text(g, font, "VORSCHAU", x + 4, y + vb + 4, 0.75f, Farben.GRAU, false);

		int rx = x + vb + 12;
		int rb = b - vb - 18;
		karten(g, mx, my, List.of(new Karte("Eigenes Fadenkreuz", "Ersetzt das normale Fadenkreuz",
				() -> c.fadenkreuzAn, () -> c.fadenkreuzAn = !c.fadenkreuzAn, Farben.NEON)), rx, y, rb, 1);

		int zy = y + 46;
		auswahl(g, mx, my, "FORM", Fadenkreuz.FORMEN[Math.floorMod(c.fadenkreuzForm, Fadenkreuz.FORMEN.length)],
				rx, zy, rb,
				() -> c.fadenkreuzForm = Math.floorMod(c.fadenkreuzForm - 1, Fadenkreuz.FORMEN.length),
				() -> c.fadenkreuzForm = Math.floorMod(c.fadenkreuzForm + 1, Fadenkreuz.FORMEN.length));

		zy += 26;
		Neon.text(g, font, "FARBE", rx, zy + 5, 0.75f, Farben.GRAU, false);
		int fx = rx + 44;
		for (int i = 0; i < Fadenkreuz.FARBEN.length; i++) {
			int sx = fx + i * 18;
			boolean gewaehlt = Math.floorMod(c.fadenkreuzFarbe, Fadenkreuz.FARBEN.length) == i;
			if (gewaehlt) {
				Neon.leuchtRahmen(g, sx - 1, zy - 1, 16, 16, Fadenkreuz.FARBEN[i], 1f);
			}
			g.fill(sx + 1, zy + 1, sx + 13, zy + 13, Fadenkreuz.FARBEN[i]);
			final int index = i;
			klick(sx, zy, 14, 14, () -> {
				c.fadenkreuzFarbe = index;
				speichern();
			}, true);
		}

		zy += 24;
		auswahl(g, mx, my, "GRÖSSE", String.valueOf(c.fadenkreuzGroesse), rx, zy, rb,
				() -> c.fadenkreuzGroesse = Math.max(Fadenkreuz.MIN_GROESSE, c.fadenkreuzGroesse - 1),
				() -> c.fadenkreuzGroesse = Math.min(Fadenkreuz.MAX_GROESSE, c.fadenkreuzGroesse + 1));

		return Math.max(vb + 16, zy + 22 - y);
	}

	/** Zeile „NAME   ‹ Wert ›“ mit zwei Pfeilen. */
	private void auswahl(GuiGraphics g, int mx, int my, String name, String wert, int x, int y, int b,
						 Runnable links, Runnable rechts) {
		Neon.text(g, font, name, x, y + 5, 0.75f, Farben.GRAU, false);
		int wx = x + 44;
		int wb = Math.min(b - 44, 150);
		g.fill(wx, y, wx + wb, y + 16, Farben.KARTE);
		g.renderOutline(wx, y, wb, 16, Farben.LINIE);

		boolean hl = imInhalt(mx, my) && Neon.ueber(mx, my, wx, y, 16, 16);
		boolean hr = imInhalt(mx, my) && Neon.ueber(mx, my, wx + wb - 16, y, 16, 16);
		g.drawString(font, "‹", wx + 6, y + 4, hl ? Farben.NEON : Farben.GRAU, false);
		g.drawString(font, "›", wx + wb - 10, y + 4, hr ? Farben.NEON : Farben.GRAU, false);
		String w = font.plainSubstrByWidth(wert, wb - 36);
		g.drawString(font, w, wx + (wb - font.width(w)) / 2, y + 4, Farben.WEISS, false);

		klick(wx, y, 16, 16, () -> {
			links.run();
			speichern();
		}, true);
		klick(wx + wb - 16, y, 16, 16, () -> {
			rechts.run();
			speichern();
		}, true);
	}

	// ------------------------------------------------------------- Kosmetik

	private int kosmetik(GuiGraphics g, int mx, int my, int x, int y, int b) {
		BambusConfig c = cfg();

		// Vorschau mit Spielerfigur
		int vb = 96;
		int vh = Math.max(110, Math.min(ih - 20, 150));
		vorschauBox(g, x, y, vb, vh);
		if (minecraft.player != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 4, y + 4, x + vb - 4, y + vh - 4,
					(int) (vh * 0.36f), 0.0625f, mx, my, minecraft.player);
		} else {
			String[] zeilen = {"Betritt eine", "Welt für die", "3D-Vorschau"};
			for (int i = 0; i < zeilen.length; i++) {
				int tb = (int) (font.width(zeilen[i]) * 0.75f);
				Neon.text(g, font, zeilen[i], x + (vb - tb) / 2, y + vh / 2 - 12 + i * 9, 0.75f, Farben.GRAU, false);
			}
		}
		List<Kosmetik.Cape> alleCapes = Kosmetik.alleCapes();
		List<Kosmetik.Fluegel> alleFluegel = Kosmetik.alleFluegel();
		Kosmetik.Cape cape = alleCapes.stream().filter(k -> k.id().equals(c.capeDesign)).findFirst().orElse(null);
		Kosmetik.Partikel partikel = Kosmetik.gewaehltePartikel();
		Kosmetik.Fluegel fluegel = Kosmetik.gewaehlteFluegel();
		Neon.text(g, font, "CAPE", x + 2, y + vh + 5, 0.75f, Farben.GRAU, false);
		Neon.text(g, font, cape != null ? cape.name() : "Keins", x + 34, y + vh + 5, 0.75f, Farben.NEON, false);
		Neon.text(g, font, "FLÜGEL", x + 2, y + vh + 15, 0.75f, Farben.GRAU, false);
		Neon.text(g, font, fluegel != null ? fluegel.name() : "Keine", x + 34, y + vh + 15, 0.75f, Farben.NEON_PINK, false);
		Neon.text(g, font, "SPUR", x + 2, y + vh + 25, 0.75f, Farben.GRAU, false);
		Neon.text(g, font, partikel != null ? partikel.name() : "Keine", x + 34, y + vh + 25, 0.75f, Farben.NEON_CYAN, false);

		// Unter-Tabs
		int rx = x + vb + 12;
		int rb = b - vb - 18;
		unterTab(g, mx, my, "CAPES", rx, y, kosmetikArt == 0, () -> kosmetikArt = 0);
		unterTab(g, mx, my, "FLÜGEL", rx + 54, y, kosmetikArt == 1, () -> kosmetikArt = 1);
		unterTab(g, mx, my, "PARTIKEL", rx + 108, y, kosmetikArt == 2, () -> kosmetikArt = 2);
		unterTab(g, mx, my, "HÜTE", rx + 162, y, kosmetikArt == 3, () -> kosmetikArt = 3);
		unterTab(g, mx, my, "KILL", rx + 216, y, kosmetikArt == 4, () -> kosmetikArt = 4);
		unterTab(g, mx, my, "PFEIL", rx + 270, y, kosmetikArt == 5, () -> kosmetikArt = 5);
		letzteKosmetikArt = kosmetikArt;
		// Eigene Kosmetik: Ordner öffnen + neu laden (unter der Vorschau)
		kleinKnopf(g, mx, my, "ORDNER", x, y + vh + 36, 46, () -> Util.getPlatform().openPath(EigeneKosmetik.ordner()));
		kleinKnopf(g, mx, my, "⟳ LADEN", x + 50, y + vh + 36, 46, EigeneKosmetik::neuLaden);

		// Kacheln
		int tb = 54;
		int th = 70;
		int abstand = 6;
		int spalten = Math.max(1, (rb + abstand) / (tb + abstand));
		int ty0 = y + 22;
		int anzahl = (kosmetikArt == 5 ? Kosmetik.SPUREN.size() : kosmetikArt == 4 ? Kosmetik.KILL_EFFEKTE.size()
				: kosmetikArt == 3 ? Kosmetik.HUETE.size() : kosmetikArt == 2 ? Kosmetik.PARTIKEL.size()
				: kosmetikArt == 1 ? alleFluegel.size() : alleCapes.size()) + 1;

		for (int i = 0; i < anzahl; i++) {
			int tx = rx + (i % spalten) * (tb + abstand);
			int ty = ty0 + (i / spalten) * (th + abstand);
			boolean hover = imInhalt(mx, my) && Neon.ueber(mx, my, tx, ty, tb, th);
			g.fill(tx, ty, tx + tb, ty + th, hover ? Farben.KARTE_HOVER : Farben.KARTE);

			String name;
			int farbe;
			boolean gewaehlt;
			Runnable aktion;

			if (i == 0) {
				name = "Keins";
				farbe = Farben.GRAU;
				gewaehlt = switch (kosmetikArt) {
					case 5 -> Kosmetik.effekt(Kosmetik.SPUREN, c.spur) == null;
					case 4 -> Kosmetik.effekt(Kosmetik.KILL_EFFEKTE, c.killEffekt) == null;
					case 3 -> Kosmetik.gewaehlterHut() == null;
					case 2 -> partikel == null;
					case 1 -> fluegel == null;
					default -> Kosmetik.AUS.equals(c.capeDesign);
				};
				g.drawString(font, "∅", tx + tb / 2 - 3, ty + 24, Farben.GRAU, false);
				aktion = switch (kosmetikArt) {
					case 5 -> () -> c.spur = Kosmetik.AUS;
					case 4 -> () -> c.killEffekt = Kosmetik.AUS;
					case 3 -> () -> c.hut = Kosmetik.AUS;
					case 2 -> () -> c.partikel = Kosmetik.AUS;
					case 1 -> () -> c.fluegel = Kosmetik.AUS;
					default -> () -> c.capeDesign = Kosmetik.AUS;
				};
			} else if (kosmetikArt >= 3) {
				String id;
				if (kosmetikArt == 3) {
					Kosmetik.Hut h = Kosmetik.HUETE.get(i - 1);
					id = h.id();
					name = h.name();
					farbe = h.farbe();
					gewaehlt = id.equals(c.hut);
					aktion = () -> c.hut = h.id();
				} else {
					Kosmetik.Effekt e = (kosmetikArt == 4 ? Kosmetik.KILL_EFFEKTE : Kosmetik.SPUREN).get(i - 1);
					id = e.id();
					name = e.name();
					farbe = e.farbe();
					gewaehlt = id.equals(kosmetikArt == 4 ? c.killEffekt : c.spur);
					aktion = kosmetikArt == 4 ? () -> c.killEffekt = e.id() : () -> c.spur = e.id();
				}
				partikelSymbol(g, tx + tb / 2, ty + 28, farbe, i);
			} else if (kosmetikArt == 1) {
				Kosmetik.Fluegel f = alleFluegel.get(i - 1);
				name = f.name();
				farbe = f.farbe();
				gewaehlt = fluegel != null && fluegel.id().equals(f.id());
				g.fill(tx + 4, ty + 4, tx + tb - 4, ty + 44, Farben.alpha(f.farbe(), 30));
				if (Kosmetik.fluegelForm(f.id()) != null) {   // 3D-Flügel: Bild 2:1
					g.blit(RenderPipelines.GUI_TEXTURED, Kosmetik.fluegelBild(f.id()),
							tx + tb / 2 - 22, ty + 13, 0f, 0f, 44, 22, 64, 32, 64, 32);
				} else {
					g.blit(RenderPipelines.GUI_TEXTURED, Kosmetik.fluegelBild(f.id()),
							tx + tb / 2 - 18, ty + 6, 0f, 0f, 36, 36, 64, 64, 64, 64);
				}
				if (Kosmetik.istEigen(f.id())) {
					eigenMarke(g, tx, ty);
				}
				aktion = () -> c.fluegel = f.id();
			} else if (kosmetikArt == 2) {
				Kosmetik.Partikel p = Kosmetik.PARTIKEL.get(i - 1);
				name = p.name();
				farbe = p.farbe();
				gewaehlt = partikel != null && partikel.id().equals(p.id());
				partikelSymbol(g, tx + tb / 2, ty + 28, p.farbe(), i);
				aktion = () -> c.partikel = p.id();
			} else {
				Kosmetik.Cape k = alleCapes.get(i - 1);
				name = k.name();
				farbe = k.farbe();
				gewaehlt = k.id().equals(c.capeDesign);
				// Vorderseite des Capes (10×16 Pixel im 64×32-Bild), doppelt groß
				g.fill(tx + tb / 2 - 11, ty + 5, tx + tb / 2 + 11, ty + 39, Farben.alpha(k.farbe(), 40));
				g.blit(RenderPipelines.GUI_TEXTURED, Kosmetik.capeBild(k.id()),
						tx + tb / 2 - 10, ty + 6, 1f, 1f, 20, 32, 10, 16, 64, 32);
				if (Kosmetik.istEigen(k.id())) {
					eigenMarke(g, tx, ty);
				}
				aktion = () -> {
					c.capeDesign = k.id();
					c.capeAn = true;
				};
			}

			if (gewaehlt) {
				Neon.leuchtRahmen(g, tx, ty, tb, th, farbe == Farben.GRAU ? Farben.NEON : farbe, 0.6f + 0.4f * Farben.puls());
				g.fill(tx + tb - 11, ty + 2, tx + tb - 2, ty + 11, Farben.NEON);
				g.drawString(font, "✔", tx + tb - 10, ty + 2, Farben.SCHWARZ, false);
			} else {
				g.renderOutline(tx, ty, tb, th, hover ? 0xFF2E4058 : Farben.LINIE);
			}

			String n = font.plainSubstrByWidth(name, (int) ((tb - 4) / 0.75f));
			int nb = (int) (font.width(n) * 0.75f);
			Neon.text(g, font, n, tx + (tb - nb) / 2, ty + th - 22, 0.75f, gewaehlt ? Farben.WEISS : 0xFFC8D2DE, false);
			String status = gewaehlt ? "AKTIV" : "WÄHLEN";
			int sb = (int) (font.width(status) * 0.6f);
			Neon.text(g, font, status, tx + (tb - sb) / 2, ty + th - 11, 0.6f, gewaehlt ? Farben.NEON : Farben.GRAU, false);

			final Runnable a = aktion;
			klick(tx, ty, tb, th, () -> {
				a.run();
				speichern();
			}, true);
		}

		int zeilen = (anzahl + spalten - 1) / spalten;
		return Math.max(vh + 56, 22 + zeilen * (th + abstand));
	}

	/** Kleines "EIGEN"-Schild für selbst erstellte Kosmetik. */
	private void eigenMarke(GuiGraphics g, int tx, int ty) {
		g.fill(tx + 2, ty + 2, tx + 26, ty + 9, Farben.alpha(Farben.NEON_PINK, 200));
		Neon.text(g, font, "EIGEN", tx + 4, ty + 3, 0.5f, Farben.SCHWARZ, false);
	}

	private void kleinKnopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, Runnable aktion) {
		boolean hover = imInhalt(mx, my) && Neon.ueber(mx, my, x, y, b, 15);
		g.fill(x, y, x + b, y + 15, hover ? Farben.KARTE_HOVER : Farben.KARTE);
		g.renderOutline(x, y, b, 15, hover ? Farben.NEON_PINK : Farben.LINIE);
		int tb = (int) (font.width(text) * 0.6f);
		Neon.text(g, font, text, x + (b - tb) / 2, y + 5, 0.6f, hover ? Farben.WEISS : Farben.GRAU, false);
		klick(x, y, b, 15, aktion, true);
	}

	private void unterTab(GuiGraphics g, int mx, int my, String text, int x, int y, boolean aktiv, Runnable aktion) {
		int b = 50;
		boolean hover = imInhalt(mx, my) && Neon.ueber(mx, my, x, y, b, 15);
		g.fill(x, y, x + b, y + 15, aktiv ? Farben.alpha(Farben.NEON_CYAN, 40) : Farben.KARTE);
		g.renderOutline(x, y, b, 15, aktiv ? Farben.NEON_CYAN : hover ? 0xFF2E4058 : Farben.LINIE);
		int tb = (int) (font.width(text) * 0.75f);
		Neon.text(g, font, text, x + (b - tb) / 2, y + 5, 0.75f, aktiv ? Farben.NEON_CYAN : Farben.GRAU, false);
		klick(x, y, b, 15, aktion, true);
	}

	private static void partikelSymbol(GuiGraphics g, int cx, int cy, int farbe, int saat) {
		float p = Farben.puls();
		g.fill(cx - 10, cy - 10, cx + 10, cy + 10, Farben.alpha(farbe, (int) (18 + 22 * p)));
		g.fill(cx - 6, cy - 6, cx + 6, cy + 6, Farben.alpha(farbe, 50));
		int[][] punkte = {{-8, -6}, {6, -9}, {9, 4}, {-5, 8}, {1, -1}, {-10, 2}};
		long zeit = System.currentTimeMillis() / 120;
		for (int i = 0; i < punkte.length; i++) {
			int dy = (int) ((zeit + i * 3 + saat) % 6) - 3;
			int x = cx + punkte[i][0];
			int y = cy + punkte[i][1] + dy;
			g.fill(x, y, x + 2, y + 2, farbe);
		}
	}

	private void vorschauBox(GuiGraphics g, int x, int y, int b, int h) {
		g.fillGradient(x, y, x + b, y + h, 0xFF0B1626, 0xFF050A10);
		Neon.gitter(g, x, y, b, h, 12, Farben.alpha(Farben.NEON_CYAN, 14));
		Neon.ecken(g, x, y, b, h, 8, Farben.NEON_CYAN);
	}

	// ------------------------------------------------------------- Knöpfe

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int h,
					   boolean voll, boolean aktiv, Runnable aktion) {
		boolean hover = aktiv && Neon.ueber(mx, my, x, y, b, h);
		if (!aktiv) {
			g.fill(x, y, x + b, y + h, Farben.KARTE);
			g.renderOutline(x, y, b, h, Farben.LINIE);
		} else if (voll) {
			g.fill(x, y, x + b, y + h, hover ? 0xFF7DFFB0 : Farben.NEON);
			if (hover) {
				Neon.leuchtRahmen(g, x, y, b, h, Farben.NEON, 1f);
			}
		} else {
			g.fill(x, y, x + b, y + h, hover ? Farben.alpha(Farben.NEON, 30) : Farben.KARTE);
			g.renderOutline(x, y, b, h, hover ? Farben.NEON : Farben.alpha(Farben.NEON, 120));
		}
		int farbe = !aktiv ? Farben.GRAU : voll ? Farben.SCHWARZ : Farben.NEON;
		g.drawString(font, text, x + (b - font.width(text)) / 2, y + (h - 8) / 2, farbe, false);
		if (aktiv) {
			klick(x, y, b, h, aktion, false);
		}
	}

	// ============================================================ Eingabe

	private void klick(int x, int y, int b, int h, Runnable aktion, boolean imInhalt) {
		bereiche.add(new Bereich(x, y, b, h, aktion, imInhalt));
	}

	private boolean imInhalt(double mx, double my) {
		return Neon.ueber(mx, my, ix, iy, ib, ih);
	}

	private void klickGeraeusch() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == 0) {
			// rückwärts, damit oben liegende Bereiche zuerst drankommen
			for (int i = bereiche.size() - 1; i >= 0; i--) {
				Bereich b = bereiche.get(i);
				if (b.imInhalt() && !imInhalt(e.x(), e.y())) {
					continue;
				}
				if (Neon.ueber(e.x(), e.y(), b.x(), b.y(), b.b(), b.h())) {
					klickGeraeusch();
					b.aktion().run();
					return true;
				}
			}
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertikal) {
		if (imInhalt(mx, my)) {
			scroll -= (int) (vertikal * 18);
			scroll = Math.max(0, Math.min(scroll, Math.max(0, inhaltHoehe - ih)));
			return true;
		}
		return super.mouseScrolled(mx, my, horizontal, vertikal);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (e.key() == InputConstants.KEY_TAB) {
			Tab[] alle = Tab.values();
			int richtung = (e.modifiers() & 1) != 0 ? -1 : 1; // Shift+Tab = zurück
			wechsle(alle[Math.floorMod(tab.ordinal() + richtung, alle.length)]);
			klickGeraeusch();
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public void onClose() {
		if (cfg().menueAnimation && schliessSeit == 0) {
			schliessSeit = System.currentTimeMillis();   // erst ausblenden, dann schließen (siehe render)
			return;
		}
		if (schliessSeit == 0 || !cfg().menueAnimation) {
			speichern();
			minecraft.setScreen(eltern);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
