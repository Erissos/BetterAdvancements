package dev.erissos.betteradvancements.data;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.manager.SeasonManager;
import dev.erissos.betteradvancements.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PlayerDataManager {

    private final BetterAdvancementsPlugin plugin;
    private final ConfigManager configManager;
    private final SeasonManager seasonManager;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastKnownNames = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private StorageAdapter storageAdapter;

    public PlayerDataManager(BetterAdvancementsPlugin plugin, ConfigManager configManager, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.seasonManager = seasonManager;
    }

    public void start() {
        this.storageAdapter = new JdbcStorageAdapter(configManager, plugin.getDataFolder());
        this.storageAdapter.init();
    }

    public CompletableFuture<PlayerProfile> loadProfile(Player player) {
        lastKnownNames.put(player.getUniqueId(), player.getName());
        return CompletableFuture.supplyAsync(() -> {
            PlayerProfile profile = storageAdapter.loadProfile(player.getUniqueId()).orElseGet(() -> {
                PlayerProfile created = new PlayerProfile(player.getUniqueId());
                created.setLanguage(configManager.getMainConfig().getString("general.default-language", "en"));
                return created;
            });
            profile.setLastSeen(Instant.now());
            normalizeSeason(profile);
            cache.put(player.getUniqueId(), profile);
            return profile;
        }, executor);
    }

    public PlayerProfile getOrCreate(UUID uniqueId) {
        PlayerProfile profile = cache.computeIfAbsent(uniqueId, id -> {
            PlayerProfile created = new PlayerProfile(id);
            created.setLanguage(configManager.getMainConfig().getString("general.default-language", "en"));
            return created;
        });
        normalizeSeason(profile);
        return profile;
    }

    public Optional<PlayerProfile> getCachedProfile(UUID uniqueId) {
        return Optional.ofNullable(cache.get(uniqueId));
    }

    public void setLastKnownName(UUID uniqueId, String name) {
        lastKnownNames.put(uniqueId, name);
    }

    public CompletableFuture<Void> saveProfile(UUID uniqueId) {
        return CompletableFuture.runAsync(() -> {
            PlayerProfile profile = cache.get(uniqueId);
            if (profile != null) {
                profile.setLastSeen(Instant.now());
                storageAdapter.saveProfile(lastKnownNames.getOrDefault(uniqueId, Bukkit.getOfflinePlayer(uniqueId).getName()), profile);
            }
        }, executor);
    }

    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID uniqueId : cache.keySet()) {
            futures.add(saveProfile(uniqueId));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public CompletableFuture<Void> resetProfile(UUID uniqueId) {
        return CompletableFuture.runAsync(() -> {
            cache.remove(uniqueId);
            storageAdapter.deleteProfile(uniqueId);
        }, executor);
    }

    public Collection<PlayerProfile> getCachedProfiles() {
        return cache.values();
    }

    public CompletableFuture<List<PlayerProfile>> loadAllProfiles() {
        return CompletableFuture.supplyAsync(storageAdapter::loadAllProfiles, executor);
    }

    public void shutdown() {
        saveAll().join();
        storageAdapter.close();
        executor.shutdown();
    }

    private void normalizeSeason(PlayerProfile profile) {
        String currentSeason = seasonManager.currentSeasonId();
        if (!currentSeason.equals(profile.getSeasonId())) {
            profile.setSeasonId(currentSeason);
            profile.setSeasonPoints(0);
            profile.getClaimedSeasonRewards().clear();
        }
    }
}
