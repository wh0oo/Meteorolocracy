package com.wh0oo.meteorolocracy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class WeatherVoteCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("weathervote")
                .then(CommandManager.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (String option : VoteManager.getValidOptions()) {
                            builder.suggest(option);
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        String voteType = StringArgumentType.getString(context, "type");
                        ServerPlayerEntity player;
                        try {
                            player = source.getPlayer();
                        } catch (Exception e) {
                            source.sendFeedback(() -> MessageHelper.colored("Only players can vote.", MessageHelper.RED), false);
                            return 0;
                        }
                        VoteManager.handleVoteRequest(player, voteType, source);
                        return 1;
                    })
                )
                .then(CommandManager.literal("status")
                    .executes(context -> {
                        VoteManager.showStatus(context.getSource());
                        return 1;
                    })
                )
                .then(CommandManager.literal("reset")
                    .requires(source -> {
                        try {
                            ServerPlayerEntity player = source.getPlayer();
                            return source.getServer().getPlayerManager().isOperator(player.getGameProfile());
                        } catch (Exception e) {
                            // Allow console (which is not a player)
                            return true;
                        }
                    })
                    .executes(context -> {
                        VoteManager.forceReset(context.getSource());
                        return 1;
                    })
                )
        );
    }
}