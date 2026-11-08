package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Anzahl der Pfeile im Inventar (wird nur gezeigt, wenn welche da sind). */
public class PfeileModul extends TextModul {

	public PfeileModul() {
		super("pfeile", "Pfeil-Zähler", 4, 176, true, "Pfeile im Inventar", "PVP");
	}

	static int pfeile(Minecraft mc) {
		if (mc.player == null) {
			return 0;
		}
		Inventory inv = mc.player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(Items.ARROW) || s.is(Items.SPECTRAL_ARROW) || s.is(Items.TIPPED_ARROW)) {
				n += s.getCount();
			}
		}
		return n;
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		int n = editor ? Math.max(64, pfeile(mc)) : pfeile(mc);
		if (n > 0) {
			z.add(new Zeile("➶ Pfeile: " + n, n < 16 ? Farben.GELB : Farben.WEISS));
		}
		return z;
	}
}
