package de.bambus.launcher;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Zeigt, was im neuen Update steckt, und spielt den Launcher auf Wunsch neu ein. */
final class UpdateDialog extends JDialog {

	private static final int B = 640, H = 480;

	private final BambusLauncher l;
	private final NeonUI.SpielKnopf launcherKnopf = new NeonUI.SpielKnopf("LAUNCHER AKTUALISIEREN");
	private volatile String meldung = "";

	UpdateDialog(BambusLauncher l) {
		super(l, "Update", true);
		this.l = l;
		setUndecorated(true);
		setSize(B, H);
		setLocationRelativeTo(l);
		JPanel p = new JPanel(null) {
			@Override protected void paintComponent(Graphics gr) { zeichne(NeonUI.glatt(gr)); }
		};
		setContentPane(p);

		NeonUI.UmrissKnopf zu = new NeonUI.UmrissKnopf("✕", NeonUI.GRAU);
		zu.setBounds(B - 54, 16, 38, 32);
		zu.addActionListener(e -> dispose());
		p.add(zu);

		Updater.Release r = l.update;
		launcherKnopf.setFont(NeonUI.titel(15));
		launcherKnopf.setBounds(40, H - 92, 330, 54);
		launcherKnopf.setVisible(r != null && r.installerUrl != null);
		launcherKnopf.addActionListener(e -> launcherAktualisieren());
		p.add(launcherKnopf);

		NeonUI.UmrissKnopf seite = new NeonUI.UmrissKnopf("ALLE DETAILS", NeonUI.CYAN);
		seite.setBounds(386, H - 84, 150, 38);
		seite.setVisible(r != null && !r.seite.isEmpty());
		seite.addActionListener(e -> {
			try { Desktop.getDesktop().browse(new URI(r.seite)); } catch (Exception ignoriert) { /* Adresse steht im Fenster */ }
		});
		p.add(seite);
	}

	private void launcherAktualisieren() {
		Updater.Release r = l.update;
		launcherKnopf.setEnabled(false);
		meldung = "Wird geladen …";
		repaint();
		Thread t = new Thread(() -> {
			try {
				boolean beenden = Updater.starteLauncherUpdate(r, text -> { meldung = text; repaint(); });
				if (beenden) {
					SwingUtilities.invokeLater(() -> System.exit(0));   // das Setup ersetzt den Launcher und öffnet ihn neu
				} else {
					meldung = "Die neue Version ist geöffnet: Bambus Launcher in „Programme“ ziehen und ersetzen.";
				}
			} catch (Exception ex) {
				meldung = "";
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						"Das Update konnte nicht geladen werden:\n" + ex.getMessage(), "Update", JOptionPane.WARNING_MESSAGE));
			}
			SwingUtilities.invokeLater(() -> { launcherKnopf.setEnabled(true); repaint(); });
		}, "Launcher-Update");
		t.setDaemon(true);
		t.start();
	}

	private void zeichne(Graphics2D g) {
		Updater.Release r = l.update;
		g.setColor(NeonUI.HG);
		g.fillRect(0, 0, B, H);
		NeonUI.gitter(g, 0, 0, B, H, 24, NeonUI.alpha(NeonUI.NEON, 8));
		NeonUI.leuchten(g, new RoundRectangle2D.Float(1, 1, B - 3, H - 3, 18, 18), NeonUI.NEON, 2);
		g.setFont(NeonUI.mono(12));
		g.setColor(NeonUI.NEON);
		g.drawString("● NEUE VERSION", 40, 44);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "BAMBUS " + (r == null ? "" : r.version), 40, 84, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		g.drawString("Du hast " + BambusLauncher.VERSION + ".  " + l.updateStatus, 40, 108);

		g.setFont(NeonUI.mono(11));
		g.setColor(NeonUI.GRAU);
		g.drawString("WAS IST NEU", 40, 144);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.TEXT);
		List<String> zeilen = umbrechen(g.getFontMetrics(), r == null ? "" : r.text, B - 80);
		int max = 10;
		for (int i = 0; i < Math.min(max, zeilen.size()); i++) {
			String z = zeilen.get(i);
			boolean kopf = z.startsWith("\u0001");
			g.setFont(kopf ? NeonUI.mono(11) : NeonUI.normal(13));
			g.setColor(kopf ? NeonUI.CYAN : NeonUI.TEXT);
			g.drawString(kopf ? z.substring(1) : z, 40, 170 + i * 21);
		}
		if (zeilen.size() > max) { g.setColor(NeonUI.GRAU); g.drawString("… mehr unter „Alle Details“", 40, 170 + max * 21); }

		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		if (!meldung.isEmpty()) {
			g.setColor(NeonUI.NEON);
			g.drawString(meldung, 40, H - 108);
		} else if (r != null && r.installerUrl != null) {
			g.drawString("Neue Funktionen im Spiel kommen automatisch. Für den neuen Launcher einmal hier klicken:", 40, H - 108);
		} else {
			g.drawString("Neue Funktionen im Spiel kommen automatisch – du musst nichts weiter tun.", 40, H - 108);
		}
		g.dispose();
	}

	/** Bricht den Release-Text (Markdown) in Zeilen um und entfernt die gröbsten Zeichen. */
	private static List<String> umbrechen(FontMetrics fm, String text, int breite) {
		List<String> aus = new ArrayList<>();
		for (String roh : text.replace("\r", "").split("\n")) {
			boolean kopf = roh.trim().startsWith("#");
			String z = roh.replaceAll("^\\s*#+\\s*", "").replaceAll("^\\s*[-*]\\s+", "•  ").replace("*", "").replace("`", "").trim();
			if (kopf) { aus.add("\u0001" + z.toUpperCase()); continue; }
			if (z.isEmpty()) continue;
			StringBuilder zeile = new StringBuilder();
			for (String w : z.split(" ")) {
				if (fm.stringWidth(zeile + " " + w) > breite && zeile.length() > 0) { aus.add(zeile.toString()); zeile = new StringBuilder("    "); }
				if (zeile.length() > 0 && !zeile.toString().endsWith(" ")) zeile.append(' ');
				zeile.append(w);
			}
			aus.add(zeile.toString());
		}
		return aus;
	}
}
