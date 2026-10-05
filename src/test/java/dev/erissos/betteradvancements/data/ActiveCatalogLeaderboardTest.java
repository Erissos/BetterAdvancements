package dev.erissos.betteradvancements.data;

import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.model.PlayerAchievementProgress;
import dev.erissos.betteradvancements.model.PlayerProfile;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** A tiny disposable SQLite database; no server, provider installation or production data. */
class ActiveCatalogLeaderboardTest {
    @TempDir Path directory;

    @Test void sqlLimitsAfterActiveIdFilteringAndKeepsRetiredProgressAndEarnedPoints() throws Exception {
        var config = new ConfigManager(null);
        var databaseConfig = new YamlConfiguration(); databaseConfig.set("storage.type", "sqlite");
        var field = ConfigManager.class.getDeclaredField("databaseConfig"); field.setAccessible(true); field.set(config, databaseConfig);
        var adapter = new JdbcStorageAdapter(config, directory.toFile());
        try {
            adapter.init();
            var retired = new PlayerProfile(new UUID(0, 1)); retired.setPoints(1000); retired.setSeasonPoints(5);
            var active = new PlayerProfile(new UUID(0, 2)); active.setPoints(100); active.setSeasonPoints(5);
            for (int i = 0; i < 5; i++) complete(retired, "disabled_" + i);
            complete(active, "active_first"); complete(active, "active_second");
            adapter.saveProfile("retired", retired); adapter.saveProfile("active", active);
            assertEquals(retired.getUniqueId(), adapter.loadTopProfiles(1, null).getFirst().getUniqueId());
            var currentIds = Set.of("active_first", "active_second");
            assertEquals(active.getUniqueId(), adapter.loadTopProfiles(1, null, currentIds).getFirst().getUniqueId());
            assertEquals(active.getUniqueId(), adapter.loadTopProfiles(1, "default", currentIds).getFirst().getUniqueId());
            assertEquals(retired.getUniqueId(), adapter.loadTopProfiles(1, null, Set.of()).getFirst().getUniqueId());
            assertTrue(adapter.loadTopProfiles(1, "other-season", currentIds).isEmpty());
            var preserved = adapter.loadProfile(retired.getUniqueId()).orElseThrow();
            assertEquals(5, preserved.getCompletedAdvancements()); assertEquals(1000, preserved.getPoints());
            assertEquals(5, preserved.getSeasonPoints());
        } finally { adapter.close(); }
    }

    private static void complete(PlayerProfile profile, String id) {
        var progress = new PlayerAchievementProgress(); progress.complete(); profile.getAdvancementProgress().put(id, progress);
    }
}
