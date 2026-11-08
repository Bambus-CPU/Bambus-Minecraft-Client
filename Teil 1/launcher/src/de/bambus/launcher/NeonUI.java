package de.bambus.launcher;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** Farben, Schriften und Bauteile im Neon-Look. */
final class NeonUI {

	private NeonUI() {}

	static final Color HG = new Color(0x05070D);
	static final Color SEITENLEISTE = new Color(0x080C14);
	static final Color KARTE = new Color(0x0D1420);
	static final Color KARTE_HOVER = new Color(0x132036);
	static final Color LINIE = new Color(0x1A2638);
	static final Color NEON = new Color(0x3DFF8A);
	static final Color CYAN = new Color(0x00E5FF);
	static final Color PINK = new Color(0xFF2BD6);
	static final Color TEXT = new Color(0xE6F1FF);
	static final Color GRAU = new Color(0x7D8DA6);
	static final Color DUNKEL = new Color(0x04110A);

	static Font titel(int groesse) {
		return new Font(Font.SANS_SERIF, Font.BOLD, groesse);
	}

	static Font normal(int groesse) {
		return new Font(Font.SANS_SERIF, Font.PLAIN, groesse);
	}

	static Font mono(int groesse) {
		return new Font(Font.MONOSPACED, Font.BOLD, groesse);
	}

	static Color alpha(Color c, int a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
	}

	static Color farbe(String hex) {
		return new Color(Integer.parseInt(hex, 16));
	}

	/** Langsames Pulsieren zwischen 0 und 1. */
	static float puls() {
		return (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 500.0));
	}

	static Graphics2D glatt(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		return g;
	}

	/** Neon-Leuchten um eine Form. */
	static void leuchten(Graphics2D g, Shape form, Color farbe, int staerke) {
		for (int i = staerke; i >= 1; i--) {
			g.setColor(alpha(farbe, (int) (60.0 / (i + 1))));
			g.setStroke(new BasicStroke(i * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(form);
		}
		g.setStroke(new BasicStroke(1.4f));
		g.setColor(farbe);
		g.draw(form);
	}

	/** Leuchtender Text. */
	static void leuchtText(Graphics2D g, String text, int x, int y, Color farbe, int staerke) {
		for (int i = staerke; i >= 1; i--) {
			g.setColor(alpha(farbe, 22));
			for (int dx = -i; dx <= i; dx += i) {
				for (int dy = -i; dy <= i; dy += i) {
					g.drawString(text, x + dx, y + dy);
				}
			}
		}
		g.setColor(farbe);
		g.drawString(text, x, y);
	}

	static void gitter(Graphics2D g, int x, int y, int b, int h, int abstand, Color farbe) {
		g.setColor(farbe);
		g.setStroke(new BasicStroke(1f));
		for (int gx = x; gx <= x + b; gx += abstand) {
			g.drawLine(gx, y, gx, y + h);
		}
		for (int gy = y; gy <= y + h; gy += abstand) {
			g.drawLine(x, gy, x + b, gy);
		}
	}

	/** Futuristische Eckwinkel. */
	static void ecken(Graphics2D g, int x, int y, int b, int h, int l, Color farbe) {
		g.setColor(farbe);
		g.setStroke(new BasicStroke(2f));
		g.drawLine(x, y, x + l, y);
		g.drawLine(x, y, x, y + l);
		g.drawLine(x + b, y, x + b - l, y);
		g.drawLine(x + b, y, x + b, y + l);
		g.drawLine(x, y + h, x + l, y + h);
		g.drawLine(x, y + h, x, y + h - l);
		g.drawLine(x + b, y + h, x + b - l, y + h);
		g.drawLine(x + b, y + h, x + b, y + h - l);
	}

	static void zentriert(Graphics2D g, String text, int x, int y, int b, int h) {
		FontMetrics fm = g.getFontMetrics();
		g.drawString(text, x + (b - fm.stringWidth(text)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
	}

	/** Das Bambus-Zeichen. */
	static void logo(Graphics2D g, int x, int y, int groesse) {
		int seg = groesse / 3;
		int b = Math.max(6, groesse / 4);
		for (int i = 0; i < 3; i++) {
			RoundRectangle2D r = new RoundRectangle2D.Float(x, y + i * seg, b, seg - 2, 4, 4);
			leuchten(g, r, NEON, 3);
			g.setColor(alpha(NEON, 200));
			g.fill(r);
			g.setColor(new Color(0x1C7A45));
			g.fillRect(x + b * 2 / 3, y + i * seg + 2, Math.max(1, b / 3), seg - 6);
		}
		g.setColor(CYAN);
		g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(x + b + 1, y + seg / 2 + 2, x + b + groesse / 3, y + seg / 2 - groesse / 10);
	}

	// =================================================================== Bauteile

	/** Basis: Knopf mit Hover-Erkennung und Handcursor. */
	abstract static class Knopf extends JButton {
		boolean ueber;

		Knopf(String text) {
			super(text);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
			});
		}
	}

	/** Großer, leuchtender Spielen-Knopf. */
	static final class SpielKnopf extends Knopf {
		String wartenText = "BITTE WARTEN";

		SpielKnopf(String text) {
			super(text);
			setFont(titel(24));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			int b = getWidth();
			int h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(8, 8, b - 17, h - 17, 14, 14);
			if (isEnabled()) {
				float p = puls();
				leuchten(g, form, ueber ? CYAN : NEON, ueber ? 6 : 3 + (int) (2 * p));
				g.setPaint(new GradientPaint(0, 0, ueber ? CYAN : NEON, b, h, ueber ? NEON : alpha(CYAN, 255)));
				g.fill(form);
				g.setColor(DUNKEL);
				g.setFont(getFont());
				zentriert(g, "▶  " + getText(), 0, 0, b, h);
			} else {
				g.setColor(KARTE);
				g.fill(form);
				leuchten(g, form, alpha(NEON, 120), 1);
				g.setColor(GRAU);
				g.setFont(mono(15));
				zentriert(g, wartenText, 0, 0, b, h);
			}
			g.dispose();
		}
	}

	/** Umrandeter Knopf. */
	static final class UmrissKnopf extends Knopf {
		private final Color farbe;

		UmrissKnopf(String text, Color farbe) {
			super(text);
			this.farbe = farbe;
			setFont(mono(12));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			RoundRectangle2D form = new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
			g.setColor(ueber ? alpha(farbe, 40) : KARTE);
			g.fill(form);
			if (ueber) {
				leuchten(g, form, farbe, 2);
			} else {
				g.setColor(alpha(farbe, 150));
				g.setStroke(new BasicStroke(1.2f));
				g.draw(form);
			}
			g.setColor(ueber ? TEXT : farbe);
			g.setFont(getFont());
			zentriert(g, getText(), 0, 0, getWidth(), getHeight());
			g.dispose();
		}
	}

	/** Karte mit Titel, Beschreibung und Ein-/Aus-Schalter. */
	static final class SchalterKarte extends JComponent {
		interface Aenderung {
			void neu(boolean an);
		}

		private final String titel;
		private final String text;
		private final String warnung;
		private final Aenderung aenderung;
		private boolean an;
		private boolean ueber;
		private float animation;
		private String symbol;

		/** Symbol aus dem gemeinsamen Satz (wie im Spiel). */
		SchalterKarte mitSymbol(String name) {
			this.symbol = name;
			return this;
		}

		SchalterKarte(String titel, String text, String warnung, boolean an, Aenderung aenderung) {
			this.titel = titel;
			this.text = text;
			this.warnung = warnung;
			this.an = an;
			this.animation = an ? 1f : 0f;
			this.aenderung = aenderung;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(warnung != null && !warnung.isEmpty() ? "⚠ " + warnung : null);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					SchalterKarte.this.an = !SchalterKarte.this.an;
					aenderung.neu(SchalterKarte.this.an);
					repaint();
				}
			});
		}

		void setAn(boolean an) {
			this.an = an;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			int b = getWidth();
			int h = getHeight();
			animation += ((an ? 1f : 0f) - animation) * 0.35f;
			if (Math.abs((an ? 1f : 0f) - animation) > 0.01f) {
				repaint(16);
			}

			boolean warn = warnung != null && !warnung.isEmpty();
			Color akzent = warn ? PINK : NEON;
			RoundRectangle2D form = new RoundRectangle2D.Float(3, 3, b - 7, h - 7, 12, 12);
			g.setColor(ueber ? KARTE_HOVER : KARTE);
			g.fill(form);
			if (an) {
				leuchten(g, form, alpha(akzent, ueber ? 255 : 170), ueber ? 3 : 2);
				g.setColor(akzent);
				g.fillRoundRect(3, 14, 3, h - 30, 3, 3);
			} else {
				g.setColor(ueber ? new Color(0x2B3D57) : LINIE);
				g.setStroke(new BasicStroke(1.2f));
				g.draw(form);
			}

			int tx = 18;
			if (symbol != null) {
				Symbole.zeichne(g, symbol, 18, 17, 2, an ? akzent : new Color(0x4A5A6E));
				tx = 42;
			}
			g.setFont(titel(15));
			if (g.getFontMetrics().stringWidth(titel) > b - 62 - 8 - tx) g.setFont(titel(13));   // Platz für den Schalter lassen
			g.setColor(an ? TEXT : new Color(0xB5C2D4));
			g.drawString(titel, tx, 30);
			g.setFont(normal(12));
			g.setColor(GRAU);
			g.drawString(text, 18, 50);
			if (warn) {
				g.setFont(mono(10));
				g.setColor(alpha(PINK, 220));
				g.drawString("⚠ " + warnung.toUpperCase(), 18, 68);
			}

			// Schalter
			int sb = 44;
			int sh = 22;
			int sx = b - sb - 18;
			int sy = 16;
			RoundRectangle2D bahn = new RoundRectangle2D.Float(sx, sy, sb, sh, sh, sh);
			g.setColor(an ? alpha(akzent, 70) : new Color(0x1E2A3C));
			g.fill(bahn);
			if (an) {
				leuchten(g, bahn, alpha(akzent, 200), 2);
			}
			int kx = (int) (sx + 3 + animation * (sb - sh));
			g.setColor(an ? akzent : GRAU);
			g.fillOval(kx, sy + 3, sh - 6, sh - 6);
			g.dispose();
		}
	}

	/** Kleiner Schalter ohne Karte (z. B. neben einer Überschrift). */
	static final class MiniSchalter extends JComponent {
		private boolean an;

		MiniSchalter(boolean an, SchalterKarte.Aenderung aenderung) {
			this.an = an;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					MiniSchalter.this.an = !MiniSchalter.this.an;
					aenderung.neu(MiniSchalter.this.an);
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			int h = 20;
			int b = 40;
			int x = getWidth() - b - 4;
			int y = (getHeight() - h) / 2;
			g.setFont(mono(10));
			g.setColor(an ? NEON : GRAU);
			String t = an ? "HUD AN" : "HUD AUS";
			g.drawString(t, x - g.getFontMetrics().stringWidth(t) - 10, y + 14);
			RoundRectangle2D bahn = new RoundRectangle2D.Float(x, y, b, h, h, h);
			g.setColor(an ? alpha(NEON, 70) : new Color(0x1E2A3C));
			g.fill(bahn);
			if (an) {
				leuchten(g, bahn, alpha(NEON, 200), 2);
			}
			g.setColor(an ? NEON : GRAU);
			g.fillOval(an ? x + b - h + 3 : x + 3, y + 3, h - 6, h - 6);
			g.dispose();
		}
	}

	/** Sanft einblendendes Panel (für Seitenwechsel). */
	static void blende(Graphics2D g, float deckkraft) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, deckkraft))));
	}
}
