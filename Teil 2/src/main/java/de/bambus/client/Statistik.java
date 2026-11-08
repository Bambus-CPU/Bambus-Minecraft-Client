package de.bambus.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Spieler-Statistik über alle Sitzungen: Kills, Tode, beste Combo, Spielzeit. Nur lokal gespeichert. */
public final class Statistik {

	private Statistik() {}

	public static final class Daten {
		public long kills, killsSpieler, tode, besteCombo, spielSekunden, sitzungen;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static Daten daten = new Daten();
	private static long letzterSpeicher = System.currentTimeMillis();
	private static int ticks;

	private static Path datei() {
		return FabricLoader.getInstance().getConfigDir().resolve("bambus-statistik.json");
	}

	public static void laden() {
		try {
			if (Files.exists(datei())) {
				try (Reader r = Files.newBufferedReader(datei(), StandardCharsets.UTF_8)) {
					Daten d = GSON.fromJson(r, Daten.class);
					if (d != null) {
						daten = d;
					}
				}
			}
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Statistik nicht lesbar: {}", e.toString());
		}
		daten.sitzungen++;
	}

	public static void speichern() {
		try {
			Files.createDirectories(datei().getParent());
			try (Writer w = Files.newBufferedWriter(datei(), StandardCharsets.UTF_8)) {
				GSON.toJson(daten, w);
			}
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Statistik nicht speicherbar: {}", e.toString());
		}
		letzterSpeicher = System.currentTimeMillis();
	}

	static void kill(boolean spieler, int combo) {
		daten.kills++;
		if (spieler) {
			daten.killsSpieler++;
		}
		daten.besteCombo = Math.max(daten.besteCombo, combo);
		speichern();
	}

	static void tod() {
		daten.tode++;
		speichern();
	}

	/** Jeden Tick aufrufen, solange eine Welt offen ist. */
	public static void tick(boolean inWelt) {
		if (inWelt && ++ticks >= 20) {
			ticks = 0;
			daten.spielSekunden++;
		}
		if (System.currentTimeMillis() - letzterSpeicher > 60_000) {
			speichern();
		}
	}

	public static String spielzeit() {
		long s = daten.spielSekunden;
		return (s / 3600) + " h " + (s / 60 % 60) + " min";
	}

	public static String kd() {
		return daten.tode == 0 ? String.valueOf(daten.kills) : String.format("%.2f", daten.kills / (double) daten.tode);
	}
}
