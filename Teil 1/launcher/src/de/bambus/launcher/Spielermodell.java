package de.bambus.launcher;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ein kleiner 3D-Zeichner für die Minecraft-Spielfigur (ohne Bibliotheken).
 * Jede Würfelseite wird mit einer affinen Abbildung aus dem Skin-Bild gezeichnet,
 * von hinten nach vorne sortiert (Maleralgorithmus).
 *
 * Modell-Koordinaten in Minecraft-Pixeln: y nach oben (Füße bei 0),
 * +z zeigt nach vorne (Gesicht), -x ist die rechte Seite der Figur.
 */
final class Spielermodell {

	private BufferedImage skin;
	private boolean schmal;
	private BufferedImage cape;
	private BufferedImage fluegel;
	private final Map<String, BufferedImage> schattiert = new HashMap<>();

	void setSkin(BufferedImage bild, boolean schmal) {
		this.skin = argb(bild);
		this.schmal = schmal;
		schattiert.clear();
	}

	void setCape(BufferedImage bild) {
		this.cape = bild == null ? null : argb(bild);
		schattiert.clear();
	}

	private String hut;

	void setHut(String id) {
		this.hut = id == null || "aus".equals(id) ? null : id;
	}

	private final java.util.Map<Integer, BufferedImage> farbBilder = new java.util.HashMap<>();

	/** Ein einfarbiger Quader in Modell-Koordinaten (y nach oben, vorne = +z), beidseitig gezeichnet. */
	private void farbQuader(List<Flaeche> f, double x0, double y0, double z0, double x1, double y1, double z1, int argb) {
		BufferedImage bild = farbBilder.computeIfAbsent(argb, c -> {
			BufferedImage b = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			b.setRGB(0, 0, c);
			return b;
		});
		String k = "h" + Integer.toHexString(argb);
		f.add(new Flaeche(new double[]{x0, y1, z1}, new double[]{x1, y1, z1}, new double[]{x0, y1, z0}, bild, k, 0, 0, 1, 1, 1.0f, true, 1));
		f.add(new Flaeche(new double[]{x0, y0, z0}, new double[]{x1, y0, z0}, new double[]{x0, y0, z1}, bild, k, 0, 0, 1, 1, 0.65f, true, 1));
		f.add(new Flaeche(new double[]{x0, y1, z1}, new double[]{x1, y1, z1}, new double[]{x0, y0, z1}, bild, k, 0, 0, 1, 1, 0.9f, true, 1));
		f.add(new Flaeche(new double[]{x1, y1, z0}, new double[]{x0, y1, z0}, new double[]{x1, y0, z0}, bild, k, 0, 0, 1, 1, 0.9f, true, 1));
		f.add(new Flaeche(new double[]{x0, y1, z0}, new double[]{x0, y1, z1}, new double[]{x0, y0, z0}, bild, k, 0, 0, 1, 1, 0.78f, true, 1));
		f.add(new Flaeche(new double[]{x1, y1, z1}, new double[]{x1, y1, z0}, new double[]{x1, y0, z1}, bild, k, 0, 0, 1, 1, 0.78f, true, 1));
	}

	void setFluegel(BufferedImage bild) {
		this.fluegel = bild == null ? null : argb(bild);
		schattiert.clear();
	}

	boolean hatSkin() {
		return skin != null;
	}

	// ================================================================ Geometrie

	private static final class Flaeche {
		final double[] tl, tr, bl;     // Ecken im Modell: oben links, oben rechts, unten links
		final BufferedImage bild;
		final String schluessel;
		final int u, v, b, h;
		final float helligkeit;
		final boolean beidseitig;
		final double einheit;   // Breite des UV-Rasters (64 bei Skin und Cape)
		double tiefe;
		double[] sTl, sTr, sBl;

		Flaeche(double[] tl, double[] tr, double[] bl, BufferedImage bild, String schluessel,
				int u, int v, int b, int h, float helligkeit, boolean beidseitig, double einheit) {
			this.tl = tl;
			this.tr = tr;
			this.bl = bl;
			this.bild = bild;
			this.schluessel = schluessel;
			this.u = u;
			this.v = v;
			this.b = b;
			this.h = h;
			this.helligkeit = helligkeit;
			this.beidseitig = beidseitig;
			this.einheit = einheit;
		}
	}

	/** Ein Quader mit Minecraft-Textur-Netz ab (u, v) und Größe W×H×D. */
	private void quader(List<Flaeche> liste, BufferedImage bild, String sk,
						double x0, double y0, double z0, double x1, double y1, double z1,
						int u, int v, int w, int h, int d, double aufblasen, Drehung drehung) {
		x0 -= aufblasen; y0 -= aufblasen; z0 -= aufblasen;
		x1 += aufblasen; y1 += aufblasen; z1 += aufblasen;
		double[][] p = {
				{x0, y1, z1}, {x1, y1, z1}, {x0, y0, z1},   // vorne
				{x1, y1, z0}, {x0, y1, z0}, {x1, y0, z0},   // hinten
				{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z0},   // rechte Seite (-x)
				{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z1},   // linke Seite (+x)
				{x0, y1, z0}, {x1, y1, z0}, {x0, y1, z1},   // oben
				{x0, y0, z1}, {x1, y0, z1}, {x0, y0, z0},   // unten
		};
		if (drehung != null) {
			for (double[] punkt : p) {
				drehung.anwenden(punkt);
			}
		}
		int[][] uv = {
				{u + d, v + d, w, h},
				{u + 2 * d + w, v + d, w, h},
				{u, v + d, d, h},
				{u + d + w, v + d, d, h},
				{u + d, v, w, d},
				{u + d + w, v, w, d},
		};
		float[] licht = {0.92f, 0.92f, 0.74f, 0.74f, 1.0f, 0.55f};
		for (int i = 0; i < 6; i++) {
			liste.add(new Flaeche(p[i * 3], p[i * 3 + 1], p[i * 3 + 2], bild, sk,
					uv[i][0], uv[i][1], uv[i][2], uv[i][3], licht[i], false, 64));
		}
	}

	/** Drehung um einen Punkt (für Cape-Schwung und um 180°). */
	private static final class Drehung {
		final double px, py, pz, xRot, yRot;

		Drehung(double px, double py, double pz, double xRot, double yRot) {
			this.px = px;
			this.py = py;
			this.pz = pz;
			this.xRot = xRot;
			this.yRot = yRot;
		}

		void anwenden(double[] p) {
			double x = p[0] - px, y = p[1] - py, z = p[2] - pz;
			double cy = Math.cos(yRot), sy = Math.sin(yRot);
			double x2 = x * cy + z * sy;
			double z2 = -x * sy + z * cy;
			double cx = Math.cos(xRot), sx = Math.sin(xRot);
			double y3 = y * cx - z2 * sx;
			double z3 = y * sx + z2 * cx;
			p[0] = x2 + px;
			p[1] = y3 + py;
			p[2] = z3 + pz;
		}
	}

	private List<Flaeche> baue(double zeit) {
		List<Flaeche> f = new ArrayList<>();
		if (skin == null) {
			return f;
		}
		boolean alt = skin.getHeight() == 32; // altes 64×32-Format ohne eigene linke Seite
		int arm = schmal ? 3 : 4;

		// Grundkörper
		quader(f, skin, "s", -4, 24, -4, 4, 32, 4, 0, 0, 8, 8, 8, 0, null);                 // Kopf
		quader(f, skin, "s", -4, 12, -2, 4, 24, 2, 16, 16, 8, 12, 4, 0, null);              // Körper
		quader(f, skin, "s", -4 - arm, 12, -2, -4, 24, 2, 40, 16, arm, 12, 4, 0, null);     // rechter Arm
		quader(f, skin, "s", 4, 12, -2, 4 + arm, 24, 2, alt ? 40 : 32, alt ? 16 : 48, arm, 12, 4, 0, null);
		quader(f, skin, "s", -4, 0, -2, 0, 12, 2, 0, 16, 4, 12, 4, 0, null);                // rechtes Bein
		quader(f, skin, "s", 0, 0, -2, 4, 12, 2, alt ? 0 : 16, alt ? 16 : 48, 4, 12, 4, 0, null);

		// Zweite Ebene (Hut, Jacke, Ärmel, Hose)
		quader(f, skin, "s", -4, 24, -4, 4, 32, 4, 32, 0, 8, 8, 8, 0.5, null);
		if (!alt) {
			quader(f, skin, "s", -4, 12, -2, 4, 24, 2, 16, 32, 8, 12, 4, 0.25, null);
			quader(f, skin, "s", -4 - arm, 12, -2, -4, 24, 2, 40, 32, arm, 12, 4, 0.25, null);
			quader(f, skin, "s", 4, 12, -2, 4 + arm, 24, 2, 48, 48, arm, 12, 4, 0.25, null);
			quader(f, skin, "s", -4, 0, -2, 0, 12, 2, 0, 32, 4, 12, 4, 0.25, null);
			quader(f, skin, "s", 0, 0, -2, 4, 12, 2, 0, 48, 4, 12, 4, 0.25, null);
		}

		// Cape: hängt hinter dem Rücken, schwingt leicht. Wie im Spiel um 180° gedreht,
		// damit die Außenseite (Pixel 1..10) nach hinten zeigt.
		if (cape != null) {
			double schwung = 0.16 + 0.05 * Math.sin(zeit * 2.2);
			Drehung d180 = new Drehung(0, 24, -2.5, 0, Math.PI);
			Drehung neigen = new Drehung(0, 24, -2.5, schwung, 0);
			List<Flaeche> capeFlaechen = new ArrayList<>();
			quader(capeFlaechen, cape, "c", -5, 8, -3, 5, 24, -2, 0, 0, 10, 16, 1, 0, d180);
			for (Flaeche fl : capeFlaechen) {
				neigen.anwenden(fl.tl);
				neigen.anwenden(fl.tr);
				neigen.anwenden(fl.bl);
			}
			f.addAll(capeFlaechen);
		}

		// Flügel: je zwei Flächen (Innen- und Außenteil) mit Gelenk, der Außenteil schwingt nach
		if (fluegel != null) {
			double tt = zeit * 2.4;
			double innen = 0.40 + 0.20 * Math.sin(tt);
			double aussen = 0.50 + 0.30 * Math.sin(tt - 0.7);
			double hub = 0.9 * Math.sin(tt - 0.4);
			int w = fluegel.getWidth(), h = fluegel.getHeight();
			boolean breitesBild = w >= h * 1.6;   // 3D-Flügel seit 3.3: Bild 2:1
			double breite = breitesBild ? 19 : 14;
			double gelenk = 0.45;
			int ug = (int) Math.round(w * gelenk);
			double oben0 = breitesBild ? 23.6 : 24.8, oben1 = breitesBild ? 27.0 : 30.4;
			double hoehe = breitesBild ? breite * h / w : 12;
			double obenG = oben0 + (oben1 - oben0) * gelenk + hub * 0.4;
			for (int seite = -1; seite <= 1; seite += 2) {
				double x0 = seite * 1.3;
				double z0 = cape != null ? -3.6 : -2.6;
				double xe = x0 + seite * breite * gelenk * Math.cos(innen);
				double ze = z0 - breite * gelenk * Math.sin(innen);
				double x1 = xe + seite * breite * (1 - gelenk) * Math.cos(aussen);
				double z1 = ze - breite * (1 - gelenk) * Math.sin(aussen);
				// Innenteil (Bild links bis zum Gelenk)
				f.add(new Flaeche(new double[]{x0, oben0, z0}, new double[]{xe, obenG, ze}, new double[]{x0, oben0 - hoehe, z0},
						fluegel, "f", 0, 0, ug, h, 0.95f, true, w));
				// Außenteil (ab dem Gelenk bis zur Spitze)
				f.add(new Flaeche(new double[]{xe, obenG, ze}, new double[]{x1, oben1 + hub, z1}, new double[]{xe, obenG - hoehe, ze},
						fluegel, "f", ug, 0, w - ug, h, 0.95f, true, w));
			}
		}
		// Hut: Maße aus HutModelle (Minecraft-Kopfkoordinaten) in Modellkoordinaten umrechnen
		if (hut != null) {
			HutModelle.baue(hut, (float) (zeit * 20), (x1, y1, z1, x2, y2, z2, farbe) ->
					farbQuader(f, Math.min(x1, x2), 24 - Math.max(y1, y2), -Math.max(z1, z2),
							Math.max(x1, x2), 24 - Math.min(y1, y2), -Math.min(z1, z2), farbe));
		}
		return f;
	}

	// ================================================================== Zeichnen

	/**
	 * Zeichnet die Figur.
	 * @param fx    Bildschirm-x der Fußmitte
	 * @param fy    Bildschirm-y der Füße
	 * @param massstab Bildschirmpixel pro Minecraft-Pixel
	 */
	void zeichne(Graphics2D g, double fx, double fy, double massstab, double yaw, double pitch, double zeit) {
		List<Flaeche> flaechen = baue(zeit);
		double cy = Math.cos(yaw), sy = Math.sin(yaw);
		double cp = Math.cos(pitch), sp = Math.sin(pitch);
		double mitteY = 16;

		List<Flaeche> sichtbar = new ArrayList<>();
		for (Flaeche fl : flaechen) {
			fl.sTl = projiziere(fl.tl, cy, sy, cp, sp, mitteY, fx, fy, massstab);
			fl.sTr = projiziere(fl.tr, cy, sy, cp, sp, mitteY, fx, fy, massstab);
			fl.sBl = projiziere(fl.bl, cy, sy, cp, sp, mitteY, fx, fy, massstab);
			// Rückseiten weglassen (Bildschirm-Kreuzprodukt)
			double ax = fl.sTr[0] - fl.sTl[0], ay = fl.sTr[1] - fl.sTl[1];
			double bx = fl.sBl[0] - fl.sTl[0], by = fl.sBl[1] - fl.sTl[1];
			double kreuz = ax * by - ay * bx;
			if (kreuz <= 0 && !fl.beidseitig) {
				continue;
			}
			// Seiten, die man genau von der Kante sieht, weglassen – manche Grafiktreiber
			// zeichnen bei solchen fast flachen Abbildungen sonst falsche Flächen.
			if (Math.abs(kreuz) < 2.0) {
				continue;
			}
			// Tiefe = Mittelpunkt der Fläche (größer = näher)
			fl.tiefe = (fl.sTr[2] + fl.sBl[2]) / 2.0;
			sichtbar.add(fl);
		}
		Collections.sort(sichtbar, (a, b) -> Double.compare(a.tiefe, b.tiefe));

		Object altInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		for (Flaeche fl : sichtbar) {
			// HD-Flügel weich skalieren, Skin-Pixel bleiben scharf
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, "f".equals(fl.schluessel)
					? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			zeichneFlaeche(g, fl);
		}
		if (altInterp != null) {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, altInterp);
		}
	}

	private static double[] projiziere(double[] p, double cy, double sy, double cp, double sp,
									   double mitteY, double fx, double fy, double s) {
		double x = p[0] * cy + p[2] * sy;
		double z = -p[0] * sy + p[2] * cy;
		double y = p[1] - mitteY;
		double y2 = y * cp - z * sp;
		double z2 = y * sp + z * cp;
		return new double[]{fx + x * s, fy - (y2 + mitteY) * s, z2};
	}

	private void zeichneFlaeche(Graphics2D g, Flaeche fl) {
		// HD-Texturen (z. B. Cape 256×128): UV-Werte auf die echte Bildgröße umrechnen
		double sc = fl.bild.getWidth() / fl.einheit;
		int su = (int) Math.round(fl.u * sc), sv = (int) Math.round(fl.v * sc);
		int sb = Math.max(1, (int) Math.round(fl.b * sc)), sh = Math.max(1, (int) Math.round(fl.h * sc));
		if (fl.b <= 0 || fl.h <= 0 || su + sb > fl.bild.getWidth() || sv + sh > fl.bild.getHeight()) {
			return;
		}
		BufferedImage quelle = schattiert(fl.bild, fl.schluessel, fl.helligkeit).getSubimage(su, sv, sb, sh);

		// Fläche minimal vergrößern, damit keine Ritzen zwischen den Seiten entstehen
		double ax = fl.sTr[0] - fl.sTl[0], ay = fl.sTr[1] - fl.sTl[1];
		double bx = fl.sBl[0] - fl.sTl[0], by = fl.sBl[1] - fl.sTl[1];
		double la = Math.max(1e-6, Math.hypot(ax, ay)), lb = Math.max(1e-6, Math.hypot(bx, by));
		double e = 0.45;
		double ox = fl.sTl[0] - ax / la * e - bx / lb * e;
		double oy = fl.sTl[1] - ay / la * e - by / lb * e;
		double fa = (la + 2 * e) / la, fb = (lb + 2 * e) / lb;

		AffineTransform t = new AffineTransform(
				ax * fa / sb, ay * fa / sb,
				bx * fb / sh, by * fb / sh,
				ox, oy);
		g.drawImage(quelle, t, null);
	}

	private BufferedImage schattiert(BufferedImage bild, String schluessel, float helligkeit) {
		String k = schluessel + helligkeit;
		BufferedImage b = schattiert.get(k);
		if (b == null) {
			RescaleOp op = new RescaleOp(new float[]{helligkeit, helligkeit, helligkeit, 1f}, new float[4], null);
			b = op.filter(bild, null);
			schattiert.put(k, b);
		}
		return b;
	}

	static BufferedImage argb(BufferedImage bild) {
		if (bild.getType() == BufferedImage.TYPE_INT_ARGB) {
			return bild;
		}
		BufferedImage b = new BufferedImage(bild.getWidth(), bild.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = b.createGraphics();
		g.drawImage(bild, 0, 0, null);
		g.dispose();
		return b;
	}
}
