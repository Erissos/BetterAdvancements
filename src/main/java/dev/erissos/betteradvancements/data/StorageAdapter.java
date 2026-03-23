package dev.erissos.betteradvancements.data;

import dev.erissos.betteradvancements.model.PlayerProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageAdapter {

    void init();

    Optional<PlayerProfile> loadProfile(UUID uniqueId);

    void saveProfile(String lastName, PlayerProfile profile);

    void deleteProfile(UUID uniqueId);

    List<PlayerProfile> loadAllProfiles();

    void close();
}