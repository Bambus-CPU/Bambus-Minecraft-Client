package de.bambus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Kosmetik: Capes, Flügel, Hüte, Partikelspuren, Kill-Effekte und Pfeil-Spuren.
 * Alles nur auf deinem Bildschirm sichtbar – andere Spieler sehen es nicht.
 */
public final class Kosmetik {

	private Kosmetik() {}

	public record Cape(String id, String name, int farbe) {}

	public record Fluegel(String id, String name, int farbe) {}

	/**
	 * Form eines 3D-Flügels (passt zu werkzeuge/fluegel_3d.py): Vorderkante im 128×64-Raster der Textur,
	 * bis wohin der 3D-Knochen reicht, seine Dicke (0 = kein Knochen), Knochenfarben und ob er leuchtet.
	 */
	public record FluegelForm(float[][] kante, float knochenBis, float dicke, int knochenHell, int knochenDunkel,
							  boolean leuchtet) {}

	private static final float[][] FEDER_KANTE = {{0, 12}, {16, 6}, {36, 3}, {58, 3}, {80, 6}, {96, 10}, {106, 14}, {121, 25}};
	private static final float[][] DRACHE_KANTE = {{0, 13}, {20, 7}, {44, 4.5f}, {125, 17}};
	private static final float[][] FALTER_KANTE = {{1, 12}, {31, 6.9f}, {61, 4.85f}, {92, 7.7f}, {122, 15.5f}};

	private static final Map<String, FluegelForm> FORMEN = Map.ofEntries(
			Map.entry("engel", new FluegelForm(FEDER_KANTE, 104, 2.3f, 0xFFFFD66E, 0xFF965C19, false)),
			Map.entry("drache", new FluegelForm(DRACHE_KANTE, 125, 2.4f, 0xFF786C84, 0xFF16101C, false)),
			Map.entry("cyber", new FluegelForm(DRACHE_KANTE, 125, 2.4f, 0xFF3DFF8A, 0xFF0A2A18, true)),
			Map.entry("schmetterling", new FluegelForm(FALTER_KANTE, 0, 0, 0xFF3A2A1A, 0xFF120C08, false)),
			Map.entry("phoenix", new FluegelForm(FEDER_KANTE, 104, 2.3f, 0xFFFFFAD7, 0xFFEB5F0F, true)),
			Map.entry("seraph", new FluegelForm(FEDER_KANTE, 104, 2.3f, 0xFFFFF0A8, 0xFFB9811F, false)),
			Map.entry("nachtschwinge", new FluegelForm(FEDER_KANTE, 104, 2.3f, 0xFF6A5A78, 0xFF140C1C, false)),
			Map.entry("kristall", new FluegelForm(FEDER_KANTE, 104, 2.3f, 0xFFFFFFFF, 0xFF3A7FB8, true)),
			Map.entry("aurora", new FluegelForm(FEDER_KANTE, 104, 2.3f, 0xFFC9FFF0, 0xFF1B6F8A, true)),
			Map.entry("sakura", new FluegelForm(FEDER_KANTE, 106, 2.5f, 0xFF7A4A36, 0xFF3A2016, false)),
			Map.entry("bambus", new FluegelForm(FEDER_KANTE, 106, 2.8f, 0xFFAACD5F, 0xFF325816, false))
	);

	/** Form für eingebaute 3D-Flügel – null bei selbst erstellten Flügeln (altes Format). */
	public static FluegelForm fluegelForm(String id) {
		return istEigen(id) ? null : FORMEN.get(id);
	}

	/** Ebene eines eingebauten 3D-Flügels: "hinten" (Schwungfedern) oder "vorne" (Deckfedern, Knochen). */
	public static Identifier fluegelEbene(String id, String ebene) {
		return Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "textures/fluegel/" + id + "_" + ebene + ".png");
	}

	/**
	 * Animations-Ebene der eingebauten Umhänge: {Pixel pro Sekunde in x, in y, Deckkraft}.
	 * Das Bild textures/cape/&lt;id&gt;_anim.png ist nahtlos kachelbar (200×320) und wird verschoben.
	 */
	private static final Map<String, float[]> CAPE_ANIMATION = Map.of(
			"bambus", new float[]{0, -14, 0.9f},
			"neon", new float[]{0, 60, 0.8f},
			"cyber", new float[]{0, 45, 0.75f},
			"galaxie", new float[]{6, 0, 0.9f},
			"sunset", new float[]{8, 0, 0.9f},
			"matrix", new float[]{0, 90, 0.95f},
			"polarlicht", new float[]{16, 0, 0.75f},
			"glut", new float[]{0, -46, 0.85f}
	);

	public static float[] capeAnimation(String id) {
		return istEigen(id) ? null : CAPE_ANIMATION.get(id);
	}

	public static Identifier capeAnimBild(String id) {
		return Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "textures/cape/" + id + "_anim.png");
	}

	/** Id des gewählten Umhangs – oder null, wenn keiner ausgewählt ist. */
	public static String gewaehlteCapeId() {
		BambusConfig c = BambusClient.config;
		if (!c.capeAn || c.capeDesign == null || AUS.equals(c.capeDesign)) {
			return null;
		}
		if (istEigen(c.capeDesign)) {
			return EigeneKosmetik.cape(c.capeDesign) != null ? c.capeDesign : null;
		}
		return CAPES.stream().anyMatch(k -> k.id().equals(c.capeDesign)) ? c.capeDesign : CAPES.get(0).id();
	}

	public record Partikel(String id, String name, int farbe, SimpleParticleType typ, boolean ueberKopf) {}

	public record Hut(String id, String name, int farbe) {}

	/** Kill-Effekt oder Pfeil-Spur: Name, Farbe, Partikel. */
	public record Effekt(String id, String name, int farbe, SimpleParticleType typ) {}

	public static final String AUS = "aus";

	public static final List<Hut> HUETE = List.of(
			new Hut("krone", "Krone", 0xFFFFD54A),
			new Hut("zylinder", "Zylinder", 0xFF3DFF8A),
			new Hut("heiligenschein", "Heiligenschein", 0xFFFFF3A0),
			new Hut("hoerner", "Hörner", 0xFFFF4D6D),
			new Hut("katzenohren", "Katzenohren", 0xFFFF9BD6),
			new Hut("partyhut", "Partyhut", 0xFFB06BFF),
			new Hut("bandana", "Bandana", 0xFFFF2BD6),
			new Hut("visier", "Neon-Visier", 0xFF00E5FF)
	);

	public static final List<Effekt> KILL_EFFEKTE = List.of(
			new Effekt("blitz", "Neon-Blitz", 0xFF00E5FF, ParticleTypes.ELECTRIC_SPARK),
			new Effekt("feuerwerk", "Feuerwerk", 0xFFFFD54A, ParticleTypes.FIREWORK),
			new Effekt("seelen", "Seelen", 0xFF00E5FF, ParticleTypes.SOUL),
			new Effekt("flammen", "Flammen", 0xFFFF8A3D, ParticleTypes.FLAME),
			new Effekt("herzen", "Herzen", 0xFFFF4D6D, ParticleTypes.HEART),
			new Effekt("totem", "Totem", 0xFF3DFF8A, ParticleTypes.TOTEM_OF_UNDYING)
	);

	public static final List<Effekt> SPUREN = List.of(
			new Effekt("funken", "Neon-Funken", 0xFF00E5FF, ParticleTypes.ELECTRIC_SPARK),
			new Effekt("glitzer", "Sternenstaub", 0xFFFFFFFF, ParticleTypes.END_ROD),
			new Effekt("flammen", "Flammen", 0xFFFF8A3D, ParticleTypes.FLAME),
			new Effekt("seelen", "Seelenfeuer", 0xFF00E5FF, ParticleTypes.SOUL_FIRE_FLAME),
			new Effekt("noten", "Musik", 0xFF3DFF8A, ParticleTypes.NOTE),
			new Effekt("magie", "Magie", 0xFFB06BFF, ParticleTypes.WITCH),
			new Effekt("herzen", "Herzen", 0xFFFF4D6D, ParticleTypes.HEART)
	);

	public static Hut gewaehlterHut() {
		String id = BambusClient.config.hut;
		return HUETE.stream().filter(h -> h.id().equals(id)).findFirst().orElse(null);
	}

	public static Effekt effekt(List<Effekt> liste, String id) {
		return liste.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
	}

	/**
	 * Kill-Effekt an der Stelle, an der dein Gegner besiegt wurde (nur bei dir sichtbar).
	 * Seit 3.3 in Formen: ein Ring breitet sich am Boden aus, eine Doppelspirale steigt auf,
	 * in der Mitte blitzt es. Blitz und Seelen bilden zusätzlich eine Lichtsäule.
	 */
	public static void killEffekt(net.minecraft.world.entity.LivingEntity opfer) {
		Minecraft mc = Minecraft.getInstance();
		Effekt e = effekt(KILL_EFFEKTE, BambusClient.config.killEffekt);
		if (e == null || mc.level == null) {
			return;
		}
		double x = opfer.getX(), y = opfer.getY(), z = opfer.getZ();
		// Ring am Boden
		for (int i = 0; i < 40; i++) {
			double w = i * Math.PI * 2 / 40;
			mc.level.addParticle(e.typ(), x + Math.cos(w) * 0.4, y + 0.15, z + Math.sin(w) * 0.4,
					Math.cos(w) * 0.32, 0.01, Math.sin(w) * 0.32);
		}
		// Doppelspirale nach oben
		for (int i = 0; i < 48; i++) {
			double h = i / 48.0;
			for (int strang = 0; strang < 2; strang++) {
				double w = h * Math.PI * 5 + strang * Math.PI;
				double r = 0.65 - h * 0.35;
				mc.level.addParticle(e.typ(), x + Math.cos(w) * r, y + h * 2.4, z + Math.sin(w) * r, 0, 0.02, 0);
			}
		}
		// Blitz in der Mitte
		for (int i = 0; i < 24; i++) {
			double w = ZUFALL.nextDouble() * Math.PI * 2, v = 0.1 + ZUFALL.nextDouble() * 0.2;
			mc.level.addParticle(e.typ(), x, y + 1.0, z, Math.cos(w) * v, (ZUFALL.nextDouble() - 0.3) * 0.3, Math.sin(w) * v);
		}
		if (e.id().equals("blitz") || e.id().equals("seelen")) {
			for (int i = 0; i < 40; i++) {
				mc.level.addParticle(e.typ(), x + (ZUFALL.nextDouble() - 0.5) * 0.2, y + i * 0.12, z + (ZUFALL.nextDouble() - 0.5) * 0.2,
						0, 0.04, 0);
			}
		}
	}

	/** Spur hinter deinen eigenen Pfeilen, Dreizacken, Schneebällen und Perlen – seit 3.3 als Spirale. */
	private static void spuren(Minecraft mc) {
		Effekt e = effekt(SPUREN, BambusClient.config.spur);
		if (e == null || mc.level == null || mc.player == null) {
			return;
		}
		for (net.minecraft.world.entity.Entity ent : mc.level.entitiesForRendering()) {
			if (ent instanceof net.minecraft.world.entity.projectile.Projectile pr && pr.getOwner() == mc.player
					&& pr.getDeltaMovement().lengthSqr() > 0.01) {
				net.minecraft.world.phys.Vec3 v = pr.getDeltaMovement().normalize();
				// zwei Achsen senkrecht zur Flugrichtung
				net.minecraft.world.phys.Vec3 a = Math.abs(v.y) < 0.9 ? v.cross(new net.minecraft.world.phys.Vec3(0, 1, 0)).normalize()
						: v.cross(new net.minecraft.world.phys.Vec3(1, 0, 0)).normalize();
				net.minecraft.world.phys.Vec3 b = v.cross(a);
				double w = pr.tickCount * 0.9;
				for (int strang = 0; strang < 2; strang++) {
					double ww = w + strang * Math.PI;
					double ox = (a.x * Math.cos(ww) + b.x * Math.sin(ww)) * 0.18;
					double oy = (a.y * Math.cos(ww) + b.y * Math.sin(ww)) * 0.18;
					double oz = (a.z * Math.cos(ww) + b.z * Math.sin(ww)) * 0.18;
					mc.level.addParticle(e.typ(), pr.getX() + ox, pr.getY() + oy, pr.getZ() + oz, 0, 0, 0);
				}
			}
		}
	}

	public static final List<Cape> CAPES = List.of(
			new Cape("bambus", "Bambus", 0xFF3DFF8A),
			new Cape("neon", "Neon-Platine", 0xFF3DFF8A),
			new Cape("cyber", "Cyber", 0xFF00E5FF),
			new Cape("galaxie", "Galaxie", 0xFFB06BFF),
			new Cape("sunset", "Synthwave", 0xFFFF2BD6),
			new Cape("matrix", "Matrix", 0xFF39FF14),
			new Cape("polarlicht", "Polarlicht", 0xFF5AFFC0),
			new Cape("glut", "Glut", 0xFFFF8A3D)
	);

	public static final List<Fluegel> FLUEGEL = List.of(
			new Fluegel("engel", "Engel", 0xFF00E5FF),
			new Fluegel("drache", "Drache", 0xFFFF2BD6),
			new Fluegel("cyber", "Cyber", 0xFF3DFF8A),
			new Fluegel("schmetterling", "Schmetterling", 0xFFFFD54A),
			new Fluegel("phoenix", "Phönix", 0xFFFF8A3D),
			new Fluegel("seraph", "Seraph", 0xFFFFC846),
			new Fluegel("nachtschwinge", "Nachtschwinge", 0xFFB040FF),
			new Fluegel("kristall", "Kristall", 0xFF5AE6FF),
			new Fluegel("aurora", "Aurora", 0xFF3DFFC8),
			new Fluegel("sakura", "Sakura", 0xFFFF8CC0),
			new Fluegel("bambus", "Bambus", 0xFF7BD65C)
	);

	/** Eingebaute + selbst erstellte Capes. */
	public static List<Cape> alleCapes() {
		List<Cape> alle = new ArrayList<>(CAPES);
		for (EigeneKosmetik.Eintrag e : EigeneKosmetik.capes()) {
			alle.add(new Cape(e.id(), e.name(), e.farbe()));
		}
		return alle;
	}

	/** Eingebaute + selbst erstellte Flügel. */
	public static List<Fluegel> alleFluegel() {
		List<Fluegel> alle = new ArrayList<>(FLUEGEL);
		for (EigeneKosmetik.Eintrag e : EigeneKosmetik.fluegel()) {
			alle.add(new Fluegel(e.id(), e.name(), e.farbe()));
		}
		return alle;
	}

	public static boolean istEigen(String id) {
		return id != null && id.startsWith(EigeneKosmetik.PRAEFIX);
	}

	public static final List<Partikel> PARTIKEL = List.of(
			new Partikel("funken", "Neon-Funken", 0xFF00E5FF, ParticleTypes.ELECTRIC_SPARK, false),
			new Partikel("glitzer", "Sternenstaub", 0xFFFFFFFF, ParticleTypes.END_ROD, false),
			new Partikel("herzen", "Herzen", 0xFFFF4D6D, ParticleTypes.HEART, true),
			new Partikel("noten", "Musik", 0xFF3DFF8A, ParticleTypes.NOTE, true),
			new Partikel("seelen", "Seelenfeuer", 0xFF00E5FF, ParticleTypes.SOUL_FIRE_FLAME, false),
			new Partikel("kirsche", "Kirschblüten", 0xFFFF9BD6, ParticleTypes.CHERRY_LEAVES, true),
			new Partikel("magie", "Magie", 0xFFB06BFF, ParticleTypes.ENCHANT, false),
			new Partikel("flammen", "Flammen", 0xFFFF8A3D, ParticleTypes.FLAME, false)
	);

	private static final Map<String, ClientAsset.Texture> TEXTUREN = new HashMap<>();
	private static final Random ZUFALL = new Random();

	public static Identifier capeBild(String id) {
		if (istEigen(id)) {
			EigeneKosmetik.Eintrag e = EigeneKosmetik.cape(id);
			return e != null ? e.bild() : capeBild("bambus");
		}
		return Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "textures/cape/" + id + ".png");
	}

	/** Das gewählte Cape – oder null, wenn keins ausgewählt ist. */
	public static ClientAsset.Texture gewaehltesCape() {
		BambusConfig c = BambusClient.config;
		if (!c.capeAn || c.capeDesign == null || AUS.equals(c.capeDesign)) {
			return null;
		}
		if (istEigen(c.capeDesign)) {
			EigeneKosmetik.Eintrag e = EigeneKosmetik.cape(c.capeDesign);
			if (e == null) {
				return null;
			}
			// Eigene Capes werden beim Neuladen neu registriert – darum nicht dauerhaft cachen
			return new ClientAsset.ResourceTexture(e.bild(), e.bild());
		}
		Cape cape = CAPES.stream().filter(k -> k.id().equals(c.capeDesign)).findFirst().orElse(CAPES.get(0));
		return TEXTUREN.computeIfAbsent(cape.id(), id -> new ClientAsset.ResourceTexture(
				Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "cape/" + id), capeBild(id)));
	}

	public static Identifier fluegelBild(String id) {
		if (istEigen(id)) {
			EigeneKosmetik.Eintrag e = EigeneKosmetik.fluegel(id);
			return e != null ? e.bild() : fluegelBild("engel");
		}
		return Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "textures/fluegel/" + id + ".png");
	}

	/** Die gewählten Flügel – oder null. */
	public static Fluegel gewaehlteFluegel() {
		String id = BambusClient.config.fluegel;
		if (id == null || AUS.equals(id)) {
			return null;
		}
		return alleFluegel().stream().filter(f -> f.id().equals(id)).findFirst().orElse(null);
	}

	public static Partikel gewaehltePartikel() {
		String id = BambusClient.config.partikel;
		if (id == null || AUS.equals(id)) {
			return null;
		}
		return PARTIKEL.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
	}

	/** Jeden Tick: Partikelspur um den eigenen Spieler und hinter eigenen Geschossen. */
	public static void tick(Minecraft mc) {
		if (!mc.isPaused()) {
			spuren(mc);
		}
		LocalPlayer p = mc.player;
		Partikel art = gewaehltePartikel();
		if (art == null || p == null || mc.level == null || p.isInvisible() || mc.isPaused()) {
			return;
		}
		// Seit 3.3: Partikel kreisen um dich (Ring an der Hüfte) bzw. als Kranz über dem Kopf
		double w = p.tickCount * 0.35;
		if (art.ueberKopf()) {
			if (p.tickCount % 2 != 0) {
				return;
			}
			double r = 0.45;
			mc.level.addParticle(art.typ(), p.getX() + Math.cos(w) * r, p.getY() + 2.15, p.getZ() + Math.sin(w) * r, 0, 0.0, 0);
		} else {
			double r = 0.75;
			double h = 0.9 + Math.sin(p.tickCount * 0.08) * 0.5;
			for (int strang = 0; strang < 2; strang++) {
				double ww = w + strang * Math.PI;
				mc.level.addParticle(art.typ(), p.getX() + Math.cos(ww) * r, p.getY() + h, p.getZ() + Math.sin(ww) * r,
						0, 0.01, 0);
			}
		}
	}
}
