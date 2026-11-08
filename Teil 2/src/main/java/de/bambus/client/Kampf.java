package de.bambus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Wertet eigene Treffer aus: Combo, Reichweite, Ziel, Schaden, Kills und Sprint-Resets (W-/S-Tap).
 * Nur Anzeige – nichts davon greift ins Spiel ein.
 */
public final class Kampf {

	private Kampf() {}

	private static final long COMBO_ZEIT = 3000, ZIEL_ZEIT = 5000;

	public static int combo, besteCombo;
	public static double reach = -1;
	public static long letzterTreffer;
	public static LivingEntity ziel;
	private static float zielLebenVorher;
	public static float letzterSchaden;
	public static long schadenZeit;
	public static int kills, killsSpieler, tode;
	private static boolean zielGezaehlt;
	private static int letzteHurtTime;
	private static boolean warTot;

	// Sprint-Reset seit dem letzten Treffer
	private static boolean sprintete, resetGesehen, wGelassen, sGedrueckt;
	public static String tapArt = "";
	public static long tapZeit;

	/** Für Kill-Effekte und Auto-Screenshot: wird bei jedem eigenen Kill aufgerufen. */
	public interface KillHoerer { void kill(LivingEntity opfer, boolean spieler); }
	public static KillHoerer killHoerer = (o, s) -> {};

	/** Aufruf aus dem AttackEntityCallback (nur Client). */
	public static void angriff(Player spieler, Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		if (spieler != mc.player || !(entity instanceof LivingEntity le) || !le.isAlive()) {
			return;
		}
		long jetzt = System.currentTimeMillis();
		if (le instanceof Player && BambusClient.config.freunde.stream().anyMatch(f -> f.equalsIgnoreCase(le.getName().getString()))) {
			mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§d♥ " + le.getName().getString() + " ist dein Freund"), true);
		}
		reach = abstand(spieler.getEyePosition(), le.getBoundingBox());
		combo = (le == ziel && jetzt - letzterTreffer < COMBO_ZEIT) ? combo + 1 : 1;
		besteCombo = Math.max(besteCombo, combo);
		if (le != ziel) {
			zielGezaehlt = false;
		}
		ziel = le;
		zielLebenVorher = le.getHealth();
		letzterTreffer = jetzt;
		Visuals.trefferPartikel(le);

		if (resetGesehen) {
			tapArt = sGedrueckt ? "S-TAP" : wGelassen ? "W-TAP" : "SPRINT-RESET";
		} else {
			tapArt = spieler.isSprinting() ? "KEIN RESET" : "";
		}
		tapZeit = jetzt;
		resetGesehen = wGelassen = sGedrueckt = false;
	}

	/** Kürzester Abstand vom Auge zur Hitbox (so misst auch der Server die Reichweite ungefähr). */
	static double abstand(Vec3 auge, AABB box) {
		double x = Mth.clamp(auge.x, box.minX, box.maxX);
		double y = Mth.clamp(auge.y, box.minY, box.maxY);
		double z = Mth.clamp(auge.z, box.minZ, box.maxZ);
		return auge.distanceTo(new Vec3(x, y, z));
	}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			ziel = null;
			return;
		}
		long jetzt = System.currentTimeMillis();

		// Selbst getroffen -> Combo vorbei
		if (p.hurtTime > letzteHurtTime) {
			combo = 0;
		}
		letzteHurtTime = p.hurtTime;
		if (jetzt - letzterTreffer > COMBO_ZEIT) {
			combo = 0;
		}

		// Sprint-Reset erkennen: in dem Moment, in dem das Sprinten endet, merken wir uns warum
		boolean sprintetJetzt = p.isSprinting();
		if (sprintete && !sprintetJetzt) {
			resetGesehen = true;
			sGedrueckt = mc.options.keyDown.isDown();
			wGelassen = !mc.options.keyUp.isDown();
		}
		sprintete = sprintetJetzt;

		// Ziel: Schaden und Kill
		if (ziel != null) {
			if (ziel.level() != p.level() || jetzt - letzterTreffer > ZIEL_ZEIT) {
				ziel = null;
			} else {
				float leben = ziel.getHealth();
				if (leben < zielLebenVorher) {
					letzterSchaden = zielLebenVorher - leben;
					schadenZeit = jetzt;
				}
				zielLebenVorher = leben;
				if (!zielGezaehlt && (ziel.isDeadOrDying() || !ziel.isAlive()) && jetzt - letzterTreffer < COMBO_ZEIT) {
					zielGezaehlt = true;
					kills++;
					boolean istSpieler = ziel instanceof Player;
					if (istSpieler) {
						killsSpieler++;
					}
					Statistik.kill(istSpieler, combo);
					killHoerer.kill(ziel, istSpieler);
				}
			}
		}

		// Eigener Tod
		boolean tot = p.isDeadOrDying();
		if (tot && !warTot) {
			tode++;
			combo = 0;
			Statistik.tod();
		}
		warTot = tot;
	}

	public static boolean zielAktiv() {
		return ziel != null && System.currentTimeMillis() - letzterTreffer < ZIEL_ZEIT;
	}
}
