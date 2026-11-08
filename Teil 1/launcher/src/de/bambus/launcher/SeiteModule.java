package de.bambus.launcher;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Module & Funktionen per Schalter an- und ausmachen (scrollbar, weil es viele Module gibt). */
final class SeiteModule extends JPanel {

	SeiteModule(BambusLauncher l) {
		setOpaque(false);
		setLayout(new BorderLayout());
		Inhalt inhalt = new Inhalt(l);
		JScrollPane sp = new JScrollPane(inhalt, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setOpaque(false);
		sp.getViewport().setOpaque(false);
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp.getVerticalScrollBar().setUnitIncrement(24);
		sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		sp.getVerticalScrollBar().setOpaque(false);
		sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
			@Override protected void configureScrollBarColors() { thumbColor = NeonUI.alpha(NeonUI.NEON, 120); trackColor = new Color(0, 0, 0, 0); }
			@Override protected javax.swing.JButton createDecreaseButton(int o) { return leer(); }
			@Override protected javax.swing.JButton createIncreaseButton(int o) { return leer(); }
			private javax.swing.JButton leer() { javax.swing.JButton k = new javax.swing.JButton(); k.setPreferredSize(new Dimension(0, 0)); return k; }
		});
		SanftScroll.an(sp);
		add(sp, BorderLayout.CENTER);
	}

	private static final class Inhalt extends JPanel {

	private static final int X = 40;
	private static final int SPALTEN = 3;
	private static final int ABSTAND = 12;
	private static final int KARTE_H = 74;
	private static final int Y_HUD = 132;

	private final int yFunktionen;

	Inhalt(BambusLauncher l) {
		setOpaque(false);
		setLayout(null);
		ClientEinstellungen c = l.client;

		int breite = BambusLauncher.BREITE - BambusLauncher.LEISTE - 2 * X;
		int kb = (breite - (SPALTEN - 1) * ABSTAND) / SPALTEN;

		NeonUI.MiniSchalter hud = new NeonUI.MiniSchalter(c.wert("hudAn", true), an -> c.setze("hudAn", an));
		hud.setBounds(X + breite - 160, Y_HUD - 36, 160, 28);
		add(hud);

		for (int i = 0; i < ClientEinstellungen.MODULE.length; i++) {
			final String[] m = ClientEinstellungen.MODULE[i];
			NeonUI.SchalterKarte k = new NeonUI.SchalterKarte(m[1], m[2], "", c.modulAn(m[0]),
					an -> c.setzeModul(m[0], an)).mitSymbol(Symbole.fuer(m[0]));
			k.setBounds(X + (i % SPALTEN) * (kb + ABSTAND), Y_HUD + (i / SPALTEN) * (KARTE_H + ABSTAND), kb, KARTE_H);
			add(k);
		}

		int zeilen = (ClientEinstellungen.MODULE.length + SPALTEN - 1) / SPALTEN;
		yFunktionen = Y_HUD + zeilen * (KARTE_H + ABSTAND) + 44;

		for (int i = 0; i < ClientEinstellungen.FUNKTIONEN.length; i++) {
			final String[] f = ClientEinstellungen.FUNKTIONEN[i];
			NeonUI.SchalterKarte k = new NeonUI.SchalterKarte(f[1], f[2], f[4],
					c.wert(f[0], Boolean.parseBoolean(f[3])), an -> c.setze(f[0], an)).mitSymbol(Symbole.fuer(f[1]));
			k.setBounds(X + (i % SPALTEN) * (kb + ABSTAND), yFunktionen + (i / SPALTEN) * (KARTE_H + ABSTAND), kb, KARTE_H);
			add(k);
		}
		int fz = (ClientEinstellungen.FUNKTIONEN.length + SPALTEN - 1) / SPALTEN;
		setPreferredSize(new Dimension(BambusLauncher.BREITE - BambusLauncher.LEISTE - 10, yFunktionen + fz * (KARTE_H + ABSTAND) + 30));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "MODULE", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("Karte anklicken = an/aus. Gilt ab dem nächsten Spielstart · im Spiel: RECHTS-SHIFT.", X, 84);

		ueberschrift(g, "HUD-ANZEIGEN", Y_HUD - 16);
		ueberschrift(g, "FUNKTIONEN", yFunktionen - 16);
		g.dispose();
	}

	private void ueberschrift(Graphics2D g, String text, int y) {
		g.setFont(NeonUI.mono(12));
		g.setColor(NeonUI.NEON);
		g.drawString(text, X, y);
		int tb = g.getFontMetrics().stringWidth(text);
		g.setColor(NeonUI.LINIE);
		g.fillRect(X + tb + 12, y - 4, 180, 1);
	}
}
}
