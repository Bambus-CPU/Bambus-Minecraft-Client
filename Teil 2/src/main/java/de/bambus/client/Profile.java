package de.bambus.client;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Einstellungs-Profile (z. B. PvP, Bauen, Aufnehmen) und Import/Export über die Zwischenablage. */
public final class Profile {

	private Profile() {}

	public static final String[] NAMEN = {"PvP", "Bauen", "Eigenes"};

	private static Path datei(int slot) {
		return FabricLoader.getInstance().getConfigDir().resolve("bambus-profil-" + (slot + 1) + ".json");
	}

	public static boolean vorhanden(int slot) {
		return Files.isRegularFile(datei(slot));
	}

	public static boolean speichern(int slot) {
		try {
			Files.writeString(datei(slot), BambusClient.config.alsJson(), StandardCharsets.UTF_8);
			return true;
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Profil nicht speicherbar: {}", e.toString());
			return false;
		}
	}

	public static boolean laden(int slot) {
		try {
			return uebernehmen(Files.readString(datei(slot), StandardCharsets.UTF_8));
		} catch (Exception e) {
			return false;
		}
	}

	/** Übernimmt Einstellungen aus JSON (Profil oder Zwischenablage). */
	public static boolean uebernehmen(String json) {
		BambusConfig neu = BambusConfig.ausJson(json);
		if (neu == null) {
			return false;
		}
		BambusClient.config = neu;
		neu.speichern();
		return true;
	}
}
