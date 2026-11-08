package de.bambus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Eigenes Fadenkreuz mit wählbarer Form, Farbe und Größe. */
public final class Fadenkreuz {

	public static final String[] FORMEN = {"Kreuz mit Lücke", "Plus", "Punkt", "Kreis"};
	public static final String[] FARBNAMEN = {"Weiß", "Neon-Grün", "Rot", "Gelb", "Cyan", "Pink"};
	public static final int[] FARBEN = {0xFFFFFFFF, 0xFF3DFF8A, 0xFFFF4D6D, 0xFFFFE14A, 0xFF00E5FF, 0xFFFF2BD6};
	public static final int MIN_GROESSE = 2;
	public static final int MAX_GROESSE = 10;

	private Fadenkreuz() {}

	public static void zeichne(GuiGraphics g, Minecraft mc) {
		if (!mc.options.getCameraType().isFirstPerson() || mc.options.hideGui) {
			return;
		}
		zeichneBei(g, g.guiWidth() / 2, g.guiHeight() / 2);
	}

	/** Zeichnet das Fadenkreuz um den Punkt (cx, cy) – auch für die Vorschau im Menü. */
	public static void zeichneBei(GuiGraphics g, int cx, int cy) {
		BambusConfig c = BambusClient.config;
		int farbe = FARBEN[Math.floorMod(c.fadenkreuzFarbe, FARBEN.length)];
		int s = Math.max(MIN_GROESSE, Math.min(MAX_GROESSE, c.fadenkreuzGroesse));

		// Rechtecke als {x1, y1, x2, y2}
		List<int[]> teile = new ArrayList<>();
		switch (Math.floorMod(c.fadenkreuzForm, FORMEN.length)) {
			case 0 -> {
				int luecke = 2;
				teile.add(new int[]{cx - luecke - s, cy, cx - luecke, cy + 1});
				teile.add(new int[]{cx + 1 + luecke, cy, cx + 1 + luecke + s, cy + 1});
				teile.add(new int[]{cx, cy - luecke - s, cx + 1, cy - luecke});
				teile.add(new int[]{cx, cy + 1 + luecke, cx + 1, cy + 1 + luecke + s});
			}
			case 1 -> {
				teile.add(new int[]{cx - s, cy, cx + s + 1, cy + 1});
				teile.add(new int[]{cx, cy - s, cx + 1, cy + s + 1});
			}
			case 2 -> {
				int r = Math.max(1, s / 3);
				teile.add(new int[]{cx - r + 1, cy - r + 1, cx + r, cy + r});
			}
			default -> {
				int punkte = 8 * s;
				for (int i = 0; i < punkte; i++) {
					double winkel = 2 * Math.PI * i / punkte;
					int px = cx + (int) Math.round(Math.cos(winkel) * s);
					int py = cy + (int) Math.round(Math.sin(winkel) * s);
					teile.add(new int[]{px, py, px + 1, py + 1});
				}
				teile.add(new int[]{cx, cy, cx + 1, cy + 1});
			}
		}

		// erst schwarzer Rand, dann Farbe – so ist es auf jedem Hintergrund sichtbar
		for (int[] t : teile) {
			g.fill(t[0] - 1, t[1] - 1, t[2] + 1, t[3] + 1, 0xA0000000);
		}
		for (int[] t : teile) {
			g.fill(t[0], t[1], t[2], t[3], farbe);
		}
	}
}
