package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/** Aktive Trank-Effekte mit Stufe und Restzeit. */
public class EffekteModul extends TextModul {

	private static final String[] ROEMISCH = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	public EffekteModul() {
		// x = 10000 bedeutet: ganz rechts am Bildschirmrand
		super("effekte", "Trank-Effekte", 10000, 100);
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		if (mc.player != null) {
			for (MobEffectInstance e : mc.player.getActiveEffects()) {
				MobEffect effekt = e.getEffect().value();
				StringBuilder text = new StringBuilder(effekt.getDisplayName().getString());
				int stufe = e.getAmplifier() + 1;
				if (stufe > 1) {
					text.append(' ').append(stufe < ROEMISCH.length ? ROEMISCH[stufe] : String.valueOf(stufe));
				}
				text.append("  ").append(zeit(e));
				z.add(new Zeile(text.toString(), effekt.isBeneficial() ? Farben.WEISS : Farben.ROT));
			}
		}
		if (z.isEmpty() && editor) {
			z.add(new Zeile("Trank-Effekte", Farben.GRAU));
		}
		return z;
	}

	private static String zeit(MobEffectInstance e) {
		if (e.isInfiniteDuration()) {
			return "∞";
		}
		int sekunden = e.getDuration() / 20;
		return (sekunden / 60) + ":" + String.format("%02d", sekunden % 60);
	}
}
