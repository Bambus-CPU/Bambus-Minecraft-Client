package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import de.bambus.client.hud.HudModul;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * HUD-Editor: Anzeigen mit der Maus verschieben und in der Größe ändern.
 * Beim Ziehen rasten sie an Bildschirmrändern, der Mitte und an anderen Anzeigen ein
 * (Shift gedrückt halten = frei schieben). Größe: Ecke unten rechts ziehen oder Mausrad.
 */
public class HudEditorScreen extends Screen {

	private static final int RASTER = 4;      // Einrast-Abstand in Pixeln
	private static final int GRIFF = 5;       // Größe des Eck-Griffs

	private final Screen eltern;
	private HudModul gezogen = null;
	private HudModul groesseAendern = null;
	private int versatzX;
	private int versatzY;
	/** Hilfslinien des aktuellen Einrastens (-1 = keine). */
	private int linieX = -1;
	private int linieY = -1;
	private long groesseZeigenBis;
	private HudModul groesseZeigen;

	public HudEditorScreen(Screen eltern) {
		super(Component.literal("HUD verschieben"));
		this.eltern = eltern;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal("Zurücksetzen"), b -> {
			for (HudModul m : BambusClient.MODULE) {
				m.zuruecksetzen();
			}
			BambusClient.config.speichern();
		}).bounds(width / 2 - 104, height - 28, 100, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
				.bounds(width / 2 + 4, height - 28, 100, 20).build());
	}

	private boolean sichtbar(HudModul m) {
		return m.pos().an && m.breite(minecraft, true) > 0;
	}

	private int bx(HudModul m) { return m.x(minecraft, true); }
	private int by(HudModul m) { return m.y(minecraft, true); }
	private int bb(HudModul m) { return m.breiteS(minecraft, true); }
	private int bh(HudModul m) { return m.hoeheS(minecraft, true); }

	private boolean darueber(HudModul m, double mx, double my) {
		return mx >= bx(m) && my >= by(m) && mx < bx(m) + bb(m) && my < by(m) + bh(m);
	}

	private boolean aufGriff(HudModul m, double mx, double my) {
		int ex = bx(m) + bb(m), ey = by(m) + bh(m);
		return mx >= ex - GRIFF - 1 && my >= ey - GRIFF - 1 && mx <= ex + 2 && my <= ey + 2;
	}

	private HudModul oben(double mx, double my) {
		for (int i = BambusClient.MODULE.size() - 1; i >= 0; i--) {
			HudModul m = BambusClient.MODULE.get(i);
			if (sichtbar(m) && (darueber(m, mx, my) || aufGriff(m, mx, my))) return m;
		}
		return null;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mausX, int mausY, float delta) {
		// kein Weichzeichner – man soll das Spiel dahinter sehen
		g.fill(0, 0, width, height, 0x50000000);
		Neon.gitter(g, 0, 0, width, height, 20, Farben.alpha(Farben.NEON, 14));
		g.fill(width / 2, 0, width / 2 + 1, height, Farben.alpha(Farben.NEON_CYAN, 30));
		g.fill(0, height / 2, width, height / 2 + 1, Farben.alpha(Farben.NEON_CYAN, 30));
	}

	@Override
	public void render(GuiGraphics g, int mausX, int mausY, float delta) {
		HudModul hover = gezogen != null ? gezogen : groesseAendern != null ? groesseAendern : oben(mausX, mausY);

		for (HudModul m : BambusClient.MODULE) {
			if (!sichtbar(m)) {
				continue;
			}
			m.zeichneAnPosition(g, minecraft, true);
			int x = bx(m), y = by(m), b = bb(m), h = bh(m);
			boolean aktiv = m == hover;
			float k = Anim.zu("editor:" + m.id, aktiv ? 1f : 0f, 18f);
			g.renderOutline(x - 1, y - 1, b + 2, h + 2, Anim.mische(0x60FFFFFF, Farben.NEON, k));
			if (k > 0.02f) {
				Neon.leuchtRahmen(g, x - 1, y - 1, b + 2, h + 2, Farben.alpha(Farben.NEON, (int) (200 * k)), 0.6f * k);
				// Eck-Griff zum Größe-Ändern
				int gx = x + b - GRIFF, gy = y + h - GRIFF;
				g.fill(gx, gy, x + b + 1, y + h + 1, Farben.alpha(Farben.NEON, (int) (255 * k)));
				g.fill(gx + 1, gy + 1, x + b, y + h, Farben.alpha(0xFF05080C, (int) (255 * k)));
				g.fill(gx + 2, gy + 2, x + b - 1, y + h - 1, Farben.alpha(Farben.NEON, (int) (255 * k)));
				// Name darüber
				String name = m.name + "  " + Math.round(m.skala() * 100) + " %";
				int nb = (int) (font.width(name) * 0.75f) + 6;
				int ny = y - 11 < 0 ? y + h + 3 : y - 11;
				g.fill(x - 1, ny, x - 1 + nb, ny + 9, Farben.alpha(0xFF05080C, (int) (220 * k)));
				Neon.text(g, font, name, x + 2, ny + 1, 0.75f, Farben.alpha(Farben.NEON, (int) (255 * k)), false);
			}
		}

		// Hilfslinien beim Einrasten
		if (linieX >= 0) g.fill(linieX, 0, linieX + 1, height, Farben.alpha(Farben.NEON_PINK, 200));
		if (linieY >= 0) g.fill(0, linieY, width, linieY + 1, Farben.alpha(Farben.NEON_PINK, 200));

		// große Prozentanzeige beim Ändern der Größe
		HudModul gz = groesseAendern != null ? groesseAendern : (System.currentTimeMillis() < groesseZeigenBis ? groesseZeigen : null);
		if (gz != null) {
			String p = Math.round(gz.skala() * 100) + " %";
			Neon.leuchtText(g, font, p, width / 2 - (int) (font.width(p) * 1.5f), 18, 3f, Farben.NEON);
		}

		String tipp = "Ziehen = verschieben   ·   Ecke oder Mausrad = Größe   ·   Shift = ohne Einrasten   ·   Rechtsklick = 100 %";
		int tb = (int) (font.width(tipp) * 0.75f);
		g.fill(width / 2 - tb / 2 - 6, height - 44, width / 2 + tb / 2 + 6, height - 33, 0xB005080C);
		Neon.text(g, font, tipp, width / 2 - tb / 2, height - 42, 0.75f, Farben.WEISS, false);
		super.render(g, mausX, mausY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (super.mouseClicked(e, doppelt)) {
			return true;
		}
		HudModul m = oben(e.x(), e.y());
		if (m == null) {
			return false;
		}
		if (e.button() == 1) {            // Rechtsklick: Größe zurück auf 100 %
			m.pos().groesse = 1f;
			zeigeGroesse(m);
			BambusClient.config.speichern();
			return true;
		}
		if (aufGriff(m, e.x(), e.y())) {
			groesseAendern = m;
		} else {
			gezogen = m;
			versatzX = (int) e.x() - bx(m);
			versatzY = (int) e.y() - by(m);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (groesseAendern != null) {
			HudModul m = groesseAendern;
			float roh = (float) ((e.x() - bx(m)) / Math.max(1, m.breite(minecraft, true)));
			m.pos().groesse = Math.max(0.5f, Math.min(2.5f, Math.round(roh * 20) / 20f));   // in 5-%-Schritten
			return true;
		}
		if (gezogen != null) {
			int b = bb(gezogen);
			int h = bh(gezogen);
			int nx = (int) e.x() - versatzX;
			int ny = (int) e.y() - versatzY;
			linieX = -1;
			linieY = -1;
			if (!e.hasShiftDown()) {
				int[] sx = einrasten(nx, b, true);
				int[] sy = einrasten(ny, h, false);
				nx = sx[0];
				linieX = sx[1];
				ny = sy[0];
				linieY = sy[1];
			}
			gezogen.pos().x = Math.max(0, Math.min(nx, width - b));
			gezogen.pos().y = Math.max(0, Math.min(ny, height - h));
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	/**
	 * Sucht die nächste Kante (Bildschirmrand, Mitte, Kanten/Mitten anderer Anzeigen).
	 * Gibt {neue Position, Hilfslinie oder -1} zurück.
	 */
	private int[] einrasten(int pos, int groesse, boolean waagerecht) {
		int bildschirm = waagerecht ? width : height;
		int beste = pos, linie = -1, abstand = RASTER + 1;
		// Kandidaten als {Position der Anzeige, Lage der Hilfslinie}
		java.util.List<int[]> ziele = new java.util.ArrayList<>();
		ziele.add(new int[]{2, 2});
		ziele.add(new int[]{bildschirm - groesse - 2, bildschirm - 2});
		ziele.add(new int[]{bildschirm / 2 - groesse / 2, bildschirm / 2});
		for (HudModul o : BambusClient.MODULE) {
			if (o == gezogen || !sichtbar(o)) continue;
			int a = waagerecht ? bx(o) : by(o);
			int l = waagerecht ? bb(o) : bh(o);
			ziele.add(new int[]{a, a});                          // gleiche linke/obere Kante
			ziele.add(new int[]{a + l - groesse, a + l});        // gleiche rechte/untere Kante
			ziele.add(new int[]{a + l + 2, a + l});              // direkt dahinter
			ziele.add(new int[]{a - groesse - 2, a});            // direkt davor
			ziele.add(new int[]{a + l / 2 - groesse / 2, a + l / 2});   // mittig zueinander
		}
		for (int[] z : ziele) {
			int d = Math.abs(z[0] - pos);
			if (d < abstand) {
				abstand = d;
				beste = z[0];
				linie = z[1];
			}
		}
		return new int[]{beste, linie};
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		HudModul m = oben(mx, my);
		if (m != null && sy != 0) {
			m.pos().groesse = Math.max(0.5f, Math.min(2.5f, Math.round((m.skala() + (sy > 0 ? 0.05f : -0.05f)) * 20) / 20f));
			zeigeGroesse(m);
			BambusClient.config.speichern();
			return true;
		}
		return super.mouseScrolled(mx, my, sx, sy);
	}

	private void zeigeGroesse(HudModul m) {
		groesseZeigen = m;
		groesseZeigenBis = System.currentTimeMillis() + 900;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (gezogen != null || groesseAendern != null) {
			if (groesseAendern != null) zeigeGroesse(groesseAendern);
			gezogen = null;
			groesseAendern = null;
			linieX = -1;
			linieY = -1;
			BambusClient.config.speichern();
			return true;
		}
		return super.mouseReleased(e);
	}

	@Override
	public void onClose() {
		BambusClient.config.speichern();
		minecraft.setScreen(eltern);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
