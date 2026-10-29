package com.timermod;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimerManager {
    private static final int GLOBAL = 0, PERSONAL = 1;
    private static final Pattern FORMAT = Pattern.compile("^(\\d+)(hs|h|m|s)$", Pattern.CASE_INSENSITIVE);

    private static long globalTicks = -1;
    private static final Map<UUID, Long> personal = new HashMap<>();

    /** "20s", "20m", "20hs" -> segundos. -1 si es inválido. */
    public static long parse(String input) {
        Matcher m = FORMAT.matcher(input.trim());
        if (!m.matches()) return -1;
        long n;
        try { n = Long.parseLong(m.group(1)); } catch (NumberFormatException e) { return -1; }
        if (n <= 0 || n > 100000) return -1;
        return switch (m.group(2).toLowerCase()) {
            case "s" -> n;
            case "m" -> n * 60;
            default -> n * 3600;
        };
    }

    public static void startGlobal(MinecraftServer server, long seconds) {
        globalTicks = seconds * 20;
        sendAll(server, seconds);
    }

    public static void stopGlobal(MinecraftServer server) {
        globalTicks = -1;
        sendAll(server, -1);
    }

    public static void startPersonal(ServerPlayerEntity p, long seconds) {
        personal.put(p.getUuid(), seconds * 20);
        send(p, PERSONAL, seconds);
    }

    public static void stopPersonal(ServerPlayerEntity p) {
        personal.remove(p.getUuid());
        send(p, PERSONAL, -1);
    }

    public static void syncTo(ServerPlayerEntity p) {
        send(p, GLOBAL, globalTicks > 0 ? (globalTicks + 19) / 20 : -1);
        Long t = personal.get(p.getUuid());
        send(p, PERSONAL, t != null && t > 0 ? (t + 19) / 20 : -1);
    }

    public static void tick(MinecraftServer server) {
        if (globalTicks > 0) {
            globalTicks--;
            if (globalTicks % 20 == 0) {
                if (globalTicks == 0) {
                    globalTicks = -1;
                    sendAll(server, -1);
                    for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) finish(p);
                } else {
                    sendAll(server, globalTicks / 20);
                }
            }
        }
        if (!personal.isEmpty()) {
            Iterator<Map.Entry<UUID, Long>> it = personal.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> e = it.next();
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
                if (p == null) continue; // pausado hasta que vuelva a conectarse
                long t = e.getValue() - 1;
                if (t % 20 == 0) {
                    if (t <= 0) {
                        it.remove();
                        send(p, PERSONAL, -1);
                        finish(p);
                        continue;
                    }
                    send(p, PERSONAL, t / 20);
                }
                e.setValue(t);
            }
        }
    }

    private static void finish(ServerPlayerEntity p) {
        Text msg = Text.literal("Tiempo Acabado").formatted(Formatting.RED, Formatting.BOLD);
        p.sendMessage(msg, false);
        p.networkHandler.sendPacket(new TitleS2CPacket(msg));
        p.playSound(SoundEvents.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
    }

    private static void sendAll(MinecraftServer server, long seconds) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) send(p, GLOBAL, seconds);
    }

    private static void send(ServerPlayerEntity p, int kind, long seconds) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(kind);
        buf.writeLong(seconds);
        ServerPlayNetworking.send(p, TimerMod.TIMER_PACKET, buf);
    }

    private TimerManager() {}
}
