package de.bambus.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Screenshot-Manager: deine Screenshots als Galerie – ansehen, öffnen, löschen. */
public class ScreenshotScreen extends Screen {

	private static final int PRO_SEITE = 6;

	private record Bild(Path datei, Identifier id, int b, int h) {}

	private final Screen eltern;
	private final List<Path> dateien = new ArrayList<>();
	private final List<Bild> geladen = new ArrayList<>();
	private final List<int[]> flaechen = new ArrayList<>();
	private int seite = 0;
	private Bild gross = null;

	public ScreenshotScreen(Screen eltern) {
		super(Component.literal("Screenshots"));
		this.eltern = eltern;
	}

	private Path ordner() {
		return minecraft.gameDirectory.toPath().resolve("screenshots");
	}

	@Override
	protected void init() {
		dateien.clear();
		try {
			Files.createDirectories(ordner());
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(ordner(), "*.png")) {
				ds.forEach(dateien::add);
			}
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Screenshots nicht lesbar: {}", e.toString());
		}
		dateien.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString())); // neueste zuerst
		ladeSeite();
	}

	private void freigeben() {
		for (Bild b : geladen) {
			minecraft.getTextureManager().release(b.id());
		}
		geladen.clear();
	}

	private void ladeSeite() {
		freigeben();
		gross = null;
		int start = seite * PRO_SEITE;
		for (int i = start; i < Math.min(dateien.size(), start + PRO_SEITE); i++) {
			Path p = dateien.get(i);
			try (InputStream in = Files.newInputStream(p)) {
				NativeImage img = NativeImage.read(in);
				Identifier id = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "screenshot/" + (i - start));
				int b = img.getWidth(), h = img.getHeight();
				minecraft.getTextureManager().register(id, new DynamicTexture(() -> "bambus screenshot", img));
				geladen.add(new Bild(p, id, b, h));
			} catch (Exception e) {
				BambusClient.LOGGER.warn("Screenshot {} nicht lesbar: {}", p.getFileName(), e.toString());
			}
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0xE0020407);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 7));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		flaechen.clear();
		Neon.leuchtText(g, font, "SCREENSHOTS", 16, 12, 1.5f, Farben.NEON);
		Neon.text(g, font, dateien.size() + " Bilder · neueste zuerst · F2 macht einen Screenshot", 18, 30, 0.75f, Farben.GRAU, false);

		if (gross != null) {
			int b = width - 60, h = height - 90;
			float s = Math.min(b / (float) gross.b(), h / (float) gross.h());
			int bb = (int) (gross.b() * s), bh = (int) (gross.h() * s);
			int x = (width - bb) / 2, y = 44 + (h - bh) / 2;
			g.blit(RenderPipelines.GUI_TEXTURED, gross.id(), x, y, 0f, 0f, bb, bh, gross.b(), gross.h(), gross.b(), gross.h());
			Neon.ecken(g, x - 3, y - 3, bb + 6, bh + 6, 10, Farben.NEON_CYAN);
			Neon.text(g, font, gross.datei().getFileName().toString(), x, y + bh + 6, 0.75f, Farben.GRAU, false);
			knopf(g, mx, my, "ÖFFNEN", width / 2 - 135, height - 26, 85, 1);
			knopf(g, mx, my, "LÖSCHEN", width / 2 - 42, height - 26, 85, 2);
			knopf(g, mx, my, "ZURÜCK", width / 2 + 51, height - 26, 85, 3);
			return;
		}
		if (geladen.isEmpty()) {
			Neon.text(g, font, "Noch keine Screenshots. Drücke im Spiel F2.", width / 2 - 90, height / 2, 0.75f, Farben.GRAU, false);
		}
		int spalten = 3, abst = 10;
		int tb = (width - 40 - abst * (spalten - 1)) / spalten;
		int th = (int) (tb * 9 / 16f);
		while (2 * th + abst + 90 > height && th > 30) {
			tb -= 10;
			th = (int) (tb * 9 / 16f);
		}
		int x0 = (width - (spalten * tb + (spalten - 1) * abst)) / 2;
		for (int i = 0; i < geladen.size(); i++) {
			Bild b = geladen.get(i);
			int x = x0 + (i % spalten) * (tb + abst), y = 46 + (i / spalten) * (th + abst);
			g.blit(RenderPipelines.GUI_TEXTURED, b.id(), x, y, 0f, 0f, tb, th, b.b(), b.h(), b.b(), b.h());
			boolean hover = Neon.ueber(mx, my, x, y, tb, th);
			if (hover) {
				Neon.leuchtRahmen(g, x, y, tb, th, Farben.NEON, 1f);
			} else {
				g.renderOutline(x, y, tb, th, Farben.LINIE);
			}
			flaechen.add(new int[]{x, y, tb, th, 100 + i});
		}
		int seiten = Math.max(1, (dateien.size() + PRO_SEITE - 1) / PRO_SEITE);
		knopf(g, mx, my, "‹", width / 2 - 120, height - 26, 30, 4);
		String s = (seite + 1) + " / " + seiten;
		g.drawString(font, s, width / 2 - 80, height - 21, Farben.WEISS, false);
		knopf(g, mx, my, "›", width / 2 - 40, height - 26, 30, 5);
		knopf(g, mx, my, "ORDNER", width / 2 + 0, height - 26, 70, 6);
		knopf(g, mx, my, "FERTIG", width / 2 + 76, height - 26, 70, 7);
	}

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int nr) {
		boolean hover = Neon.ueber(mx, my, x, y, b, 18);
		g.fill(x, y, x + b, y + 18, hover ? Farben.alpha(Farben.NEON, 40) : Farben.KARTE);
		g.renderOutline(x, y, b, 18, hover ? Farben.NEON : Farben.LINIE);
		g.drawString(font, text, x + (b - font.width(text)) / 2, y + 5, hover ? Farben.WEISS : Farben.NEON, false);
		flaechen.add(new int[]{x, y, b, 18, nr});
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		for (int[] f : flaechen) {
			if (!Neon.ueber(e.x(), e.y(), f[0], f[1], f[2], f[3])) {
				continue;
			}
			int nr = f[4];
			int seiten = Math.max(1, (dateien.size() + PRO_SEITE - 1) / PRO_SEITE);
			if (nr >= 100) {
				gross = geladen.get(nr - 100);
			} else if (nr == 1 && gross != null) {
				Util.getPlatform().openPath(gross.datei());
			} else if (nr == 2 && gross != null) {
				try {
					Files.deleteIfExists(gross.datei());
				} catch (Exception ignoriert) {
					// Datei gerade in Benutzung
				}
				init();
			} else if (nr == 3) {
				gross = null;
			} else if (nr == 4 && seite > 0) {
				seite--;
				ladeSeite();
			} else if (nr == 5 && seite < seiten - 1) {
				seite++;
				ladeSeite();
			} else if (nr == 6) {
				Util.getPlatform().openPath(ordner());
			} else if (nr == 7) {
				onClose();
			}
			return true;
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public void onClose() {
		freigeben();
		minecraft.setScreen(eltern);
	}

	@Override
	public void removed() {
		freigeben();
	}
}
