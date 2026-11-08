package de.bambus.launcher;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** MODS: FPS-Boost, empfohlene Mods, Modrinth-Suche und eigene Mods. */
final class SeiteMods extends JPanel {

	private static final int X = 40, LB = 340;          // linke Spalte
	private static final int RX = 400, RB = 336;        // rechte Spalte
	private static final int ZEILE = 44;

	private final BambusLauncher l;
	private final ModVerwaltung mods;
	private final List<ModZeile> empfohlen = new ArrayList<>();
	private final BoostKarte boost;
	private final JTextField suchFeld = new JTextField();
	private final JPanel treffer = liste();
	private final JPanel deine = liste();
	private String suchStatus = "Tippe z. B. „minimap“, „zoom“ oder „shader“.";

	SeiteMods(BambusLauncher l) {
		this.l = l;
		this.mods = l.mods;
		setOpaque(false);
		setLayout(null);

		boost = new BoostKarte();
		boost.setBounds(X, 106, LB, 88);
		add(boost);

		for (int i = 0; i < ModVerwaltung.EMPFOHLEN.length; i++) {
			final String[] m = ModVerwaltung.EMPFOHLEN[i];
			ModZeile z = new ModZeile(m[1], m[2], "FPS".equals(m[3]) ? "FPS" : "MENÜ",
					"FPS".equals(m[3]) ? NeonUI.NEON : NeonUI.CYAN, mods.istAn(m[0]),
					an -> { mods.setzeAn(m[0], an); boost.repaint(); }, null);
			z.setBounds(X, 232 + i * ZEILE, LB, ZEILE - 4);
			empfohlen.add(z);
			add(z);
		}

		// Modrinth-Suche
		suchFeld.setBounds(RX, 116, RB - 96, 34);
		suchFeld.setBackground(NeonUI.KARTE);
		suchFeld.setForeground(NeonUI.TEXT);
		suchFeld.setCaretColor(NeonUI.NEON);
		suchFeld.setFont(NeonUI.normal(13));
		suchFeld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE, 1, true), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		suchFeld.addActionListener(e -> suche());
		add(suchFeld);
		NeonUI.UmrissKnopf suchen = new NeonUI.UmrissKnopf("SUCHEN", NeonUI.CYAN);
		suchen.setBounds(RX + RB - 88, 116, 88, 34);
		suchen.addActionListener(e -> suche());
		add(suchen);
		add(scroll(treffer, RX, 176, RB, 196));

		// Deine Mods
		add(scroll(deine, RX, 412, RB, 124));
		NeonUI.UmrissKnopf datei = new NeonUI.UmrissKnopf("+ MOD-DATEI", NeonUI.PINK);
		datei.setFont(NeonUI.mono(11));
		datei.setBounds(RX, 544, 164, 32);
		datei.addActionListener(e -> dateiHinzufuegen());
		add(datei);
		NeonUI.UmrissKnopf ordner = new NeonUI.UmrissKnopf("ORDNER ÖFFNEN", NeonUI.GRAU);
		ordner.setFont(NeonUI.mono(11));
		ordner.setBounds(RX + 172, 544, 164, 32);
		ordner.addActionListener(e -> {
			try {
				java.nio.file.Files.createDirectories(l.installer.modsOrdner);
				java.awt.Desktop.getDesktop().open(l.installer.modsOrdner.toFile());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(l, "Mods-Ordner: " + l.installer.modsOrdner);
			}
		});
		add(ordner);

		baueDeine();
		zeigeTreffer(new ArrayList<>());
	}

	// ============================================================ Aktionen

	private void suche() {
		String text = suchFeld.getText().trim();
		if (text.isEmpty()) {
			return;
		}
		suchStatus = "Suche auf Modrinth …";
		repaint();
		Thread t = new Thread(() -> {
			try {
				List<ModVerwaltung.Projekt> ergebnis = mods.suche(text);
				SwingUtilities.invokeLater(() -> {
					suchStatus = ergebnis.isEmpty() ? "Nichts gefunden für Minecraft " + Installer.MC_VERSION + "." : "";
					zeigeTreffer(ergebnis);
				});
			} catch (Exception ex) {
				SwingUtilities.invokeLater(() -> {
					suchStatus = "Keine Verbindung zu Modrinth. Internet prüfen.";
					zeigeTreffer(new ArrayList<>());
				});
			}
		}, "Modrinth-Suche");
		t.setDaemon(true);
		t.start();
	}

	private void zeigeTreffer(List<ModVerwaltung.Projekt> ergebnis) {
		treffer.removeAll();
		int y = 0;
		for (ModVerwaltung.Projekt p : ergebnis) {
			boolean schon = mods.istExtra(p.slug) || ModVerwaltung.istEmpfohlen(p.slug);
			TrefferZeile z = new TrefferZeile(p, schon);
			z.setBounds(0, y, RB - 10, ZEILE - 4);
			treffer.add(z);
			y += ZEILE;
		}
		treffer.setPreferredSize(new Dimension(RB - 10, Math.max(y, 10)));
		treffer.revalidate();
		treffer.repaint();
		repaint();
	}

	private void baueDeine() {
		deine.removeAll();
		int y = 0;
		for (ModVerwaltung.Projekt p : mods.extras()) {
			ModZeile z = new ModZeile(p.titel, p.beschreibung, "MODRINTH", NeonUI.CYAN, mods.istAn(p.slug),
					an -> mods.setzeAn(p.slug, an), () -> { mods.entferne(p.slug); baueDeine(); });
			z.setBounds(0, y, RB - 10, ZEILE - 4);
			deine.add(z);
			y += ZEILE;
		}
		for (ModVerwaltung.EigeneMod m : mods.eigeneMods()) {
			ModZeile z = new ModZeile(m.name(), m.datei.getFileName().toString(), "DATEI", NeonUI.PINK, m.an,
					an -> {
						try {
							mods.schalteEigene(m, an);
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(l, "Umschalten fehlgeschlagen: " + ex.getMessage());
						}
						SwingUtilities.invokeLater(this::baueDeine);
					},
					() -> {
						if (JOptionPane.showConfirmDialog(l, "„" + m.name() + "“ löschen?", "Mod entfernen",
								JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
							try {
								mods.loescheEigene(m);
							} catch (Exception ex) {
								JOptionPane.showMessageDialog(l, "Löschen fehlgeschlagen: " + ex.getMessage());
							}
							baueDeine();
						}
					});
			z.setBounds(0, y, RB - 10, ZEILE - 4);
			deine.add(z);
			y += ZEILE;
		}
		deine.setPreferredSize(new Dimension(RB - 10, Math.max(y, 10)));
		deine.revalidate();
		deine.repaint();
		repaint();
	}

	private void dateiHinzufuegen() {
		JFileChooser wahl = new JFileChooser();
		wahl.setDialogTitle("Fabric-Mod (.jar) für Minecraft " + Installer.MC_VERSION + " auswählen");
		wahl.setFileFilter(new FileNameExtensionFilter("Minecraft-Mod (*.jar)", "jar"));
		wahl.setMultiSelectionEnabled(true);
		if (wahl.showOpenDialog(l) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		for (File f : wahl.getSelectedFiles()) {
			try {
				mods.kopiereEigene(f.toPath());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(l, f.getName() + ": " + ex.getMessage());
			}
		}
		baueDeine();
	}

	void aktualisiere() {
		for (int i = 0; i < empfohlen.size(); i++) {
			empfohlen.get(i).setAn(mods.istAn(ModVerwaltung.EMPFOHLEN[i][0]));
		}
		baueDeine();
	}

	// ============================================================ Zeichnen

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "MODS", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("FPS-Boost, Mods-Menü und eigene Mods · wird beim nächsten SPIELEN eingerichtet.", X, 84);

		ueberschrift(g, "EMPFOHLEN", X, 220);
		ueberschrift(g, "MODRINTH DURCHSUCHEN", RX, 106);
		ueberschrift(g, "DEINE MODS", RX, 400);
		if (!suchStatus.isEmpty()) {
			g.setFont(NeonUI.normal(12));
			g.setColor(NeonUI.GRAU);
			g.drawString(suchStatus, RX + 4, 196);
		}
		if (deine.getComponentCount() == 0) {
			g.setFont(NeonUI.normal(12));
			g.setColor(NeonUI.GRAU);
			g.drawString("Noch keine. Suche oben oder füge eine .jar hinzu.", RX + 4, 432);
		}
		g.setFont(NeonUI.mono(10));
		g.setColor(new Color(0x5C6B80));
		g.drawString("Alle Downloads direkt von modrinth.com · nur Fabric-Mods für " + Installer.MC_VERSION, X, 584);
		g.dispose();
	}

	private static void ueberschrift(Graphics2D g, String text, int x, int y) {
		g.setFont(NeonUI.mono(12));
		g.setColor(NeonUI.NEON);
		g.drawString(text, x, y);
		int tb = g.getFontMetrics().stringWidth(text);
		g.setColor(NeonUI.LINIE);
		g.fillRect(x + tb + 12, y - 4, 120, 1);
	}

	private static JPanel liste() {
		JPanel p = new JPanel(null);
		p.setOpaque(false);
		return p;
	}

	private static JScrollPane scroll(JPanel inhalt, int x, int y, int b, int h) {
		JScrollPane sp = new JScrollPane(inhalt, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setBounds(x, y, b, h);
		sp.setOpaque(false);
		sp.getViewport().setOpaque(false);
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp.getVerticalScrollBar().setUnitIncrement(16);
		sp.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
		sp.getVerticalScrollBar().setOpaque(false);
		sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
			@Override protected void configureScrollBarColors() { thumbColor = NeonUI.alpha(NeonUI.NEON, 120); trackColor = new Color(0, 0, 0, 0); }
			@Override protected javax.swing.JButton createDecreaseButton(int o) { return leer(); }
			@Override protected javax.swing.JButton createIncreaseButton(int o) { return leer(); }
			private javax.swing.JButton leer() { javax.swing.JButton k = new javax.swing.JButton(); k.setPreferredSize(new Dimension(0, 0)); return k; }
		});
		SanftScroll.an(sp);
		return sp;
	}

	// ============================================================ Bauteile

	/** Große FPS-Boost-Karte: schaltet alle Leistungs-Mods gemeinsam. */
	private final class BoostKarte extends JComponent {
		private boolean ueber;

		BoostKarte() {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					mods.setzeFpsBoost(!mods.fpsBoost());
					aktualisiere();
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			boolean an = mods.fpsBoost();
			int b = getWidth(), h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(3, 3, b - 7, h - 7, 14, 14);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			if (an) {
				double p = NeonUI.puls();
				NeonUI.leuchten(g, form, NeonUI.alpha(NeonUI.NEON, (int) (150 + 100 * p)), 3);
			} else {
				g.setColor(NeonUI.LINIE);
				g.draw(form);
			}
			g.setFont(NeonUI.titel(24));
			NeonUI.leuchtText(g, "⚡ FPS-BOOST", 20, 40, an ? NeonUI.NEON : NeonUI.GRAU, an ? 1 : 0);
			g.setFont(NeonUI.normal(12));
			g.setColor(NeonUI.GRAU);
			g.drawString(an ? "An · bis zu doppelte FPS, weniger Ruckler" : "Aus · Klicken für mehr FPS", 20, 64);
			int sb = 54, sh = 26, sx = b - sb - 20, sy = (h - sh) / 2;
			RoundRectangle2D bahn = new RoundRectangle2D.Float(sx, sy, sb, sh, sh, sh);
			g.setColor(an ? NeonUI.alpha(NeonUI.NEON, 70) : new Color(0x1E2A3C));
			g.fill(bahn);
			g.setColor(an ? NeonUI.NEON : NeonUI.GRAU);
			g.fillOval(an ? sx + sb - sh + 4 : sx + 4, sy + 4, sh - 8, sh - 8);
			g.dispose();
		}
	}

	/** Eine Mod-Zeile mit Schalter (und optional ✕ zum Entfernen). */
	private static final class ModZeile extends JComponent {
		private final String titel, text, marke;
		private final Color farbe;
		private final NeonUI.SchalterKarte.Aenderung aenderung;
		private final Runnable entfernen;
		private boolean an, ueber;

		ModZeile(String titel, String text, String marke, Color farbe, boolean an,
				 NeonUI.SchalterKarte.Aenderung aenderung, Runnable entfernen) {
			this.titel = titel;
			this.text = text;
			this.marke = marke;
			this.farbe = farbe;
			this.an = an;
			this.aenderung = aenderung;
			this.entfernen = entfernen;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(text);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					if (ModZeile.this.entfernen != null && e.getX() > getWidth() - 96 && e.getX() < getWidth() - 66) {
						ModZeile.this.entfernen.run();
						return;
					}
					ModZeile.this.an = !ModZeile.this.an;
					ModZeile.this.aenderung.neu(ModZeile.this.an);
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
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 10, 10);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			g.setColor(an ? NeonUI.alpha(farbe, 140) : NeonUI.LINIE);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(form);
			if (an) {
				g.setColor(farbe);
				g.fillRoundRect(1, 8, 3, h - 18, 3, 3);
			}
			g.setFont(NeonUI.mono(9));
			int textB = b - 64 - (g.getFontMetrics().stringWidth(marke) + 10) - (entfernen != null ? 34 : 0) - 22;
			g.setFont(NeonUI.titel(13));
			g.setColor(an ? NeonUI.TEXT : new Color(0x9AA8BC));
			g.drawString(kuerze(g, titel, textB), 14, 17);
			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(kuerze(g, text, textB), 14, 32);
			// Marke
			g.setFont(NeonUI.mono(9));
			int mb = g.getFontMetrics().stringWidth(marke) + 10;
			int mx = b - 64 - mb - (entfernen != null ? 34 : 0);
			g.setColor(NeonUI.alpha(farbe, 40));
			g.fillRoundRect(mx, h / 2 - 8, mb, 15, 6, 6);
			g.setColor(farbe);
			g.drawString(marke, mx + 5, h / 2 + 3);
			if (entfernen != null) {
				g.setFont(NeonUI.mono(12));
				g.setColor(NeonUI.GRAU);
				g.drawString("✕", b - 86, h / 2 + 5);
			}
			// Schalter
			int sb = 40, sh = 20, sx = b - sb - 12, sy = (h - sh) / 2;
			RoundRectangle2D bahn = new RoundRectangle2D.Float(sx, sy, sb, sh, sh, sh);
			g.setColor(an ? NeonUI.alpha(farbe, 70) : new Color(0x1E2A3C));
			g.fill(bahn);
			g.setColor(an ? farbe : NeonUI.GRAU);
			g.fillOval(an ? sx + sb - sh + 3 : sx + 3, sy + 3, sh - 6, sh - 6);
			g.dispose();
		}
	}

	/** Suchtreffer mit Knopf zum Hinzufügen. */
	private final class TrefferZeile extends JComponent {
		private final ModVerwaltung.Projekt p;
		private boolean drin, ueber;

		TrefferZeile(ModVerwaltung.Projekt p, boolean drin) {
			this.p = p;
			this.drin = drin;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(p.beschreibung);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					if (!TrefferZeile.this.drin) {
						mods.fuegeHinzu(TrefferZeile.this.p);
						TrefferZeile.this.drin = true;
						baueDeine();
						repaint();
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 10, 10);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			g.setColor(ueber && !drin ? NeonUI.alpha(NeonUI.CYAN, 160) : NeonUI.LINIE);
			g.draw(form);
			int textB = b - 110;
			g.setFont(NeonUI.titel(13));
			g.setColor(NeonUI.TEXT);
			g.drawString(kuerze(g, p.titel, textB), 12, 17);
			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(kuerze(g, p.beschreibung, textB), 12, 32);
			g.setFont(NeonUI.mono(9));
			g.drawString(downloads(p.downloads), b - 96, h / 2 + 3);
			g.setFont(NeonUI.mono(12));
			g.setColor(drin ? NeonUI.NEON : NeonUI.CYAN);
			g.drawString(drin ? "✓" : "+", b - 24, h / 2 + 5);
			g.dispose();
		}
	}

	private static String downloads(long n) {
		if (n >= 1_000_000) return String.format("%.1f", n / 1e6).replace('.', ',') + " Mio.";
		if (n >= 1_000) return (n / 1000) + " Tsd.";
		return n > 0 ? String.valueOf(n) : "";
	}

	private static String kuerze(Graphics2D g, String s, int max) {
		if (s == null) return "";
		if (g.getFontMetrics().stringWidth(s) <= max) return s;
		while (s.length() > 1 && g.getFontMetrics().stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
		return s + "…";
	}
}
