package dev.erissos.betteradvancements.integration;

import dev.desperis.suite.SuiteIntegrationService;
import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/** Product policy on top of the optional, source-verified suite bridge. */
public final class SuiteHooks {
    private static final NamespacedKey WORKER = new NamespacedKey("smartnpcworkers", "worker");
    private SuiteHooks() {}

    public static void install(BetterAdvancementsPlugin plugin, SuiteIntegrationService bridge) {
        bridge.onMilestone((player, context) -> {
            if (!enabled(plugin.getConfig(), "betteradvancements-milestones", true)) return;
            var safe = SuiteMilestoneContext.validated(context);
            if (safe.isEmpty()) return;
            // Ordinary trigger matching, dependencies, limits and earned rewards still apply.
            // The bridge never calls the administrative grant API.
            plugin.getAchievementManager().handleSuiteMilestone(player, safe);
            plugin.getChallengeManager().handleSuiteMilestone(player, safe);
        });
    }

    public static boolean enabled(FileConfiguration config, String feature, boolean fallback) {
        return config.getBoolean("suite-integrations.features." + feature, fallback);
    }

    /** NPCs are not combat targets, even when genuine managed boss kills are allowed. */
    public static boolean isNpc(Entity entity) {
        return entity.hasMetadata("NPC") || entity.getPersistentDataContainer().has(WORKER, PersistentDataType.STRING);
    }

    public static void validateConfig(FileConfiguration config) {
        for (String feature : java.util.List.of("betteradvancements-milestones",
                "betteradvancements-ignore-synthetic-progress", "betteradvancements-count-managed-mob-kills")) {
            String path = "suite-integrations.features." + feature;
            if (config.contains(path) && !config.isBoolean(path)) throw new IllegalArgumentException(path + " must be a boolean");
        }
    }
}
