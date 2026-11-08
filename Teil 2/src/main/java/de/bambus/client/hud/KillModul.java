package de.bambus.client.hud;

import de.bambus.client.Farben;
import de.bambus.client.Kampf;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Kills und Tode dieser Sitzung. */
public class KillModul extends TextModul {

	public KillModul() {
		super("kills", "Kill-Zähler", 4, 162, false, "Kills, Tode und K/D dieser Sitzung", "PVP");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		String kd = Kampf.tode == 0 ? String.valueOf(Kampf.kills) : String.format("%.2f", Kampf.kills / (double) Kampf.tode);
		z.add(new Zeile("Kills: " + Kampf.kills + "  (Spieler " + Kampf.killsSpieler + ")", Farben.WEISS));
		z.add(new Zeile("Tode: " + Kampf.tode + "   K/D: " + kd, Farben.GRAU));
		return z;
	}
}
