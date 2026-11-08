package de.bambus.client.mixin;

import de.bambus.client.Visuals;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Eigene Tageszeit – nur auf deinem Bildschirm, der Server merkt davon nichts. */
@Mixin(ClientLevel.ClientLevelData.class)
public class ClientLevelDataMixin {
	@Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
	private void bambus$eigeneZeit(CallbackInfoReturnable<Long> cir) {
		long zeit = Visuals.eigeneZeit();
		if (zeit >= 0) {
			cir.setReturnValue(zeit);
		}
	}
}
