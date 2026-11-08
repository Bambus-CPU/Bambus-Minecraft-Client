package de.bambus.client.hud;

import de.bambus.client.Farben;
import de.bambus.client.Kampf;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Treffer in Folge, ohne selbst getroffen zu werden. */
public class ComboModul extends TextModul {

	public ComboModul() {
		super("combo", "Combo-Zähler", 4, 120, true, "Treffer in Folge ohne Gegentreffer", "PVP");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		int c = editor && Kampf.combo == 0 ? 3 : Kampf.combo;
		if (c > 0) {
			int farbe = c >= 5 ? Farben.NEON_PINK : c >= 3 ? Farben.NEON : Farben.WEISS;
			z.add(new Zeile(c + " Combo", farbe));
		}
		return z;
	}
}
