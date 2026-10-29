package com.timermod;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ModCommands {
    private static final Predicate<ServerCommandSource> PERM = s -> s.hasPermissionLevel(2);
    private static final String BAD = "Formato inválido. Usá por ejemplo: 20s, 20m o 20hs";

    public static void register() {
        CommandRegistrationCallback.EVENT.register((d, access, env) -> {

            d.register(literal("timer").requires(PERM)
                    .then(literal("stop").executes(c -> {
                        TimerManager.stopGlobal(c.getSource().getServer());
                        c.getSource().sendFeedback(() -> Text.literal("Temporizador detenido"), true);
                        return 1;
                    }))
                    .then(argument("tiempo", StringArgumentType.word()).executes(ModCommands::startGlobal)));

            d.register(literal("timerplayer").requires(PERM)
                    .then(argument("jugador", EntityArgumentType.player())
                            .then(literal("stop").executes(c -> {
                                ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "jugador");
                                TimerManager.stopPersonal(p);
                                c.getSource().sendFeedback(() -> Text.literal("Temporizador de " + p.getEntityName() + " detenido"), true);
                                return 1;
                            }))
                            .then(argument("tiempo", StringArgumentType.word()).executes(c -> {
                                ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "jugador");
                                String raw = StringArgumentType.getString(c, "tiempo");
                                long s = TimerManager.parse(raw);
                                if (s < 0) { c.getSource().sendError(Text.literal(BAD)); return 0; }
                                TimerManager.startPersonal(p, s);
                                c.getSource().sendFeedback(() -> Text.literal("Temporizador de " + raw + " para " + p.getEntityName()), true);
                                return 1;
                            }))));

            d.register(literal("vanish").requires(PERM).executes(c -> {
                ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                if (VanishManager.vanish(p)) c.getSource().sendFeedback(() -> Text.literal("Vanish activado: nadie te ve."), false);
                else c.getSource().sendError(Text.literal("Ya estás en vanish."));
                return 1;
            }));

            d.register(literal("unvanish").requires(PERM).executes(c -> {
                ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                if (VanishManager.unvanish(p)) c.getSource().sendFeedback(() -> Text.literal("Vanish desactivado."), false);
                else c.getSource().sendError(Text.literal("No estabas en vanish."));
                return 1;
            }));

            d.register(literal("tagvanish").requires(PERM).executes(c -> {
                ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                if (VanishManager.tagVanish(p)) c.getSource().sendFeedback(() -> Text.literal("Nombre sobre tu cabeza oculto."), false);
                else c.getSource().sendError(Text.literal("Ya tenías el nombre oculto."));
                return 1;
            }));

            d.register(literal("untagvanish").requires(PERM).executes(c -> {
                ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                if (VanishManager.untagVanish(p)) c.getSource().sendFeedback(() -> Text.literal("Nombre visible otra vez."), false);
                else c.getSource().sendError(Text.literal("No tenías el nombre oculto."));
                return 1;
            }));
        });
    }

    private static int startGlobal(CommandContext<ServerCommandSource> c) {
        String raw = StringArgumentType.getString(c, "tiempo");
        long s = TimerManager.parse(raw);
        if (s < 0) { c.getSource().sendError(Text.literal(BAD)); return 0; }
        TimerManager.startGlobal(c.getSource().getServer(), s);
        c.getSource().sendFeedback(() -> Text.literal("Temporizador iniciado: " + raw), true);
        return 1;
    }

    private ModCommands() {}
}
