package com.timermod.mixin;

import com.timermod.VanishManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public abstract class EntityTrackerMixin {
    @Shadow @Final Entity entity;

    @Shadow public abstract void stopTracking(ServerPlayerEntity player);

    @Inject(method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void timermod$hideVanished(ServerPlayerEntity viewer, CallbackInfo ci) {
        if (this.entity instanceof ServerPlayerEntity target
                && target != viewer
                && VanishManager.isVanished(target)) {
            this.stopTracking(viewer);
            ci.cancel();
        }
    }
}
