package de.bambus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.ArrayList;
import java.util.List;

/**
 * Sammelt Vierecke (Position, UV, Farbe, Normale), die erst später in submitCustomGeometry gezeichnet werden.
 * So wird alles einmal pro Bild berechnet und danach nur noch ausgegeben.
 */
final class Geometrie {

	/** Ein Viereck: 4 Ecken (x,y,z), 4 UVs, eine Normale, Farbe und Licht. */
	record Viereck(float[] pos, float[] uv, float nx, float ny, float nz, int farbe, int licht) {}

	private final List<Viereck> liste = new ArrayList<>();

	void add(float[] pos, float[] uv, float[] normale, int farbe, int licht) {
		liste.add(new Viereck(pos, uv, normale[0], normale[1], normale[2], farbe, licht));
	}

	boolean leer() {
		return liste.isEmpty();
	}

	void zeichne(PoseStack.Pose pose, VertexConsumer vc) {
		for (Viereck v : liste) {
			for (int i = 0; i < 4; i++) {
				vc.addVertex(pose, v.pos[i * 3], v.pos[i * 3 + 1], v.pos[i * 3 + 2])
						.setColor(v.farbe)
						.setUv(v.uv[i * 2], v.uv[i * 2 + 1])
						.setOverlay(OverlayTexture.NO_OVERLAY)
						.setLight(v.licht)
						.setNormal(pose, v.nx, v.ny, v.nz);
			}
		}
	}

	/** Graustufen-Farbe (für Glanz und Schattierung über die Vertex-Farbe). */
	static int grau(float hell) {
		int g = Math.max(0, Math.min(255, Math.round(hell * 255)));
		return 0xFF000000 | (g << 16) | (g << 8) | g;
	}

	/** Mischt zwei ARGB-Farben (t = 0 → a, t = 1 → b). */
	static int mische(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}
}
