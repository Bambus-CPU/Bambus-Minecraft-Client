package de.bambus.launcher;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;

/** Microsoft-Konto: App-ID eintragen, anmelden (Gerätecode), abmelden, Direktstart an/aus. */
final class MicrosoftDialog extends JDialog {

	private static final int B = 640, H = 470;

	private final MicrosoftKonto konto;
	private final Runnable geaendert;
	private final JTextField appFeld = new JTextField();
	private final NeonUI.SpielKnopf anmelden = new NeonUI.SpielKnopf("MIT MICROSOFT ANMELDEN");
	private final NeonUI.UmrissKnopf seite = new NeonUI.UmrissKnopf("SEITE ÖFFNEN", NeonUI.CYAN);
	private final NeonUI.UmrissKnopf kopieren = new NeonUI.UmrissKnopf("CODE KOPIEREN", NeonUI.CYAN);
	private final NeonUI.UmrissKnopf abmelden = new NeonUI.UmrissKnopf("ABMELDEN", NeonUI.PINK);
	private final NeonUI.UmrissKnopf direkt = new NeonUI.UmrissKnopf("", NeonUI.NEON);
	private volatile MicrosoftKonto.Geraetecode code;
	private volatile boolean abgebrochen;
	private volatile String meldung = "";

	MicrosoftDialog(JFrame eltern, MicrosoftKonto konto, Runnable geaendert) {
		super(eltern, "Microsoft-Konto", true);
		this.konto = konto;
		this.geaendert = geaendert;
		setUndecorated(true);
		setSize(B, H);
		setLocationRelativeTo(eltern);
		JPanel p = new JPanel(null) {
			@Override protected void paintComponent(Graphics gr) { zeichne(NeonUI.glatt(gr)); }
		};
		setContentPane(p);

		NeonUI.UmrissKnopf zu = new NeonUI.UmrissKnopf("✕", NeonUI.GRAU);
		zu.setBounds(B - 54, 16, 38, 32);
		zu.addActionListener(e -> { abgebrochen = true; dispose(); });
		p.add(zu);

		anmelden.setFont(NeonUI.titel(16));
		anmelden.setBounds(40, 196, 380, 58);
		anmelden.addActionListener(e -> anmelden());
		p.add(anmelden);
		seite.setBounds(40, 262, 180, 36);
		seite.addActionListener(e -> oeffne(code != null ? code.adresse : "https://www.microsoft.com/link"));
		p.add(seite);
		kopieren.setBounds(232, 262, 180, 36);
		kopieren.addActionListener(e -> {
			if (code != null) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code.userCode), null);
		});
		p.add(kopieren);
		abmelden.setBounds(40, 262, 180, 36);
		abmelden.addActionListener(e -> { konto.abmelden(); meldung = "Abgemeldet."; aktualisiere(); });
		p.add(abmelden);
		direkt.setBounds(40, 196, 380, 44);
		direkt.addActionListener(e -> { konto.setzeDirektStart(!konto.direktStart()); aktualisiere(); });
		p.add(direkt);

		appFeld.setText(konto.appId());
		appFeld.setBounds(40, 392, 380, 34);
		appFeld.setBackground(NeonUI.KARTE);
		appFeld.setForeground(NeonUI.TEXT);
		appFeld.setCaretColor(NeonUI.NEON);
		appFeld.setFont(NeonUI.mono(12));
		appFeld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE, 1, true), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		p.add(appFeld);
		NeonUI.UmrissKnopf speichern = new NeonUI.UmrissKnopf("SPEICHERN", NeonUI.NEON);
		speichern.setBounds(430, 392, 130, 34);
		speichern.addActionListener(e -> {
			konto.setzeAppId(appFeld.getText());
			meldung = konto.freigeschaltet() ? "App-ID gespeichert." : "Das sieht nicht wie eine App-ID aus (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx).";
			aktualisiere();
		});
		p.add(speichern);
		aktualisiere();
	}

	private void aktualisiere() {
		boolean frei = konto.freigeschaltet(), drin = konto.angemeldet();
		anmelden.setVisible(frei && !drin && code == null);
		seite.setVisible(code != null);
		kopieren.setVisible(code != null);
		abmelden.setVisible(drin);
		direkt.setVisible(drin);
		direkt.setText(konto.direktStart() ? "✓  DIREKT STARTEN (ohne Minecraft Launcher)" : "DIREKT STARTEN: AUS (über Minecraft Launcher)");
		repaint();
		if (geaendert != null) geaendert.run();
	}

	private void anmelden() {
		meldung = "Verbinde mit Microsoft …";
		abgebrochen = false;
		repaint();
		Thread t = new Thread(() -> {
			try {
				MicrosoftKonto.Geraetecode c = konto.starteAnmeldung();
				code = c;
				SwingUtilities.invokeLater(() -> { meldung = "Warte auf deine Anmeldung bei Microsoft …"; aktualisiere(); oeffne(c.adresse); });
				MicrosoftKonto.Sitzung s = konto.warteAufAnmeldung(c, () -> abgebrochen);
				code = null;
				SwingUtilities.invokeLater(() -> { meldung = "Angemeldet als " + s.name + ". Viel Spaß!"; aktualisiere(); });
			} catch (InterruptedException ex) {
				code = null;
			} catch (Exception ex) {
				code = null;
				SwingUtilities.invokeLater(() -> { meldung = ex.getMessage() == null ? "Anmeldung fehlgeschlagen." : ex.getMessage(); aktualisiere(); });
			}
		}, "Microsoft-Anmeldung");
		t.setDaemon(true);
		t.start();
	}

	private static void oeffne(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (Exception ignoriert) {
			// Adresse steht auch im Fenster
		}
	}

	private void zeichne(Graphics2D g) {
		g.setColor(NeonUI.HG);
		g.fillRect(0, 0, B, H);
		NeonUI.gitter(g, 0, 0, B, H, 24, NeonUI.alpha(NeonUI.CYAN, 8));
		NeonUI.leuchten(g, new RoundRectangle2D.Float(1, 1, B - 3, H - 3, 18, 18), NeonUI.CYAN, 2);
		g.setFont(NeonUI.titel(26));
		NeonUI.leuchtText(g, "MICROSOFT-KONTO", 40, 54, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("Anmeldung nur auf der offiziellen Microsoft-Seite – Bambus sieht nie dein Passwort.", 40, 80);

		g.setFont(NeonUI.mono(12));
		if (konto.angemeldet()) {
			g.setColor(NeonUI.NEON);
			g.drawString("● ANGEMELDET", 40, 122);
			g.setFont(NeonUI.titel(30));
			NeonUI.leuchtText(g, konto.name(), 40, 166, NeonUI.TEXT, 1);
			g.setFont(NeonUI.normal(12));
			g.setColor(NeonUI.GRAU);
			g.drawString("SPIELEN startet Minecraft jetzt direkt aus Bambus – Spiel und Java kommen frisch von Mojang.", 40, 330);
		} else if (konto.freigeschaltet()) {
			g.setColor(NeonUI.CYAN);
			g.drawString("○ NICHT ANGEMELDET", 40, 122);
			g.setFont(NeonUI.normal(13));
			g.setColor(NeonUI.TEXT);
			if (code != null) {
				g.drawString("Öffne " + code.adresse.replace("https://", "") + " und gib diesen Code ein:", 40, 150);
				g.setFont(NeonUI.titel(40));
				NeonUI.leuchtText(g, code.userCode, 40, 238, NeonUI.NEON, 2);
			} else {
				g.drawString("Melde dich einmal an – danach startet SPIELEN Minecraft direkt.", 40, 150);
				g.setColor(NeonUI.GRAU);
				g.drawString("Ohne Anmeldung geht alles wie bisher über den offiziellen Minecraft Launcher.", 40, 172);
			}
		} else {
			g.setColor(NeonUI.alpha(NeonUI.PINK, 230));
			g.drawString("○ NOCH NICHT FREIGESCHALTET", 40, 122);
			g.setFont(NeonUI.normal(13));
			g.setColor(NeonUI.TEXT);
			String[] schritte = {
					"1.  Auf portal.azure.com eine App registrieren (Konten: „Persönliche Microsoft-Konten“),",
					"     unter „Authentifizierung“ öffentliche Clientflows erlauben.",
					"2.  Bei Mojang die Freigabe für Minecraft beantragen: aka.ms/mce-reviewappid",
					"3.  Nach der Zusage die Anwendungs-ID (Client-ID) unten eintragen.",
			};
			for (int i = 0; i < schritte.length; i++) g.drawString(schritte[i], 40, 156 + i * 24);
			g.setColor(NeonUI.GRAU);
			g.drawString("Bis dahin startet Bambus über den offiziellen Minecraft Launcher – das funktioniert immer.", 40, 270);
		}
		if (!meldung.isEmpty()) {
			g.setFont(NeonUI.normal(12));
			g.setColor(meldung.startsWith("Angemeldet") || meldung.startsWith("App-ID gespeichert") ? NeonUI.NEON : new Color(0xFFB0C4));
			g.drawString(meldung, 40, 356);
		}
		g.setFont(NeonUI.mono(11));
		g.setColor(NeonUI.GRAU);
		g.drawString("AZURE-APP-ID (CLIENT-ID)", 40, 384);
		g.setFont(NeonUI.normal(10));
		g.drawString("Zugangsdaten liegen nur auf diesem Computer (Benutzerordner/.bambus-launcher).", 40, 450);
		g.dispose();
	}
}
