package de.bambus.client.render;

/**
 * Kleine 3D-Transformation (Drehung + Verschiebung) für selbst gebaute Geometrie wie Flügel und Umhang.
 * Funktioniert wie ein PoseStack: jede Drehung/Verschiebung wirkt im aktuellen (schon gedrehten) Rahmen.
 * Winkel in Grad. Drehmatrizen wie in CSS/DOMMatrix, damit die Werte aus der Web-Vorschau direkt passen.
 */
final class Rahmen {

	private final float[] m = {1, 0, 0, 0, 1, 0, 0, 0, 1};
	private float tx, ty, tz;

	Rahmen() {}

	private Rahmen(Rahmen o) {
		System.arraycopy(o.m, 0, m, 0, 9);
		tx = o.tx;
		ty = o.ty;
		tz = o.tz;
	}

	Rahmen kopie() {
		return new Rahmen(this);
	}

	Rahmen schiebe(float x, float y, float z) {
		tx += m[0] * x + m[1] * y + m[2] * z;
		ty += m[3] * x + m[4] * y + m[5] * z;
		tz += m[6] * x + m[7] * y + m[8] * z;
		return this;
	}

	Rahmen dreheX(float grad) {
		float c = cos(grad), s = sin(grad);
		return mal(1, 0, 0, 0, c, -s, 0, s, c);
	}

	Rahmen dreheY(float grad) {
		float c = cos(grad), s = sin(grad);
		return mal(c, 0, s, 0, 1, 0, -s, 0, c);
	}

	Rahmen dreheZ(float grad) {
		float c = cos(grad), s = sin(grad);
		return mal(c, -s, 0, s, c, 0, 0, 0, 1);
	}

	/** Spiegelt die x-Achse (für den linken Flügel: faktor -1). */
	Rahmen spiegelX(float faktor) {
		return mal(faktor, 0, 0, 0, 1, 0, 0, 0, 1);
	}

	/** Wendet den Rahmen auf einen Punkt an; Ergebnis in ziel[0..2]. */
	void punkt(float x, float y, float z, float[] ziel, int ab) {
		ziel[ab] = m[0] * x + m[1] * y + m[2] * z + tx;
		ziel[ab + 1] = m[3] * x + m[4] * y + m[5] * z + ty;
		ziel[ab + 2] = m[6] * x + m[7] * y + m[8] * z + tz;
	}

	/** Wendet nur die Drehung an (für Normalen). */
	void richtung(float x, float y, float z, float[] ziel, int ab) {
		ziel[ab] = m[0] * x + m[1] * y + m[2] * z;
		ziel[ab + 1] = m[3] * x + m[4] * y + m[5] * z;
		ziel[ab + 2] = m[6] * x + m[7] * y + m[8] * z;
	}

	private Rahmen mal(float a0, float a1, float a2, float a3, float a4, float a5, float a6, float a7, float a8) {
		float[] a = {a0, a1, a2, a3, a4, a5, a6, a7, a8};
		float[] r = new float[9];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				r[i * 3 + j] = m[i * 3] * a[j] + m[i * 3 + 1] * a[3 + j] + m[i * 3 + 2] * a[6 + j];
			}
		}
		System.arraycopy(r, 0, m, 0, 9);
		return this;
	}

	private static float cos(float grad) {
		return (float) Math.cos(Math.toRadians(grad));
	}

	private static float sin(float grad) {
		return (float) Math.sin(Math.toRadians(grad));
	}
}
