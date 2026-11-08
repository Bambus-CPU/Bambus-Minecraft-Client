package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.ArrayList;
import java.util.List;

/** Gezündetes TNT in der Nähe mit Restzeit bis zur Explosion. */
public class TntModul extends TextModul {

	public TntModul() {
		super("tnt", "TNT-Timer", 10000, 260, true, "Gezündetes TNT in der Nähe mit Restzeit", "PVP");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		if (mc.level != null && mc.player != null) {
			List<PrimedTnt> tnt = new ArrayList<>();
			for (Entity e : mc.level.entitiesForRendering()) {
				if (e instanceof PrimedTnt t && t.distanceTo(mc.player) < 24) {
					tnt.add(t);
				}
			}
			tnt.sort((a, b) -> Integer.compare(a.getFuse(), b.getFuse()));
			for (int i = 0; i < Math.min(4, tnt.size()); i++) {
				PrimedTnt t = tnt.get(i);
				float s = t.getFuse() / 20f;
				z.add(new Zeile(String.format("TNT %.1f s · %.0f m", s, t.distanceTo(mc.player)), s < 1.0f ? Farben.ROT : Farben.GELB));
			}
		}
		if (z.isEmpty() && editor) {
			z.add(new Zeile("TNT 2.4 s · 5 m", Farben.GELB));
		}
		return z;
	}
}
