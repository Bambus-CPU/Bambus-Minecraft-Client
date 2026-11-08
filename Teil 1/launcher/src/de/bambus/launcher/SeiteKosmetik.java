package de.bambus.launcher;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kosmetik-Menü: drehbare 3D-Figur mit deinem eigenen Skin,
 * dazu Capes, Flügel und Partikelspuren.
 */
final class SeiteKosmetik extends JPanel {

	private static final int X = 40;
	private static final int V_X = 40, V_Y = 104, V_B = 320, V_H = 380;
	private static final int R_X = 380, R_B = 356;
	private static final int K_B = 81, K_H = 104, ABSTAND = 10;

	private static final int CAPES = 0, FLUEGEL = 1, PARTIKEL = 2, HUETE = 3, KILL = 4, SPUR = 5;
	private static final String[] TAB_NAMEN = {"CAPES", "FLÜGEL", "PARTIKEL", "HÜTE", "KILL", "PFEIL"};
	private static final Color[] TAB_FARBEN = {NeonUI.NEON, NeonUI.PINK, NeonUI.CYAN, new Color(0xFFD54A), new Color(0xFF4D6D), new Color(0xB06BFF)};

	private final BambusLauncher l;
	private final Spielermodell modell = new Spielermodell();
	private final SkinLader skinLader;
	private final Map<String, BufferedImage> capeBilder = new HashMap<>();
	private final Map<String, BufferedImage> fluegelBilder = new HashMap<>();
	private final List<Kachel> kacheln = new ArrayList<>();
	private final JTextField nameFeld = new JTextField();

	private final EigeneKosmetik eigene;
	private BufferedImage skinBild;
	private boolean skinSchmal;
	private int seite = 0;
	private static final int PRO_SEITE = 12;
	private static final String NEU = "+neu";
	private final NeonUI.UmrissKnopf zurueck = new NeonUI.UmrissKnopf("‹", NeonUI.CYAN);
	private final NeonUI.UmrissKnopf weiter = new NeonUI.UmrissKnopf("›", NeonUI.CYAN);

	private int art = CAPES;
	private String seitenText = "";
	private NeonUI.UmrissKnopf ordner;
	private String skinName = "…";
	private String skinStatus = "Skin wird geladen …";
	private final Vorschau3D vorschau = new Vorschau3D();

	SeiteKosmetik(BambusLauncher l) {
		this.l = l;
		setOpaque(false);
		setLayout(null);
		skinLader = new SkinLader(l.installer.mcOrdner, l.installer.spielOrdner);
		eigene = new EigeneKosmetik(l.installer.spielOrdner);

		for (String[] c : ClientEinstellungen.CAPES) {
			Image b = BambusLauncher.bild("/capes/" + c[0] + ".png");
			if (b instanceof BufferedImage) capeBilder.put(c[0], (BufferedImage) b);
		}
		for (String[] f : ClientEinstellungen.FLUEGEL) {
			Image b = BambusLauncher.bild("/fluegel/" + f[0] + ".png");
			if (b instanceof BufferedImage) fluegelBilder.put(f[0], (BufferedImage) b);
		}

		// Knöpfe unter der Vorschau
		NeonUI.UmrissKnopf vorne = new NeonUI.UmrissKnopf("VORNE", NeonUI.CYAN);
		vorne.setBounds(V_X, V_Y + V_H + 8, 100, 34);
		vorne.addActionListener(e -> vorschau.drehZu(0));
		add(vorne);
		NeonUI.UmrissKnopf hinten = new NeonUI.UmrissKnopf("HINTEN", NeonUI.CYAN);
		hinten.setBounds(V_X + 110, V_Y + V_H + 8, 100, 34);
		hinten.addActionListener(e -> vorschau.drehZu(Math.PI));
		add(hinten);
		NeonUI.UmrissKnopf drehen = new NeonUI.UmrissKnopf("⟳ DREHEN", NeonUI.NEON);
		drehen.setBounds(V_X + 220, V_Y + V_H + 8, 100, 34);
		drehen.addActionListener(e -> vorschau.autoDrehen = !vorschau.autoDrehen);
		add(drehen);

		// Skin-Name eingeben
		nameFeld.setBounds(V_X + 52, V_Y + V_H + 52, 168, 32);
		nameFeld.setBackground(NeonUI.KARTE);
		nameFeld.setForeground(NeonUI.TEXT);
		nameFeld.setCaretColor(NeonUI.NEON);
		nameFeld.setFont(NeonUI.normal(13));
		nameFeld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE, 1, true),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		nameFeld.setToolTipText("Minecraft-Name eingeben und LADEN klicken");
		nameFeld.addActionListener(e -> ladeSkin(nameFeld.getText().trim()));
		add(nameFeld);
		NeonUI.UmrissKnopf laden = new NeonUI.UmrissKnopf("LADEN", NeonUI.NEON);
		laden.setBounds(V_X + 228, V_Y + V_H + 52, 92, 32);
		laden.addActionListener(e -> ladeSkin(nameFeld.getText().trim()));
		add(laden);

		// Unter-Tabs
		for (int i = 0; i < TAB_NAMEN.length; i++) {
			UnterTab t = new UnterTab(i);
			t.setBounds(R_X + i * 61, V_Y, 58, 32);
			add(t);
		}

		// Seiten blättern + Ordner für eigene Kosmetik
		zurueck.setBounds(R_X + 4 * (K_B + ABSTAND) - 96, 490, 40, 30);
		zurueck.addActionListener(e -> { seite--; baueKacheln(); });
		add(zurueck);
		weiter.setBounds(R_X + 4 * (K_B + ABSTAND) - 50, 490, 40, 30);
		weiter.addActionListener(e -> { seite++; baueKacheln(); });
		add(weiter);
		ordner = new NeonUI.UmrissKnopf("ORDNER ÖFFNEN", NeonUI.PINK);
		ordner.setFont(NeonUI.mono(10));
		ordner.setBounds(R_X, 490, 130, 30);
		ordner.setToolTipText("Hier liegen deine eigenen Capes und Flügel als PNG");
		ordner.addActionListener(e -> oeffneOrdner());
		add(ordner);

		vorschau.setBounds(V_X, V_Y, V_B, V_H);
		add(vorschau);

		ladeEigene();
		baueKacheln();
		aktualisiereModell();
		startSkin();
	}

	// ================================================================== Skin

	private void startSkin() {
		SkinLader.Skin cache = skinLader.ausCache();
		setzeSkin(cache != null ? cache : SkinLader.standard(), cache != null ? "Zuletzt geladen" : "Standard-Skin");

		Thread t = new Thread(() -> {
			String[] konto = skinLader.kontoAusLauncher();
			if (konto == null) {
				SwingUtilities.invokeLater(() -> skinStatus = skinStatus.startsWith("Zuletzt")
						? skinStatus : "Kein Konto gefunden – Namen eingeben");
				return;
			}
			try {
				SkinLader.Skin s = skinLader.lade(konto[0]);
				SwingUtilities.invokeLater(() -> setzeSkin(s, "Aus deinem Minecraft-Konto"));
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> skinStatus = "Offline – " + skinStatus);
			}
		}, "Skin laden");
		t.setDaemon(true);
		t.start();
	}

	private void ladeSkin(String name) {
		if (name.isEmpty()) {
			return;
		}
		skinStatus = "Lade " + name + " …";
		Thread t = new Thread(() -> {
			try {
				SkinLader.Skin s = skinLader.lade(name);
				SwingUtilities.invokeLater(() -> setzeSkin(s, "Geladen"));
			} catch (Exception e) {
				String meldung = e.getMessage() != null ? e.getMessage() : "Fehler beim Laden";
				SwingUtilities.invokeLater(() -> skinStatus = meldung);
			}
		}, "Skin laden");
		t.setDaemon(true);
		t.start();
	}

	private void setzeSkin(SkinLader.Skin s, String status) {
		modell.setSkin(s.bild, s.schmal);
		skinBild = s.bild;
		skinSchmal = s.schmal;
		skinName = s.name;
		skinStatus = status;
		if (!s.standard && nameFeld.getText().isEmpty()) {
			nameFeld.setText(s.name);
		}
		repaint();
	}

	private void aktualisiereModell() {
		String cape = l.client.cape();
		modell.setCape(ClientEinstellungen.AUS.equals(cape) ? null : capeBilder.get(cape));
		String fl = l.client.fluegel();
		modell.setFluegel(ClientEinstellungen.AUS.equals(fl) ? null : fluegelBilder.get(fl));
		modell.setHut(l.client.hut());
	}

	/** Eigene Capes/Flügel neu einlesen (Bilder für Kacheln und 3D-Vorschau). */
	private void ladeEigene() {
		capeBilder.keySet().removeIf(EigeneKosmetik::istEigen);
		fluegelBilder.keySet().removeIf(EigeneKosmetik::istEigen);
		for (EigeneKosmetik.Eintrag e : eigene.liste(EigeneKosmetik.CAPES)) {
			capeBilder.put(e.id, e.bild);
		}
		for (EigeneKosmetik.Eintrag e : eigene.liste(EigeneKosmetik.FLUEGEL)) {
			fluegelBilder.put(e.id, e.bild);
		}
	}

	private String[][] eigeneListe(String art) {
		List<EigeneKosmetik.Eintrag> l = eigene.liste(art);
		String[][] out = new String[l.size()][];
		for (int i = 0; i < l.size(); i++) {
			out[i] = new String[]{l.get(i).id, l.get(i).name, l.get(i).farbe};
		}
		return out;
	}

	private void oeffneStudio() {
		new KosmetikStudio(l, eigene, skinBild, skinSchmal, art == CAPES, ergebnis -> {
			ladeEigene();
			if (EigeneKosmetik.CAPES.equals(ergebnis[0])) {
				art = CAPES;
				l.client.setzeCape(ergebnis[1]);
			} else {
				art = FLUEGEL;
				l.client.setzeFluegel(ergebnis[1]);
			}
			seite = 99;   // zur letzten Seite, dort liegt die neue Kosmetik
			baueKacheln();
			aktualisiereModell();
			vorschau.drehZu(Math.PI * 0.85);
		}).setVisible(true);
	}

	private void loeschen(String id, String name) {
		int antwort = javax.swing.JOptionPane.showConfirmDialog(l, "„" + name + "“ wirklich löschen?",
				"Eigene Kosmetik löschen", javax.swing.JOptionPane.YES_NO_OPTION);
		if (antwort != javax.swing.JOptionPane.YES_OPTION) {
			return;
		}
		try {
			eigene.loeschen(art == CAPES ? EigeneKosmetik.CAPES : EigeneKosmetik.FLUEGEL, id);
			if (id.equals(gewaehlt())) {
				waehle(ClientEinstellungen.AUS);
			}
		} catch (Exception ex) {
			System.err.println("[Bambus] Löschen fehlgeschlagen: " + ex);
		}
		ladeEigene();
		baueKacheln();
	}

	private void oeffneOrdner() {
		try {
			java.nio.file.Files.createDirectories(eigene.ordner().resolve(EigeneKosmetik.CAPES));
			java.nio.file.Files.createDirectories(eigene.ordner().resolve(EigeneKosmetik.FLUEGEL));
			java.awt.Desktop.getDesktop().open(eigene.ordner().toFile());
		} catch (Exception ex) {
			javax.swing.JOptionPane.showMessageDialog(l, "Ordner: " + eigene.ordner());
		}
	}

	// =============================================================== Kacheln

	private String[][] liste() {
		if (art == PARTIKEL) {
			return ClientEinstellungen.PARTIKEL;
		}
		if (art == HUETE) return ClientEinstellungen.HUETE;
		if (art == KILL) return ClientEinstellungen.KILL_EFFEKTE;
		if (art == SPUR) return ClientEinstellungen.SPUREN;
		String[][] fest = art == CAPES ? ClientEinstellungen.CAPES : ClientEinstellungen.FLUEGEL;
		String[][] eig = eigeneListe(art == CAPES ? EigeneKosmetik.CAPES : EigeneKosmetik.FLUEGEL);
		String[][] alle = new String[fest.length + eig.length][];
		System.arraycopy(fest, 0, alle, 0, fest.length);
		System.arraycopy(eig, 0, alle, fest.length, eig.length);
		return alle;
	}

	private String gewaehlt() {
		switch (art) {
			case CAPES: return l.client.cape();
			case FLUEGEL: return l.client.fluegel();
			case HUETE: return l.client.hut();
			case KILL: return l.client.killEffekt();
			case SPUR: return l.client.spur();
			default: return l.client.partikel();
		}
	}

	private void waehle(String id) {
		if (art == CAPES) {
			l.client.setzeCape(id);
		} else if (art == FLUEGEL) {
			l.client.setzeFluegel(id);
		} else if (art == HUETE) {
			l.client.setzeHut(id);
		} else if (art == KILL) {
			l.client.setzeKillEffekt(id);
		} else if (art == SPUR) {
			l.client.setzeSpur(id);
		} else {
			l.client.setzePartikel(id);
		}
		aktualisiereModell();
		// Beim Wählen von Cape oder Flügeln die Figur von hinten zeigen
		if ((art == CAPES || art == FLUEGEL) && !ClientEinstellungen.AUS.equals(id)) {
			vorschau.drehZu(Math.PI * 0.85);
		} else if (art == HUETE && !ClientEinstellungen.AUS.equals(id)) {
			vorschau.drehZu(Math.PI * 0.15);
		}
		repaint();
	}

	private void baueKacheln() {
		for (Kachel k : kacheln) {
			remove(k);
		}
		kacheln.clear();
		List<String[]> eintraege = new ArrayList<>();
		eintraege.add(new String[]{ClientEinstellungen.AUS, art == CAPES ? "Kein Cape" : art == HUETE ? "Kein Hut" : "Keine", "7D8DA6"});
		if (art == CAPES || art == FLUEGEL) {
			eintraege.add(new String[]{NEU, "Erstellen", "FF2BD6"});
		}
		for (String[] e : liste()) {
			eintraege.add(e);
		}
		int seiten = Math.max(1, (eintraege.size() + PRO_SEITE - 1) / PRO_SEITE);
		seite = Math.max(0, Math.min(seite, seiten - 1));
		if (ordner != null) ordner.setVisible(art == CAPES || art == FLUEGEL);
		zurueck.setVisible(seiten > 1);
		weiter.setVisible(seiten > 1);
		zurueck.setEnabled(seite > 0);
		weiter.setEnabled(seite < seiten - 1);
		seitenText = seiten > 1 ? (seite + 1) + " / " + seiten : "";
		for (int n = 0; n < PRO_SEITE; n++) {
			int i = seite * PRO_SEITE + n;
			if (i >= eintraege.size()) {
				break;
			}
			Kachel k = new Kachel(eintraege.get(i), i);
			k.setBounds(R_X + (n % 4) * (K_B + ABSTAND), V_Y + 46 + (n / 4) * (K_H + ABSTAND), K_B, K_H);
			kacheln.add(k);
			add(k);
		}
		revalidate();
		repaint();
	}

	// ================================================================ Zeichnen

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "KOSMETIK", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("Deine Figur mit der Maus drehen. Cape, Flügel und Partikel sind nur für dich sichtbar.", X, 84);

		// Skin-Leiste
		int sy = V_Y + V_H + 52;
		g.setFont(NeonUI.mono(11));
		g.setColor(NeonUI.NEON);
		g.drawString("SKIN", V_X, sy + 21);
		g.setFont(NeonUI.normal(11));
		g.setColor(NeonUI.GRAU);
		g.drawString(skinName + "  ·  " + skinStatus, V_X, sy + 50);

		if (!seitenText.isEmpty()) {
			g.setFont(NeonUI.mono(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(seitenText, R_X + 4 * (K_B + ABSTAND) - 150, 510);
		}

		// Ausrüstung unter den Kacheln
		int ay = 548;
		g.setFont(NeonUI.mono(10));
		String[][] zeilen = {
				{"CAPE", name(ClientEinstellungen.CAPES, l.client.cape())},
				{"FLÜGEL", name(ClientEinstellungen.FLUEGEL, l.client.fluegel())},
				{"SPUR", name(ClientEinstellungen.PARTIKEL, l.client.partikel())},
				{"HUT", name(ClientEinstellungen.HUETE, l.client.hut())},
		};
		for (int i = 0; i < zeilen.length; i++) {
			int x = R_X + i * 92;
			g.setColor(NeonUI.GRAU);
			g.drawString(zeilen[i][0], x, ay);
			g.setColor(TAB_FARBEN[i]);
			g.drawString(zeilen[i][1].toUpperCase(), x, ay + 16);
		}
		g.dispose();
	}

	private String name(String[][] liste, String id) {
		if (ClientEinstellungen.AUS.equals(id)) {
			return "—";
		}
		if (EigeneKosmetik.istEigen(id)) {
			EigeneKosmetik.Eintrag e = eigene.finde(liste == ClientEinstellungen.CAPES ? EigeneKosmetik.CAPES : EigeneKosmetik.FLUEGEL, id);
			return e != null ? e.name : "—";
		}
		return ClientEinstellungen.name(liste, id);
	}

	private static Color farbeVon(String[][] liste, String id) {
		for (String[] e : liste) {
			if (e[0].equals(id)) {
				return NeonUI.farbe(e[2]);
			}
		}
		return NeonUI.NEON;
	}

	static void zeichneEinPartikel(Graphics2D g, String id, int x, int y, Color farbe, int a, int i) {
		g.setColor(NeonUI.alpha(farbe, a / 4));
		g.fillOval(x - 7, y - 7, 14, 14);
		g.setColor(NeonUI.alpha(farbe, a));
		if ("herzen".equals(id)) {
			g.setFont(NeonUI.titel(16));
			g.drawString("♥", x - 6, y + 6);
		} else if ("noten".equals(id)) {
			g.setFont(NeonUI.titel(16));
			g.drawString(i % 2 == 0 ? "♪" : "♫", x - 5, y + 6);
		} else if ("kirsche".equals(id)) {
			g.fillOval(x - 4, y - 2, 8, 5);
		} else if ("funken".equals(id)) {
			g.setStroke(new BasicStroke(1.5f));
			g.drawLine(x - 4, y, x + 4, y);
			g.drawLine(x, y - 4, x, y + 4);
		} else {
			g.fillRect(x - 2, y - 2, 4, 4);
		}
	}

	// ============================================================ 3D-Vorschau

	private final class Vorschau3D extends JComponent {
		double yaw = 0.5;
		double pitch = 0.12;
		double zielYaw = Double.NaN;
		boolean autoDrehen = true;
		private int mausX, mausY;
		private long letzteBewegung = 0;

		Vorschau3D() {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			MouseAdapter m = new MouseAdapter() {
				@Override public void mousePressed(MouseEvent e) {
					mausX = e.getX();
					mausY = e.getY();
					zielYaw = Double.NaN;
					letzteBewegung = System.currentTimeMillis();
				}

				@Override public void mouseDragged(MouseEvent e) {
					yaw += (e.getX() - mausX) * 0.012;
					pitch = Math.max(-0.6, Math.min(0.6, pitch + (e.getY() - mausY) * 0.008));
					mausX = e.getX();
					mausY = e.getY();
					letzteBewegung = System.currentTimeMillis();
					repaint();
				}

				@Override public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						drehZu(yaw + Math.PI);
					}
				}
			};
			addMouseListener(m);
			addMouseMotionListener(m);
		}

		void drehZu(double ziel) {
			// kürzesten Weg nehmen
			double d = ((ziel - yaw) % (2 * Math.PI) + 3 * Math.PI) % (2 * Math.PI) - Math.PI;
			zielYaw = yaw + d;
			letzteBewegung = System.currentTimeMillis();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth();
			int h = getHeight();
			double t = System.currentTimeMillis() / 1000.0;

			// Bewegung
			if (!Double.isNaN(zielYaw)) {
				yaw += (zielYaw - yaw) * 0.15;
				if (Math.abs(zielYaw - yaw) < 0.005) {
					yaw = zielYaw;
					zielYaw = Double.NaN;
				}
			} else if (autoDrehen && System.currentTimeMillis() - letzteBewegung > 2500) {
				yaw += 0.012;
			}

			RoundRectangle2D rahmen = new RoundRectangle2D.Float(0, 0, b - 1, h - 1, 16, 16);
			g.setPaint(new GradientPaint(0, 0, new Color(0x0B1626), 0, h, new Color(0x04070C)));
			g.fill(rahmen);
			Shape alt = g.getClip();
			g.clip(rahmen);
			NeonUI.gitter(g, 0, 0, b, h, 20, NeonUI.alpha(NeonUI.CYAN, 12));

			int fx = b / 2;
			int fy = h - 58;
			double massstab = 7.6;

			// Plattform
			for (int i = 4; i >= 0; i--) {
				g.setColor(NeonUI.alpha(NeonUI.CYAN, 16 + (4 - i) * 9));
				g.fill(new Ellipse2D.Double(fx - 78 - i * 6, fy - 12 - i * 2, 156 + i * 12, 26 + i * 4));
			}
			g.setColor(NeonUI.CYAN);
			g.setStroke(new BasicStroke(1.5f));
			g.draw(new Ellipse2D.Double(fx - 78, fy - 12, 156, 26));

			String partikel = l.client.partikel();
			zeichnePartikel(g, partikel, fx, fy, t, false);
			modell.zeichne(g, fx, fy, massstab, yaw, pitch, t);
			zeichnePartikel(g, partikel, fx, fy, t, true);

			// Scanlinie
			int scan = (int) ((t * 80) % h);
			g.setPaint(new GradientPaint(0, scan - 30, NeonUI.alpha(NeonUI.CYAN, 0), 0, scan, NeonUI.alpha(NeonUI.CYAN, 30)));
			g.fillRect(0, scan - 30, b, 30);
			g.setClip(alt);

			NeonUI.ecken(g, 1, 1, b - 3, h - 3, 16, NeonUI.CYAN);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.GRAU);
			g.drawString("3D-VORSCHAU", 16, 24);
			g.setColor(NeonUI.alpha(NeonUI.CYAN, 200));
			g.drawString("ZIEHEN = DREHEN", b - 120, 24);
			g.dispose();
		}

		private void zeichnePartikel(Graphics2D g, String id, int cx, int fuss, double t, boolean vorne) {
			if (ClientEinstellungen.AUS.equals(id)) {
				return;
			}
			Color farbe = farbeVon(ClientEinstellungen.PARTIKEL, id);
			int anzahl = 18;
			for (int i = 0; i < anzahl; i++) {
				if ((i % 2 == 0) != vorne) {
					continue;
				}
				double phase = (t * 0.35 + i / (double) anzahl) % 1.0;
				double px = cx + Math.sin(i * 2.3 + t * 1.3) * (70 + (i % 3) * 14);
				double py = fuss - 10 - phase * 300;
				int a = (int) (255 * Math.sin(phase * Math.PI));
				zeichneEinPartikel(g, id, (int) px, (int) py, farbe, a, i);
			}
		}
	}

	// ================================================================ Bauteile

	private final class UnterTab extends JComponent {
		private final int nummer;
		private boolean ueber;

		UnterTab(int nummer) {
			this.nummer = nummer;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					if (art != UnterTab.this.nummer) {
						art = UnterTab.this.nummer;
						seite = 0;
						baueKacheln();
						SeiteKosmetik.this.repaint();
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			boolean aktiv = art == nummer;
			Color farbe = TAB_FARBEN[nummer];
			RoundRectangle2D form = new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
			if (aktiv) {
				g.setColor(NeonUI.alpha(farbe, 40));
				g.fill(form);
				NeonUI.leuchten(g, form, farbe, 2);
			} else {
				g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
				g.fill(form);
				g.setColor(NeonUI.LINIE);
				g.draw(form);
			}
			g.setFont(NeonUI.mono(10));
			g.setColor(aktiv ? NeonUI.TEXT : NeonUI.GRAU);
			NeonUI.zentriert(g, TAB_NAMEN[nummer], 0, 0, getWidth(), getHeight());
			g.dispose();
		}
	}

	private final class Kachel extends JComponent {
		private final String id;
		private final String name;
		private final Color farbe;
		private final int nummer;
		private boolean ueber;

		Kachel(String[] eintrag, int nummer) {
			this.id = eintrag[0];
			this.name = eintrag[1];
			this.farbe = NeonUI.farbe(eintrag[2]);
			this.nummer = nummer;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					if (NEU.equals(id)) {
						oeffneStudio();
					} else if (SwingUtilities.isRightMouseButton(e) && EigeneKosmetik.istEigen(id)) {
						loeschen(id, name);
					} else {
						waehle(id);
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth();
			int h = getHeight();
			boolean aktiv = id.equals(gewaehlt());
			RoundRectangle2D form = new RoundRectangle2D.Float(3, 3, b - 7, h - 7, 12, 12);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			Color rahmen = ClientEinstellungen.AUS.equals(id) ? NeonUI.NEON : farbe;
			if (aktiv) {
				NeonUI.leuchten(g, form, rahmen, 3 + (int) (2 * NeonUI.puls()));
			} else {
				g.setColor(ueber ? NeonUI.alpha(rahmen, 150) : NeonUI.LINIE);
				g.setStroke(new BasicStroke(1.2f));
				g.draw(form);
			}

			int mx = b / 2;
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			if (NEU.equals(id)) {
				double p = NeonUI.puls();
				g.setColor(NeonUI.alpha(NeonUI.PINK, (int) (25 + 30 * p)));
				g.fillOval(mx - 24, 14, 48, 48);
				g.setColor(NeonUI.PINK);
				g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.drawLine(mx - 11, 38, mx + 11, 38);
				g.drawLine(mx, 27, mx, 49);
				g.setStroke(new BasicStroke(1.5f));
				g.drawOval(mx - 24, 14, 48, 48);
			} else if (ClientEinstellungen.AUS.equals(id)) {
				g.setColor(NeonUI.GRAU);
				g.setStroke(new BasicStroke(2f));
				g.drawOval(mx - 14, 22, 28, 28);
				g.drawLine(mx - 10, 46, mx + 10, 26);
			} else if (art == CAPES) {
				BufferedImage bild = capeBilder.get(id);
				if (bild != null) {
					g.setColor(NeonUI.alpha(farbe, 35));
					g.fillRoundRect(mx - 20, 10, 40, 58, 8, 8);
					int f = Math.max(1, bild.getWidth() / 64);   // HD-Capes
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g.drawImage(bild, mx - 15, 13, mx + 15, 61, f, f, 11 * f, 17 * f, null);
				}
			} else if (art == FLUEGEL) {
				BufferedImage bild = fluegelBilder.get(id);
				if (bild != null) {
					g.setColor(NeonUI.alpha(farbe, 30));
					g.fillOval(mx - 30, 8, 60, 60);
					// beide Flügel nebeneinander, links gespiegelt
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					int fh = bild.getWidth() >= bild.getHeight() * 1.6 ? 15 : 30;   // 3D-Flügel sind 2:1
					g.drawImage(bild, mx, 27 - fh / 2, mx + 30, 27 - fh / 2 + fh, 0, 0, bild.getWidth(), bild.getHeight(), null);
					g.drawImage(bild, mx, 27 - fh / 2, mx - 30, 27 - fh / 2 + fh, 0, 0, bild.getWidth(), bild.getHeight(), null);
				}
			} else if (art == HUETE) {
				// kleines Hut-Symbol: Kopf + Hut in seiner Farbe
				g.setColor(new Color(0x2A3A4E));
				g.fillRoundRect(mx - 14, 30, 28, 28, 6, 6);
				g.setColor(farbe);
				g.fillRoundRect(mx - 18, 24, 36, 7, 4, 4);
				g.fillRoundRect(mx - 11, 12, 22, 14, 4, 4);
				g.setColor(NeonUI.alpha(farbe, (int) (40 + 40 * NeonUI.puls())));
				g.drawRoundRect(mx - 20, 10, 40, 50, 10, 10);
			} else {
				double t = System.currentTimeMillis() / 1000.0;
				g.setColor(NeonUI.alpha(farbe, (int) (20 + 25 * NeonUI.puls())));
				g.fillOval(mx - 26, 12, 52, 52);
				for (int i = 0; i < 6; i++) {
					double w = t * 1.4 + i * Math.PI / 3;
					zeichneEinPartikel(g, id, (int) (mx + Math.cos(w) * 18), (int) (38 + Math.sin(w) * 18), farbe, 230, i + nummer);
				}
			}

			if (EigeneKosmetik.istEigen(id)) {
				g.setColor(NeonUI.PINK);
				g.fillRoundRect(6, 6, 34, 13, 6, 6);
				g.setFont(NeonUI.mono(8));
				g.setColor(NeonUI.HG);
				NeonUI.zentriert(g, "EIGEN", 6, 6, 34, 13);
			}
			g.setFont(NeonUI.titel(11));
			g.setColor(aktiv ? NeonUI.TEXT : new Color(0xB5C2D4));
			String kurz = name;
			while (g.getFontMetrics().stringWidth(kurz) > b - 10 && kurz.length() > 3) {
				kurz = kurz.substring(0, kurz.length() - 2) + "…";
			}
			NeonUI.zentriert(g, kurz, 0, h - 36, b, 14);
			g.setFont(NeonUI.mono(9));
			g.setColor(aktiv ? rahmen : ueber ? NeonUI.TEXT : NeonUI.GRAU);
			String status = NEU.equals(id) ? "STUDIO" : aktiv ? "● AKTIV"
					: ueber && EigeneKosmetik.istEigen(id) ? "RECHTSKLICK: ✕" : "WÄHLEN";
			NeonUI.zentriert(g, status, 0, h - 21, b, 12);
			g.dispose();
		}
	}
}
