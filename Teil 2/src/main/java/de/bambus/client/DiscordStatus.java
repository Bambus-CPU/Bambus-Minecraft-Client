package de.bambus.client;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Discord-Status („Spielt Bambus Client“) über die lokale Discord-Schnittstelle (IPC) – ohne Zusatzbibliothek.
 * Braucht eine Discord-Anwendungs-ID (kostenlos im Discord Developer Portal). Ohne ID bleibt alles aus.
 */
public final class DiscordStatus {

	private DiscordStatus() {}

	/** Fest eingebaute Discord-App-ID (leer = keine). Eine ID in der Konfiguration hat Vorrang. */
	public static final String STANDARD_APP_ID = "";

	private static SocketChannel unix;
	private static RandomAccessFile pipe;
	private static long start = System.currentTimeMillis() / 1000;
	private static long letzterVersuch, letztesUpdate;
	private static String letzterText = "";

	public static String appId() {
		String id = BambusClient.config.discordAppId;
		return id == null || id.isBlank() ? STANDARD_APP_ID : id.trim();
	}

	public static boolean moeglich() {
		return appId().matches("\\d{15,22}");
	}

	/** Jeden Tick; schickt höchstens alle 15 s ein Update. */
	public static void tick(net.minecraft.client.Minecraft mc) {
		if (!BambusClient.config.discordAn || !moeglich()) {
			trennen();
			return;
		}
		long jetzt = System.currentTimeMillis();
		if (!verbunden()) {
			if (jetzt - letzterVersuch < 30_000) {
				return;
			}
			letzterVersuch = jetzt;
			if (!verbinden()) {
				return;
			}
		}
		String details = mc.level == null ? "Im Hauptmenü"
				: mc.isLocalServer() ? "Einzelspieler"
				: mc.getCurrentServer() != null ? "Auf " + mc.getCurrentServer().ip : "Mehrspieler";
		String text = details + "|" + Kampf.kills;
		if (text.equals(letzterText) && jetzt - letztesUpdate < 60_000) {
			return;
		}
		if (jetzt - letztesUpdate < 15_000) {
			return;
		}
		letzterText = text;
		letztesUpdate = jetzt;
		String json = "{\"cmd\":\"SET_ACTIVITY\",\"nonce\":\"" + jetzt + "\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
				+ ",\"activity\":{\"details\":\"" + esc(details) + "\",\"state\":\"Bambus Client " + BambusClient.VERSION
				+ (Kampf.kills > 0 ? " · " + Kampf.kills + " Kills" : "") + "\",\"timestamps\":{\"start\":" + start
				+ "},\"assets\":{\"large_image\":\"bambus\",\"large_text\":\"Bambus Client\"}}}}";
		if (!senden(1, json)) {
			trennen();
		}
	}

	private static String esc(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static boolean verbunden() {
		return (unix != null && unix.isOpen()) || pipe != null;
	}

	private static boolean verbinden() {
		for (int i = 0; i < 10; i++) {
			try {
				if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
					pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
				} else {
					Path p = ipcOrdner().resolve("discord-ipc-" + i);
					if (!Files.exists(p)) {
						continue;
					}
					unix = SocketChannel.open(StandardProtocolFamily.UNIX);
					unix.connect(UnixDomainSocketAddress.of(p));
					unix.configureBlocking(false);
				}
				if (senden(0, "{\"v\":1,\"client_id\":\"" + appId() + "\"}")) {
					BambusClient.LOGGER.info("Discord-Status verbunden");
					return true;
				}
			} catch (Exception e) {
				trennen();
			}
		}
		return false;
	}

	private static Path ipcOrdner() {
		for (String var : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
			String v = System.getenv(var);
			if (v != null && !v.isEmpty()) {
				return Paths.get(v);
			}
		}
		return Paths.get("/tmp");
	}

	private static boolean senden(int op, String json) {
		byte[] daten = json.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buf = ByteBuffer.allocate(8 + daten.length).order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(op).putInt(daten.length).put(daten).flip();
		try {
			if (pipe != null) {
				pipe.write(buf.array());
				lesen();
			} else if (unix != null) {
				ByteBuffer weg = ByteBuffer.allocate(8192);
				while (unix.read(weg) > 0) {
					weg.clear(); // Antworten von Discord verwerfen
				}
				int versuche = 0;
				while (buf.hasRemaining() && versuche++ < 200) {
					unix.write(buf);
				}
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/** Antwort von Discord abholen, damit der Puffer nicht volläuft (nur Windows-Pipe, blockiert kurz). */
	private static void lesen() {
		try {
			lesenRoh();
		} catch (Exception ignoriert) {
			// Pipe kann keine Länge melden – dann eben nicht
		}
	}

	private static void lesenRoh() throws IOException {
		if (pipe != null && pipe.length() > 0) {
			byte[] kopf = new byte[8];
			pipe.readFully(kopf);
			int laenge = ByteBuffer.wrap(kopf).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
			pipe.skipBytes(laenge);
		}
	}

	public static void trennen() {
		try {
			if (unix != null) unix.close();
			if (pipe != null) pipe.close();
		} catch (IOException ignoriert) {
			// schon zu
		}
		unix = null;
		pipe = null;
	}
}
