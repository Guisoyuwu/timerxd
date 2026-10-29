package com.timermod.client;

import com.timermod.TimerMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class TimerModClient implements ClientModInitializer {
    private static volatile long globalSec = -1;
    private static volatile long personalSec = -1;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TimerMod.TIMER_PACKET, (client, handler, buf, sender) -> {
            int kind = buf.readByte();
            long sec = buf.readLong();
            client.execute(() -> {
                if (kind == 0) globalSec = sec; else personalSec = sec;
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            globalSec = -1;
            personalSec = -1;
        });
        HudRenderCallback.EVENT.register(TimerModClient::render);
    }

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        int y = mc.player.getStatusEffects().isEmpty() ? 6 : 56; // esquiva los iconos de efectos
        if (globalSec >= 0) y = line(ctx, mc, fmt(globalSec), y, globalSec <= 10 ? 0xFF5555 : 0xFFFFFF);
        if (personalSec >= 0) line(ctx, mc, "Tu tiempo: " + fmt(personalSec), y, personalSec <= 10 ? 0xFF5555 : 0xFFAA00);
    }

    private static int line(DrawContext ctx, MinecraftClient mc, String text, int y, int color) {
        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth(text);
        int x = mc.getWindow().getScaledWidth() - w - 8;
        ctx.fill(x - 4, y - 3, x + w + 4, y + tr.fontHeight + 2, 0x90000000);
        ctx.drawTextWithShadow(tr, text, x, y, color);
        return y + tr.fontHeight + 8;
    }

    private static String fmt(long s) {
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }
}
