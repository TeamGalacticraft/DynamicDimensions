package dev.galacticraft.dynamicdimensions.impl.mixin.compat.sable;

import dev.galacticraft.dynamicdimensions.impl.compat.SableDimensionPhysicsCompat;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem", remap = false)
public class SubLevelPhysicsSystemMixin {

    @Shadow
    private ServerLevel level;

    @Inject(method = "initialize", at = @At("HEAD"))
    private void dynamicdimensions$flushPendingPhysics(final CallbackInfo ci) {
        SableDimensionPhysicsCompat.flushForLevel(this.level.dimension());
    }
}