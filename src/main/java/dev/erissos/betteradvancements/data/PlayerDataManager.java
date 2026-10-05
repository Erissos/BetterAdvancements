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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<UUID, CompletableFuture<PlayerProfile>> loading = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> sessions = ConcurrentHashMap.newKeySet();
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
        UUID id = player.getUniqueId();
        lastKnownNames.put(id, player.getName());
        sessions.add(id);
        synchronized (loading) {
            CompletableFuture<PlayerProfile> existing = loading.get(id);
            if (existing != null) return existing;
            String defaultLanguage = configManager.getMainConfig().getString("general.default-language", "en");
            String currentSeason = seasonManager.currentSeasonId();
            var future = CompletableFuture.supplyAsync(() -> {
                PlayerProfile profile = storageAdapter.loadProfile(id).orElseGet(() -> create(id, defaultLanguage, currentSeason));
                profile.setLastSeen(Instant.now());
                profile.setSessionJoinMillis(System.currentTimeMillis());
                cache.put(id, profile);
                return profile;
            }, executor);
            loading.put(id, future);
            future.whenComplete((value, failure) -> loading.remove(id, future));
            return future;
        }
    }

    public PlayerProfile getOrCreate(UUID uniqueId) {
        var pending = loading.get(uniqueId);
        if (pending != null) pending.join();
        PlayerProfile profile = cache.get(uniqueId);
        if (profile == null) {
            String language = configManager.getMainConfig().getString("general.default-language", "en");
            String season = seasonManager.currentSeasonId();
            profile = CompletableFuture.supplyAsync(() -> storageAdapter.loadProfile(uniqueId)
                .orElseGet(() -> create(uniqueId, language, season)), executor).join();
            cache.put(uniqueId, profile);
        }
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
        PlayerProfile profile = cache.get(uniqueId);
        if (profile == null) return CompletableFuture.completedFuture(null);
        profile.setLastSeen(Instant.now());
        PlayerProfile snapshot = profile.copy();
        String name = lastKnownNames.getOrDefault(uniqueId, uniqueId.toString().substring(0, 8));
        var future = CompletableFuture.runAsync(() -> storageAdapter.saveProfile(name, snapshot), executor);
        future.whenComplete((unused, failure) -> {
            if (failure != null) plugin.getLogger().severe("Profile save failed for " + uniqueId + ": " + failure.getMessage());
        });
        return future;
    }

    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID uniqueId : cache.keySet()) {
            futures.add(saveProfile(uniqueId));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public CompletableFuture<Void> resetProfile(UUID uniqueId) {
        // Remove on the caller/server thread; the serialized executor deletes after earlier saves.
        var pending=loading.get(uniqueId);
        if (pending!=null) pending.join();
        plugin.getRewardDelivery().resetEpoch(uniqueId);
        cache.remove(uniqueId);
        return CompletableFuture.runAsync(() -> {
            storageAdapter.deleteProfile(uniqueId);
        }, executor);
    }

    public Collection<PlayerProfile> getCachedProfiles() {
        return cache.values();
    }

    public List<PlayerProfile> getSessionProfiles() {
        return sessions.stream().map(cache::get).filter(java.util.Objects::nonNull).map(PlayerProfile::copy).toList();
    }

    public CompletableFuture<Void> release(UUID id) {
        sessions.remove(id);
        PlayerProfile previous = cache.get(id);
        return saveProfile(id).thenRun(() -> {
            if (previous != null && !sessions.contains(id) && cache.remove(id, previous)) lastKnownNames.remove(id);
        });
    }

    public CompletableFuture<List<PlayerProfile>> loadTopProfiles(int limit, boolean season) {
        String currentSeason = seasonManager.currentSeasonId();
        return CompletableFuture.supplyAsync(() -> storageAdapter.loadTopProfiles(limit, season ? currentSeason : null), executor);
    }

    public CompletableFuture<List<PlayerProfile>> loadAllProfiles() {
        return CompletableFuture.supplyAsync(storageAdapter::loadAllProfiles, executor);
    }

    public void shutdown() {
        try { saveAll().join(); }
        finally { executor.shutdown(); storageAdapter.close(); }
    }

    private void normalizeSeason(PlayerProfile profile) {
        String currentSeason = seasonManager.currentSeasonId();
        if (!currentSeason.equals(profile.getSeasonId())) {
            profile.setSeasonId(currentSeason);
            profile.setSeasonPoints(0);
            profile.getClaimedSeasonRewards().clear();
        }
    }

    private PlayerProfile create(UUID id, String language, String season) {
        PlayerProfile profile = new PlayerProfile(id); profile.setLanguage(language); profile.setSeasonId(season); return profile;
    }
}
