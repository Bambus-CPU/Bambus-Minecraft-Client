package de.bambus.client.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Zugriff auf das Wetter der Client-Welt (für eigenes Wetter, nur Anzeige). */
@Mixin(Level.class)
public interface LevelWetterAccessor {
	@Accessor("rainLevel") void bambus$setRegen(float wert);
	@Accessor("oRainLevel") void bambus$setRegenAlt(float wert);
	@Accessor("thunderLevel") void bambus$setGewitter(float wert);
	@Accessor("oThunderLevel") void bambus$setGewitterAlt(float wert);
}
