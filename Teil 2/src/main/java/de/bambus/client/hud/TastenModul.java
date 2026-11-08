package de.bambus.client.hud;

import de.bambus.client.Farben;
import de.bambus.client.Funktionen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Keystrokes: W A S D, Maustasten mit CPS, Leertaste. */
public class TastenModul extends HudModul {

	private static final int K = 22;      // Größe einer Taste
	private static final int ABSTAND = 2;
	private static final int BREITE = 3 * K + 2 * ABSTAND;

	public TastenModul() {
		super("tasten", "Keystrokes", 4, 54);
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		return BREITE;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		return 3 * (K + ABSTAND) + 12;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		Font font = mc.font;
		int ky = y;

		taste(g, font, mc.options.keyUp, x + K + ABSTAND, ky);
		ky += K + ABSTAND;
		taste(g, font, mc.options.keyLeft, x, ky);
		taste(g, font, mc.options.keyDown, x + K + ABSTAND, ky);
		taste(g, font, mc.options.keyRight, x + 2 * (K + ABSTAND), ky);
		ky += K + ABSTAND;

		int mausBreite = (BREITE - ABSTAND) / 2;
		maus(g, font, "LMB", Funktionen.cpsLinks(), mc.options.keyAttack.isDown(), x, ky, mausBreite);
		maus(g, font, "RMB", Funktionen.cpsRechts(), mc.options.keyUse.isDown(), x + mausBreite + ABSTAND, ky, mausBreite);
		ky += K + ABSTAND;

		boolean springen = mc.options.keyJump.isDown();
		g.fill(x, ky, x + BREITE, ky + 12, springen ? Farben.GRUEN_GEDRUECKT : Farben.HINTERGRUND);
		g.fill(x + BREITE / 2 - 12, ky + 5, x + BREITE / 2 + 12, ky + 7, springen ? Farben.SCHWARZ : Farben.WEISS);
	}

	private static void taste(GuiGraphics g, Font font, KeyMapping mapping, int x, int y) {
		boolean gedrueckt = mapping.isDown();
		g.fill(x, y, x + K, y + K, gedrueckt ? Farben.GRUEN_GEDRUECKT : Farben.HINTERGRUND);
		String name = mapping.getTranslatedKeyMessage().getString().toUpperCase();
		if (font.width(name) > K - 2) {
			name = name.substring(0, 1);
		}
		g.drawString(font, name, x + (K - font.width(name)) / 2, y + (K - 8) / 2,
				gedrueckt ? Farben.SCHWARZ : Farben.WEISS, !gedrueckt);
	}

	private static void maus(GuiGraphics g, Font font, String name, int cps, boolean gedrueckt, int x, int y, int b) {
		g.fill(x, y, x + b, y + K, gedrueckt ? Farben.GRUEN_GEDRUECKT : Farben.HINTERGRUND);
		g.drawString(font, name, x + (b - font.width(name)) / 2, y + 3,
				gedrueckt ? Farben.SCHWARZ : Farben.WEISS, !gedrueckt);
		String cpsText = cps + " CPS";
		g.drawString(font, cpsText, x + (b - font.width(cpsText)) / 2, y + 12,
				gedrueckt ? Farben.SCHWARZ : Farben.GRAU, false);
	}
}
