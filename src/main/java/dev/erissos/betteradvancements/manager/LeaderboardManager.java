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
    private List<LeaderboardEntry> globalLeaderboard = new ArrayList<>();
    private List<LeaderboardEntry> sessionLeaderboard = new ArrayList<>();
    private List<LeaderboardEntry> seasonLeaderboard = new ArrayList<>();
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
        playerDataManager.loadAllProfiles().thenAccept(allProfiles -> {
            this.globalLeaderboard = buildEntries(allProfiles);
            this.seasonLeaderboard = buildSeasonEntries(allProfiles);
            this.sessionLeaderboard = buildEntries(new ArrayList<>(playerDataManager.getCachedProfiles())).stream()
                    .sorted(Comparator.comparingInt(LeaderboardEntry::completed).reversed().thenComparingInt(LeaderboardEntry::points).reversed())
                    .toList();
        });
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
                        profile.getCompletedAdvancements(),
                        achievementManager.getProgressPercent(profile),
                        profile.getPoints(),
                profile.getHighestTierCompleted(registry)
                ))
                .sorted(Comparator.comparingInt(LeaderboardEntry::completed).reversed()
                        .thenComparingDouble(LeaderboardEntry::progression).reversed()
                        .thenComparingInt(LeaderboardEntry::points).reversed())
                .toList();
    }

            private List<LeaderboardEntry> buildSeasonEntries(List<PlayerProfile> profiles) {
            Map<String, BetterAdvancement> registry = achievementManager.getAdvancements().stream()
                .collect(java.util.stream.Collectors.toMap(BetterAdvancement::id, advancement -> advancement));
            return profiles.stream()
                .map(profile -> new LeaderboardEntry(
                    profile.getUniqueId(),
                    resolveName(profile.getUniqueId()),
                    profile.getCompletedAdvancements(),
                    achievementManager.getProgressPercent(profile),
                    profile.getSeasonPoints(),
                    profile.getHighestTierCompleted(registry)
                ))
                .sorted(Comparator.comparingInt(LeaderboardEntry::points).reversed()
                    .thenComparingInt(LeaderboardEntry::completed).reversed())
                .toList();
            }

    private String resolveName(UUID uniqueId) {
        return Bukkit.getOfflinePlayer(uniqueId).getName() == null ? uniqueId.toString().substring(0, 8) : Bukkit.getOfflinePlayer(uniqueId).getName();
    }
}