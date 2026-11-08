package de.bambus.client.hud;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Zeigt an, ob Sprinten oder Schleichen gerade umgeschaltet (dauerhaft an) ist. */
public class StatusModul extends TextModul {

	public StatusModul() {
		super("status", "Sprint-/Schleich-Status", 4, 172);
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		if (BambusClient.config.toggleSprint && mc.options.keySprint.isDown()) {
			z.add(new Zeile("[Sprinten (umgeschaltet)]", Farben.GRUEN));
		}
		if (BambusClient.config.toggleSneak && mc.options.keyShift.isDown()) {
			z.add(new Zeile("[Schleichen (umgeschaltet)]", Farben.GRUEN));
		}
		if (z.isEmpty() && editor) {
			z.add(new Zeile("[Sprint-Status]", Farben.GRAU));
		}
		return z;
	}
}
