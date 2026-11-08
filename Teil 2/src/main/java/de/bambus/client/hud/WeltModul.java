package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Richtung, Biom, Tempo, Uhrzeit und Arbeitsspeicher – jede Zeile einzeln schaltbar als eigenes Modul. */
public class WeltModul extends TextModul {

	public enum Art { RICHTUNG, BIOM, TEMPO, UHR, SPEICHER }

	private static final String[] RICHTUNGEN = {"Süden (+Z)", "Westen (−X)", "Norden (−Z)", "Osten (+X)"};
	private static double letzteX, letzteZ;
	private static double tempo;

	private final Art art;

	public WeltModul(Art art) {
		super(id(art), name(art), 4, 60 + art.ordinal() * 14, art == Art.RICHTUNG, text(art), "INFO");
		this.art = art;
	}

	private static String id(Art a) {
		return switch (a) {
			case RICHTUNG -> "richtung";
			case BIOM -> "biom";
			case TEMPO -> "tempo";
			case UHR -> "uhr";
			case SPEICHER -> "speicher";
		};
	}

	private static String name(Art a) {
		return switch (a) {
			case RICHTUNG -> "Richtung";
			case BIOM -> "Biom";
			case TEMPO -> "Tempo";
			case UHR -> "Uhr";
			case SPEICHER -> "Arbeitsspeicher";
		};
	}

	private static String text(Art a) {
		return switch (a) {
			case RICHTUNG -> "Himmelsrichtung und Blickwinkel";
			case BIOM -> "In welchem Biom du gerade bist";
			case TEMPO -> "Geschwindigkeit in Blöcken pro Sekunde";
			case UHR -> "Echte Uhrzeit";
			case SPEICHER -> "Belegter Arbeitsspeicher von Minecraft";
		};
	}

	/** Jeden Tick: Tempo aus der Bewegung messen (waagerecht). */
	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			return;
		}
		double dx = p.getX() - letzteX, dz = p.getZ() - letzteZ;
		double neu = Math.sqrt(dx * dx + dz * dz) * 20.0;
		tempo = neu > 200 ? tempo : tempo * 0.7 + neu * 0.3; // Teleports ignorieren, sanft glätten
		letzteX = p.getX();
		letzteZ = p.getZ();
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		LocalPlayer p = mc.player;
		switch (art) {
			case RICHTUNG -> {
				if (p != null) {
					float yaw = Mth.wrapDegrees(p.getYRot());
					int i = Math.floorMod(Math.round(yaw / 90f), 4);
					z.add(new Zeile(RICHTUNGEN[i] + "  " + Math.round(yaw < 0 ? yaw + 360 : yaw) + "°", Farben.WEISS));
				}
			}
			case BIOM -> {
				if (p != null && mc.level != null) {
					String n = mc.level.getBiome(p.blockPosition()).unwrapKey()
							.map(k -> Component.translatable("biome." + k.identifier().getNamespace() + "." + k.identifier().getPath()).getString())
							.orElse("?");
					z.add(new Zeile("Biom: " + n, Farben.WEISS));
				}
			}
			case TEMPO -> z.add(new Zeile(String.format("Tempo: %.1f B/s", tempo), Farben.WEISS));
			case UHR -> {
				LocalTime t = LocalTime.now();
				z.add(new Zeile(String.format("%02d:%02d", t.getHour(), t.getMinute()), Farben.WEISS));
			}
			case SPEICHER -> {
				Runtime r = Runtime.getRuntime();
				long belegt = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20;
				int prozent = (int) (belegt * 100 / Math.max(1, max));
				z.add(new Zeile(String.format("RAM: %.1f / %.1f GB (%d %%)", belegt / 1024.0, max / 1024.0, prozent),
						prozent > 85 ? Farben.ROT : Farben.WEISS));
			}
		}
		return z;
	}
}
