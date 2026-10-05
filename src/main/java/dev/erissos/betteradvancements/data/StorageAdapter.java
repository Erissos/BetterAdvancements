package dev.erissos.betteradvancements.data;

import dev.erissos.betteradvancements.model.PlayerProfile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface StorageAdapter {

    void init();

    Optional<PlayerProfile> loadProfile(UUID uniqueId);

    void saveProfile(String lastName, PlayerProfile profile);

    void deleteProfile(UUID uniqueId);

    List<PlayerProfile> loadAllProfiles();

    default List<PlayerProfile> loadTopProfiles(int limit, String season) {
        return loadAllProfiles().stream().limit(limit).toList();
    }

    default List<PlayerProfile> loadTopProfiles(int limit, String season, Set<String> activeIds) {
        var ranking = java.util.Comparator.comparingInt((PlayerProfile profile) -> profile.getCompletedAdvancements(activeIds))
                .reversed().thenComparing(java.util.Comparator.comparingInt(PlayerProfile::getPoints).reversed())
                .thenComparing(profile -> profile.getUniqueId().toString());
        if (season != null) ranking = java.util.Comparator.comparingInt(PlayerProfile::getSeasonPoints).reversed()
                .thenComparing(java.util.Comparator.comparingInt((PlayerProfile profile) -> profile.getCompletedAdvancements(activeIds)).reversed())
                .thenComparing(profile -> profile.getUniqueId().toString());
        return loadAllProfiles().stream().filter(profile -> season == null || season.equals(profile.getSeasonId()))
                .sorted(ranking).limit(Math.max(1, Math.min(1000, limit))).toList();
    }

    void close();
}
