package dev.erissos.betteradvancements.manager;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.model.BetterAdvancement;
import dev.erissos.betteradvancements.model.LeaderboardEntry;
import dev.erissos.betteradvancements.model.PlayerProfile;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LeaderboardManager {

    private final BetterAdvancementsPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final AchievementManager achievementManager;
    private volatile List<LeaderboardEntry> globalLeaderboard = List.of();
    private volatile List<LeaderboardEntry> sessionLeaderboard = List.of();
    private volatile List<LeaderboardEntry> seasonLeaderboard = List.of();
    private java.util.Set<String> rankingCatalog = java.util.Set.of();
    private int taskId = -1;

    public LeaderboardManager(BetterAdvancementsPlugin plugin, PlayerDataManager playerDataManager, AchievementManager achievementManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.achievementManager = achievementManager;
    }

    public void start() {
        refresh();
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::refresh, 20L * 60L, 20L * 60L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void refresh() {
        int limit = Math.max(10, Math.min(1000, plugin.getConfig().getInt("general.leaderboard-limit", 100)));
        var activeIds = achievementManager.getAdvancements().stream().map(BetterAdvancement::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!rankingCatalog.equals(activeIds)) {
            rankingCatalog = activeIds;
            globalLeaderboard = List.of(); seasonLeaderboard = List.of();
        }
        this.sessionLeaderboard = playerDataManager.getSessionProfiles().stream().map(profile -> new LeaderboardEntry(
            profile.getUniqueId(), resolveName(profile.getUniqueId()), profile.getSessionCompletions(),
            achievementManager.getProgressPercent(profile), profile.getPoints(), 0))
            .sorted(Comparator.comparingInt(LeaderboardEntry::completed).reversed()
                .thenComparing(Comparator.comparingInt(LeaderboardEntry::points).reversed())
                .thenComparing(entry -> entry.uniqueId().toString())).toList();
        playerDataManager.loadTopProfiles(limit, false, activeIds).thenCombine(playerDataManager.loadTopProfiles(limit, true, activeIds),
            (global, season) -> Map.of("global", global, "season", season)).thenAccept(result -> {
            if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> {
                if (!activeIds.equals(rankingCatalog)) return;
                this.globalLeaderboard = buildEntries(result.get("global"));
                this.seasonLeaderboard = buildSeasonEntries(result.get("season"));
            });
        }).exceptionally(failure -> { plugin.getLogger().warning("Leaderboard refresh failed: " + failure.getMessage()); return null; });
    }

    public List<LeaderboardEntry> getGlobalLeaderboard(int limit) {
        return globalLeaderboard.stream().limit(limit).toList();
    }

    public List<LeaderboardEntry> getSessionLeaderboard(int limit) {
        return sessionLeaderboard.stream().limit(limit).toList();
    }

    public List<LeaderboardEntry> getSeasonLeaderboard(int limit) {
        return seasonLeaderboard.stream().limit(limit).toList();
    }

    private List<LeaderboardEntry> buildEntries(List<PlayerProfile> profiles) {
        Map<String, BetterAdvancement> registry = achievementManager.getAdvancements().stream()
            .collect(java.util.stream.Collectors.toMap(BetterAdvancement::id, advancement -> advancement));
        return profiles.stream()
                .map(profile -> new LeaderboardEntry(
                        profile.getUniqueId(),
                        resolveName(profile.getUniqueId()),
                        achievementManager.getCompletedCount(profile),
                        achievementManager.getProgressPercent(profile),
                        profile.getPoints(),
                profile.getHighestTierCompleted(registry)
                ))
                .sorted(Comparator.comparingInt(LeaderboardEntry::completed).reversed()
                        .thenComparing(Comparator.comparingDouble(LeaderboardEntry::progression).reversed())
                        .thenComparing(Comparator.comparingInt(LeaderboardEntry::points).reversed())
                        .thenComparing(entry -> entry.uniqueId().toString()))
                .toList();
    }

            private List<LeaderboardEntry> buildSeasonEntries(List<PlayerProfile> profiles) {
            Map<String, BetterAdvancement> registry = achievementManager.getAdvancements().stream()
                .collect(java.util.stream.Collectors.toMap(BetterAdvancement::id, advancement -> advancement));
            return profiles.stream()
                .map(profile -> new LeaderboardEntry(
                    profile.getUniqueId(),
                    resolveName(profile.getUniqueId()),
                    achievementManager.getCompletedCount(profile),
                    achievementManager.getProgressPercent(profile),
                    profile.getSeasonPoints(),
                    profile.getHighestTierCompleted(registry)
                ))
                .sorted(Comparator.comparingInt(LeaderboardEntry::points).reversed()
                    .thenComparing(Comparator.comparingInt(LeaderboardEntry::completed).reversed())
                    .thenComparing(entry -> entry.uniqueId().toString()))
                .toList();
            }

    private String resolveName(UUID uniqueId) {
        return Bukkit.getOfflinePlayer(uniqueId).getName() == null ? uniqueId.toString().substring(0, 8) : Bukkit.getOfflinePlayer(uniqueId).getName();
    }
}
