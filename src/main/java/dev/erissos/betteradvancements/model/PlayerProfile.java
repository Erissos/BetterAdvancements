package dev.erissos.betteradvancements.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerProfile {

    private final UUID uniqueId;
    private String language = "en";
    private int points;
    private int seasonPoints;
    private String seasonId = "default";
    private int prestigeLevel;
    private int sessionCompletions;
    private long sessionJoinMillis = System.currentTimeMillis();
    private boolean chatNotificationsEnabled = true;
    private boolean titleNotificationsEnabled = true;
    private boolean actionBarNotificationsEnabled = true;
    private boolean bossBarNotificationsEnabled = true;
    private boolean soundNotificationsEnabled = true;
    private final Set<Integer> claimedSeasonRewards = new HashSet<>();
    private final Map<String, PlayerAchievementProgress> advancementProgress = new HashMap<>();
    private final Map<String, PlayerChallengeProgress> challengeProgress = new HashMap<>();
    private Instant lastSeen = Instant.now();

    public PlayerProfile(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public int getSeasonPoints() {
        return seasonPoints;
    }

    public void setSeasonPoints(int seasonPoints) {
        this.seasonPoints = seasonPoints;
    }

    public void addSeasonPoints(int amount) {
        this.seasonPoints += amount;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public int getPrestigeLevel() {
        return prestigeLevel;
    }

    public void setPrestigeLevel(int prestigeLevel) {
        this.prestigeLevel = prestigeLevel;
    }

    public void incrementPrestige() {
        this.prestigeLevel++;
    }

    public Set<Integer> getClaimedSeasonRewards() {
        return claimedSeasonRewards;
    }

    public int getSessionCompletions() {
        return sessionCompletions;
    }

    public void incrementSessionCompletions() {
        this.sessionCompletions++;
    }

    public long getSessionJoinMillis() {
        return sessionJoinMillis;
    }

    public void setSessionJoinMillis(long sessionJoinMillis) {
        this.sessionJoinMillis = sessionJoinMillis;
    }

    public boolean isChatNotificationsEnabled() {
        return chatNotificationsEnabled;
    }

    public void setChatNotificationsEnabled(boolean chatNotificationsEnabled) {
        this.chatNotificationsEnabled = chatNotificationsEnabled;
    }

    public boolean isTitleNotificationsEnabled() {
        return titleNotificationsEnabled;
    }

    public void setTitleNotificationsEnabled(boolean titleNotificationsEnabled) {
        this.titleNotificationsEnabled = titleNotificationsEnabled;
    }

    public boolean isActionBarNotificationsEnabled() {
        return actionBarNotificationsEnabled;
    }

    public void setActionBarNotificationsEnabled(boolean actionBarNotificationsEnabled) {
        this.actionBarNotificationsEnabled = actionBarNotificationsEnabled;
    }

    public boolean isBossBarNotificationsEnabled() {
        return bossBarNotificationsEnabled;
    }

    public void setBossBarNotificationsEnabled(boolean bossBarNotificationsEnabled) {
        this.bossBarNotificationsEnabled = bossBarNotificationsEnabled;
    }

    public boolean isSoundNotificationsEnabled() {
        return soundNotificationsEnabled;
    }

    public void setSoundNotificationsEnabled(boolean soundNotificationsEnabled) {
        this.soundNotificationsEnabled = soundNotificationsEnabled;
    }

    public Map<String, PlayerAchievementProgress> getAdvancementProgress() {
        return advancementProgress;
    }

    public Map<String, PlayerChallengeProgress> getChallengeProgress() {
        return challengeProgress;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public int getCompletedAdvancements() {
        return (int) advancementProgress.values().stream().filter(PlayerAchievementProgress::isCompleted).count();
    }

    /** Stored progress remains intact when an optional catalog is disabled; only active goals count in its UI. */
    public int getCompletedAdvancements(Set<String> activeIds) {
        return (int) advancementProgress.entrySet().stream().filter(entry -> activeIds.contains(entry.getKey())
                && entry.getValue().isCompleted()).count();
    }

    public int getCompletedChallenges() {
        return (int) challengeProgress.values().stream().filter(PlayerChallengeProgress::isCompleted).count();
    }

    public int getHighestTierCompleted(Map<String, BetterAdvancement> registry) {
        return advancementProgress.entrySet().stream()
                .filter(entry -> entry.getValue().isCompleted())
                .map(entry -> registry.get(entry.getKey()))
                .filter(java.util.Objects::nonNull)
                .map(BetterAdvancement::tier)
                .max(Comparator.comparingInt(Tier::getWeight))
                .map(Tier::getWeight)
                .orElse(0);
    }

    /** Capture on the server thread before handing data to the storage executor. */
    public PlayerProfile copy() {
        PlayerProfile copy = new PlayerProfile(uniqueId);
        copy.language = language; copy.points = points; copy.seasonPoints = seasonPoints; copy.seasonId = seasonId;
        copy.prestigeLevel = prestigeLevel; copy.sessionCompletions = sessionCompletions; copy.sessionJoinMillis = sessionJoinMillis;
        copy.chatNotificationsEnabled = chatNotificationsEnabled; copy.titleNotificationsEnabled = titleNotificationsEnabled;
        copy.actionBarNotificationsEnabled = actionBarNotificationsEnabled; copy.bossBarNotificationsEnabled = bossBarNotificationsEnabled;
        copy.soundNotificationsEnabled = soundNotificationsEnabled; copy.lastSeen = lastSeen;
        copy.claimedSeasonRewards.addAll(claimedSeasonRewards);
        advancementProgress.forEach((key, value) -> copy.advancementProgress.put(key,
            new PlayerAchievementProgress(value.getProgress(), value.isCompleted(), value.getCompletedAt())));
        challengeProgress.forEach((key, value) -> copy.challengeProgress.put(key, value.copy()));
        return copy;
    }
}
