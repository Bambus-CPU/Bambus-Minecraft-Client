package de.bambus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.bambus.client.Kosmetik;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Wehender Bambus-Umhang (seit 3.3). Statt des starren Minecraft-Capes besteht der Umhang aus 24 Reihen
 * mit Stoffdicke, die sich einzeln biegen: Wellen laufen von oben nach unten, beim Laufen, Sprinten und
 * Fallen weht er stärker nach hinten, im Stand bewegt er sich leicht im Wind.
 * Eingebaute Umhänge haben zusätzlich eine leuchtende, laufende Animations-Ebene (Kosmetik.capeAnimation).
 * Bilder im normalen Minecraft-Cape-Layout (64×32-Raster, auch in HD) – eigene Capes funktionieren genauso.
 * Nur auf deinem Bildschirm sichtbar.
 */
public class CapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

	private static final int VOLL_HELL = 0xF000F0;
	private static final int R = 24;                  // Reihen
	private static final float CW = 10 / 16f;         // Breite (Blöcke)
	private static final float CH = 16 / 16f;         // Höhe
	private static final float CT = 1 / 16f;          // Stoffdicke
	private static final float RH = CH / R;
	private static final float UEBERLAPP = RH * 0.5f; // Reihen überlappen leicht → keine Ritzen beim Biegen

	private static float glattVor, glattHoch;
	private static long zuletzt;

	public CapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> eltern) {
		super(eltern);
	}

	/** Wird der Bambus-Umhang gerade gezeigt? (nicht mit Elytra, nicht unsichtbar) */
	public static boolean zeigtUmhang(LocalPlayer spieler) {
		if (spieler == null || spieler.isInvisible() || Kosmetik.gewaehlteCapeId() == null) {
			return false;
		}
		return !spieler.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector sammler, int licht, AvatarRenderState zustand,
					   float yRot, float xRot) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer spieler = mc.player;
		if (spieler == null || zustand.id != spieler.getId() || zustand.isInvisible || zustand.isSpectator
				|| !zeigtUmhang(spieler)) {
			return;
		}
		String id = Kosmetik.gewaehlteCapeId();
		Identifier bild = Kosmetik.capeBild(id);
		float[] anim = Kosmetik.capeAnimation(id);

		// ---- Bewegung messen (vorwärts / hoch), weich geglättet
		long jetzt = System.nanoTime();
		float dt = zuletzt == 0 ? 0 : Math.min(0.05f, Math.max(0, (jetzt - zuletzt) / 1e9f));
		zuletzt = jetzt;
		Vec3 v = spieler.getDeltaMovement();
		float yaw = (float) Math.toRadians(spieler.yBodyRot);
		float vor = (float) (-v.x * Math.sin(yaw) + v.z * Math.cos(yaw));
		float k = 1 - (float) Math.pow(0.02, dt);
		glattVor += (vor - glattVor) * k;
		glattHoch += ((float) v.y - glattHoch) * k;

		float zeit = zustand.ageInTicks / 20f;
		float wind = Mth.clamp(0.3f + Math.abs(glattVor) * 3.5f + Math.abs(glattHoch) * 1.5f, 0, 1);
		float om = 3.2f + 4.5f * wind;
		float th0 = 4 + Mth.clamp(glattVor * 110, -5, 55) + Mth.clamp(-glattHoch * 35, 0, 18)
				+ 4 * wind * Mth.sin(zeit * 1.3f) + (zustand.isCrouching ? 18 : 0);
		boolean brustplatte = !spieler.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
		float rueckenZ = 0.125f + (brustplatte ? 0.0625f : 0) + CT / 2 + 0.004f;

		// ---- Reihen aufbauen
		Geometrie stoff = new Geometrie(), leuchten = new Geometrie();
		Rahmen m = new Rahmen().schiebe(0, 0.01f, rueckenZ);
		float vorher = 0, vorherDreh = 0;
		float su = anim == null ? 0 : frac(zeit * anim[0] / 200f);
		float sv = anim == null ? 0 : frac(zeit * anim[1] / 320f);
		for (int i = 0; i < R; i++) {
			float q = i / (float) R;
			float th = th0 * (1 - (1 - wind) * 0.6f * q) + wind * (3 + 14 * q) * Mth.sin(zeit * om - i * 0.42f)
					+ 1.5f * Mth.sin(zeit * 0.9f - i * 0.2f);
			th = Math.max(1, th);
			float dreh = wind * 4 * q * Mth.sin(zeit * om * 0.6f - i * 0.3f);
			m.dreheX(th - vorher).dreheY(dreh - vorherDreh);
			vorher = th;
			vorherDreh = dreh;

			float h = RH + (i == R - 1 ? 0 : UEBERLAPP);
			float v0 = (1 + 16 * i / (float) R) / 32f, v1 = v0 + 16 * (h / CH) / 32f;
			float a = CW / 2, d = CT / 2;
			// außen (+z, vom Körper weg): Vorderseite des Cape-Bildes (1,1)-(11,17)
			viereck(stoff, m, new float[]{a, 0, d, -a, 0, d, -a, h, d, a, h, d},
					new float[]{1 / 64f, v0, 11 / 64f, v0, 11 / 64f, v1, 1 / 64f, v1}, 0, 0, 1, licht);
			// innen: (12,1)-(22,17)
			viereck(stoff, m, new float[]{-a, 0, -d, a, 0, -d, a, h, -d, -a, h, -d},
					new float[]{12 / 64f, v0, 22 / 64f, v0, 22 / 64f, v1, 12 / 64f, v1}, 0, 0, -1, licht);
			// Kanten links/rechts
			viereck(stoff, m, new float[]{a, 0, -d, a, 0, d, a, h, d, a, h, -d},
					new float[]{0, v0, 1 / 64f, v0, 1 / 64f, v1, 0, v1}, 1, 0, 0, licht);
			viereck(stoff, m, new float[]{-a, 0, d, -a, 0, -d, -a, h, -d, -a, h, d},
					new float[]{11 / 64f, v0, 12 / 64f, v0, 12 / 64f, v1, 11 / 64f, v1}, -1, 0, 0, licht);
			if (i == 0) {
				viereck(stoff, m, new float[]{a, 0, -d, -a, 0, -d, -a, 0, d, a, 0, d},
						new float[]{1 / 64f, 0, 11 / 64f, 0, 11 / 64f, 1 / 32f, 1 / 64f, 1 / 32f}, 0, -1, 0, licht);
			}
			if (i == R - 1) {
				viereck(stoff, m, new float[]{a, h, d, -a, h, d, -a, h, -d, a, h, -d},
						new float[]{11 / 64f, 0, 21 / 64f, 0, 21 / 64f, 1 / 32f, 11 / 64f, 1 / 32f}, 0, 1, 0, licht);
			}
			if (anim != null) {
				float deck = anim[2] * (0.8f + 0.2f * Mth.sin(zeit * 3.1f + i * 0.2f));
				int farbe = (Math.round(deck * 255) << 24) | 0xFFFFFF;
				animationsReihe(leuchten, m, i, h, d + 0.0025f, su, sv, farbe);
			}
			m.schiebe(0, RH, 0);
		}

		poseStack.pushPose();
		getParentModel().body.translateAndRotate(poseStack);
		sammler.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(bild), stoff::zeichne);
		if (anim != null && !leuchten.leer()) {
			sammler.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(Kosmetik.capeAnimBild(id)), leuchten::zeichne);
		}
		poseStack.popPose();
	}

	/**
	 * Animations-Ebene einer Reihe. Die Ebene ist ein nahtlos kachelbares Bild, das verschoben wird.
	 * Damit kein Wiederholungs-Modus der Textur nötig ist, wird das Viereck an der Nahtstelle geteilt.
	 */
	private static void animationsReihe(Geometrie g, Rahmen m, int reihe, float h, float z, float su, float sv, int farbe) {
		float a = CW / 2;
		float vStart = reihe * RH / CH + sv, vLaenge = h / CH;
		float[] sCuts = schnitte(su, 1f);
		float[] rCuts = schnitte(vStart, vLaenge);
		for (int si = 0; si < sCuts.length - 1; si++) {
			for (int ri = 0; ri < rCuts.length - 1; ri++) {
				float s0 = sCuts[si], s1 = sCuts[si + 1], r0 = rCuts[ri], r1 = rCuts[ri + 1];
				float uBasis = (float) Math.floor(su + (s0 + s1) / 2);
				float vBasis = (float) Math.floor(vStart + vLaenge * (r0 + r1) / 2);
				float u0 = su + s0 - uBasis, u1 = su + s1 - uBasis;
				float w0 = vStart + vLaenge * r0 - vBasis, w1 = vStart + vLaenge * r1 - vBasis;
				float x0 = a - s0 * CW, x1 = a - s1 * CW, y0 = r0 * h, y1 = r1 * h;
				viereck(g, m, new float[]{x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z},
						new float[]{u0, w0, u1, w0, u1, w1, u0, w1}, 0, 0, 1, VOLL_HELL, farbe);
			}
		}
	}

	/** Teilt [0,1] dort, wo start + t*laenge eine ganze Zahl überschreitet. */
	private static float[] schnitte(float start, float laenge) {
		float[] tmp = new float[8];
		int n = 0;
		tmp[n++] = 0;
		for (int ganz = (int) Math.floor(start) + 1; ganz < start + laenge && n < 7; ganz++) {
			float t = (ganz - start) / laenge;
			if (t > 1e-4f && t < 1 - 1e-4f) {
				tmp[n++] = t;
			}
		}
		tmp[n++] = 1;
		float[] r = new float[n];
		System.arraycopy(tmp, 0, r, 0, n);
		return r;
	}

	private static void viereck(Geometrie g, Rahmen m, float[] lokal, float[] uv, float nx, float ny, float nz, int licht) {
		viereck(g, m, lokal, uv, nx, ny, nz, licht, 0xFFFFFFFF);
	}

	private static void viereck(Geometrie g, Rahmen m, float[] lokal, float[] uv, float nx, float ny, float nz,
								int licht, int farbe) {
		float[] pos = new float[12];
		for (int i = 0; i < 4; i++) {
			m.punkt(lokal[i * 3], lokal[i * 3 + 1], lokal[i * 3 + 2], pos, i * 3);
		}
		float[] n = new float[3];
		m.richtung(nx, ny, nz, n, 0);
		g.add(pos, uv, n, farbe, licht);
	}

	private static float frac(float x) {
		return x - (float) Math.floor(x);
	}
}
