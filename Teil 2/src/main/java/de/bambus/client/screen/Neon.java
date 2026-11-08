package de.bambus.client.screen;

import de.bambus.client.Farben;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Zeichenhilfen für den Neon-Look. */
public final class Neon {

	private Neon() {}

	/** Rahmen mit weichem Leuchten nach außen. */
	public static void leuchtRahmen(GuiGraphics g, int x, int y, int b, int h, int farbe, float staerke) {
		int a1 = (int) (70 * staerke);
		int a2 = (int) (30 * staerke);
		int a3 = (int) (12 * staerke);
		g.renderOutline(x - 3, y - 3, b + 6, h + 6, Farben.alpha(farbe, a3));
		g.renderOutline(x - 2, y - 2, b + 4, h + 4, Farben.alpha(farbe, a2));
		g.renderOutline(x - 1, y - 1, b + 2, h + 2, Farben.alpha(farbe, a1));
		g.renderOutline(x, y, b, h, farbe);
	}

	/** Futuristische Ecken statt eines vollen Rahmens. */
	public static void ecken(GuiGraphics g, int x, int y, int b, int h, int laenge, int farbe) {
		g.fill(x, y, x + laenge, y + 1, farbe);
		g.fill(x, y, x + 1, y + laenge, farbe);
		g.fill(x + b - laenge, y, x + b, y + 1, farbe);
		g.fill(x + b - 1, y, x + b, y + laenge, farbe);
		g.fill(x, y + h - 1, x + laenge, y + h, farbe);
		g.fill(x, y + h - laenge, x + 1, y + h, farbe);
		g.fill(x + b - laenge, y + h - 1, x + b, y + h, farbe);
		g.fill(x + b - 1, y + h - laenge, x + b, y + h, farbe);
	}

	/** Gitter-Hintergrund wie in einem Sci-Fi-Interface. */
	public static void gitter(GuiGraphics g, int x, int y, int b, int h, int abstand, int farbe) {
		for (int gx = x; gx < x + b; gx += abstand) {
			g.fill(gx, y, gx + 1, y + h, farbe);
		}
		for (int gy = y; gy < y + h; gy += abstand) {
			g.fill(x, gy, x + b, gy + 1, farbe);
		}
	}

	/** Ein-/Aus-Schalter, dessen Knopf weich hinübergleitet (schluessel = welcher Schalter). */
	public static void schalter(GuiGraphics g, int x, int y, boolean an, String schluessel) {
		if (!de.bambus.client.BambusClient.config.menueAnimation) {
			schalter(g, x, y, an);
			return;
		}
		float k = Anim.zu("schalter:" + schluessel, an ? 1f : 0f, 16f);
		int b = 18;
		int h = 9;
		int spur = Anim.mische(0xFF2A3445, Farben.alpha(Farben.NEON, 90), k);
		g.fill(x, y + 1, x + b, y + h - 1, spur);
		g.fill(x + 1, y, x + b - 1, y + h, spur);
		int kx = x + Math.round((b - h) * k);
		g.fill(kx + 1, y + 1, kx + h - 1, y + h - 1, Anim.mische(Farben.GRAU, Farben.NEON, k));
		if (k > 0.05f) {
			g.renderOutline(x - 1, y - 1, b + 2, h + 2, Farben.alpha(Farben.NEON, (int) (50 * k)));
		}
	}

	/** Ein-/Aus-Schalter in Pillenform. */
	public static void schalter(GuiGraphics g, int x, int y, boolean an) {
		int b = 18;
		int h = 9;
		g.fill(x, y + 1, x + b, y + h - 1, an ? Farben.alpha(Farben.NEON, 90) : 0xFF2A3445);
		g.fill(x + 1, y, x + b - 1, y + h, an ? Farben.alpha(Farben.NEON, 90) : 0xFF2A3445);
		int kx = an ? x + b - h : x;
		g.fill(kx + 1, y + 1, kx + h - 1, y + h - 1, an ? Farben.NEON : Farben.GRAU);
		if (an) {
			g.renderOutline(x - 1, y - 1, b + 2, h + 2, Farben.alpha(Farben.NEON, 50));
		}
	}

	/** Text in anderer Größe. */
	public static void text(GuiGraphics g, Font font, String text, int x, int y, float groesse, int farbe, boolean schatten) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(groesse, groesse);
		g.drawString(font, text, 0, 0, farbe, schatten);
		g.pose().popMatrix();
	}

	/** Text mit Neon-Schimmer (leicht versetzte, durchsichtige Kopie). */
	public static void leuchtText(GuiGraphics g, Font font, String text, int x, int y, float groesse, int farbe) {
		text(g, font, text, x + 1, y + 1, groesse, Farben.alpha(farbe, 60), false);
		text(g, font, text, x, y, groesse, farbe, false);
	}

	/** Das Bambus-Zeichen: drei leuchtende Segmente und ein Blatt. */
	public static void logo(GuiGraphics g, int x, int y, int hoehe) {
		int seg = Math.max(4, hoehe / 3);
		for (int i = 0; i < 3; i++) {
			int sy = y + i * seg;
			g.fill(x - 1, sy, x + 6, sy + seg - 1, Farben.alpha(Farben.NEON, 40));
			g.fill(x, sy, x + 5, sy + seg - 2, Farben.NEON);
			g.fill(x + 3, sy, x + 5, sy + seg - 2, Farben.GRUEN_DUNKEL);
		}
		g.fill(x + 6, y + seg / 2, x + 11, y + seg / 2 + 2, Farben.NEON_CYAN);
		g.fill(x + 9, y + seg / 2 - 2, x + 13, y + seg / 2, Farben.NEON_CYAN);
	}

	public static boolean ueber(double mx, double my, int x, int y, int b, int h) {
		return mx >= x && my >= y && mx < x + b && my < y + h;
	}
}
