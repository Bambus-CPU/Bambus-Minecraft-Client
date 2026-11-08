package de.bambus.launcher;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Random;

/**
 * Zeichnet Capes und Flügel aus Vorlage + Farben (oder aus einem eigenen Bild)
 * im Format, das der Bambus Client erwartet.
 *
 * Cape: 256×128 (Minecraft-Cape-Netz in 4-facher Auflösung, Außenseite bei (4,4) 40×64).
 * Flügel: 256×256, Gelenk links oben, der Flügel breitet sich nach rechts aus.
 */
final class KosmetikGenerator {

	private KosmetikGenerator() {}

	static final String[] CAPE_VORLAGEN = {"Verlauf", "Streifen", "Bambus", "Sterne", "Platine", "Welle"};
	static final String[] FLUEGEL_VORLAGEN = {"Federn", "Drache", "Schmetterling", "Cyber"};

	/** Einstellungen aus dem Studio. */
	static final class Rezept {
		boolean cape = true;
		String vorlage = "Verlauf";
		Color farbe1 = new Color(0x3DFF8A);
		Color farbe2 = new Color(0x00E5FF);
		Color leuchten = new Color(0xFF2BD6);
		String initialen = "";
		BufferedImage eigenesBild;
		long zufall = 1;
	}

	static BufferedImage erzeuge(Rezept r) {
		return r.cape ? cape(r) : fluegel(r);
	}

	// ================================================================== Capes

	private static final int FB = 40, FH = 64;   // Außenseite in der 256×128-Textur
	private static final int SS = 4;             // Überabtastung

	static BufferedImage cape(Rezept r) {
		if (r.eigenesBild != null && istCapeNetz(r.eigenesBild)) {
			return skaliere(r.eigenesBild, 256, 128, r.eigenesBild.getWidth() <= 128);
		}
		int w = FB * SS, h = FH * SS;
		BufferedImage front = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = glatt(front);
		if (r.eigenesBild != null) {
			// beliebiges Bild: füllend auf die Außenseite setzen
			g.setColor(dunkel(r.farbe1, 0.25f));
			g.fillRect(0, 0, w, h);
			zeichneFuellend(g, r.eigenesBild, 0, 0, w, h);
		} else {
			capeMotiv(g, r, w, h);
		}
		if (r.initialen != null && !r.initialen.trim().isEmpty()) {
			monogramm(g, r.initialen.trim(), w, h, r.leuchten);
		}
		// Leuchtrand
		g.setComposite(AlphaComposite.SrcOver);
		for (int i = 4; i >= 1; i--) {
			g.setColor(alpha(r.leuchten, 30 + (4 - i) * 30));
			g.setStroke(new BasicStroke(i * 4f));
			g.drawRect(0, 0, w - 1, h - 1);
		}
		g.setColor(r.leuchten);
		g.setStroke(new BasicStroke(4f));
		g.drawRect(2, 2, w - 5, h - 5);
		g.dispose();

		BufferedImage klein = skaliere(front, FB, FH, false);
		BufferedImage tex = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
		Graphics2D t = tex.createGraphics();
		t.drawImage(klein, 4, 4, null);
		// Innenseite: abgedunkelte, weiche Version
		BufferedImage innen = weich(klein);
		t.drawImage(innen, 48, 4, null);
		t.setColor(new Color(0, 0, 0, 140));
		t.fillRect(48, 4, FB, FH);
		Color rand = dunkel(r.leuchten, 0.55f);
		t.setColor(rand);
		t.fillRect(4, 0, FB, 4);
		t.fillRect(44, 0, FB, 4);
		t.fillRect(0, 4, 4, FH);
		t.fillRect(44, 4, 4, FH);
		t.dispose();
		return tex;
	}

	/** Sieht das Bild schon wie ein fertiges Cape-Netz aus (2:1, z. B. 64×32)? */
	static boolean istCapeNetz(BufferedImage b) {
		return b.getWidth() == b.getHeight() * 2 && b.getWidth() >= 64;
	}

	private static void capeMotiv(Graphics2D g, Rezept r, int w, int h) {
		Random z = new Random(r.zufall);
		g.setPaint(new GradientPaint(0, 0, r.farbe1, 0, h, dunkel(r.farbe2, 0.6f)));
		g.fillRect(0, 0, w, h);
		switch (r.vorlage) {
			case "Streifen": {
				g.setStroke(new BasicStroke(w * 0.09f));
				for (int i = -h; i < w + h; i += (int) (w * 0.28f)) {
					g.setColor(alpha(r.farbe2, 170));
					g.drawLine(i, 0, i - h, h);
					g.setColor(alpha(r.leuchten, 120));
					g.setStroke(new BasicStroke(w * 0.02f));
					g.drawLine(i + (int) (w * 0.07f), 0, i + (int) (w * 0.07f) - h, h);
					g.setStroke(new BasicStroke(w * 0.09f));
				}
				break;
			}
			case "Bambus": {
				g.setPaint(new GradientPaint(0, 0, dunkel(r.farbe1, 0.25f), 0, h, dunkel(r.farbe1, 0.1f)));
				g.fillRect(0, 0, w, h);
				int[] xs = {(int) (w * 0.22f), (int) (w * 0.55f), (int) (w * 0.83f)};
				for (int k = 0; k < xs.length; k++) {
					int bx = xs[k], bb = (int) (w * (0.15f - k * 0.02f));
					int seg = (int) (h * (0.17f + k * 0.03f));
					for (int y = -z.nextInt(seg); y < h; y += seg) {
						g.setPaint(new GradientPaint(bx - bb / 2f, 0, r.farbe1, bx + bb / 2f, 0, dunkel(r.farbe1, 0.55f)));
						g.fillRoundRect(bx - bb / 2, y + 3, bb, seg - 6, bb / 2, bb / 2);
						g.setColor(r.farbe2);
						g.fillRect(bx - bb / 2 - 3, y + seg - 5, bb + 6, 5);
						if (z.nextBoolean()) {
							blatt(g, bx + bb / 2, y + seg - 4, w * 0.22f, -0.5, r.farbe2);
						} else {
							blatt(g, bx - bb / 2, y + seg - 4, w * 0.22f, Math.PI + 0.5, r.farbe2);
						}
					}
				}
				break;
			}
			case "Sterne": {
				g.setPaint(new GradientPaint(0, 0, dunkel(r.farbe1, 0.35f), 0, h, dunkel(r.farbe2, 0.25f)));
				g.fillRect(0, 0, w, h);
				for (int i = 0; i < 4; i++) {
					float cx = z.nextFloat() * w, cy = z.nextFloat() * h, rad = w * (0.3f + z.nextFloat() * 0.4f);
					Color c = i % 2 == 0 ? r.farbe1 : r.farbe2;
					g.setPaint(new RadialGradientPaint(cx, cy, rad, new float[]{0, 1}, new Color[]{alpha(c, 110), alpha(c, 0)}));
					g.fill(new Ellipse2D.Float(cx - rad, cy - rad, rad * 2, rad * 2));
				}
				for (int i = 0; i < 70; i++) {
					float x = z.nextFloat() * w, y = z.nextFloat() * h, s = 1.5f + z.nextFloat() * 4;
					g.setColor(i % 9 == 0 ? r.leuchten : Color.WHITE);
					g.fill(new Ellipse2D.Float(x - s / 2, y - s / 2, s, s));
					if (i % 12 == 0) {
						stern(g, x, y, s * 4, r.leuchten);
					}
				}
				break;
			}
			case "Platine": {
				g.setColor(dunkel(r.farbe1, 0.15f));
				g.fillRect(0, 0, w, h);
				g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				for (int i = 0; i < 16; i++) {
					int x = z.nextInt(w), y = z.nextInt(h);
					GeneralPath p = new GeneralPath();
					p.moveTo(x, y);
					for (int k = 0; k < 4; k++) {
						if (k % 2 == 0) {
							y += (z.nextBoolean() ? 1 : -1) * (20 + z.nextInt(60));
						} else {
							x += (z.nextBoolean() ? 1 : -1) * (12 + z.nextInt(40));
							y += (z.nextBoolean() ? 1 : -1) * (12 + z.nextInt(20));
						}
						p.lineTo(x, y);
					}
					Color c = i % 3 == 0 ? r.leuchten : i % 3 == 1 ? r.farbe1 : r.farbe2;
					g.setColor(alpha(c, 70));
					g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g.draw(p);
					g.setColor(c);
					g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g.draw(p);
					g.fill(new Ellipse2D.Float(x - 7, y - 7, 14, 14));
				}
				break;
			}
			case "Welle": {
				for (int k = 0; k < 7; k++) {
					GeneralPath p = new GeneralPath();
					float y0 = h * (0.15f + k * 0.13f);
					p.moveTo(-10, y0);
					for (int x = 0; x <= w + 10; x += 8) {
						p.lineTo(x, y0 + Math.sin(x / (double) w * Math.PI * 2 + k * 0.9) * h * 0.05);
					}
					p.lineTo(w + 10, h + 10);
					p.lineTo(-10, h + 10);
					p.closePath();
					float t = k / 6f;
					g.setColor(alpha(mische(r.farbe1, r.farbe2, t), 150));
					g.fill(p);
					g.setColor(alpha(r.leuchten, 160));
					g.setStroke(new BasicStroke(3f));
					g.draw(p);
				}
				break;
			}
			default: { // Verlauf
				g.setPaint(new GradientPaint(0, 0, r.farbe1, w * 0.4f, h, r.farbe2));
				g.fillRect(0, 0, w, h);
				g.setPaint(new RadialGradientPaint(w / 2f, h * 0.35f, w * 0.8f, new float[]{0, 1},
						new Color[]{new Color(255, 255, 255, 70), new Color(255, 255, 255, 0)}));
				g.fillRect(0, 0, w, h);
				break;
			}
		}
	}

	private static void monogramm(Graphics2D g, String text, int w, int h, Color farbe) {
		if (text.length() > 3) {
			text = text.substring(0, 3);
		}
		text = text.toUpperCase();
		int groesse = text.length() == 1 ? (int) (w * 0.75f) : text.length() == 2 ? (int) (w * 0.5f) : (int) (w * 0.36f);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, groesse));
		FontMetrics fm = g.getFontMetrics();
		int x = (w - fm.stringWidth(text)) / 2;
		int y = (int) (h * 0.42f) + fm.getAscent() / 2 - fm.getDescent() / 2;
		for (int i = 6; i >= 1; i--) {
			g.setColor(alpha(farbe, 22));
			for (int dx = -i; dx <= i; dx += i) {
				for (int dy = -i; dy <= i; dy += i) {
					g.drawString(text, x + dx * 2, y + dy * 2);
				}
			}
		}
		g.setColor(new Color(0, 0, 0, 120));
		g.drawString(text, x + 4, y + 5);
		g.setColor(Color.WHITE);
		g.drawString(text, x, y);
	}

	private static void blatt(Graphics2D g, float x, float y, float laenge, double winkel, Color farbe) {
		AffineTransform alt = g.getTransform();
		g.translate(x, y);
		g.rotate(winkel);
		GeneralPath p = new GeneralPath();
		p.moveTo(0, 0);
		p.quadTo(laenge * 0.5f, -laenge * 0.22f, laenge, 0);
		p.quadTo(laenge * 0.5f, laenge * 0.22f, 0, 0);
		g.setColor(farbe);
		g.fill(p);
		g.setColor(alpha(Color.WHITE, 120));
		g.setStroke(new BasicStroke(1.5f));
		g.drawLine(0, 0, (int) (laenge * 0.9f), 0);
		g.setTransform(alt);
	}

	private static void stern(Graphics2D g, float x, float y, float s, Color farbe) {
		g.setColor(alpha(farbe, 200));
		g.setStroke(new BasicStroke(1.6f));
		g.drawLine((int) (x - s), (int) y, (int) (x + s), (int) y);
		g.drawLine((int) x, (int) (y - s), (int) x, (int) (y + s));
	}

	// ================================================================= Flügel

	private static final int FW = 256;
	private static final int GS = FW * 2;

	static BufferedImage fluegel(Rezept r) {
		if (r.eigenesBild != null) {
			BufferedImage out = new BufferedImage(FW, FW, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = glatt(out);
			BufferedImage b = r.eigenesBild;
			boolean pixel = b.getWidth() <= 64;
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, pixel
					? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			double s = Math.min(FW / (double) b.getWidth(), FW / (double) b.getHeight());
			int bw = (int) (b.getWidth() * s), bh = (int) (b.getHeight() * s);
			// links oben ausrichten: dort sitzt im Spiel das Gelenk am Rücken
			g.drawImage(b, 0, 0, bw, bh, null);
			g.dispose();
			return out;
		}
		BufferedImage img = new BufferedImage(GS, GS, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = glatt(img);
		switch (r.vorlage) {
			case "Drache": membran(g, r, false); break;
			case "Cyber": membran(g, r, true); break;
			case "Schmetterling": schmetterling(g, r); break;
			default: federn(g, r); break;
		}
		g.dispose();
		return skaliere(img, FW, FW, false);
	}

	private static Point2D.Double polar(double winkelGrad, double laenge) {
		double w = Math.toRadians(winkelGrad);
		return new Point2D.Double(GS * 0.035 + Math.cos(w) * laenge, GS * 0.27 + Math.sin(w) * laenge);
	}

	/** {Anzahl, Startwinkel, Schritt, Länge, Breite, Abnahme} – wie die eingebauten HD-Flügel. */
	private static final double[][] REIHEN = {
			{10, -14, 5.6, 0.97, 0.052, 0.34},
			{12, 30, 5.0, 0.64, 0.054, 0.12},
			{12, -8, 7.8, 0.42, 0.050, 0.18},
			{10, -10, 9.6, 0.26, 0.046, 0.15},
			{7, -12, 14.0, 0.14, 0.040, 0.10},
	};

	private static void federn(Graphics2D g, Rezept r) {
		Random z = new Random(r.zufall);
		for (int reihe = 0; reihe < REIHEN.length; reihe++) {
			double[] rr = REIHEN[reihe];
			int anzahl = (int) rr[0];
			for (int i = anzahl - 1; i >= 0; i--) {
				double frac = i / Math.max(1.0, anzahl - 1);
				double winkel = rr[1] + i * rr[2] + z.nextGaussian() * 0.6;
				double laenge = GS * rr[3] * (1 - frac * rr[5]);
				double breite = GS * rr[4] * 1.7;
				// Wurzel: Hauptfarbe, Spitze: zweite Farbe – Deckfedern heller
				float hell = reihe >= 2 ? 0.25f + reihe * 0.08f : 0f;
				Color wurzel = mische(r.farbe1, Color.WHITE, hell * 0.5f);
				Color spitze = mische(r.farbe2, Color.WHITE, hell * 0.3f);
				feder(g, winkel, laenge, breite, wurzel, spitze, r.leuchten);
			}
		}
	}

	private static void feder(Graphics2D g, double winkel, double laenge, double breite,
							  Color wurzel, Color spitze, Color leuchten) {
		Point2D.Double a = polar(winkel, laenge * 0.08);
		Point2D.Double e = polar(winkel, laenge);
		double w = Math.toRadians(winkel);
		double nx = -Math.sin(w), ny = Math.cos(w);
		double lang = laenge * 0.92;
		GeneralPath p = new GeneralPath();
		p.moveTo(a.x, a.y);
		// eine Seite breiter (Innenfahne), Spitze leicht rund
		p.curveTo(a.x + nx * breite * 1.1 + Math.cos(w) * lang * 0.3, a.y + ny * breite * 1.1 + Math.sin(w) * lang * 0.3,
				e.x + nx * breite * 0.9 - Math.cos(w) * lang * 0.15, e.y + ny * breite * 0.9 - Math.sin(w) * lang * 0.15,
				e.x, e.y);
		p.curveTo(e.x - nx * breite * 0.6 - Math.cos(w) * lang * 0.2, e.y - ny * breite * 0.6 - Math.sin(w) * lang * 0.2,
				a.x - nx * breite * 0.75 + Math.cos(w) * lang * 0.3, a.y - ny * breite * 0.75 + Math.sin(w) * lang * 0.3,
				a.x, a.y);
		p.closePath();

		// Schatten auf die Federn darunter
		AffineTransform alt = g.getTransform();
		g.translate(nx * 5, ny * 5);
		g.setColor(new Color(0, 0, 0, 70));
		g.fill(p);
		g.setTransform(alt);
		// Leuchten
		g.setColor(alpha(leuchten, 22));
		g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(p);
		// Fahne mit Verlauf Wurzel → Spitze
		g.setPaint(new GradientPaint((float) a.x, (float) a.y, wurzel, (float) e.x, (float) e.y, spitze));
		g.fill(p);
		// Glanz auf der Innenfahne
		Shape altClip = g.getClip();
		g.clip(p);
		g.setPaint(new GradientPaint((float) (a.x + nx * breite), (float) (a.y + ny * breite), new Color(255, 255, 255, 70),
				(float) a.x, (float) a.y, new Color(255, 255, 255, 0)));
		g.fill(p);
		// feine Federäste
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(new Color(0, 0, 0, 28));
		for (double t = 0.12; t < 0.95; t += 0.035) {
			double sx = a.x + Math.cos(w) * lang * t, sy = a.y + Math.sin(w) * lang * t;
			for (int s = -1; s <= 1; s += 2) {
				double ex = sx + nx * breite * 1.3 * s + Math.cos(w) * breite * 0.9;
				double ey = sy + ny * breite * 1.3 * s + Math.sin(w) * breite * 0.9;
				g.drawLine((int) sx, (int) sy, (int) ex, (int) ey);
			}
		}
		g.setClip(altClip);
		// Schaft
		g.setColor(new Color(255, 255, 255, 170));
		g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Point2D.Double s0 = polar(winkel, laenge * 0.12), s1 = polar(winkel, laenge * 0.9);
		g.drawLine((int) s0.x, (int) s0.y, (int) s1.x, (int) s1.y);
		// Kante
		g.setColor(alpha(leuchten, 210));
		g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(p);
		// leuchtende Spitze
		g.setPaint(new RadialGradientPaint((float) e.x, (float) e.y, (float) (breite * 1.4),
				new float[]{0, 1}, new Color[]{alpha(leuchten, 90), alpha(leuchten, 0)}));
		g.fill(new Ellipse2D.Double(e.x - breite * 1.4, e.y - breite * 1.4, breite * 2.8, breite * 2.8));
	}

	private static void membran(Graphics2D g, Rezept r, boolean cyber) {
		double[][] knochen = {{-14, 0.98}, {8, 0.92}, {28, 0.80}, {48, 0.66}, {68, 0.50}, {86, 0.36}};
		Point2D.Double piv = polar(0, 0);
		Point2D.Double[] spitzen = new Point2D.Double[knochen.length];
		for (int i = 0; i < knochen.length; i++) {
			spitzen[i] = polar(knochen[i][0], GS * knochen[i][1]);
		}
		GeneralPath haut = new GeneralPath();
		haut.moveTo(piv.x, piv.y);
		haut.lineTo(spitzen[0].x, spitzen[0].y);
		for (int i = 0; i + 1 < spitzen.length; i++) {
			Point2D.Double a = spitzen[i], b = spitzen[i + 1];
			double mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
			haut.quadTo(piv.x + (mx - piv.x) * 0.62, piv.y + (my - piv.y) * 0.62, b.x, b.y);
		}
		haut.closePath();

		g.setColor(alpha(r.leuchten, 60));
		g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(haut);
		g.setPaint(new GradientPaint(0, 0, alpha(r.farbe1, 235), GS * 0.6f, GS * 0.9f, alpha(dunkel(r.farbe2, 0.7f), 235)));
		g.fill(haut);
		Shape altClip = g.getClip();
		g.clip(haut);
		if (cyber) {
			g.setStroke(new BasicStroke(3f));
			g.setColor(alpha(r.farbe2, 160));
			double rr = 26;
			for (int row = 0; row < GS / (rr * 1.5) + 2; row++) {
				for (int col = 0; col < GS / (rr * 1.73) + 2; col++) {
					double cx = col * rr * 1.73 + (row % 2) * rr * 0.86, cy = row * rr * 1.5;
					Path2D.Double hex = new Path2D.Double();
					for (int k = 0; k < 6; k++) {
						double w = Math.PI / 3 * k + Math.PI / 6;
						if (k == 0) {
							hex.moveTo(cx + rr * Math.cos(w), cy + rr * Math.sin(w));
						} else {
							hex.lineTo(cx + rr * Math.cos(w), cy + rr * Math.sin(w));
						}
					}
					hex.closePath();
					g.draw(hex);
				}
			}
		} else {
			// Adern in der Haut
			g.setStroke(new BasicStroke(3f));
			g.setColor(alpha(dunkel(r.farbe1, 0.5f), 150));
			for (double[] k : knochen) {
				for (int s = -1; s <= 1; s += 2) {
					Point2D.Double a = polar(k[0], GS * k[1] * 0.35), b = polar(k[0] + s * 7, GS * k[1] * 0.75);
					g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
				}
			}
		}
		g.setClip(altClip);
		// Knochen
		for (int i = 0; i < knochen.length; i++) {
			Point2D.Double s = spitzen[i];
			g.setColor(alpha(r.leuchten, 90));
			g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine((int) piv.x, (int) piv.y, (int) s.x, (int) s.y);
			g.setColor(r.leuchten);
			g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine((int) piv.x, (int) piv.y, (int) s.x, (int) s.y);
			g.setColor(Color.WHITE);
			g.fill(new Ellipse2D.Double(s.x - 8, s.y - 8, 16, 16));
		}
		g.setColor(r.leuchten);
		g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(haut);
	}

	private static void schmetterling(Graphics2D g, Rezept r) {
		Shape vorder = ellipse(GS * 0.48, GS * 0.24, GS * 0.48, GS * 0.22, -12);
		Shape hinter = ellipse(GS * 0.30, GS * 0.58, GS * 0.30, GS * 0.25, 35);
		for (Shape s : new Shape[]{hinter, vorder}) {
			g.setColor(alpha(r.leuchten, 70));
			g.setStroke(new BasicStroke(16f));
			g.draw(s);
			g.setPaint(new RadialGradientPaint(GS * 0.05f, GS * 0.3f, GS * 0.95f, new float[]{0f, 0.55f, 1f},
					new Color[]{r.farbe1, mische(r.farbe1, r.farbe2, 0.6f), r.farbe2}, MultipleGradientPaint.CycleMethod.NO_CYCLE));
			g.fill(s);
		}
		Shape altClip = g.getClip();
		g.clip(vorder);
		adern(g);
		g.setClip(altClip);
		g.clip(hinter);
		adern(g);
		g.setClip(altClip);
		double[][] augen = {{GS * 0.66, GS * 0.20, 50}, {GS * 0.36, GS * 0.62, 42}, {GS * 0.86, GS * 0.30, 24}};
		for (double[] a : augen) {
			kreis(g, a[0], a[1], a[2], new Color(15, 8, 35));
			kreis(g, a[0], a[1], a[2] * 0.7, r.leuchten);
			kreis(g, a[0], a[1], a[2] * 0.35, new Color(15, 8, 35));
			kreis(g, a[0] - a[2] * 0.1, a[1] - a[2] * 0.1, a[2] * 0.12, Color.WHITE);
		}
		for (Shape s : new Shape[]{vorder, hinter}) {
			g.setColor(new Color(20, 10, 40));
			g.setStroke(new BasicStroke(9f));
			g.draw(s);
		}
	}

	private static void adern(Graphics2D g) {
		g.setColor(new Color(20, 10, 40, 200));
		g.setStroke(new BasicStroke(4f));
		Point2D.Double p = polar(0, 0);
		for (int w = -20; w < 80; w += 12) {
			Point2D.Double e = polar(w, GS * 0.9);
			g.drawLine((int) p.x, (int) p.y, (int) e.x, (int) e.y);
		}
	}

	private static Shape ellipse(double cx, double cy, double rx, double ry, double winkel) {
		Ellipse2D.Double e = new Ellipse2D.Double(-rx, -ry, rx * 2, ry * 2);
		AffineTransform t = new AffineTransform();
		t.translate(cx, cy);
		t.rotate(Math.toRadians(winkel));
		return t.createTransformedShape(e);
	}

	private static void kreis(Graphics2D g, double x, double y, double r, Color c) {
		g.setColor(c);
		g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
	}

	// ================================================================= Helfer

	static Graphics2D glatt(BufferedImage b) {
		Graphics2D g = b.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		return g;
	}

	/** Schrittweise verkleinern – deutlich schöner als ein einzelner Sprung. */
	static BufferedImage skaliere(BufferedImage quelle, int w, int h, boolean pixelig) {
		BufferedImage akt = Spielermodell.argb(quelle);
		if (pixelig) {
			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = out.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.drawImage(akt, 0, 0, w, h, null);
			g.dispose();
			return out;
		}
		int cw = akt.getWidth(), ch = akt.getHeight();
		do {
			cw = Math.max(w, cw / 2 < w ? w : cw / 2);
			ch = Math.max(h, ch / 2 < h ? h : ch / 2);
			if (cw < w) cw = w;
			if (ch < h) ch = h;
			BufferedImage n = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = n.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(akt, 0, 0, cw, ch, null);
			g.dispose();
			akt = n;
		} while (cw != w || ch != h);
		return akt;
	}

	private static void zeichneFuellend(Graphics2D g, BufferedImage b, int x, int y, int w, int h) {
		double s = Math.max(w / (double) b.getWidth(), h / (double) b.getHeight());
		int bw = (int) Math.ceil(b.getWidth() * s), bh = (int) Math.ceil(b.getHeight() * s);
		Shape alt = g.getClip();
		g.clipRect(x, y, w, h);
		if (b.getWidth() <= 64) {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		}
		g.drawImage(b, x + (w - bw) / 2, y + (h - bh) / 2, bw, bh, null);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setClip(alt);
	}

	private static BufferedImage weich(BufferedImage b) {
		float[] k = new float[25];
		java.util.Arrays.fill(k, 1f / 25);
		BufferedImage rand = new BufferedImage(b.getWidth() + 4, b.getHeight() + 4, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = rand.createGraphics();
		g.drawImage(b, 2, 2, null);
		g.dispose();
		BufferedImage w = new ConvolveOp(new Kernel(5, 5, k), ConvolveOp.EDGE_NO_OP, null).filter(rand, null);
		return w.getSubimage(2, 2, b.getWidth(), b.getHeight());
	}

	static Color alpha(Color c, int a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
	}

	static Color dunkel(Color c, float f) {
		return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f));
	}

	static Color mische(Color a, Color b, float t) {
		t = Math.max(0, Math.min(1, t));
		return new Color((int) (a.getRed() * (1 - t) + b.getRed() * t),
				(int) (a.getGreen() * (1 - t) + b.getGreen() * t),
				(int) (a.getBlue() * (1 - t) + b.getBlue() * t));
	}

	static String hex(Color c) {
		return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}
}
