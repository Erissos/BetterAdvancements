package dev.erissos.betteradvancements.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProfile {

    private final UUID uniqueId;
    private String language = "en";
    private int points;
    private int sessionCompletions;
    private long sessionJoinMillis = System.currentTimeMillis();
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
}