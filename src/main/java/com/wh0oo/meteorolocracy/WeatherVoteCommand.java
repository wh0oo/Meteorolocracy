package com.wh0oo.meteorolocracy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.*;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class WeatherVoteCommand {
    private static final Map<UUID, String> votes = new HashMap<>();
    private static final Map<UUID, Long> lastVoteTimes = new HashMap<>();
    private static final List<String> VALID_OPTIONS = List.of("sun", "rain", "thunder");

    private static final double VOTE_THRESHOLD = 0.5;
    private static final long VOTE_DURATION_SECONDS = 60;
    private static final long PLAYER_COOLDOWN_SECONDS = 10800; // 3 hours

    private static boolean voteInProgress = false;
    private static String currentVoteType = null;
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static List<ScheduledFuture<?>> scheduledReminders = new ArrayList<>();

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
                        long now = System.currentTimeMillis();

                        if (!voteInProgress) {
                            long lastUsed = lastVoteTimes.getOrDefault(playerId, 0L);
                            if ((now - lastUsed) < PLAYER_COOLDOWN_SECONDS * 1000) {
                                long waitSec = ((lastUsed + PLAYER_COOLDOWN_SECONDS * 1000) - now) / 1000;
                                long waitMin = waitSec / 60;
                                long waitHr = waitMin / 60;
                                long remMin = waitMin % 60;
                                source.sendFeedback(() -> Text.literal("You must wait " + waitHr + "h " + remMin + "m before starting another vote."), false);
                                return 0;
                            }
                            lastVoteTimes.put(playerId, now);
                            startVote(source.getServer(), vote);
                        } else if (!vote.equals(currentVoteType)) {
                            source.sendFeedback(() -> Text.literal("A vote for " + currentVoteType + " is already in progress."), false);
                            return 0;
                        }

                        votes.put(playerId, vote);
                        broadcast(source.getServer(), player.getName().getString() + " voted for " + vote + ".");
                        checkVotes(source.getServer());

                        return 1;
                    })
                )
        );
    }

    private static void startVote(MinecraftServer server, String voteType) {
        voteInProgress = true;
        currentVoteType = voteType;
        votes.clear();

        broadcast(server, "Voting started for weather: " + voteType + ". Use /weathervote " + voteType + " to vote! Voting ends in " + VOTE_DURATION_SECONDS + " seconds.");

        // Schedule end of vote
        scheduler.schedule(() -> endVote(server), VOTE_DURATION_SECONDS, TimeUnit.SECONDS);

        // Schedule reminders
        scheduledReminders.clear();
        scheduledReminders.add(scheduler.schedule(() ->
            broadcast(server, "Vote for " + voteType + " is still open! 30 seconds left."), 30, TimeUnit.SECONDS));
        scheduledReminders.add(scheduler.schedule(() ->
            broadcast(server, "10 seconds left to vote for " + voteType + "!"), 50, TimeUnit.SECONDS));
    }

    private static void checkVotes(MinecraftServer server) {
        int totalVoters = server.getPlayerManager().getPlayerList().size();
        long matchingVotes = votes.values().stream().filter(v -> v.equals(currentVoteType)).count();

        double percent = (double) matchingVotes / totalVoters;

        broadcast(server, matchingVotes + "/" + totalVoters + " voted for " + currentVoteType + " (" + (int)(percent * 100) + "%)");

        if (percent >= VOTE_THRESHOLD) {
            applyWeather(server.getOverworld(), currentVoteType);
            broadcast(server, "Vote passed! Weather changed to " + currentVoteType + ".");
            resetVotes();
        }
    }

    private static void endVote(MinecraftServer server) {
        if (!voteInProgress) return;

        int totalVoters = server.getPlayerManager().getPlayerList().size();
        long matchingVotes = votes.values().stream().filter(v -> v.equals(currentVoteType)).count();
        double percent = (double) matchingVotes / totalVoters;

        if (percent < VOTE_THRESHOLD) {
            broadcast(server, "Vote failed. Not enough players voted for " + currentVoteType + ".");
        }

        resetVotes();
    }

    private static void resetVotes() {
        votes.clear();
        voteInProgress = false;
        currentVoteType = null;

        for (ScheduledFuture<?> reminder : scheduledReminders) {
            reminder.cancel(false);
        }
        scheduledReminders.clear();
    }

    private static void applyWeather(World world, String weather) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        switch (weather) {
            case "sun" -> serverWorld.setWeather(6000, 0, false, false);
            case "rain" -> serverWorld.setWeather(0, 6000, true, false);
            case "thunder" -> serverWorld.setWeather(0, 6000, true, true);
        }
    }

    private static void broadcast(MinecraftServer server, String message) {
        server.getPlayerManager().broadcast(Text.literal("[Meteorolocracy] " + message), false);
    }
}
