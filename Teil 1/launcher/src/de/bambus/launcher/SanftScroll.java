package de.bambus.launcher;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Timer;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/** Weiches Scrollen: Mausrad/Trackpad setzt ein Ziel, die Ansicht gleitet mit ~60 Bildern pro Sekunde dorthin. */
final class SanftScroll {

	private static final double SCHRITT = 70;   // Pixel pro Rad-Raste
	private static final double DAEMPFUNG = 0.22; // Anteil des Restwegs pro Bild

	private SanftScroll() {}

	static void an(JScrollPane sp) {
		for (MouseWheelListener m : sp.getMouseWheelListeners()) sp.removeMouseWheelListener(m);
		sp.setWheelScrollingEnabled(false);
		final JViewport vp = sp.getViewport();
		final double[] ziel = { 0 };
		final double[] pos = { 0 };
		final Timer timer = new Timer(16, null);
		timer.addActionListener(e -> {
			double d = ziel[0] - pos[0];
			if (Math.abs(d) < 0.5) { pos[0] = ziel[0]; timer.stop(); }
			else pos[0] += d * DAEMPFUNG;
			vp.setViewPosition(new Point(0, (int) Math.round(pos[0])));
		});
		sp.addMouseWheelListener((MouseWheelEvent e) -> {
			int max = Math.max(0, vp.getView().getHeight() - vp.getHeight());
			if (!timer.isRunning()) { pos[0] = vp.getViewPosition().y; ziel[0] = pos[0]; }
			ziel[0] = Math.max(0, Math.min(max, ziel[0] + e.getPreciseWheelRotation() * SCHRITT));
			if (!timer.isRunning()) timer.start();
		});
	}
}
