package de.bambus.client.mixin;

import de.bambus.client.Funktionen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: Während Freelook dreht die Maus nur die Kamera, nicht den Spieler. */
@Mixin(Entity.class)
public class EntityMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void bambus$freelook(double dYaw, double dPitch, CallbackInfo ci) {
		if (Funktionen.drehen((Entity) (Object) this, dYaw, dPitch)) {
			ci.cancel();
		}
	}
}
