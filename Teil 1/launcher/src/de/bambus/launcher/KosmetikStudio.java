package de.bambus.launcher;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Kosmetik-Studio: eigene Capes und Flügel erstellen.
 * Vorlage wählen, Farben anpassen (oder ein eigenes Bild laden), Namen geben, speichern –
 * die Kosmetik landet im Ordner, den der Client beim Start einliest, und wird direkt angezogen.
 */
final class KosmetikStudio extends JDialog {

	private static final int B = 900, H = 600;
	private static final int PX = 24, PY = 70, PB = 320, PH = 440;   // Vorschau
	private static final int RX = 372;                               // rechte Spalte

	/** Neon-Farbpalette für die schnelle Auswahl. */
	private static final int[] PALETTE = {
			0x3DFF8A, 0x00E5FF, 0xFF2BD6, 0xB06BFF, 0xFFD54A, 0xFF8A3D,
			0xFF4D6D, 0x39FF14, 0x2B6BFF, 0xFFFFFF, 0x8A9BB0, 0x0A0F17,
	};

	private final KosmetikGenerator.Rezept rezept = new KosmetikGenerator.Rezept();
	private final Spielermodell modell = new Spielermodell();
	private final EigeneKosmetik eigene;
	private final Consumer<String[]> fertig;     // {art, id}
	private BufferedImage ergebnis;
	private String bildName = null;

	private final JTextField nameFeld = feld("Mein Cape");
	private final JTextField initialenFeld = feld("");
	private final List<Chip> vorlagenChips = new ArrayList<>();
	private final Chip artCape, artFluegel;
	private final FarbFeld[] farbFelder = new FarbFeld[3];
	private final NeonUI.UmrissKnopf bildKnopf = new NeonUI.UmrissKnopf("EIGENES BILD …", NeonUI.CYAN);
	private final NeonUI.UmrissKnopf bildWegKnopf = new NeonUI.UmrissKnopf("✕", NeonUI.GRAU);
	private String meldung = "";
	private Point ziehStart;

	private double yaw = Math.PI * 0.85, pitch = 0.1;
	private long letzteDrehung = 0;

	KosmetikStudio(JFrame eltern, EigeneKosmetik eigene, BufferedImage skin, boolean schmal, boolean cape,
				   Consumer<String[]> fertig) {
		super(eltern, "Kosmetik-Studio", true);
		this.eigene = eigene;
		this.fertig = fertig;
		setUndecorated(true);
		setSize(B, H);
		setLocationRelativeTo(eltern);
		if (skin != null) {
			modell.setSkin(skin, schmal);
		}
		rezept.cape = cape;
		rezept.zufall = System.nanoTime();

		JPanel inhalt = new Inhalt();
		inhalt.setLayout(null);
		setContentPane(inhalt);

		// Fenster verschieben über die Titelzeile
		MouseAdapter zieh = new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) { ziehStart = e.getPoint(); }
			@Override public void mouseDragged(MouseEvent e) {
				if (ziehStart != null && ziehStart.y < 56) {
					Point p = getLocation();
					setLocation(p.x + e.getX() - ziehStart.x, p.y + e.getY() - ziehStart.y);
				}
			}
		};
		inhalt.addMouseListener(zieh);
		inhalt.addMouseMotionListener(zieh);

		Vorschau vorschau = new Vorschau();
		vorschau.setBounds(PX, PY, PB, PH);
		inhalt.add(vorschau);
		NeonUI.UmrissKnopf vorne = new NeonUI.UmrissKnopf("VORNE", NeonUI.CYAN);
		vorne.setBounds(PX, PY + PH + 10, 155, 34);
		vorne.addActionListener(e -> { yaw = 0; letzteDrehung = System.currentTimeMillis(); });
		inhalt.add(vorne);
		NeonUI.UmrissKnopf hinten = new NeonUI.UmrissKnopf("HINTEN", NeonUI.CYAN);
		hinten.setBounds(PX + 165, PY + PH + 10, 155, 34);
		hinten.addActionListener(e -> { yaw = Math.PI; letzteDrehung = System.currentTimeMillis(); });
		inhalt.add(hinten);

		// Art
		artCape = new Chip("CAPE", NeonUI.NEON, () -> setzeArt(true));
		artFluegel = new Chip("FLÜGEL", NeonUI.PINK, () -> setzeArt(false));
		artCape.setBounds(RX, 92, 120, 32);
		artFluegel.setBounds(RX + 128, 92, 120, 32);
		inhalt.add(artCape);
		inhalt.add(artFluegel);

		// Farben
		String[] farbNamen = {"HAUPTFARBE", "AKZENT", "LEUCHTEN"};
		for (int i = 0; i < 3; i++) {
			final int nr = i;
			farbFelder[i] = new FarbFeld(farbNamen[i], () -> nr == 0 ? rezept.farbe1 : nr == 1 ? rezept.farbe2 : rezept.leuchten,
					c -> {
						if (nr == 0) rezept.farbe1 = c;
						else if (nr == 1) rezept.farbe2 = c;
						else rezept.leuchten = c;
						neuZeichnen();
					});
			farbFelder[i].setBounds(RX + i * 168, 260, 160, 74);
			inhalt.add(farbFelder[i]);
		}

		// Initialen (nur Cape)
		initialenFeld.setBounds(RX + 120, 352, 90, 32);
		initialenFeld.setToolTipText("1–3 Buchstaben groß auf dem Cape, z. B. deine Initialen");
		initialenFeld.getDocument().addDocumentListener(beiAenderung(() -> {
			rezept.initialen = initialenFeld.getText();
			neuZeichnen();
		}));
		inhalt.add(initialenFeld);

		// Eigenes Bild
		NeonUI.UmrissKnopf bild = bildKnopf;
		bild.setBounds(RX + 228, 352, 170, 32);
		bild.addActionListener(e -> bildWaehlen());
		inhalt.add(bild);
		NeonUI.UmrissKnopf bildWeg = bildWegKnopf;
		bildWeg.setBounds(RX + 404, 352, 38, 32);
		bildWeg.setToolTipText("Eigenes Bild entfernen");
		bildWeg.addActionListener(e -> {
			rezept.eigenesBild = null;
			bildName = null;
			neuZeichnen();
		});
		inhalt.add(bildWeg);

		// Zufall
		NeonUI.UmrissKnopf zufall = new NeonUI.UmrissKnopf("🎲 ZUFALL", NeonUI.NEON);
		zufall.setBounds(RX + 380, 92, 118, 32);
		zufall.addActionListener(e -> zufall());
		inhalt.add(zufall);

		// Name
		nameFeld.setBounds(RX, 438, 300, 36);
		inhalt.add(nameFeld);

		// Unten
		NeonUI.UmrissKnopf abbrechen = new NeonUI.UmrissKnopf("ABBRECHEN", NeonUI.GRAU);
		abbrechen.setBounds(RX, 512, 150, 48);
		abbrechen.addActionListener(e -> dispose());
		inhalt.add(abbrechen);
		NeonUI.SpielKnopf speichern = new NeonUI.SpielKnopf("SPEICHERN & ANZIEHEN");
		speichern.setFont(NeonUI.titel(17));
		speichern.setBounds(RX + 160, 504, 340, 64);
		speichern.addActionListener(e -> speichern());
		inhalt.add(speichern);

		NeonUI.UmrissKnopf schliessen = new NeonUI.UmrissKnopf("✕", NeonUI.GRAU);
		schliessen.setBounds(B - 54, 16, 38, 32);
		schliessen.addActionListener(e -> dispose());
		inhalt.add(schliessen);

		setzeArt(cape);
		new Timer(33, e -> vorschau.repaint()).start();
	}

	// ============================================================== Logik

	private void setzeArt(boolean cape) {
		boolean alterName = nameFeld.getText().isEmpty() || nameFeld.getText().equals("Mein Cape") || nameFeld.getText().equals("Meine Flügel");
		rezept.cape = cape;
		String[] vorlagen = cape ? KosmetikGenerator.CAPE_VORLAGEN : KosmetikGenerator.FLUEGEL_VORLAGEN;
		rezept.vorlage = vorlagen[0];
		if (alterName) {
			nameFeld.setText(cape ? "Mein Cape" : "Meine Flügel");
		}
		for (Chip c : vorlagenChips) {
			getContentPane().remove(c);
		}
		vorlagenChips.clear();
		for (int i = 0; i < vorlagen.length; i++) {
			final String v = vorlagen[i];
			Chip c = new Chip(v.toUpperCase(), NeonUI.CYAN, () -> {
				rezept.vorlage = v;
				rezept.zufall = System.nanoTime();
				neuZeichnen();
			});
			c.setBounds(RX + (i % 4) * 126, 160 + (i / 4) * 40, 118, 32);
			vorlagenChips.add(c);
			getContentPane().add(c);
		}
		initialenFeld.setVisible(cape);
		// ohne Initialen-Feld rückt der Bild-Knopf nach links
		bildKnopf.setLocation(RX + (cape ? 228 : 60), 352);
		bildWegKnopf.setLocation(RX + (cape ? 404 : 236), 352);
		if (!cape) {
			yaw = Math.PI * 0.85;
		}
		neuZeichnen();
	}

	private void zufall() {
		Random z = new Random();
		String[] vorlagen = rezept.cape ? KosmetikGenerator.CAPE_VORLAGEN : KosmetikGenerator.FLUEGEL_VORLAGEN;
		rezept.vorlage = vorlagen[z.nextInt(vorlagen.length)];
		float h = z.nextFloat();
		rezept.farbe1 = Color.getHSBColor(h, 0.75f + z.nextFloat() * 0.25f, 1f);
		rezept.farbe2 = Color.getHSBColor((h + 0.15f + z.nextFloat() * 0.3f) % 1f, 0.8f, 1f);
		rezept.leuchten = new Color(PALETTE[z.nextInt(6)]);
		rezept.zufall = z.nextLong();
		rezept.eigenesBild = null;
		bildName = null;
		neuZeichnen();
	}

	private void neuZeichnen() {
		ergebnis = KosmetikGenerator.erzeuge(rezept);
		if (rezept.cape) {
			modell.setCape(ergebnis);
			modell.setFluegel(null);
		} else {
			modell.setCape(null);
			modell.setFluegel(ergebnis);
		}
		meldung = "";
		repaint();
	}

	private void bildWaehlen() {
		FileDialog fd = new FileDialog(this, rezept.cape ? "Bild für dein Cape wählen" : "Bild für deine Flügel wählen", FileDialog.LOAD);
		fd.setFilenameFilter((dir, name) -> {
			String n = name.toLowerCase();
			return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".bmp");
		});
		fd.setVisible(true);
		if (fd.getFile() == null) {
			return;
		}
		File f = new File(fd.getDirectory(), fd.getFile());
		try {
			BufferedImage b = ImageIO.read(f);
			if (b == null) {
				throw new IllegalArgumentException("Kein Bild");
			}
			if (b.getWidth() > 4096 || b.getHeight() > 4096) {
				throw new IllegalArgumentException("Das Bild ist zu groß (max. 4096 Pixel)");
			}
			rezept.eigenesBild = Spielermodell.argb(b);
			bildName = f.getName();
			if (nameFeld.getText().startsWith("Mein")) {
				String n = f.getName().replaceAll("\\.[^.]+$", "");
				nameFeld.setText(n.substring(0, Math.min(24, n.length())));
			}
			neuZeichnen();
		} catch (Exception ex) {
			meldung = "Bild nicht lesbar: " + (ex.getMessage() != null ? ex.getMessage() : f.getName());
			repaint();
		}
	}

	private void speichern() {
		String name = nameFeld.getText().trim();
		if (name.isEmpty()) {
			meldung = "Gib deiner Kosmetik einen Namen.";
			repaint();
			return;
		}
		try {
			String art = rezept.cape ? EigeneKosmetik.CAPES : EigeneKosmetik.FLUEGEL;
			String id = eigene.speichern(art, name, KosmetikGenerator.hex(rezept.cape ? rezept.leuchten : rezept.farbe1), ergebnis);
			dispose();
			fertig.accept(new String[]{art, id});
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Speichern fehlgeschlagen:\n" + ex.getMessage(),
					"Kosmetik-Studio", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ============================================================== Zeichnen

	private final class Inhalt extends JPanel {
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			g.setColor(NeonUI.HG);
			g.fillRect(0, 0, B, H);
			NeonUI.gitter(g, 0, 0, B, H, 24, NeonUI.alpha(NeonUI.NEON, 8));
			RoundRectangle2D rand = new RoundRectangle2D.Float(1, 1, B - 3, H - 3, 18, 18);
			NeonUI.leuchten(g, rand, NeonUI.PINK, 2);

			g.setFont(NeonUI.titel(26));
			NeonUI.leuchtText(g, "KOSMETIK-STUDIO", PX, 44, NeonUI.TEXT, 1);
			g.setFont(NeonUI.mono(11));
			g.setColor(NeonUI.PINK);
			g.drawString("ERSTELLE DEIN EIGENES " + (rezept.cape ? "CAPE" : "FLÜGELPAAR"), PX + 262, 42);

			g.setFont(NeonUI.mono(11));
			g.setColor(NeonUI.GRAU);
			g.drawString("ART", RX, 82);
			g.drawString("VORLAGE", RX, 150);
			g.drawString("FARBEN  ·  KLICKEN ZUM ÄNDERN", RX, 250);
			if (rezept.cape) {
				g.drawString("INITIALEN", RX, 373);
			} else {
				g.drawString("ODER", RX, 373);
			}
			g.drawString("NAME", RX, 430);
			g.setFont(NeonUI.normal(11));
			if (bildName != null) {
				g.setColor(NeonUI.CYAN);
				String info = rezept.cape && KosmetikGenerator.istCapeNetz(rezept.eigenesBild)
						? "Cape-Vorlage erkannt: " : "Bild: ";
				g.drawString(info + bildName + "  (ersetzt die Vorlage)", RX + 120, 400);
			} else {
				g.setColor(NeonUI.GRAU);
				g.drawString(rezept.cape ? "PNG/JPG: Bild wird aufs Cape gesetzt · 64×32-Cape-Dateien gehen direkt"
						: "PNG mit Transparenz: Gelenk links oben, Flügel nach rechts", RX + (rezept.cape ? 0 : 0), 400);
			}
			if (!meldung.isEmpty()) {
				g.setColor(new Color(0xFF4D6D));
				g.drawString(meldung, RX, 492);
			} else {
				g.setColor(NeonUI.GRAU);
				g.drawString("Wird in config/bambus-kosmetik gespeichert · nur für dich sichtbar", RX, 492);
			}
			// Textur-Ansicht klein neben dem Namen
			if (ergebnis != null) {
				int tx = B - 112, ty = 410;
				g.setColor(NeonUI.KARTE);
				g.fillRoundRect(tx - 6, ty - 6, 100, 92, 10, 10);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				if (rezept.cape) {
					// Außenseite des Capes zeigen (bei 64×32-Vorlagen 1..11, sonst skaliert)
					int f = Math.max(1, ergebnis.getWidth() / 64);
					g.drawImage(ergebnis, tx + 22, ty, tx + 22 + 40, ty + 64 + 0, f, f, 11 * f, 17 * f, null);
				} else {
					g.drawImage(ergebnis, tx, ty, 80, 80, null);
				}
				g.setFont(NeonUI.mono(9));
				g.setColor(NeonUI.GRAU);
				g.drawString(rezept.cape ? "AUSSENSEITE" : "TEXTUR", tx, ty - 10);
			}
			g.dispose();
		}
	}

	private final class Vorschau extends JComponent {
		private int mx, my;

		Vorschau() {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			MouseAdapter m = new MouseAdapter() {
				@Override public void mousePressed(MouseEvent e) { mx = e.getX(); my = e.getY(); letzteDrehung = System.currentTimeMillis(); }
				@Override public void mouseDragged(MouseEvent e) {
					yaw += (e.getX() - mx) * 0.012;
					pitch = Math.max(-0.6, Math.min(0.6, pitch + (e.getY() - my) * 0.008));
					mx = e.getX();
					my = e.getY();
					letzteDrehung = System.currentTimeMillis();
				}
			};
			addMouseListener(m);
			addMouseMotionListener(m);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			double t = System.currentTimeMillis() / 1000.0;
			if (System.currentTimeMillis() - letzteDrehung > 3000) {
				// sanft um die Rückansicht pendeln
				double ziel = Math.PI + Math.sin(t * 0.6) * 0.9;
				yaw += (ziel - yaw) * 0.04;
			}
			RoundRectangle2D rahmen = new RoundRectangle2D.Float(0, 0, b - 1, h - 1, 16, 16);
			g.setPaint(new GradientPaint(0, 0, new Color(0x0B1626), 0, h, new Color(0x04070C)));
			g.fill(rahmen);
			Shape alt = g.getClip();
			g.clip(rahmen);
			NeonUI.gitter(g, 0, 0, b, h, 20, NeonUI.alpha(NeonUI.PINK, 12));
			int fx = b / 2, fy = h - 60;
			for (int i = 4; i >= 0; i--) {
				g.setColor(NeonUI.alpha(NeonUI.PINK, 16 + (4 - i) * 9));
				g.fill(new Ellipse2D.Double(fx - 78 - i * 6, fy - 12 - i * 2, 156 + i * 12, 26 + i * 4));
			}
			g.setColor(NeonUI.PINK);
			g.setStroke(new BasicStroke(1.5f));
			g.draw(new Ellipse2D.Double(fx - 78, fy - 12, 156, 26));
			modell.zeichne(g, fx, fy, 8.2, yaw, pitch, t);
			g.setClip(alt);
			NeonUI.ecken(g, 1, 1, b - 3, h - 3, 16, NeonUI.PINK);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.GRAU);
			g.drawString("LIVE-VORSCHAU", 16, 24);
			g.setColor(NeonUI.alpha(NeonUI.PINK, 200));
			g.drawString("ZIEHEN = DREHEN", b - 120, 24);
			g.dispose();
		}
	}

	// ============================================================== Bauteile

	private static JTextField feld(String text) {
		JTextField f = new JTextField(text);
		f.setBackground(NeonUI.KARTE);
		f.setForeground(NeonUI.TEXT);
		f.setCaretColor(NeonUI.NEON);
		f.setFont(NeonUI.normal(14));
		f.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE, 1, true),
				BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		return f;
	}

	private static DocumentListener beiAenderung(Runnable r) {
		return new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) { r.run(); }
			@Override public void removeUpdate(DocumentEvent e) { r.run(); }
			@Override public void changedUpdate(DocumentEvent e) { r.run(); }
		};
	}

	/** Auswahl-Chip (Art oder Vorlage). */
	private final class Chip extends JComponent {
		private final String text;
		private final Color farbe;
		private boolean ueber;

		Chip(String text, Color farbe, Runnable aktion) {
			this.text = text;
			this.farbe = farbe;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					aktion.run();
					KosmetikStudio.this.repaint();
				}
			});
		}

		private boolean aktiv() {
			if (this == artCape) return rezept.cape;
			if (this == artFluegel) return !rezept.cape;
			return rezept.eigenesBild == null && text.equalsIgnoreCase(rezept.vorlage);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			boolean aktiv = aktiv();
			RoundRectangle2D form = new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
			if (aktiv) {
				g.setColor(NeonUI.alpha(farbe, 45));
				g.fill(form);
				NeonUI.leuchten(g, form, farbe, 2);
			} else {
				g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
				g.fill(form);
				g.setColor(ueber ? NeonUI.alpha(farbe, 150) : NeonUI.LINIE);
				g.draw(form);
			}
			g.setFont(NeonUI.mono(11));
			g.setColor(aktiv ? NeonUI.TEXT : NeonUI.GRAU);
			NeonUI.zentriert(g, text, 0, 0, getWidth(), getHeight());
			g.dispose();
		}
	}

	interface FarbQuelle { Color farbe(); }

	/** Farbfeld mit Palette zum Aufklappen. */
	private final class FarbFeld extends JComponent {
		private final String titel;
		private final FarbQuelle quelle;
		private final Consumer<Color> setzen;
		private boolean ueber;

		FarbFeld(String titel, FarbQuelle quelle, Consumer<Color> setzen) {
			this.titel = titel;
			this.quelle = quelle;
			this.setzen = setzen;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) { palette(); }
			});
		}

		private void palette() {
			JDialog p = new JDialog(KosmetikStudio.this, false);
			p.setUndecorated(true);
			int sp = 6, gr = 30;
			JPanel feld = new JPanel(null) {
				@Override protected void paintComponent(Graphics gr2) {
					Graphics2D g = NeonUI.glatt(gr2);
					g.setColor(NeonUI.KARTE);
					g.fillRect(0, 0, getWidth(), getHeight());
					g.setColor(NeonUI.PINK);
					g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
					for (int i = 0; i < PALETTE.length; i++) {
						int x = sp + (i % 6) * (gr + sp), y = sp + (i / 6) * (gr + sp);
						g.setColor(new Color(PALETTE[i]));
						g.fillRoundRect(x, y, gr, gr, 8, 8);
						g.setColor(NeonUI.LINIE);
						g.drawRoundRect(x, y, gr, gr, 8, 8);
					}
					g.setFont(NeonUI.mono(11));
					g.setColor(NeonUI.TEXT);
					NeonUI.zentriert(g, "EIGENE FARBE …", 0, sp + 2 * (gr + sp), getWidth(), 26);
					g.dispose();
				}
			};
			int breite = sp + 6 * (gr + sp), hoehe = sp + 2 * (gr + sp) + 32;
			feld.addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					int col = (e.getX() - sp) / (gr + sp), row = (e.getY() - sp) / (gr + sp);
					p.dispose();
					if (row < 2 && col >= 0 && col < 6) {
						setzen.accept(new Color(PALETTE[row * 6 + col]));
					} else {
						Color c = JColorChooser.showDialog(KosmetikStudio.this, titel, quelle.farbe());
						if (c != null) {
							setzen.accept(c);
						}
					}
					FarbFeld.this.repaint();
				}
			});
			p.addWindowFocusListener(new java.awt.event.WindowAdapter() {
				@Override public void windowLostFocus(java.awt.event.WindowEvent e) { p.dispose(); }
			});
			p.setContentPane(feld);
			p.setSize(breite, hoehe);
			Point ort = getLocationOnScreen();
			p.setLocation(ort.x, ort.y + getHeight() + 4);
			p.setVisible(true);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			Color c = quelle.farbe();
			RoundRectangle2D form = new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			if (ueber) {
				NeonUI.leuchten(g, form, c, 2);
			} else {
				g.setColor(NeonUI.LINIE);
				g.draw(form);
			}
			RoundRectangle2D probe = new RoundRectangle2D.Float(12, 14, 44, 44, 10, 10);
			NeonUI.leuchten(g, probe, c, 2);
			g.setColor(c);
			g.fill(probe);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.GRAU);
			g.drawString(titel, 66, 30);
			g.setFont(NeonUI.mono(13));
			g.setColor(NeonUI.TEXT);
			g.drawString("#" + KosmetikGenerator.hex(c), 66, 50);
			g.dispose();
		}
	}
}
