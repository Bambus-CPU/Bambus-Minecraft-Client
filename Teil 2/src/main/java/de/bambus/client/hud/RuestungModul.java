package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Rüstung und Item in der Hand mit Haltbarkeit. */
public class RuestungModul extends HudModul {

	private static final int ZEILE = 17;
	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	public RuestungModul() {
		// x = 10000 bedeutet: ganz rechts am Bildschirmrand
		super("ruestung", "Rüstungsanzeige", 10000, 4);
	}

	private static List<ItemStack> items(Minecraft mc) {
		List<ItemStack> liste = new ArrayList<>();
		if (mc.player == null) {
			return liste;
		}
		for (EquipmentSlot slot : SLOTS) {
			ItemStack s = mc.player.getItemBySlot(slot);
			if (!s.isEmpty()) {
				liste.add(s);
			}
		}
		ItemStack hand = mc.player.getMainHandItem();
		if (!hand.isEmpty()) {
			liste.add(hand);
		}
		return liste;
	}

	private static String text(ItemStack s) {
		if (s.isDamageableItem()) {
			return String.valueOf(s.getMaxDamage() - s.getDamageValue());
		}
		return s.getCount() > 1 ? String.valueOf(s.getCount()) : "";
	}

	private static int farbe(ItemStack s) {
		if (!s.isDamageableItem()) {
			return Farben.WEISS;
		}
		float rest = (s.getMaxDamage() - s.getDamageValue()) / (float) s.getMaxDamage();
		return rest > 0.5f ? Farben.GRUEN : rest > 0.2f ? Farben.GELB : Farben.ROT;
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		List<ItemStack> liste = items(mc);
		if (liste.isEmpty()) {
			return editor ? mc.font.width("Rüstung") + 10 : 0;
		}
		int b = 0;
		for (ItemStack s : liste) {
			b = Math.max(b, mc.font.width(text(s)));
		}
		return 20 + b;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		int n = items(mc).size();
		if (n == 0) {
			return editor ? 15 : 0;
		}
		return n * ZEILE;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		List<ItemStack> liste = items(mc);
		if (liste.isEmpty()) {
			panel(g, x, y, breite(mc, true), 15);
			g.drawString(mc.font, "Rüstung", x + 6, y + 4, Farben.GRAU, true);
			return;
		}
		for (int i = 0; i < liste.size(); i++) {
			ItemStack s = liste.get(i);
			int zy = y + i * ZEILE;
			g.renderItem(s, x, zy);
			g.renderItemDecorations(mc.font, s, x, zy);
			g.drawString(mc.font, text(s), x + 19, zy + 4, farbe(s), true);
		}
	}
}
