package de.bambus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.bambus.client.BambusClient;
import de.bambus.client.Kosmetik;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Bambus-Flügel (3D, seit 3.3): Jeder Flügel besteht aus 24 schmalen, überlappenden Streifen, die sich
 * einzeln biegen – so entstehen beim Schlagen keine Lücken. Die Deckfedern (Ebene "vorne") liegen etwas
 * vor und hinter den Schwungfedern (Ebene "hinten"), die Vorderkante ist ein echter 3D-Knochen und ein
 * Gelenkstück führt in den Rücken. Ein Glanzstreifen läuft über den Flügel.
 * Selbst erstellte Flügel (ein Bild im alten Format) werden weiter mit zwei Flächen gezeichnet.
 * Nur auf deinem Bildschirm sichtbar.
 */
public class FluegelLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

	private static final Identifier WEISS = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "textures/weiss.png");
	private static final int VOLL_HELL = 0xF000F0;

	// Maße wie in der Web-Vorschau (Einheit "px"), am Ende mit K in Blöcke umgerechnet
	private static final int N = 24;                  // Streifen pro Flügel
	private static final float WTW = 300f;            // Flügelbreite in px
	private static final float WTH = WTW / 2f;        // Flügelhöhe in px (Bild 2:1)
	private static final float PX = WTW / 128f;       // px pro Texturpixel im 128er-Raster
	private static final float SW = WTW / N;          // Streifenbreite
	private static final float OV = 10f;              // Überlappung der Streifen
	private static final float EBENE_Z = 3f;          // Abstand der Deckfedern zur hinteren Ebene
	private static final float BREITE = 1.30f;        // Flügelbreite im Spiel (Blöcke)
	private static final float K = BREITE / WTW;

	// Wie stark jeder Streifen an einer Biegung beteiligt ist (Summe je 1)
	private static final float[] W_WOELBUNG = gewichte(0, 0, true);
	private static final float[] W_BOGEN_INNEN = gewichte(0.20f, 0.15f, false);
	private static final float[] W_BOGEN_AUSSEN = gewichte(0.75f, 0.18f, false);
	private static final float[] W_GELENK = gewichte(0.45f, 0.15f, false);
	private static final float[] W_SPITZE = gewichte(0.80f, 0.12f, false);

	// Bewegungszustand (sweep, lift, Gelenk-Schwenk, Gelenk-Hub, Spitzen-Hub) – weich nachgeführt
	private static final float[] P = {30, -10, 8, 0, 0};
	private static float phase;
	private static long zuletzt;

	public FluegelLayer(RenderLayerParent<AvatarRenderState, PlayerModel> eltern) {
		super(eltern);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector sammler, int licht, AvatarRenderState zustand,
					   float yRot, float xRot) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || zustand.id != mc.player.getId() || zustand.isInvisible || zustand.isSpectator) {
			return;
		}
		Kosmetik.Fluegel fluegel = Kosmetik.gewaehlteFluegel();
		if (fluegel == null) {
			return;
		}
		poseStack.pushPose();
		// Am Oberkörper befestigt: kippt beim Schleichen automatisch mit
		getParentModel().body.translateAndRotate(poseStack);
		Kosmetik.FluegelForm form = Kosmetik.fluegelForm(fluegel.id());
		if (form == null) {
			alteFluegel(poseStack, sammler, licht, zustand, fluegel);
		} else {
			neueFluegel(poseStack, sammler, licht, zustand, mc.player, fluegel.id(), form);
		}
		poseStack.popPose();
	}

	// ================================================================== 3D-Flügel

	private void neueFluegel(PoseStack poseStack, SubmitNodeCollector sammler, int licht, AvatarRenderState zustand,
							 LocalPlayer spieler, String id, Kosmetik.FluegelForm form) {
		bewege(zustand, spieler);
		int l = form.leuchtet() ? VOLL_HELL : licht;
		float zeit = zustand.ageInTicks / 20f;
		float glanzBand = ((zeit * 0.45f) % 1.6f - 0.3f) * WTW;
		boolean umhang = CapeLayer.zeigtUmhang(spieler);

		Geometrie hinten = new Geometrie(), vorne = new Geometrie(), knochen = new Geometrie();
		for (int seite = 1; seite >= -1; seite -= 2) {
			Ziel ziel = new Ziel(seite * 0.125f, 0.1125f, umhang ? 0.225f : 0.13f);
			Rahmen wurzel = new Rahmen().spiegelX(seite).dreheY(P[0]).dreheZ(P[1]);

			if (form.dicke() > 0) {   // Gelenkstück: ragt schräg in den Rücken
				float tk = form.dicke() * 2 * PX * 1.3f;
				quader(knochen, ziel, wurzel.kopie().dreheY(-35).schiebe(-tk * 0.9f, 0, 0), tk * 1.4f, tk * 0.85f, true, form, l);
			}

			Rahmen m = wurzel.kopie().schiebe(0, -kante(form, 0) * PX, 0);
			for (int i = 0; i < N; i++) {
				float w = i == N - 1 ? SW : SW + OV;
				float u0 = i * SW / WTW, u1 = (i * SW + w) / WTW;
				float mitte = i * SW + SW / 2;
				float glanz = (float) Math.exp(-Math.pow((mitte - glanzBand) / 45f, 2));
				int farbe = Geometrie.grau(0.90f + 0.10f * glanz);

				flaeche(hinten, ziel, m, 0, 0, w, WTH, 0, u0, 0, u1, 1, farbe, l);
				flaeche(vorne, ziel, m, 0, 0, w, WTH, EBENE_Z, u0, 0, u1, 1, farbe, l);
				flaeche(vorne, ziel, m, 0, 0, w, WTH, -EBENE_Z, u0, 0, u1, 1, farbe, l);

				// Knochen entlang der Vorderkante
				float x0 = i * SW / PX, x1 = (i + 1) * SW / PX;
				if (form.dicke() > 0 && x0 < form.knochenBis()) {
					float y0 = kante(form, x0) * PX, y1 = kante(form, Math.min(x1, form.knochenBis())) * PX;
					float laenge = (float) Math.hypot(SW, y1 - y0) + 1.5f;
					float winkel = (float) Math.toDegrees(Math.atan2(y1 - y0, SW));
					float t = form.dicke() * 2 * PX * (1 - 0.55f * Math.min(1, x0 / form.knochenBis()));
					quader(knochen, ziel, m.kopie().schiebe(0, y0, 0).dreheZ(winkel), laenge, t, false, form, l);
				}

				// nächster Streifen: Biegung um die Vorderkante (dort liegt der Knochen)
				float drehpunkt = kante(form, x1) * PX;
				float dY = 24 * W_WOELBUNG[i] + P[2] * W_GELENK[i];
				float dZ = -8 * W_BOGEN_INNEN[i] + 14 * W_BOGEN_AUSSEN[i] + P[3] * W_GELENK[i] + P[4] * W_SPITZE[i];
				m.schiebe(SW, drehpunkt, 0).dreheY(dY).dreheZ(dZ).schiebe(0, -drehpunkt, 0);
			}
		}

		sammler.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Kosmetik.fluegelEbene(id, "hinten")), hinten::zeichne);
		sammler.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(Kosmetik.fluegelEbene(id, "vorne")), vorne::zeichne);
		if (!knochen.leer()) {
			sammler.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(WEISS), knochen::zeichne);
		}
	}

	/** Wo der Flügel am Rücken sitzt (Blöcke). Die Flügel-Geometrie wird in "px" gerechnet und hier umgerechnet. */
	private record Ziel(float x, float y, float z) {
		void setze(float[] p, int ab) {
			// Vorschau-Raum → Spiel: Rücken liegt im Spiel bei +z, in der Vorschau bei -z
			p[ab] = x + K * p[ab];
			p[ab + 1] = y + K * p[ab + 1];
			p[ab + 2] = z - K * p[ab + 2];
		}
	}

	private static void flaeche(Geometrie g, Ziel ziel, Rahmen m, float x0, float y0, float x1, float y1, float z,
								float u0, float v0, float u1, float v1, int farbe, int licht) {
		float[] pos = new float[12];
		m.punkt(x0, y0, z, pos, 0);
		m.punkt(x1, y0, z, pos, 3);
		m.punkt(x1, y1, z, pos, 6);
		m.punkt(x0, y1, z, pos, 9);
		for (int i = 0; i < 4; i++) {
			ziel.setze(pos, i * 3);
		}
		float[] n = new float[3];
		m.richtung(0, 0, 1, n, 0);
		n[2] = -n[2];
		g.add(pos, new float[]{u0, v0, u1, v0, u1, v1, u0, v1}, n, farbe, licht);
	}

	/** Quader entlang x: Länge laenge, quadratischer Querschnitt t (wie in der Vorschau). */
	private static void quader(Geometrie g, Ziel ziel, Rahmen b, float laenge, float t, boolean enden,
							   Kosmetik.FluegelForm form, int licht) {
		int hell = form.knochenHell(), dunkel = form.knochenDunkel();
		float h = t / 2;
		flaeche(g, ziel, b.kopie().schiebe(0, -h, h), 0, 0, laenge, t, 0, 0, 0, 1, 1, Geometrie.mische(dunkel, hell, 0.65f), licht);
		flaeche(g, ziel, b.kopie().schiebe(0, -h, -h), 0, 0, laenge, t, 0, 0, 0, 1, 1, Geometrie.mische(dunkel, hell, 0.65f), licht);
		flaeche(g, ziel, b.kopie().schiebe(0, -h, -h).dreheX(90), 0, 0, laenge, t, 0, 0, 0, 1, 1, hell, licht);
		flaeche(g, ziel, b.kopie().schiebe(0, h, -h).dreheX(90), 0, 0, laenge, t, 0, 0, 0, 1, 1, dunkel, licht);
		if (enden) {
			flaeche(g, ziel, b.kopie().schiebe(0, -h, -h).dreheY(-90), 0, 0, t, t, 0, 0, 0, 1, 1, Geometrie.mische(dunkel, hell, 0.5f), licht);
			flaeche(g, ziel, b.kopie().schiebe(laenge, -h, -h).dreheY(-90), 0, 0, t, t, 0, 0, 0, 1, 1, Geometrie.mische(dunkel, hell, 0.5f), licht);
		}
	}

	/** Vorderkante des Flügels in Texturpixeln (128er-Raster), aus Kosmetik.FluegelForm. */
	private static float kante(Kosmetik.FluegelForm form, float x) {
		float[][] p = form.kante();
		if (x <= p[0][0]) {
			return p[0][1];
		}
		for (int i = 1; i < p.length; i++) {
			if (x <= p[i][0]) {
				float k = (x - p[i - 1][0]) / (p[i][0] - p[i - 1][0]);
				return p[i - 1][1] * (1 - k) + p[i][1] * k;
			}
		}
		return p[p.length - 1][1];
	}

	/** Bewegung: Schlagen beim Laufen/Springen/Fliegen, Gleiten im Stand, Anlegen beim Schleichen. */
	private static void bewege(AvatarRenderState zustand, LocalPlayer spieler) {
		long jetzt = System.nanoTime();
		float dt = zuletzt == 0 ? 0 : Math.min(0.05f, Math.max(0, (jetzt - zuletzt) / 1e9f));
		zuletzt = jetzt;

		Vec3 v = spieler.getDeltaMovement();
		float waagrecht = (float) Math.sqrt(v.x * v.x + v.z * v.z);
		int modus;          // 0 = schlagen, 1 = gleiten, 2 = angelegt
		float tempo;
		if (zustand.isCrouching) {
			modus = 2; tempo = 0.6f;
		} else if (zustand.isFallFlying) {
			modus = 0; tempo = 1.8f;
		} else if (!spieler.onGround()) {
			modus = 0; tempo = 1.35f;
		} else if (spieler.isSprinting()) {
			modus = 0; tempo = 1.15f;
		} else if (waagrecht > 0.03f) {
			modus = 0; tempo = 0.8f;
		} else {
			modus = 1; tempo = 1.0f;
		}
		phase += dt * 5.2f * tempo;
		float p = phase;
		float[] ziel;
		if (modus == 0) {
			ziel = new float[]{30 + 12 * Mth.cos(p), -8 - 30 * Mth.sin(p), 10 + 10 * Mth.sin(p),
					-22 * Mth.sin(p - 0.9f), -16 * Mth.sin(p - 1.8f)};
		} else if (modus == 1) {
			ziel = new float[]{20 + 2 * Mth.sin(p * 0.5f), -12 - 4 * Mth.sin(p * 0.5f), 4,
					3 + 3 * Mth.sin(p * 0.5f - 0.6f), 2 + 3 * Mth.sin(p * 0.5f - 1.2f)};
		} else {
			ziel = new float[]{18 + 1.5f * Mth.sin(p * 0.4f), 62, 6, 8, 6};
		}
		float k = 1 - (float) Math.pow(0.0015, dt);
		for (int i = 0; i < P.length; i++) {
			P[i] += (ziel[i] - P[i]) * k;
		}
	}

	private static float[] gewichte(float mitte, float breite, boolean linear) {
		float[] w = new float[N];
		float summe = 0;
		for (int i = 0; i < N; i++) {
			float t = (i + 1) / (float) N;
			w[i] = linear ? t : (float) Math.exp(-Math.pow((t - mitte) / breite, 2));
			summe += w[i];
		}
		for (int i = 0; i < N; i++) {
			w[i] /= summe;
		}
		return w;
	}

	// ================================================================== Selbst erstellte Flügel (altes Format)

	private void alteFluegel(PoseStack poseStack, SubmitNodeCollector sammler, int licht, AvatarRenderState zustand,
							 Kosmetik.Fluegel fluegel) {
		// Flattern: beim Fliegen schneller und weiter geöffnet.
		float tempo = zustand.isFallFlying ? 0.45f : 0.12f;
		float basis = zustand.isFallFlying ? 0.10f : 0.40f;
		float t = zustand.ageInTicks * tempo;
		float innen = basis + 0.20f * Mth.sin(t);
		float aussen = basis + 0.10f + 0.30f * Mth.sin(t - 0.7f);
		float hub = -0.07f * Mth.sin(t - 0.4f);
		Identifier bild = Kosmetik.fluegelBild(fluegel.id());
		sammler.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(bild), (pose, vc) -> {
			zweiFlaechen(pose, vc, licht, -1, innen, aussen, hub);
			zweiFlaechen(pose, vc, licht, 1, innen, aussen, hub);
		});
	}

	/** Wo das Gelenk im Bild liegt (Anteil der Bildbreite). */
	private static final float GELENK_U = 0.45f;

	private static void zweiFlaechen(PoseStack.Pose pose, VertexConsumer vc, int licht, int seite,
									 float winkelInnen, float winkelAussen, float hub) {
		float breite = 1.05f;
		float li = breite * GELENK_U;
		float la = breite * (1 - GELENK_U);
		float x0 = seite * 0.08f;
		float z0 = 0.16f;
		float xe = x0 + seite * li * Mth.cos(winkelInnen);
		float ze = z0 + li * Mth.sin(winkelInnen);
		float x1 = xe + seite * la * Mth.cos(winkelAussen);
		float z1 = ze + la * Mth.sin(winkelAussen);
		float obenWurzel = -0.05f, obenSpitze = -0.45f;
		float untenWurzel = 0.72f, untenSpitze = 0.55f;
		float obenGelenk = obenWurzel + (obenSpitze - obenWurzel) * GELENK_U + hub * 0.4f;
		float untenGelenk = untenWurzel + (untenSpitze - untenWurzel) * GELENK_U + hub * 0.4f;
		ecke(pose, vc, licht, x0, obenWurzel, z0, 0f, 0f);
		ecke(pose, vc, licht, xe, obenGelenk, ze, GELENK_U, 0f);
		ecke(pose, vc, licht, xe, untenGelenk, ze, GELENK_U, 1f);
		ecke(pose, vc, licht, x0, untenWurzel, z0, 0f, 1f);
		ecke(pose, vc, licht, xe, obenGelenk, ze, GELENK_U, 0f);
		ecke(pose, vc, licht, x1, obenSpitze + hub, z1, 1f, 0f);
		ecke(pose, vc, licht, x1, untenSpitze + hub, z1, 1f, 1f);
		ecke(pose, vc, licht, xe, untenGelenk, ze, GELENK_U, 1f);
	}

	private static void ecke(PoseStack.Pose pose, VertexConsumer vc, int licht,
							 float x, float y, float z, float u, float v) {
		vc.addVertex(pose, x, y, z)
				.setColor(0xFFFFFFFF)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(licht)
				.setNormal(pose, 0.0f, 0.0f, 1.0f);
	}
}
