package de.bambus.client.mixin;

import de.bambus.client.Funktionen;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Freelook: Die Kamera nutzt die eigene Blickrichtung statt der des Spielers. */
@Mixin(Camera.class)
public class CameraMixin {
	@ModifyVariable(method = "setRotation", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private float bambus$yaw(float yaw) {
		return Funktionen.freelookAktiv ? Funktionen.kameraYaw : yaw;
	}

	@ModifyVariable(method = "setRotation", at = @At("HEAD"), ordinal = 1, argsOnly = true)
	private float bambus$pitch(float pitch) {
		return Funktionen.freelookAktiv ? Funktionen.kameraPitch : pitch;
	}
}
