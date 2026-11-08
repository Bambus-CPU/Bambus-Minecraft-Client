package de.bambus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.bambus.client.BambusClient;
import de.bambus.client.Kosmetik;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Hüte und Kopf-Kosmetik (Krone, Zylinder, Heiligenschein, Hörner, Katzenohren, Partyhut, Bandana, Neon-Visier).
 * Aus farbigen Quadern gebaut, sitzt am Kopf und dreht sich mit. Seit 3.3 detaillierter und animiert
 * (funkelnde Steine, pulsierendes Band, kreisende Funken, flatternde Bandana-Enden, Scan-Balken).
 * Nur auf deinem Bildschirm sichtbar.
 */
public class HutLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

	private static final Identifier WEISS = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "textures/weiss.png");
	private static final int VOLL_HELL = 0xF000F0;

	public HutLayer(RenderLayerParent<AvatarRenderState, PlayerModel> eltern) {
		super(eltern);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector sammler, int licht, AvatarRenderState zustand,
					   float yRot, float xRot) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || zustand.id != mc.player.getId() || zustand.isInvisible || zustand.isSpectator) {
			return;
		}
		Kosmetik.Hut hut = Kosmetik.gewaehlterHut();
		if (hut == null) {
			return;
		}
		float t = zustand.ageInTicks;
		poseStack.pushPose();
		getParentModel().getHead().translateAndRotate(poseStack);
		sammler.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(WEISS), (pose, vc) -> {
			Bauer b = new Bauer(pose, vc, licht);
			switch (hut.id()) {
				case "krone" -> krone(b, t);
				case "zylinder" -> zylinder(b, t);
				case "heiligenschein" -> heiligenschein(b, t);
				case "hoerner" -> hoerner(b, t);
				case "katzenohren" -> katzenohren(b, t);
				case "partyhut" -> partyhut(b, t);
				case "bandana" -> bandana(b, t);
				case "visier" -> visier(b, t);
				default -> { }
			}
		});
		poseStack.popPose();
	}

	// ----------------------------------------------------------------- Designs 3.3 (Maße in Kopf-Pixeln, Kopf = 8×8×8, y nach unten, vorne = -z)
	// Dieser Block ist in HutLayer (Mod) und HutModelle (Launcher) identisch – bei Änderungen beide anpassen.

	/** Quader an einer Kopfseite: a = Position entlang der Seite, d = Tiefe nach innen (0 = außen bündig). */
	private static void seitenBox(Bauer b, int seite, float rand, float a1, float y1, float d1, float a2, float y2, float d2, int farbe) {
		float x1, z1, x2, z2;
		switch (seite) {
			case 0: x1 = a1; z1 = -rand + d1; x2 = a2; z2 = -rand + d2; break;      // vorne
			case 1: x1 = rand - d1; z1 = a1; x2 = rand - d2; z2 = a2; break;        // rechts
			case 2: x1 = -a1; z1 = rand - d1; x2 = -a2; z2 = rand - d2; break;      // hinten
			default: x1 = -rand + d1; z1 = -a1; x2 = -rand + d2; z2 = -a2; break;   // links
		}
		b.box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), farbe);
	}

	private static int mischeFarbe(int a, int c, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int r = Math.round(((a >> 16) & 255) * (1 - t) + ((c >> 16) & 255) * t);
		int g = Math.round(((a >> 8) & 255) * (1 - t) + ((c >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1 - t) + (c & 255) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	private static void krone(Bauer b, float t) {
		int gold = 0xFFFFC83D, hell = 0xFFFFE89A, dunkel = 0xFFB07A18, samt = 0xFF8A1030;
		b.box(-3.9f, -10.4f, -3.9f, 3.9f, -8.6f, 3.9f, samt);                 // Samtkappe
		b.box(-4.5f, -9.5f, -4.5f, 4.5f, -8.0f, 4.5f, gold);                  // Reif
		b.box(-4.6f, -8.5f, -4.6f, 4.6f, -8.0f, 4.6f, dunkel);                // untere Kante
		b.box(-4.6f, -9.7f, -4.6f, 4.6f, -9.4f, 4.6f, hell);                  // obere Kante (Glanz)
		float[] lage = {-3.2f, 0f, 3.2f};
		for (int seite = 0; seite < 4; seite++) {
			for (int i = 0; i < 3; i++) {
				float a = lage[i];
				float spitze = i == 1 ? -12.6f : -11.6f;
				seitenBox(b, seite, 4.5f, a - 0.6f, -10.5f, 0f, a + 0.6f, -9.6f, 0.7f, gold);
				seitenBox(b, seite, 4.5f, a - 0.4f, spitze + 0.8f, 0.1f, a + 0.4f, -10.5f, 0.6f, hell);
				b.hell(true);
				seitenBox(b, seite, 4.5f, a - 0.32f, spitze, 0.05f, a + 0.32f, spitze + 0.8f, 0.65f, 0xFFFFF6C8);
				b.hell(false);
			}
		}
		// Edelsteine im Reif, funkeln nacheinander
		int[] steine = {0xFFFF2B5E, 0xFF00E5FF, 0xFF3DFF8A, 0xFFB06BFF};
		b.hell(true);
		for (int seite = 0; seite < 4; seite++) {
			float funkeln = 0.75f + 0.25f * (float) Math.sin(t * 0.15f + seite * 1.6f);
			int f = mischeFarbe(0xFF000000, steine[seite], funkeln);
			seitenBox(b, seite, 4.5f, -0.75f, -9.2f, -0.25f, 0.75f, -8.3f, 0f, f);
			seitenBox(b, seite, 4.5f, -3.0f, -9.0f, -0.15f, -2.4f, -8.5f, 0f, 0xFFFFFFFF);
			seitenBox(b, seite, 4.5f, 2.4f, -9.0f, -0.15f, 3.0f, -8.5f, 0f, 0xFFFFFFFF);
		}
		b.hell(false);
	}

	private static void zylinder(Bauer b, float t) {
		int schwarz = 0xFF14161C, kante = 0xFF262A34;
		b.box(-5.8f, -8.5f, -5.8f, 5.8f, -8.0f, 5.8f, schwarz);               // Krempe
		b.box(-5.8f, -9.0f, -5.8f, -5.2f, -8.5f, 5.8f, kante);                // Krempe seitlich hochgebogen
		b.box(5.2f, -9.0f, -5.8f, 5.8f, -8.5f, 5.8f, kante);
		b.box(-3.8f, -15.0f, -3.8f, 3.8f, -8.5f, 3.8f, schwarz);              // Hut
		b.box(-3.95f, -15.4f, -3.95f, 3.95f, -15.0f, 3.95f, kante);           // Deckel
		float puls = 0.5f + 0.5f * (float) Math.sin(t * 0.12f);
		b.hell(true);
		b.box(-3.9f, -10.3f, -3.9f, 3.9f, -8.6f, 3.9f, mischeFarbe(0xFF3DFF8A, 0xFF00E5FF, puls));   // Neonband
		b.hell(false);
		b.box(-1.0f, -10.5f, -4.05f, 1.0f, -8.4f, -3.9f, 0xFFFFC83D);         // Schnalle
		b.box(-0.5f, -10.0f, -4.1f, 0.5f, -8.9f, -4.05f, schwarz);
	}

	private static void heiligenschein(Bauer b, float t) {
		int n = 32;
		float dreh = t * 0.04f;
		float hoehe = -12.6f + 0.35f * (float) Math.sin(t * 0.08f);
		b.hell(true);
		for (int i = 0; i < n; i++) {
			float w = dreh + i * (float) (Math.PI * 2) / n;
			float x = (float) Math.cos(w) * 4.8f, z = (float) Math.sin(w) * 4.8f;
			float glanz = 0.5f + 0.5f * (float) Math.sin(w * 2 - t * 0.2f);
			b.box(x - 0.55f, hoehe - 0.3f, z - 0.55f, x + 0.55f, hoehe + 0.3f, z + 0.55f, mischeFarbe(0xFFFFD54A, 0xFFFFFBE0, glanz));
		}
		for (int i = 0; i < 3; i++) {   // kleine Funken, die schneller kreisen
			float w = -t * 0.12f + i * 2.094f;
			float x = (float) Math.cos(w) * 4.8f, z = (float) Math.sin(w) * 4.8f;
			b.box(x - 0.3f, hoehe - 1.0f, z - 0.3f, x + 0.3f, hoehe - 0.4f, z + 0.3f, 0xFFFFFFFF);
		}
		b.hell(false);
	}

	private static void hoerner(Bauer b, float t) {
		int basis = 0xFFB8243C, spitze = 0xFF2A0810;
		for (int s = -1; s <= 1; s += 2) {
			float x = s * 2.7f, y = -8.0f, z = -0.6f;
			for (int k = 0; k < 7; k++) {
				float g = 1.15f - k * 0.12f;
				int farbe = mischeFarbe(basis, spitze, k / 6f);
				if (k % 2 == 1) {
					farbe = mischeFarbe(farbe, 0xFF000000, 0.18f);
				}
				b.box(x - g, y - g * 1.1f, z - g, x + g, y, z + g, farbe);
				x += s * (0.55f - k * 0.05f);
				y -= 0.85f - k * 0.04f;
				z += 0.12f + k * 0.07f;
			}
		}
	}

	private static void katzenohren(Bauer b, float t) {
		int aussen = 0xFF2A2230, innen = 0xFFFF9BD6, spitze = 0xFFF2E8F6;
		float zucken = (float) Math.max(0, Math.sin(t * 0.05f) - 0.92f) * 6f;   // ab und zu zuckt ein Ohr
		for (int s = -1; s <= 1; s += 2) {
			float x = s * 2.6f;
			float neigung = s == 1 ? zucken : 0;
			for (int k = 0; k < 5; k++) {
				float br = 1.7f - k * 0.32f;
				float xs = x + s * k * (0.12f + neigung * 0.1f);
				b.box(xs - br, -8.6f - (k + 1) * 0.6f, -0.6f, xs + br, -8.0f - k * 0.6f, 0.6f, aussen);
				if (k < 4) {
					float bi = br - 0.55f;
					b.box(xs - bi, -8.6f - (k + 1) * 0.6f, -0.7f, xs + bi, -8.0f - k * 0.6f, -0.6f, innen);
				}
			}
			float xt = x + s * 4 * (0.12f + neigung * 0.1f);
			b.box(xt - 0.25f, -11.8f, -0.4f, xt + 0.25f, -11.2f, 0.4f, spitze);
		}
	}

	private static void partyhut(Bauer b, float t) {
		int[] farben = {0xFFB06BFF, 0xFFFFD54A, 0xFF00E5FF, 0xFFFF2BD6, 0xFF3DFF8A};
		int lauf = (int) Math.floor(t * 0.15f);
		for (int i = 0; i < 10; i++) {
			float r = 3.2f - i * 0.29f;
			b.box(-r, -8.6f - i * 0.65f, -r, r, -8.0f - i * 0.65f, r, farben[(i + lauf) % farben.length]);
		}
		b.hell(true);
		float y = -15.1f;
		b.box(-0.7f, y - 1.2f, -0.7f, 0.7f, y, 0.7f, 0xFFFFFFFF);            // Bommel
		b.box(-1.0f, y - 0.9f, -0.4f, 1.0f, y - 0.3f, 0.4f, 0xFFFFE0F4);
		b.box(-0.4f, y - 0.9f, -1.0f, 0.4f, y - 0.3f, 1.0f, 0xFFFFE0F4);
		b.hell(false);
		b.box(-3.3f, -8.2f, -3.3f, 3.3f, -8.0f, 3.3f, 0xFFFFFFFF);           // Rand
	}

	private static void bandana(Bauer b, float t) {
		int rot = 0xFFE0245E, hell = 0xFFFF8FB3, dunkel = 0xFFA0163F;
		b.box(-4.3f, -7.0f, -4.3f, 4.3f, -5.4f, 4.3f, rot);
		b.box(-4.35f, -5.6f, -4.35f, 4.35f, -5.4f, 4.35f, dunkel);
		for (int seite = 0; seite < 4; seite++) {   // Punktmuster rundum
			for (int i = -1; i <= 1; i++) {
				seitenBox(b, seite, 4.3f, i * 2.4f - 0.35f, -6.5f, -0.06f, i * 2.4f + 0.35f, -5.9f, 0f, hell);
			}
		}
		b.box(-0.9f, -6.9f, 4.3f, 0.9f, -5.5f, 5.1f, dunkel);                // Knoten
		for (int s = -1; s <= 1; s += 2) {   // zwei Enden, die im Wind flattern
			float x = s * 0.9f, y = -5.6f, z = 4.7f;
			for (int k = 0; k < 4; k++) {
				float wehen = (float) Math.sin(t * 0.25f + k * 0.9f + s) * 0.18f * (k + 1);
				b.box(x - 0.6f, y, z, x + 0.6f, y + 0.9f, z + 0.35f, k % 2 == 0 ? rot : dunkel);
				x += s * 0.15f + wehen;
				y += 0.85f;
				z += 0.2f;
			}
		}
	}

	private static void visier(Bauer b, float t) {
		int rahmen = 0xFF101824;
		float p = 0.6f + 0.4f * (float) Math.sin(t * 0.15f);
		b.box(-4.6f, -5.6f, -4.7f, 4.6f, -4.2f, -4.3f, rahmen);
		b.box(-4.7f, -5.6f, -4.3f, -4.3f, -4.2f, 2.0f, rahmen);
		b.box(4.3f, -5.6f, -4.3f, 4.7f, -4.2f, 2.0f, rahmen);
		b.hell(true);
		b.box(-4.1f, -5.35f, -4.78f, 4.1f, -4.45f, -4.7f, mischeFarbe(0xFF003A55, 0xFF00E5FF, p));
		float scan = (float) Math.sin(t * 0.12f) * 3.4f;                      // Scan-Balken läuft hin und her
		b.box(scan - 0.45f, -5.35f, -4.84f, scan + 0.45f, -4.45f, -4.78f, 0xFFE8FFFF);
		b.box(-4.75f, -5.2f, -3.0f, -4.68f, -4.6f, -2.2f, mischeFarbe(0xFF3DFF8A, 0xFFFF2BD6, p));
		b.box(4.68f, -5.2f, -3.0f, 4.75f, -4.6f, -2.2f, mischeFarbe(0xFF3DFF8A, 0xFFFF2BD6, p));
		b.hell(false);
	}

	// ----------------------------------------------------------------- Quader zeichnen

	private static final class Bauer {
		private final PoseStack.Pose pose;
		private final VertexConsumer vc;
		private final int licht;
		private boolean leuchtet;

		Bauer(PoseStack.Pose pose, VertexConsumer vc, int licht) {
			this.pose = pose;
			this.vc = vc;
			this.licht = licht;
		}

		void hell(boolean an) {
			leuchtet = an;
		}

		/** Quader von (x1,y1,z1) bis (x2,y2,z2) in Kopf-Pixeln. */
		void box(float x1, float y1, float z1, float x2, float y2, float z2, int farbe) {
			float a = x1 / 16f, b = y1 / 16f, c = z1 / 16f, d = x2 / 16f, e = y2 / 16f, f = z2 / 16f;
			int l = leuchtet ? VOLL_HELL : licht;
			int dunkel = schatten(farbe, 0.75f), seite = schatten(farbe, 0.88f);
			flaeche(a, b, c, d, b, c, d, b, f, a, b, f, farbe, l, 0, -1, 0);   // oben
			flaeche(a, e, f, d, e, f, d, e, c, a, e, c, dunkel, l, 0, 1, 0);   // unten
			flaeche(a, b, c, a, e, c, d, e, c, d, b, c, seite, l, 0, 0, -1);   // vorne
			flaeche(d, b, f, d, e, f, a, e, f, a, b, f, seite, l, 0, 0, 1);    // hinten
			flaeche(a, b, f, a, e, f, a, e, c, a, b, c, dunkel, l, -1, 0, 0);  // links
			flaeche(d, b, c, d, e, c, d, e, f, d, b, f, dunkel, l, 1, 0, 0);   // rechts
		}

		private void flaeche(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
							 float x4, float y4, float z4, int farbe, int l, float nx, float ny, float nz) {
			ecke(x1, y1, z1, 0, 0, farbe, l, nx, ny, nz);
			ecke(x2, y2, z2, 0, 1, farbe, l, nx, ny, nz);
			ecke(x3, y3, z3, 1, 1, farbe, l, nx, ny, nz);
			ecke(x4, y4, z4, 1, 0, farbe, l, nx, ny, nz);
		}

		private void ecke(float x, float y, float z, float u, float v, int farbe, int l, float nx, float ny, float nz) {
			vc.addVertex(pose, x, y, z).setColor(farbe).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(l).setNormal(pose, nx, ny, nz);
		}

		private static int schatten(int farbe, float f) {
			int r = (int) (((farbe >> 16) & 255) * f), g = (int) (((farbe >> 8) & 255) * f), bl = (int) ((farbe & 255) * f);
			return (farbe & 0xFF000000) | (r << 16) | (g << 8) | bl;
		}
	}
}
