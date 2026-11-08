package de.bambus.client;

import de.bambus.client.screen.Neon;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Ladebildschirm im Bambus-Stil beim Betreten einer Welt bzw. eines Servers.
 * Er liegt über den Minecraft-Ladebildschirmen; deren Knöpfe (z. B. „Abbrechen“) bleiben bedienbar.
 */
public final class Ladebildschirm {

	private static final String[] TIPPS = {
			"RECHTS-SHIFT öffnet das Bambus-Menü",
			"Im HUD-Editor rasten Anzeigen an Kanten ein – mit Shift frei schieben",
			"Mausrad oder Ecke ziehen im HUD-Editor ändert die Größe",
			"Mit ★ markierst du Lieblingsmodule für die Favoriten-Liste",
			"Im Kosmetik-Studio im Launcher baust du eigene Capes und Flügel",
			"Low-End-Modus: mehr FPS auf langsamen Computern",
			"H schaltet das ganze Bambus-HUD an und aus",
			"Neue Bambus-Versionen kommen automatisch über den Launcher",
	};

	private static long seit;

	private Ladebildschirm() {}

	public static void registrieren() {
		ScreenEvents.AFTER_INIT.register((client, screen, breite, hoehe) -> {
			if (!istLadebildschirm(screen)) return;
			seit = System.currentTimeMillis();
			ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
				if (!BambusClient.config.ladebildschirm) return;
				zeichne(g, client, s);
				// Knöpfe der Minecraft-Seite wieder obendrauf zeichnen, damit man sie sieht
				for (AbstractWidget w : Screens.getButtons(s)) {
					w.render(g, mx, my, delta);
				}
			});
		});
	}

	private static boolean istLadebildschirm(Screen s) {
		return s instanceof LevelLoadingScreen || s instanceof ConnectScreen
				|| s instanceof ProgressScreen || s instanceof GenericMessageScreen;
	}

	private static void zeichne(GuiGraphics g, Minecraft mc, Screen s) {
		Font font = mc.font;
		int b = s.width;
		int h = s.height;
		long t = System.currentTimeMillis();
		float sek = (t - seit) / 1000f;

		// Hintergrund: dunkel, Gitter, weiches Leuchten in der Mitte
		g.fill(0, 0, b, h, 0xFF05080C);
		Neon.gitter(g, 0, 0, b, h, 22, Farben.alpha(Farben.NEON, 9));
		for (int i = 6; i > 0; i--) {
			int r = 40 + i * 26;
			g.fill(b / 2 - r, h / 2 - r / 2 - 20, b / 2 + r, h / 2 + r / 2 - 20, Farben.alpha(Farben.NEON, 4));
		}
		// wandernde Leuchtlinie über das Gitter
		int lx = (int) ((sek * 90) % (b + 120)) - 60;
		g.fillGradient(lx, 0, lx + 2, h, Farben.alpha(Farben.NEON_CYAN, 0), Farben.alpha(Farben.NEON_CYAN, 60));

		// Logo mit sanftem Pulsieren
		float puls = 0.5f + 0.5f * (float) Math.sin(sek * 3);
		int cy = h / 2 - 34;
		g.pose().pushMatrix();
		g.pose().translate(b / 2f - 20, cy - 26);
		g.pose().scale(2f, 2f);
		Neon.logo(g, 0, 0, 24);
		g.pose().popMatrix();
		Neon.leuchtRahmen(g, b / 2 - 34, cy - 34, 68, 64, Farben.alpha(Farben.NEON, (int) (90 + 120 * puls)), 0.4f + 0.6f * puls);

		String titel = "BAMBUS";
		int tb = (int) (font.width(titel) * 2.5f + font.width(" CLIENT") * 1.25f);
		int tx = b / 2 - tb / 2;
		Neon.leuchtText(g, font, titel, tx, cy + 40, 2.5f, Farben.NEON);
		Neon.text(g, font, " CLIENT", tx + (int) (font.width(titel) * 2.5f), cy + 48, 1.25f, Farben.WEISS, false);

		// Status (Titel der Minecraft-Seite, z. B. „Verbinden …“)
		String status = s.getTitle().getString();
		if (status.isEmpty()) status = "Welt wird geladen";
		int punkte = (int) (sek * 3) % 4;
		String st = status + ".".repeat(punkte);
		Neon.text(g, font, st, b / 2 - (int) (font.width(status) * 0.85f / 2), cy + 72, 0.85f, Farben.NEON_CYAN, false);

		// Ladebalken ohne festen Fortschritt: ein Leuchtsegment gleitet hin und her
		int bw = Math.min(220, b - 60);
		int bx = b / 2 - bw / 2;
		int by = cy + 86;
		g.fill(bx, by, bx + bw, by + 3, 0xFF14202E);
		float p = (float) (0.5 - 0.5 * Math.cos(sek * 2.2));
		int seg = bw / 4;
		int sx = bx + Math.round((bw - seg) * p);
		g.fillGradient(sx, by, sx + seg, by + 3, Farben.NEON, Farben.NEON_CYAN);
		g.fill(sx, by - 1, sx + seg, by, Farben.alpha(Farben.NEON, 60));
		g.fill(sx, by + 3, sx + seg, by + 4, Farben.alpha(Farben.NEON, 60));

		// Tipp, wechselt alle 4 Sekunden mit Einblenden
		int nr = (int) (sek / 4) % TIPPS.length;
		float k = Math.min(1f, (sek % 4) / 0.4f) * Math.min(1f, (4 - sek % 4) / 0.4f);
		String tipp = "TIPP  ·  " + TIPPS[nr];
		int tw = (int) (font.width(tipp) * 0.75f);
		Neon.text(g, font, tipp, b / 2 - tw / 2, h - 34, 0.75f, Farben.alpha(Farben.WEISS, (int) (40 + 200 * k)), false);
		String hinweis = "Kein offizielles Minecraft-Produkt. Nicht von Mojang oder Microsoft genehmigt.";
		int hw = (int) (font.width(hinweis) * 0.6f);
		Neon.text(g, font, hinweis, b / 2 - hw / 2, h - 14, 0.6f, 0xFF4A5A6E, false);
	}
}
