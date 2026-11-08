package de.bambus.client.hud;

import de.bambus.client.Farben;
import de.bambus.client.Kampf;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Zeigt nach jedem Treffer, ob vorher ein Sprint-Reset (W-Tap, S-Tap) gemacht wurde. */
public class TapModul extends TextModul {

	public TapModul() {
		super("tap", "W-/S-Tap-Anzeige", 4, 148, false, "Sprint-Reset vor dem Treffer?", "PVP");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		boolean frisch = System.currentTimeMillis() - Kampf.tapZeit < 1500;
		String art = frisch ? Kampf.tapArt : editor ? "W-TAP" : "";
		if (!art.isEmpty()) {
			boolean gut = !art.equals("KEIN RESET");
			z.add(new Zeile((gut ? "✔ " : "✘ ") + art, gut ? Farben.NEON : Farben.ROT));
		}
		return z;
	}
}
