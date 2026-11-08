package de.bambus.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** HUD-Modul, das aus einfachen Textzeilen in einem Kasten besteht. */
public abstract class TextModul extends HudModul {

	public record Zeile(String text, int farbe) {}

	protected TextModul(String id, String name, int stdX, int stdY) {
		super(id, name, stdX, stdY);
	}

	protected TextModul(String id, String name, int stdX, int stdY, boolean stdAn, String beschreibung, String kategorie) {
		super(id, name, stdX, stdY, stdAn, beschreibung, kategorie);
	}

	protected abstract List<Zeile> zeilen(Minecraft mc, boolean editor);

	@Override
	public int breite(Minecraft mc, boolean editor) {
		List<Zeile> z = zeilen(mc, editor);
		if (z.isEmpty()) {
			return 0;
		}
		int b = 0;
		for (Zeile zeile : z) {
			b = Math.max(b, mc.font.width(zeile.text()));
		}
		return b + 10;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		int n = zeilen(mc, editor).size();
		return n == 0 ? 0 : 5 + n * 10;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		List<Zeile> z = zeilen(mc, editor);
		panel(g, x, y, breite(mc, editor), hoehe(mc, editor));
		for (int i = 0; i < z.size(); i++) {
			g.drawString(mc.font, z.get(i).text(), x + 6, y + 4 + i * 10, z.get(i).farbe(), true);
		}
	}
}
