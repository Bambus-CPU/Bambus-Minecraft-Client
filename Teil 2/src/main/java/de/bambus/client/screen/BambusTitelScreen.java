package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.EigeneKosmetik;
import de.bambus.client.Farben;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Das Bambus-Hauptmenü: ersetzt den Minecraft-Titelbildschirm.
 * Synthwave-Gitter, fliegende Bambus-Segmente, großes Logo, Neon-Knöpfe.
 * Über "Minecraft-Menü" kommt man jederzeit zum normalen Titelbildschirm.
 */
public class BambusTitelScreen extends Screen {

	/** Wenn true, ersetzt der Client den normalen Titelbildschirm in dieser Sitzung nicht mehr. */
	public static boolean vanillaGewuenscht = false;

	private record Knopf(String text, String unter, int akzent, Runnable aktion) {}

	private record Segment(float x, float y, float tempo, float groesse, int farbe) {}

	private final List<Knopf> knoepfe = new ArrayList<>();
	private final List<int[]> flaechen = new ArrayList<>(); // x, y, b, h, index
	private final List<Segment> segmente = new ArrayList<>();
	private final long start = System.currentTimeMillis();
	private final float[] hoverAnim = new float[10];

	public BambusTitelScreen() {
		super(Component.literal("Bambus Client"));
		Random r = new Random(7);
		for (int i = 0; i < 26; i++) {
			int farbe = r.nextInt(5) == 0 ? Farben.NEON_CYAN : r.nextInt(7) == 0 ? Farben.NEON_PINK : Farben.NEON;
			segmente.add(new Segment(r.nextFloat(), r.nextFloat(), 0.012f + r.nextFloat() * 0.03f, 2 + r.nextFloat() * 4, farbe));
		}
	}

	@Override
	protected void init() {
		knoepfe.clear();
		knoepfe.add(new Knopf("EINZELSPIELER", "Deine Welten", Farben.NEON,
				() -> minecraft.setScreen(new SelectWorldScreen(this))));
		knoepfe.add(new Knopf("MEHRSPIELER", "Server & LAN", Farben.NEON_CYAN,
				() -> minecraft.setScreen(new JoinMultiplayerScreen(this))));
		knoepfe.add(new Knopf("BAMBUS-MENÜ", "Module · Kosmetik · Fadenkreuz", Farben.NEON_PINK,
				() -> minecraft.setScreen(new BambusMenuScreen(this))));
		if (FabricLoader.getInstance().isModLoaded("modmenu")) {
			knoepfe.add(new Knopf("MODS", "Alle Mods ansehen und einstellen", 0xFFFFC846, this::oeffneModMenu));
		}
		knoepfe.add(new Knopf("OPTIONEN", "Grafik, Steuerung, Ton", 0xFFB0C4DE,
				() -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
		knoepfe.add(new Knopf("BEENDEN", "Bis bald!", Farben.ROT, () -> minecraft.stop()));
	}

	/** Öffnet die Mod-Liste von Mod Menu (über dessen öffentliche API, ohne feste Abhängigkeit). */
	private void oeffneModMenu() {
		try {
			Class<?> api = Class.forName("com.terraformersmc.modmenu.api.ModMenuApi");
			Object screen = api.getMethod("createModsScreen", Screen.class).invoke(null, this);
			minecraft.setScreen((Screen) screen);
		} catch (Throwable t) {
			BambusClient.LOGGER.warn("Mod Menu konnte nicht geöffnet werden: {}", t.toString());
		}
	}

	private float zeit() {
		return (System.currentTimeMillis() - start) / 1000f;
	}

	// ============================================================ Zeichnen

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		Farben.themaAnwenden(BambusClient.config.thema);
		float t = zeit();
		// Himmel: dunkler Verlauf
		g.fillGradient(0, 0, width, height, 0xFF03060C, 0xFF0A0418);
		int horizont = (int) (height * 0.62f);

		// Neon-Sonne hinter dem Horizont
		int sx = width * 3 / 4;
		int sr = Math.min(width, height) / 5;
		for (int y = -sr; y < 0; y += 2) {
			float k = (float) Math.sqrt(Math.max(0, sr * sr - y * y));
			float p = (y + sr) / (float) sr;            // 0 oben .. 1 Horizont
			if (p > 0.45f && ((int) ((y + t * 8) / 3) & 3) == 0) {
				continue;                                 // Synthwave-Streifen
			}
			int farbe = mischen(Farben.NEON_PINK, 0xFFFFD54A, 1 - p);
			g.fill((int) (sx - k), horizont + y, (int) (sx + k), horizont + y + 2, Farben.alpha(farbe, 150));
		}
		g.fill(0, horizont - 1, width, horizont + 1, Farben.alpha(Farben.NEON_PINK, 200));

		// Boden: perspektivisches Gitter, das auf dich zufährt
		g.fill(0, horizont + 1, width, height, 0xFF05030C);
		float lauf = (t * 0.6f) % 1f;
		for (int i = 0; i < 18; i++) {
			float z = (i + 1 - lauf);                      // 0.x .. 18
			int y = horizont + (int) ((height - horizont) * 1.6f / (z + 0.6f));
			if (y > height || y <= horizont) {
				continue;
			}
			int al = (int) Mth.clamp(220 - z * 11, 30, 220);
			g.fill(0, y, width, y + 1, Farben.alpha(Farben.NEON, al));
		}
		int mitte = width / 2;
		for (int i = -14; i <= 14; i++) {
			for (int y = horizont + 1; y < height; y += 2) {
				float p = (y - horizont) / (float) (height - horizont);
				int x = mitte + (int) (i * 30 * (0.08f + p * 2.4f));
				if (x < -2 || x > width + 2) {
					continue;
				}
				g.fill(x, y, x + 1, y + 2, Farben.alpha(Farben.NEON_CYAN, (int) (40 + 120 * p)));
			}
		}

		// Fliegende Bambus-Segmente
		for (Segment s : segmente) {
			float y = (s.y() - t * s.tempo()) % 1f;
			if (y < 0) {
				y += 1;
			}
			int x = (int) (s.x() * width + Mth.sin(t * 0.7f + s.x() * 20) * 6);
			int py = (int) (y * horizont);
			int b = (int) s.groesse();
			int h = b * 4;
			g.fill(x - 1, py - 1, x + b + 1, py + h + 1, Farben.alpha(s.farbe(), 30));
			g.fill(x, py, x + b, py + h, Farben.alpha(s.farbe(), 120));
			g.fill(x, py + h / 2, x + b, py + h / 2 + 1, Farben.alpha(0xFF000000, 120));
		}
		// leichte Vignette
		g.fillGradient(0, 0, width, height / 5, 0x90000000, 0x00000000);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		flaechen.clear();
		float t = zeit();
		float einblenden = Mth.clamp(t / 0.8f, 0, 1);

		// Logo
		int lx = Math.max(16, width / 12);
		int ly = Math.max(14, height / 9);
		float gross = Mth.clamp(width / 220f, 2.4f, 4.5f);
		Neon.logo(g, lx, ly + 2, (int) (gross * 9));
		int tx = lx + (int) (gross * 6);
		int glanz = Farben.alpha(Farben.NEON, (int) (50 + 40 * Farben.puls()));
		Neon.text(g, font, "BAMBUS", tx + 2, ly + 2, gross, glanz, false);
		Neon.text(g, font, "BAMBUS", tx, ly, gross, Farben.NEON, false);
		Neon.text(g, font, "C L I E N T", tx + 2, ly + (int) (gross * 9) + 2, 1.0f, Farben.NEON_CYAN, false);
		Neon.text(g, font, "v" + BambusClient.VERSION + "  ·  Minecraft 1.21.11", tx + 2, ly + (int) (gross * 9) + 14, 0.75f, Farben.GRAU, false);

		// Knöpfe
		int kb = Math.min(210, width / 3);
		int kh = height < 300 ? 22 : 26;
		int abstand = height < 300 ? 4 : 6;
		int ky = ly + (int) (gross * 9) + 34;
		for (int i = 0; i < knoepfe.size(); i++) {
			Knopf k = knoepfe.get(i);
			float verzoegert = Mth.clamp((t - 0.15f - i * 0.08f) / 0.4f, 0, 1);
			int x = lx - (int) ((1 - verzoegert) * 40);
			int y = ky + i * (kh + abstand);
			if (i == knoepfe.size() - 1) {
				y += 8;
			}
			boolean hover = Neon.ueber(mx, my, x, y, kb, kh);
			hoverAnim[i] += ((hover ? 1f : 0f) - hoverAnim[i]) * 0.25f;
			float h = hoverAnim[i];
			int verschub = (int) (h * 8);
			int al = (int) (255 * verzoegert * einblenden);
			if (al < 8) {
				continue;
			}
			g.fill(x, y, x + kb, y + kh, Farben.alpha(0xFF0A1220, (int) (al * 0.85f)));
			g.fill(x, y, x + (int) (kb * h), y + kh, Farben.alpha(k.akzent(), (int) (40 * h)));
			g.fill(x, y, x + 3, y + kh, Farben.alpha(k.akzent(), al));
			if (hover) {
				Neon.leuchtRahmen(g, x, y, kb, kh, k.akzent(), 0.6f + 0.4f * Farben.puls());
			} else {
				g.renderOutline(x, y, kb, kh, Farben.alpha(Farben.LINIE, al));
			}
			Neon.text(g, font, k.text(), x + 12 + verschub, y + 5, 1.0f, Farben.alpha(hover ? Farben.WEISS : k.akzent(), al), false);
			Neon.text(g, font, k.unter(), x + 12 + verschub, y + 16, 0.6f, Farben.alpha(Farben.GRAU, al), false);
			Neon.text(g, font, "›", x + kb - 14 + verschub / 2, y + 9, 1.0f, Farben.alpha(k.akzent(), (int) (al * (0.3f + 0.7f * h))), false);
			flaechen.add(new int[]{x, y, kb, kh, i});
		}

		// Info-Karte rechts oben
		int cb = 150;
		int cx = width - cb - 14;
		int cy = 14;
		if (cx > lx + kb + 20) {
			int ch = 106;
			g.fill(cx, cy, cx + cb, cy + ch, 0xC0080D16);
			Neon.ecken(g, cx - 3, cy - 3, cb + 6, ch + 6, 8, Farben.NEON_CYAN);
			Neon.text(g, font, "TASTEN", cx + 8, cy + 7, 0.75f, Farben.NEON_CYAN, false);
			String[][] tasten = {{"Rechts-Shift", "Bambus-Menü"}, {"C", "Zoom"}, {"Linke Alt", "Freelook"}, {"H", "HUD an/aus"}};
			for (int i = 0; i < tasten.length; i++) {
				Neon.text(g, font, tasten[i][0], cx + 8, cy + 20 + i * 11, 0.75f, Farben.WEISS, false);
				Neon.text(g, font, tasten[i][1], cx + 72, cy + 20 + i * 11, 0.75f, Farben.GRAU, false);
			}
			boolean boost = FabricLoader.getInstance().isModLoaded("sodium");
			Neon.text(g, font, boost ? "⚡ FPS-Boost aktiv" : "FPS-Boost aus", cx + 8, cy + 68, 0.75f, boost ? Farben.NEON : Farben.GRAU, false);
			int eigene = EigeneKosmetik.capes().size() + EigeneKosmetik.fluegel().size();
			Neon.text(g, font, "Eigene Kosmetik: " + eigene, cx + 8, cy + 82, 0.75f, Farben.NEON_PINK, false);
			boolean hover = Neon.ueber(mx, my, cx + 8, cy + 91, 90, 9);
			Neon.text(g, font, "› Ordner öffnen", cx + 8, cy + 92, 0.75f, hover ? Farben.WEISS : Farben.GRAU, false);
			flaechen.add(new int[]{cx + 8, cy + 91, 90, 9, 100});
		}

		// unten: Minecraft-Menü + Hinweis
		String vanilla = "Minecraft-Menü";
		int vb = (int) (font.width(vanilla) * 0.75f);
		int vx = width - vb - 10;
		int vy = height - 14;
		boolean vh = Neon.ueber(mx, my, vx - 4, vy - 3, vb + 8, 13);
		g.fill(vx - 4, vy - 3, vx + vb + 4, vy + 10, vh ? 0xC0152233 : 0x80080D16);
		g.renderOutline(vx - 4, vy - 3, vb + 8, 13, vh ? Farben.NEON : Farben.LINIE);
		Neon.text(g, font, vanilla, vx, vy, 0.75f, vh ? Farben.WEISS : Farben.GRAU, false);
		flaechen.add(new int[]{vx - 4, vy - 3, vb + 8, 13, 101});
		// Pflichthinweis nach den Minecraft-Markenrichtlinien von Mojang
		Neon.text(g, font, "Kein offizielles Minecraft-Produkt.", 8, height - 18, 0.6f, 0xFF4A5A6E, false);
		Neon.text(g, font, "Nicht von Mojang oder Microsoft genehmigt oder mit ihnen verbunden.", 8, height - 11, 0.6f, 0xFF4A5A6E, false);
	}

	private static int mischen(int a, int b, float t) {
		t = Mth.clamp(t, 0, 1);
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int gr = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | (r << 16) | (gr << 8) | bl;
	}

	// ============================================================ Eingabe

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == 0) {
			for (int[] f : flaechen) {
				if (Neon.ueber(e.x(), e.y(), f[0], f[1], f[2], f[3])) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					if (f[4] == 100) {
						Util.getPlatform().openPath(EigeneKosmetik.ordner());
					} else if (f[4] == 101) {
						vanillaGewuenscht = true;
						minecraft.setScreen(new TitleScreen());
					} else {
						knoepfe.get(f[4]).aktion().run();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
