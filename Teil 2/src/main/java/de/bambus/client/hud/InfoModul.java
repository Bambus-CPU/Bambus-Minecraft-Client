package de.bambus.client.hud;

import de.bambus.client.Farben;
import de.bambus.client.Funktionen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

/** FPS, Koordinaten und CPS. */
public class InfoModul extends TextModul {

	public InfoModul() {
		super("info", "Info (FPS, XYZ, CPS)", 4, 4);
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		z.add(new Zeile("Bambus Client", Farben.GRUEN));
		z.add(new Zeile("FPS: " + mc.getFps(), Farben.WEISS));
		LocalPlayer p = mc.player;
		if (p != null) {
			z.add(new Zeile("XYZ: " + p.getBlockX() + " / " + p.getBlockY() + " / " + p.getBlockZ(), Farben.WEISS));
		}
		z.add(new Zeile("CPS: " + Funktionen.cpsLinks() + " | " + Funktionen.cpsRechts(), Farben.WEISS));
		return z;
	}
}
