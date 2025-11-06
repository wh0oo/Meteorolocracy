package com.wh0oo.meteorolocracy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WeatherVoteCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("weathervote")
                .then(argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        VoteManager.getValidOptions().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String voteType = StringArgumentType.getString(ctx, "type").toLowerCase();
                        ServerCommandSource source = ctx.getSource();
                        ServerPlayerEntity player;
                        try {
                            player = source.getPlayer();
                        } catch (Exception e) {
                            source.sendError(MessageHelper.colored("Only players can vote.", MessageHelper.RED));
                            return 0;
                        }

                        if (!VoteManager.getValidOptions().contains(voteType)) {
                            source.sendError(MessageHelper.colored("Invalid vote. Use sun, rain, or thunder.", MessageHelper.RED));
                            return 0;
                        }

                        VoteManager.handleVoteRequest(player, voteType, source);
                        return 1;
                    })
                )
                .then(literal("reset")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        VoteManager.forceReset(ctx.getSource());
                        return 1;
                    })
                )
                .then(literal("status")
                    .executes(ctx -> {
                        VoteManager.showStatus(ctx.getSource());
                        return 1;
                    })
                )
        );
    }
}
