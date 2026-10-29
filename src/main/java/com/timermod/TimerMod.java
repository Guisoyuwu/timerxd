package com.timermod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;

public class TimerMod implements ModInitializer {
    public static final String MOD_ID = "timermod";
    public static final Identifier TIMER_PACKET = new Identifier(MOD_ID, "timer");

    @Override
    public void onInitialize() {
        ModItems.register();
        ModCommands.register();
        ServerTickEvents.END_SERVER_TICK.register(TimerManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TimerManager.syncTo(handler.player);
            VanishManager.onJoin(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                VanishManager.onDisconnect(handler.player));
    }
}
