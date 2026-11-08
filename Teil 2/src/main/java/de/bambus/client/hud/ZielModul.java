package de.bambus.client.hud;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import de.bambus.client.Kampf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Target-HUD: wen du zuletzt getroffen hast – Name, Abstand, letzter Schaden.
 * Die genauen Lebenspunkte des Gegners sind extra schaltbar, weil manche Server das verbieten.
 */
public class ZielModul extends HudModul {

	private static final int B = 140, H = 40;

	public ZielModul() {
		super("ziel", "Target-HUD", 10000, 60, true, "Name, Leben, Abstand und Schaden deines Ziels", "PVP");
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		return (Kampf.zielAktiv() || editor) ? B : 0;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		return (Kampf.zielAktiv() || editor) ? H : 0;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		LivingEntity z = Kampf.zielAktiv() ? Kampf.ziel : null;
		String name = z != null ? z.getName().getString() : "Gegner";
		float leben = z != null ? z.getHealth() : 14f;
		float max = z != null ? Math.max(1f, z.getMaxHealth()) : 20f;
		double abstand = z != null && mc.player != null ? mc.player.distanceTo(z) : 2.9;

		panel(g, x, y, B, H);
		boolean freund = false;
		for (String f : BambusClient.config.freunde) {
			freund |= f.equalsIgnoreCase(name);
		}
		if (freund) {
			g.fill(x + 2, y, x + B, y + 1, Farben.NEON_PINK);
			name = "♥ " + name;
		}
		g.drawString(mc.font, mc.font.plainSubstrByWidth(name, B - 50), x + 6, y + 4, Farben.WEISS, true);
		g.drawString(mc.font, String.format("%.1f m", abstand), x + B - 6 - mc.font.width(String.format("%.1f m", abstand)), y + 4, Farben.GRAU, true);

		int bx = x + 6, by = y + 16, bb = B - 12;
		float anteil = Mth.clamp(leben / max, 0, 1);
		int farbe = anteil > 0.5f ? Farben.NEON : anteil > 0.25f ? Farben.GELB : Farben.ROT;
		g.fill(bx, by, bx + bb, by + 6, 0xFF1E2C3D);
		g.fill(bx, by, bx + (int) (bb * anteil), by + 6, farbe);

		String unten;
		if (BambusClient.config.gegnerLebenZahl) {
			unten = String.format("%.1f / %.0f ❤", leben, max);
		} else {
			unten = (int) (anteil * 100) + " %";
		}
		g.drawString(mc.font, unten, x + 6, y + 26, Farben.WEISS, true);
		if (System.currentTimeMillis() - Kampf.schadenZeit < 1500 && Kampf.letzterSchaden > 0) {
			String s = String.format("-%.1f", Kampf.letzterSchaden);
			g.drawString(mc.font, s, x + B - 6 - mc.font.width(s), y + 26, Farben.ROT, true);
		} else if (Kampf.combo > 1) {
			String s = Kampf.combo + "x";
			g.drawString(mc.font, s, x + B - 6 - mc.font.width(s), y + 26, Farben.NEON_PINK, true);
		}
	}
}
