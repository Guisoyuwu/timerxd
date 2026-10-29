package com.timermod.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface ChunkStorageInvoker {
    @Invoker("updatePosition")
    void timermod$updatePosition(ServerPlayerEntity player);
}
