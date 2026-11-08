package de.bambus.client;

import de.bambus.client.mixin.OptionInstanceAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

/** Zoom, Scroll-Zoom, Freelook, Fullbright, Toggle Sprint/Sneak und CPS. */
public final class Funktionen {

	private Funktionen() {}

	private static BambusConfig cfg() {
		return BambusClient.config;
	}

	@SuppressWarnings("unchecked")
	private static void setzeOhnePruefung(Object option, Object wert) {
		((OptionInstanceAccessor) option).bambus$setValue(wert);
	}

	// ------------------------------------------------------------------ CPS

	private static final Deque<Long> LINKS = new ArrayDeque<>();
	private static final Deque<Long> RECHTS = new ArrayDeque<>();
	private static boolean linksWar;
	private static boolean rechtsWar;

	/** Wird jedes Bild aufgerufen, damit auch schnelle Klicks gezählt werden. */
	public static void zaehleKlicks(Minecraft mc) {
		long jetzt = System.currentTimeMillis();
		boolean links = mc.options.keyAttack.isDown();
		if (links && !linksWar) {
			LINKS.addLast(jetzt);
		}
		linksWar = links;
		boolean rechts = mc.options.keyUse.isDown();
		if (rechts && !rechtsWar) {
			RECHTS.addLast(jetzt);
		}
		rechtsWar = rechts;
		while (!LINKS.isEmpty() && jetzt - LINKS.peekFirst() > 1000) {
			LINKS.removeFirst();
		}
		while (!RECHTS.isEmpty() && jetzt - RECHTS.peekFirst() > 1000) {
			RECHTS.removeFirst();
		}
	}

	public static int cpsLinks() {
		return LINKS.size();
	}

	public static int cpsRechts() {
		return RECHTS.size();
	}

	// ----------------------------------------------------------------- Zoom

	private static final int ZOOM_START = 30;
	private static Integer alteFov = null;
	private static boolean alteSmoothCamera;
	private static int zoomFov = ZOOM_START;

	public static void zoomTick(Minecraft mc, boolean gedrueckt) {
		boolean zoomen = gedrueckt && mc.screen == null && mc.player != null;
		if (zoomen && alteFov == null) {
			alteFov = mc.options.fov().get();
			alteSmoothCamera = mc.options.smoothCamera;
			zoomFov = ZOOM_START;
			setzeOhnePruefung(mc.options.fov(), zoomFov);
			mc.options.smoothCamera = true;
		} else if (!zoomen && alteFov != null) {
			setzeOhnePruefung(mc.options.fov(), alteFov);
			mc.options.smoothCamera = alteSmoothCamera;
			alteFov = null;
		}
	}

	/** Mausrad beim Zoomen. Gibt true zurück, wenn das Scrollen verbraucht wurde. */
	public static boolean scroll(double menge) {
		Minecraft mc = Minecraft.getInstance();
		if (alteFov == null || !cfg().scrollZoom || mc.screen != null || menge == 0) {
			return false;
		}
		zoomFov = Mth.clamp(zoomFov - (int) Math.signum(menge) * 4, 5, 70);
		setzeOhnePruefung(mc.options.fov(), zoomFov);
		return true;
	}

	// ------------------------------------------------------------- Freelook

	public static boolean freelookAktiv = false;
	public static float kameraYaw;
	public static float kameraPitch;
	private static CameraType alteKamera;

	public static void freelookTick(Minecraft mc, boolean gedrueckt) {
		boolean an = gedrueckt && cfg().freelook && mc.screen == null && mc.player != null;
		if (an && !freelookAktiv) {
			freelookAktiv = true;
			kameraYaw = mc.player.getYRot();
			kameraPitch = mc.player.getXRot();
			alteKamera = mc.options.getCameraType();
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		} else if (!an && freelookAktiv) {
			freelookAktiv = false;
			if (alteKamera != null) {
				mc.options.setCameraType(alteKamera);
			}
		}
	}

	/** Mausbewegung während Freelook: nur die Kamera dreht sich, nicht der Spieler. */
	public static boolean drehen(Entity entity, double dYaw, double dPitch) {
		if (!freelookAktiv || entity != Minecraft.getInstance().player) {
			return false;
		}
		kameraYaw += (float) dYaw * 0.15f;
		kameraPitch = Mth.clamp(kameraPitch + (float) dPitch * 0.15f, -90.0f, 90.0f);
		return true;
	}

	// ----------------------------------------------------------- Fullbright

	private static Double alteGamma = null;

	public static void fullbrightTick(Minecraft mc) {
		if (cfg().fullbright && alteGamma == null) {
			alteGamma = mc.options.gamma().get();
			setzeOhnePruefung(mc.options.gamma(), 15.0);
		} else if (!cfg().fullbright && alteGamma != null) {
			setzeOhnePruefung(mc.options.gamma(), alteGamma);
			alteGamma = null;
		}
	}

	// ------------------------------------------------- Toggle Sprint / Sneak

	public static void sprintUndSchleichenAnwenden(Minecraft mc) {
		mc.options.toggleSprint().set(cfg().toggleSprint);
		mc.options.toggleCrouch().set(cfg().toggleSneak);
	}
}
