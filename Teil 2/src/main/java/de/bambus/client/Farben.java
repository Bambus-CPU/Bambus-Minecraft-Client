package de.bambus.client;

/** Neon-Farben im ARGB-Format (das erste Byte ist die Deckkraft und muss gesetzt sein). */
public final class Farben {
	// Neon – nicht final, weil das Farbthema sie ändern kann
	public static int NEON = 0xFF3DFF8A;          // Hauptfarbe
	public static int NEON_CYAN = 0xFF00E5FF;     // Akzent
	public static int NEON_PINK = 0xFFFF2BD6;

	// alte Namen – zeigen jetzt auf die Neon-Farben
	public static int GRUEN = NEON;
	public static final int GRUEN_DUNKEL = 0xFF1C7A45;
	public static final int GRUEN_GEDRUECKT = 0xC03DFF8A;

	// Flächen
	public static final int HINTERGRUND = 0xA0060B10;
	public static final int PANEL = 0xE00A0F17;
	public static final int KARTE = 0xFF101824;
	public static final int KARTE_HOVER = 0xFF152233;
	public static final int LINIE = 0xFF1E2C3D;

	// Text
	public static final int WEISS = 0xFFFFFFFF;
	public static final int GRAU = 0xFF8A9BB0;
	public static final int SCHWARZ = 0xFF05080C;
	public static final int ROT = 0xFFFF4D6D;
	public static final int GELB = 0xFFFFD54A;

	private Farben() {}

	/** Farbthemen: {Name, Haupt, Akzent, Zweitakzent}. "RGB" läuft durch alle Farben. */
	public static final String[] THEMEN = {"Neon", "Ozean", "Sakura", "Gold", "Lila", "Eis", "RGB"};
	private static final int[][] THEMA_FARBEN = {
			{0xFF3DFF8A, 0xFF00E5FF, 0xFFFF2BD6},
			{0xFF00E5FF, 0xFF2B6BFF, 0xFF7DF9FF},
			{0xFFFF8CC0, 0xFFFFC2DD, 0xFFFF2BD6},
			{0xFFFFC846, 0xFFFF8A3D, 0xFFFF4D6D},
			{0xFFB06BFF, 0xFFFF2BD6, 0xFF00E5FF},
			{0xFFAEEBFF, 0xFFFFFFFF, 0xFF5AE6FF},
			{0xFF3DFF8A, 0xFF00E5FF, 0xFFFF2BD6},
	};

	/** HSB → RGB ohne AWT (AWT soll im Spiel nicht geladen werden). */
	private static int hsb(float h, float s, float v) {
		float r, g, b;
		float f = (h * 6f) % 6f;
		int i = (int) f;
		float p = v * (1 - s), q = v * (1 - s * (f - i)), t = v * (1 - s * (1 - (f - i)));
		switch (i) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}

	/** Jedes Bild aufrufen: setzt die Farben des gewählten Themas (RGB dreht sich langsam weiter). */
	public static void themaAnwenden(int thema) {
		int t = Math.floorMod(thema, THEMEN.length);
		if (THEMEN[t].equals("RGB")) {
			float h = (System.currentTimeMillis() % 8000L) / 8000f;
			NEON = 0xFF000000 | hsb(h, 0.75f, 1f);
			NEON_CYAN = 0xFF000000 | hsb((h + 0.33f) % 1f, 0.8f, 1f);
			NEON_PINK = 0xFF000000 | hsb((h + 0.66f) % 1f, 0.8f, 1f);
		} else {
			NEON = THEMA_FARBEN[t][0];
			NEON_CYAN = THEMA_FARBEN[t][1];
			NEON_PINK = THEMA_FARBEN[t][2];
		}
		GRUEN = NEON;
	}

	/** Gleiche Farbe mit anderer Deckkraft (0–255). */
	public static int alpha(int farbe, int alpha) {
		return (alpha << 24) | (farbe & 0x00FFFFFF);
	}

	/** Langsames Pulsieren zwischen 0 und 1 – für Neon-Leuchten. */
	public static float puls() {
		return (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 450.0));
	}
}
