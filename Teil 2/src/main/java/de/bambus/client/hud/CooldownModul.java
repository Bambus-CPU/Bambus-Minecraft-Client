package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Abklingzeiten von Items in der Schnellleiste (Enderperle, Schild, Windkugel, Chorusfrucht …). */
public class CooldownModul extends HudModul {

	public CooldownModul() {
		super("cooldown", "Item-Cooldowns", 10000, 200, true, "Enderperle, Schild und Co. mit Restzeit-Balken", "PVP");
	}

	private static List<ItemStack> mitCooldown(Minecraft mc) {
		List<ItemStack> l = new ArrayList<>();
		if (mc.player == null) {
			return l;
		}
		Inventory inv = mc.player.getInventory();
		List<String> schon = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			pruefe(mc, inv.getItem(i), l, schon);
		}
		pruefe(mc, mc.player.getOffhandItem(), l, schon);
		return l;
	}

	private static void pruefe(Minecraft mc, ItemStack s, List<ItemStack> l, List<String> schon) {
		if (s.isEmpty() || !mc.player.getCooldowns().isOnCooldown(s)) {
			return;
		}
		String n = s.getHoverName().getString();
		if (!schon.contains(n)) {
			schon.add(n);
			l.add(s);
		}
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		return (editor || !mitCooldown(mc).isEmpty()) ? 110 : 0;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		int n = mitCooldown(mc).size();
		return n == 0 ? (editor ? 24 : 0) : 4 + n * 20;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		List<ItemStack> l = mitCooldown(mc);
		panel(g, x, y, 110, hoehe(mc, editor));
		if (l.isEmpty()) {
			g.drawString(mc.font, "Cooldowns", x + 6, y + 8, Farben.GRAU, true);
			return;
		}
		for (int i = 0; i < l.size(); i++) {
			ItemStack s = l.get(i);
			int zy = y + 3 + i * 20;
			g.renderItem(s, x + 4, zy);
			float rest = mc.player.getCooldowns().getCooldownPercent(s, 0f);
			int bb = 80;
			g.fill(x + 24, zy + 7, x + 24 + bb, zy + 11, 0xFF1E2C3D);
			g.fill(x + 24, zy + 7, x + 24 + (int) (bb * rest), zy + 11, rest > 0.5f ? Farben.ROT : rest > 0.2f ? Farben.GELB : Farben.NEON);
		}
	}
}
