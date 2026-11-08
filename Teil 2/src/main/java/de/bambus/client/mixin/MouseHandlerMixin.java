package de.bambus.client.mixin;

import de.bambus.client.Funktionen;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scroll-Zoom: Beim Zoomen verändert das Mausrad die Zoomstufe statt den Hotbar-Platz. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void bambus$scrollZoom(long fenster, double horizontal, double vertikal, CallbackInfo ci) {
		if (Funktionen.scroll(vertikal)) {
			ci.cancel();
		}
	}
}
