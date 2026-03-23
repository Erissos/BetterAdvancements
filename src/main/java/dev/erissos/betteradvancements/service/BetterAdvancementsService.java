package dev.erissos.betteradvancements.service;

import dev.erissos.betteradvancements.api.BetterAdvancementsAPI;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.manager.AchievementManager;
import dev.erissos.betteradvancements.manager.LeaderboardManager;
import dev.erissos.betteradvancements.model.BetterAdvancement;
import dev.erissos.betteradvancements.model.LeaderboardEntry;
import dev.erissos.betteradvancements.model.PlayerProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BetterAdvancementsService implements BetterAdvancementsAPI {

    private final AchievementManager achievementManager;
    private final PlayerDataManager playerDataManager;
    private final LeaderboardManager leaderboardManager;

    public BetterAdvancementsService(AchievementManager achievementManager, PlayerDataManager playerDataManager, LeaderboardManager leaderboardManager) {
        this.achievementManager = achievementManager;
        this.playerDataManager = playerDataManager;
        this.leaderboardManager = leaderboardManager;
    }

    @Override
    public Collection<BetterAdvancement> getAdvancements() {
        return achievementManager.getAdvancements();
    }

    @Override
    public Optional<BetterAdvancement> getAdvancement(String id) {
        return achievementManager.getAdvancement(id);
    }

    @Override
    public Optional<PlayerProfile> getProfile(UUID uniqueId) {
        return playerDataManager.getCachedProfile(uniqueId);
    }

    @Override
    public boolean grant(UUID uniqueId, String advancementId) {
        return achievementManager.forceGrant(uniqueId, advancementId);
    }

    @Override
    public List<LeaderboardEntry> getGlobalLeaderboard(int limit) {
        return leaderboardManager.getGlobalLeaderboard(limit);
    }

    @Override
    public List<LeaderboardEntry> getSessionLeaderboard(int limit) {
        return leaderboardManager.getSessionLeaderboard(limit);
    }
}