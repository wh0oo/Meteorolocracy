package com.wh0oo.meteorolocracy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.*;

public class VoteManager {
    private static final Map<UUID, String> votes = new HashMap<>();
    private static final Map<UUID, Long> lastVoteTimes = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final List<ScheduledFuture<?>> reminders = new ArrayList<>();
    private static final List<String> VALID_OPTIONS = List.of("sun", "rain", "thunder");

    private static boolean voteInProgress = false;
    private static long voteEndTime = 0;
    private static ScheduledFuture<?> endTask = null;

    public static List<String> getValidOptions() {
        return VALID_OPTIONS;
    }

    public static void handleVoteRequest(ServerPlayerEntity player, String voteType, ServerCommandSource source) {
        UUID playerId = player.getUuid();
        long now = System.currentTimeMillis();

        if (!VALID_OPTIONS.contains(voteType)) {
            source.sendFeedback(() -> MessageHelper.colored("Invalid vote option: " + voteType, MessageHelper.RED), false);
            return;
        }

        if (!voteInProgress) {
            // Snapshot-safe OP gate: permission level from the command source
            boolean isOp = source.getPermissionLevel() >= 2;

            long lastUsed = lastVoteTimes.getOrDefault(playerId, 0L);
            long cooldownMillis = VoteConfig.getPlayerCooldown() * 1000L;

            if (!isOp && (now - lastUsed) < cooldownMillis) {
                long wait = (lastUsed + cooldownMillis - now) / 1000;
                long waitMin = wait / 60;
                long waitHr = waitMin / 60;
                long remMin = waitMin % 60;
                source.sendFeedback(() -> MessageHelper.colored("You must wait " + waitHr + "h " + remMin + "m before starting another vote.", MessageHelper.RED), false);
                return;
            }

            lastVoteTimes.put(playerId, now);
            startVote(source.getServer());
        }

        votes.put(playerId, voteType);
        MessageHelper.broadcast(source.getServer(), player.getName().getString() + " voted for " + voteType + ".", MessageHelper.YELLOW);
        checkVotes(source.getServer());
    }

    public static void forceReset(ServerCommandSource source) {
        if (!voteInProgress) {
            source.sendFeedback(() -> MessageHelper.colored("There is no active weather vote to reset.", MessageHelper.RED), false);
            return;
        }

        resetVotes();
        MessageHelper.broadcast(source.getServer(), "The active weather vote has been cancelled by an operator.", MessageHelper.RED);
    }

    public static void showStatus(ServerCommandSource source) {
        if (!voteInProgress) {
            source.sendFeedback(() -> MessageHelper.colored("No vote is currently in progress.", MessageHelper.GRAY), false);
            return;
        }

        int total = source.getServer().getPlayerManager().getPlayerList().size();
        long secondsLeft = (voteEndTime - System.currentTimeMillis()) / 1000;

        source.sendFeedback(() -> MessageHelper.colored("Vote in progress for weather. Current standings:", MessageHelper.GOLD), false);
        for (String option : VALID_OPTIONS) {
            long count = votes.values().stream().filter(v -> v.equals(option)).count();
            source.sendFeedback(() -> MessageHelper.colored("- " + option + ": " + count + "/" + total + " (" + (int)((double)count/total*100) + "%)", MessageHelper.GRAY), false);
        }
        source.sendFeedback(() -> MessageHelper.colored("Time remaining: " + secondsLeft + " seconds", MessageHelper.GRAY), false);
    }

    private static void startVote(MinecraftServer server) {
        voteInProgress = true;
        votes.clear();

        long duration = VoteConfig.getVoteDuration();
        voteEndTime = System.currentTimeMillis() + duration * 1000L;

        MessageHelper.broadcast(server, "Weather voting is open! Use /weathervote <sun|rain|thunder>. Voting ends in " + duration + " seconds.", MessageHelper.GOLD);

        reminders.clear();
        if (duration >= 60)
            reminders.add(scheduler.schedule(() ->
                MessageHelper.broadcast(server, "Weather vote is still open! 1 minute left.", MessageHelper.YELLOW),
                duration - 60, TimeUnit.SECONDS));

        if (duration >= 30)
            reminders.add(scheduler.schedule(() ->
                MessageHelper.broadcast(server, "30 seconds left to vote!", MessageHelper.YELLOW),
                duration - 30, TimeUnit.SECONDS));

        if (duration >= 10)
            reminders.add(scheduler.schedule(() ->
                MessageHelper.broadcast(server, "10 seconds left to vote!", MessageHelper.YELLOW),
                duration - 10, TimeUnit.SECONDS));

        endTask = scheduler.schedule(() -> endVote(server), duration, TimeUnit.SECONDS);
    }

    private static void checkVotes(MinecraftServer server) {
        int total = server.getPlayerManager().getPlayerList().size();
        Map<String, Long> tally = new HashMap<>();

        for (String vote : votes.values()) {
            tally.put(vote, tally.getOrDefault(vote, 0L) + 1);
        }

        for (Map.Entry<String, Long> entry : tally.entrySet()) {
            String weather = entry.getKey();
            long count = entry.getValue();
            double percent = (double) count / total;

            MessageHelper.broadcast(server, count + "/" + total + " voted for " + weather + " (" + (int)(percent * 100) + "%)", MessageHelper.GRAY);

            if (percent >= VoteConfig.getVoteThreshold()) {
                applyWeather(server.getOverworld(), weather);
                MessageHelper.broadcast(server, "Vote passed! Weather changed to " + weather + ".", MessageHelper.GREEN);
                resetVotes();
                return;
            }
        }

        if (VoteConfig.shouldEndEarly() && votes.size() == total) {
            MessageHelper.broadcast(server, "All players have voted. Vote ended early.", MessageHelper.GRAY);
            endVote(server);
        }
    }

    private static void endVote(MinecraftServer server) {
        if (!voteInProgress) return;

        MessageHelper.broadcast(server, "Vote ended. No option reached majority threshold.", MessageHelper.RED);
        resetVotes();
    }

    private static void resetVotes() {
        votes.clear();
        voteInProgress = false;
        voteEndTime = 0;

        for (ScheduledFuture<?> r : reminders) r.cancel(false);
        reminders.clear();

        if (endTask != null) endTask.cancel(false);
        endTask = null;
    }

    private static void applyWeather(World world, String weather) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        switch (weather) {
            case "sun" -> serverWorld.setWeather(6000, 0, false, false);
            case "rain" -> serverWorld.setWeather(0, 6000, true, false);
            case "thunder" -> serverWorld.setWeather(0, 6000, true, true);
        }
    }
}
