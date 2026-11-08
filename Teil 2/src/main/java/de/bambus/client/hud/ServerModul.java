package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;

import java.util.ArrayList;
import java.util.List;

/** Server-Adresse und Ping. */
public class ServerModul extends TextModul {

	public ServerModul() {
		super("server", "Server & Ping", 4, 146);
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		if (mc.hasSingleplayerServer()) {
			z.add(new Zeile("Einzelspieler", Farben.WEISS));
			return z;
		}

		ServerData server = mc.getCurrentServer();
		z.add(new Zeile("Server: " + (server != null ? server.ip : "?"), Farben.WEISS));

		int ping = -1;
		if (mc.getConnection() != null && mc.player != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (info != null) {
				ping = info.getLatency();
			}
		}
		int farbe = ping < 0 ? Farben.GRAU : ping < 80 ? Farben.GRUEN : ping < 180 ? Farben.GELB : Farben.ROT;
		z.add(new Zeile("Ping: " + (ping < 0 ? "?" : ping + " ms"), farbe));
		return z;
	}
}
