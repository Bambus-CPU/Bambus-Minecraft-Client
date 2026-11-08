package de.bambus.client;

import de.bambus.client.mixin.LevelWetterAccessor;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

/** Eigene Zeit, eigenes Wetter, Treffer-Partikel und Low-End-Modus. Alles nur auf deinem Bildschirm. */
public final class Visuals {

	private Visuals() {}

	public static final String[] ZEITEN = {"Normal", "Morgen", "Mittag", "Abend", "Nacht"};
	private static final long[] ZEIT_WERTE = {-1, 0, 6000, 12500, 18000};
	public static final String[] WETTER = {"Normal", "Klar", "Regen", "Gewitter"};

	/** -1 = Zeit vom Server benutzen. */
	public static long eigeneZeit() {
		int i = BambusClient.config.zeitModus;
		return i > 0 && i < ZEIT_WERTE.length ? ZEIT_WERTE[i] : -1;
	}

	public static void tick(Minecraft mc) {
		int w = BambusClient.config.wetterModus;
		if (mc.level == null || w <= 0) {
			return;
		}
		float regen = w == 1 ? 0f : 1f;
		float gewitter = w == 3 ? 1f : 0f;
		LevelWetterAccessor a = (LevelWetterAccessor) mc.level;
		a.bambus$setRegen(regen);
		a.bambus$setRegenAlt(regen);
		a.bambus$setGewitter(gewitter);
		a.bambus$setGewitterAlt(gewitter);
	}

	/** Zusätzliche Funken beim Treffen. */
	public static void trefferPartikel(LivingEntity ziel) {
		Minecraft mc = Minecraft.getInstance();
		if (!BambusClient.config.trefferPartikel || mc.level == null) {
			return;
		}
		double x = ziel.getX(), y = ziel.getY() + ziel.getBbHeight() * 0.6, z = ziel.getZ();
		for (int i = 0; i < 12; i++) {
			double vx = (Math.random() - 0.5) * 0.6, vy = Math.random() * 0.4, vz = (Math.random() - 0.5) * 0.6;
			mc.level.addParticle(i % 3 == 0 ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT, x, y, z, vx, vy, vz);
		}
	}

	// ---------------------------------------------------------------- Low-End-Modus

	/** Schaltet sparsame Grafik an (alte Werte werden gemerkt und beim Ausschalten zurückgesetzt). */
	public static void lowEnd(Minecraft mc, boolean an) {
		BambusConfig c = BambusClient.config;
		Options o = mc.options;
		if (an && c.lowEndAlt == null) {
			c.lowEndAlt = new int[]{o.renderDistance().get(), o.simulationDistance().get(), o.biomeBlendRadius().get(),
					o.mipmapLevels().get(), o.cloudStatus().get().ordinal(), (int) Math.round(o.entityDistanceScaling().get() * 100)};
			o.renderDistance().set(Math.min(o.renderDistance().get(), 6));
			o.simulationDistance().set(5);
			o.biomeBlendRadius().set(0);
			o.mipmapLevels().set(0);
			o.cloudStatus().set(CloudStatus.OFF);
			o.entityDistanceScaling().set(0.75);
		} else if (!an && c.lowEndAlt != null) {
			int[] a = c.lowEndAlt;
			o.renderDistance().set(a[0]);
			o.simulationDistance().set(a[1]);
			o.biomeBlendRadius().set(a[2]);
			o.mipmapLevels().set(a[3]);
			o.cloudStatus().set(CloudStatus.values()[Math.max(0, Math.min(CloudStatus.values().length - 1, a[4]))]);
			o.entityDistanceScaling().set(a[5] / 100.0);
			c.lowEndAlt = null;
		}
		c.lowEnd = an;
		o.save();
	}
}
