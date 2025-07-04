package com.wh0oo.meteorolocracy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;

import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class WeatherVoteCommand {
    private static final Map<UUID, String> votes = new HashMap<>();
    private static final List<String> VALID_OPTIONS = List.of("sun", "rain", "thunder");
    private static final double VOTE_THRESHOLD = 0.6;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("weathervote")
                .then(argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (String option : VALID_OPTIONS) {
                            builder.suggest(option);
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String vote = StringArgumentType.getString(ctx, "type").toLowerCase();
                        ServerCommandSource source = ctx.getSource();
                        ServerPlayerEntity player = source.getPlayer();

                        if (!VALID_OPTIONS.contains(vote)) {
                            source.sendFeedback(() -> Text.literal("Invalid vote. Use sun, rain, or thunder."), false);
                            return 0;
                        }

                        UUID playerId = player.getUuid();
                        votes.put(playerId, vote);

                        source.sendFeedback(() -> Text.literal("You voted for " + vote + "."), false);
                        checkVotes(source.getServer());

                        return 1;
                    })
                )
        );
    }

    private static void checkVotes(MinecraftServer server) {
        Map<String, Integer> tally = new HashMap<>();
        for (String vote : votes.values()) {
            tally.put(vote, tally.getOrDefault(vote, 0) + 1);
        }

        int online = server.getPlayerManager().getPlayerList().size();
        for (Map.Entry<String, Integer> entry : tally.entrySet()) {
            String weather = entry.getKey();
            int count = entry.getValue();

            double percent = (double) count / online;
            if (percent >= VOTE_THRESHOLD) {
                applyWeather(server.getOverworld(), weather);
                broadcast(server, "Vote passed! Weather changed to " + weather + ".");
                votes.clear();
                return;
            }
        }

        broadcast(server, "Weather vote ongoing. " + votes.size() + "/" + server.getPlayerManager().getPlayerList().size() + " players have voted.");
    }

    private static void applyWeather(World world, String weather) {
        switch (weather) {
            case "sun" -> world.setWeather(6000, 0, false, false);
            case "rain" -> world.setWeather(0, 6000, true, false);
            case "thunder" -> world.setWeather(0, 6000, true, true);
        }
    }

    private static void broadcast(MinecraftServer server, String message) {
        server.getPlayerManager().broadcast(Text.literal("[Meteorolocracy] " + message), false);
    }
}
