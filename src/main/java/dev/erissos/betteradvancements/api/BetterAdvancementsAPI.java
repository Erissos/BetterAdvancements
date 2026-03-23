package dev.erissos.betteradvancements.api;

import dev.erissos.betteradvancements.model.BetterAdvancement;
import dev.erissos.betteradvancements.model.LeaderboardEntry;
import dev.erissos.betteradvancements.model.PlayerProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BetterAdvancementsAPI {

    Collection<BetterAdvancement> getAdvancements();

    Optional<BetterAdvancement> getAdvancement(String id);

    Optional<PlayerProfile> getProfile(UUID uniqueId);

    boolean grant(UUID uniqueId, String advancementId);

    List<LeaderboardEntry> getGlobalLeaderboard(int limit);

    List<LeaderboardEntry> getSessionLeaderboard(int limit);
}