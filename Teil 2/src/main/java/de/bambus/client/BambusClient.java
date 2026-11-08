package de.bambus.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.hud.*;
import de.bambus.client.screen.BambusMenuScreen;
import de.bambus.client.screen.BambusTitelScreen;
import de.bambus.client.screen.HudEditorScreen;
import de.bambus.client.screen.Neon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import de.bambus.client.render.CapeLayer;
import de.bambus.client.render.FluegelLayer;
import de.bambus.client.render.HutLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Bambus Client – ein leichter PvP-/HUD-Client für Minecraft 1.21.11 (Fabric).
 *
 * Tasten (änderbar unter Optionen → Steuerung):
 *  Rechts-Shift  Mod-Menü
 *  C             Zoom (mit Mausrad näher/weiter)
 *  Linke Alt     Freelook
 *  H             HUD an/aus
 */
public class BambusClient implements ClientModInitializer {

	public static final String MOD_ID = "bambus";
	public static final String VERSION = "3.3.0";
	private static final String FENSTERTITEL = "Bambus Client " + VERSION + " – Minecraft 1.21.11";

	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Bambus");

	public static BambusConfig config = new BambusConfig();
	private static int screenshotIn = 0;

	public static final List<HudModul> MODULE = List.of(
			new InfoModul(),
			new TastenModul(),
			new ServerModul(),
			new StatusModul(),
			new RuestungModul(),
			new EffekteModul(),
			new ZielModul(),
			new ComboModul(),
			new ReachModul(),
			new TapModul(),
			new KillModul(),
			new PfeileModul(),
			new CooldownModul(),
			new WeltModul(WeltModul.Art.RICHTUNG),
			new WeltModul(WeltModul.Art.BIOM),
			new WeltModul(WeltModul.Art.TEMPO),
			new WeltModul(WeltModul.Art.UHR),
			new WeltModul(WeltModul.Art.SPEICHER),
			new TntModul()
	);

	private static KeyMapping menuTaste;
	private static KeyMapping zoomTaste;
	private static KeyMapping freelookTaste;
	private static KeyMapping hudTaste;

	private static boolean ersterTick = true;

	@Override
	public void onInitializeClient() {
		config = BambusConfig.laden();

		KeyMapping.Category kategorie = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "bambus"));
		menuTaste = taste("menu", InputConstants.KEY_RSHIFT, kategorie);
		zoomTaste = taste("zoom", InputConstants.KEY_C, kategorie);
		freelookTaste = taste("freelook", InputConstants.KEY_LALT, kategorie);
		hudTaste = taste("hud", InputConstants.KEY_H, kategorie);

		ClientTickEvents.END_CLIENT_TICK.register(BambusClient::tick);
		Statistik.laden();
		Kampf.killHoerer = (opfer, spieler) -> {
			Kosmetik.killEffekt(opfer);
			if (spieler && config.autoScreenshot) {
				screenshotIn = 6; // ein paar Ticks warten, damit der Kill-Effekt mit drauf ist
			}
		};
		Anzeigen.registrieren();
		Ladebildschirm.registrieren();
		// Eigene Treffer auswerten (Combo, Reach, Target-HUD, Kills) – nur lesen, nichts verändern
		AttackEntityCallback.EVENT.register((spieler, welt, hand, entity, treffer) -> {
			if (welt.isClientSide()) {
				Kampf.angriff(spieler, entity);
			}
			return InteractionResult.PASS;
		});
		// Eigene Capes/Flügel aus config/bambus-kosmetik/ laden, sobald die Texturen bereit sind
		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
			EigeneKosmetik.neuLaden();
			Hinweise.versionPruefen();
		});

		// Flügel als zusätzliche Ebene am Spieler-Modell
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register(BambusClient::registriereFluegel);

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
				BambusClient::zeichneHud);

		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (g, delta) -> {
			if (config.fadenkreuzAn) {
				Fadenkreuz.zeichne(g, Minecraft.getInstance());
			} else {
				vanilla.render(g, delta);
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, breite, hoehe) ->
				ScreenEvents.afterRender(screen).register((s, grafik, mausX, mausY, d) -> Hinweise.zeichne(grafik, client)));
		ScreenEvents.AFTER_INIT.register((client, screen, breite, hoehe) -> {
			if (screen instanceof TitleScreen) {
				Screens.getButtons(screen).add(Button.builder(Component.literal("§bBAMBUS"),
								b -> client.setScreen(new BambusMenuScreen(screen)))
						.bounds(breite - 84, 6, 78, 20).build());
				ScreenEvents.afterRender(screen).register(
						(s, grafik, mausX, mausY, d) -> zeichneTitelLogo(grafik));
			}
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void registriereFluegel(EntityType<? extends LivingEntity> typ, LivingEntityRenderer<?, ?, ?> renderer,
										   LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helfer,
										   EntityRendererProvider.Context kontext) {
		if (renderer instanceof AvatarRenderer) {
			helfer.register(new CapeLayer((AvatarRenderer) renderer));
			helfer.register(new FluegelLayer((AvatarRenderer) renderer));
			helfer.register(new HutLayer((AvatarRenderer) renderer));
		}
	}

	private static KeyMapping taste(String name, int taste, KeyMapping.Category kategorie) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.bambus." + name, InputConstants.Type.KEYSYM, taste, kategorie));
	}

	// ------------------------------------------------------------------ Tick

	private static void tick(Minecraft mc) {
		mc.getWindow().setTitle(FENSTERTITEL);

		// Eigenes Bambus-Hauptmenü statt des Minecraft-Titelbildschirms
		if (mc.screen instanceof TitleScreen && config.bambusHauptmenue && !BambusTitelScreen.vanillaGewuenscht) {
			mc.setScreen(new BambusTitelScreen());
		}

		if (ersterTick) {
			ersterTick = false;
			if (config.toggleSprint || config.toggleSneak) {
				Funktionen.sprintUndSchleichenAnwenden(mc);
			}
			// Low-End-Modus kann auch im Launcher geschaltet werden
			if (config.lowEnd != (config.lowEndAlt != null)) {
				Visuals.lowEnd(mc, config.lowEnd);
			}
		}

		while (menuTaste.consumeClick()) {
			if (mc.screen == null) {
				mc.setScreen(new BambusMenuScreen(null));
			}
		}

		while (hudTaste.consumeClick()) {
			config.hudAn = !config.hudAn;
			config.speichern();
			Hinweise.zeige("hud", "Bambus-HUD", config.hudAn ? "eingeschaltet" : "ausgeschaltet", Farben.NEON);
			if (mc.player != null) {
				mc.player.displayClientMessage(
						Component.literal("§aBambus-HUD: " + (config.hudAn ? "an" : "aus")), true);
			}
		}

		Funktionen.zoomTick(mc, zoomTaste.isDown());
		Funktionen.freelookTick(mc, freelookTaste.isDown());
		Funktionen.fullbrightTick(mc);
		Kosmetik.tick(mc);
		Kampf.tick(mc);
		Visuals.tick(mc);
		WeltModul.tick(mc);
		Statistik.tick(mc.level != null);
		DiscordStatus.tick(mc);
		Hinweise.tick(mc);
		if (screenshotIn > 0 && --screenshotIn == 0 && mc.player != null) {
			net.minecraft.client.Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), nachricht -> {
				mc.player.displayClientMessage(nachricht, false);
				Hinweise.zeige("kamera", "Screenshot gespeichert", "Kill-Screenshot im Ordner screenshots", Farben.NEON_CYAN);
			});
		}
	}

	// ------------------------------------------------------------------- HUD

	private static void zeichneHud(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		Farben.themaAnwenden(config.thema);
		Funktionen.zaehleKlicks(mc);

		if (mc.screen == null && !mc.options.hideGui) {
			Hinweise.zeichne(g, mc);   // über Menüs zeichnet sie ScreenEvents.afterRender
		}
		if (!config.hudAn || mc.player == null || mc.options.hideGui || mc.screen instanceof HudEditorScreen) {
			return;
		}
		for (HudModul m : MODULE) {
			if (m.pos().an) {
				m.zeichneAnPosition(g, mc, false);
			}
		}
	}

	// ---------------------------------------------------------- Titelbildschirm

	private static void zeichneTitelLogo(GuiGraphics g) {
		Font font = Minecraft.getInstance().font;
		g.fill(4, 4, 150, 42, 0xA0050A10);
		Neon.ecken(g, 4, 4, 146, 38, 8, Farben.NEON_CYAN);
		Neon.logo(g, 12, 11, 24);
		Neon.leuchtText(g, font, "BAMBUS", 30, 11, 2.0f, Farben.NEON);
		Neon.text(g, font, "CLIENT  v" + VERSION, 31, 30, 0.75f, Farben.NEON_CYAN, false);
	}
}
