package de.bambus.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Scoreboard-, Bossbar- und Tablisten-Anpassung: ausblenden und Größe ändern.
 * Die Chat-Größe nutzt die Minecraft-Option selbst, damit Klicks im Chat weiter stimmen.
 */
public final class Anzeigen {

	private Anzeigen() {}

	private enum Anker { RECHTS_MITTE, OBEN_MITTE }

	public static void registrieren() {
		BambusConfig c = BambusClient.config;
		umhuelle(VanillaHudElements.SCOREBOARD, () -> c.scoreboardAn, () -> c.scoreboardGroesse, Anker.RECHTS_MITTE);
		umhuelle(VanillaHudElements.BOSS_BAR, () -> c.bossbarAn, () -> c.bossbarGroesse, Anker.OBEN_MITTE);
		umhuelle(VanillaHudElements.PLAYER_LIST, () -> true, () -> c.tablisteGroesse, Anker.OBEN_MITTE);
	}

	private static void umhuelle(Identifier id, BooleanSupplier an, IntSupplier prozent, Anker anker) {
		HudElementRegistry.replaceElement(id, vanilla -> (HudElement) (g, delta) -> {
			if (!an.getAsBoolean()) {
				return;
			}
			int p = prozent.getAsInt();
			if (p == 100) {
				vanilla.render(g, delta);
				return;
			}
			float s = p / 100f;
			float ax = anker == Anker.RECHTS_MITTE ? g.guiWidth() : g.guiWidth() / 2f;
			float ay = anker == Anker.RECHTS_MITTE ? g.guiHeight() / 2f : 0f;
			g.pose().pushMatrix();
			g.pose().translate(ax, ay);
			g.pose().scale(s, s);
			g.pose().translate(-ax, -ay);
			vanilla.render(g, delta);
			g.pose().popMatrix();
		});
	}

	/** Größen-Stufen für das Menü. */
	public static final int[] STUFEN = {50, 60, 70, 80, 90, 100, 110, 125, 150};

	public static int naechste(int wert, int richtung) {
		int i = 0;
		for (int k = 0; k < STUFEN.length; k++) {
			if (STUFEN[k] == wert) {
				i = k;
			}
		}
		return STUFEN[Math.max(0, Math.min(STUFEN.length - 1, i + richtung))];
	}
}
