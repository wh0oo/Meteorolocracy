package com.wh0oo.meteorolocracy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WeatherVoteCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("weathervote")
                .then(argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        VoteManager.getValidOptions().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String voteType = StringArgumentType.getString(ctx, "type").toLowerCase();
                        CommandSourceStack source = ctx.getSource();
                        ServerPlayer player = source.getPlayer();

                        if (!VoteManager.getValidOptions().contains(voteType)) {
                            source.sendFailure(MessageHelper.colored("Invalid vote. Use sun, rain, or thunder.", MessageHelper.RED));
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
