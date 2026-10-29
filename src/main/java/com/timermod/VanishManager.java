package com.timermod;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.timermod.mixin.ChunkStorageInvoker;

import java.util.*;

public final class VanishManager {
    private static final Set<UUID> vanished = new HashSet<>();
    private static final Map<UUID, String> previousTeam = new HashMap<>();
    private static final String TAG_TEAM = "timermod_notag";

    public static boolean isVanished(Entity e) {
        return vanished.contains(e.getUuid());
    }

    // ---------- /vanish ----------
    public static boolean vanish(ServerPlayerEntity p) {
        if (!vanished.add(p.getUuid())) return false;
        MinecraftServer server = p.getServer();
        PlayerRemoveS2CPacket remove = new PlayerRemoveS2CPacket(List.of(p.getUuid()));
        for (ServerPlayerEntity o : server.getPlayerManager().getPlayerList()) {
            if (o != p) o.networkHandler.sendPacket(remove);
        }
        refreshTracking(p);
        server.getPlayerManager().broadcast(
                Text.translatable("multiplayer.player.left", p.getDisplayName()).formatted(Formatting.YELLOW), false);
        return true;
    }

    public static boolean unvanish(ServerPlayerEntity p) {
        if (!vanished.remove(p.getUuid())) return false;
        MinecraftServer server = p.getServer();
        PlayerListS2CPacket add = new PlayerListS2CPacket(EnumSet.of(
                PlayerListS2CPacket.Action.ADD_PLAYER,
                PlayerListS2CPacket.Action.INITIALIZE_CHAT,
                PlayerListS2CPacket.Action.UPDATE_GAME_MODE,
                PlayerListS2CPacket.Action.UPDATE_LISTED,
                PlayerListS2CPacket.Action.UPDATE_LATENCY,
                PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME), List.of(p));
        for (ServerPlayerEntity o : server.getPlayerManager().getPlayerList()) {
            if (o != p) o.networkHandler.sendPacket(add);
        }
        refreshTracking(p);
        server.getPlayerManager().broadcast(
                Text.translatable("multiplayer.player.joined", p.getDisplayName()).formatted(Formatting.YELLOW), false);
        return true;
    }

    private static void refreshTracking(ServerPlayerEntity p) {
        ((ChunkStorageInvoker) p.getServerWorld().getChunkManager().threadedAnvilChunkStorage)
                .timermod$updatePosition(p);
    }

    public static void onJoin(ServerPlayerEntity joining) {
        if (vanished.isEmpty()) return;
        List<UUID> hidden = new ArrayList<>(vanished);
        hidden.remove(joining.getUuid());
        if (!hidden.isEmpty()) joining.networkHandler.sendPacket(new PlayerRemoveS2CPacket(hidden));
    }

    public static void onDisconnect(ServerPlayerEntity p) {
        vanished.remove(p.getUuid());
    }

    // ---------- /tagvanish ----------
    public static boolean tagVanish(ServerPlayerEntity p) {
        Scoreboard sb = p.getServer().getScoreboard();
        Team team = sb.getTeam(TAG_TEAM);
        if (team == null) {
            team = sb.addTeam(TAG_TEAM);
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
        }
        String name = p.getEntityName();
        Team current = sb.getPlayerTeam(name);
        if (current == team) return false;
        if (current != null) previousTeam.put(p.getUuid(), current.getName());
        sb.addPlayerToTeam(name, team);
        return true;
    }

    public static boolean untagVanish(ServerPlayerEntity p) {
        Scoreboard sb = p.getServer().getScoreboard();
        String name = p.getEntityName();
        Team current = sb.getPlayerTeam(name);
        if (current == null || !current.getName().equals(TAG_TEAM)) return false;
        sb.clearPlayerTeam(name);
        String prev = previousTeam.remove(p.getUuid());
        if (prev != null) {
            Team old = sb.getTeam(prev);
            if (old != null) sb.addPlayerToTeam(name, old);
        }
        return true;
    }

    private VanishManager() {}
}
