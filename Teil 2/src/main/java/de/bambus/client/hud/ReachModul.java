package de.bambus.client.hud;

import de.bambus.client.Farben;
import de.bambus.client.Kampf;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Abstand beim letzten eigenen Treffer. */
public class ReachModul extends TextModul {

	public ReachModul() {
		super("reach", "Reach-Anzeige", 4, 134, true, "Abstand beim letzten Treffer", "PVP");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		boolean frisch = System.currentTimeMillis() - Kampf.letzterTreffer < 4000;
		if (Kampf.reach >= 0 && frisch) {
			z.add(new Zeile(String.format("Reach: %.2f m", Kampf.reach), Kampf.reach > 3.0 ? Farben.GELB : Farben.WEISS));
		} else if (editor) {
			z.add(new Zeile("Reach: 2.87 m", Farben.WEISS));
		}
		return z;
	}
}
