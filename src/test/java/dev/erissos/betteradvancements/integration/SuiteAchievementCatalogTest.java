package dev.erissos.betteradvancements.integration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Small receiver/configuration checks, not third-party provider or connected-player certification. */
class SuiteAchievementCatalogTest {
    // Contract whitelist: catalog/slot/reward checks run without installing a fake Bukkit registry.
    // Native item usability is independently checked by the compact real-Paper fixture.
    private static final java.util.Set<org.bukkit.Material> KNOWN_ITEMS = java.util.Set.of(
            org.bukkit.Material.SADDLE, org.bukkit.Material.GOLDEN_CARROT, org.bukkit.Material.WHEAT,
            org.bukkit.Material.GOLD_INGOT, org.bukkit.Material.CHEST, org.bukkit.Material.OAK_SIGN,
            org.bukkit.Material.BARREL, org.bukkit.Material.COMPASS, org.bukkit.Material.IRON_PICKAXE,
            org.bukkit.Material.ENDER_EYE, org.bukkit.Material.NETHERITE_SWORD, org.bukkit.Material.FIREWORK_ROCKET,
            org.bukkit.Material.TORCH);
    @Test void acceptsKnownLivePairAndDetailsButRejectsForgedSourceAndUnsafeAmounts() {
        var context = new HashMap<>(Map.of("source", "SmartNPCWorkers", "event", "worker_produced", "amount", "64", "material", "COBBLESTONE"));
        assertEquals("64", SuiteMilestoneContext.validated(context).get("amount"));
        assertEquals("COBBLESTONE", SuiteMilestoneContext.validated(context).get("material"));
        context.put("amount", Long.toString(Long.MAX_VALUE));
        assertEquals(Integer.toString(Integer.MAX_VALUE), SuiteMilestoneContext.validated(context).get("amount"));
        for (String amount : java.util.List.of("0", "-1", "NaN", "999999999999999999999999999")) {
            context.put("amount", amount); assertTrue(SuiteMilestoneContext.validated(context).isEmpty());
        }
        context.put("amount", "1"); context.put("source", "AdaptiveBosses");
        assertTrue(SuiteMilestoneContext.validated(context).isEmpty());
        context.put("source", "SmartNPCWorkers"); context.put("event", "unknown_event");
        assertTrue(SuiteMilestoneContext.validated(context).isEmpty());
        context.remove("event"); assertTrue(SuiteMilestoneContext.validated(context).isEmpty());
    }

    @Test void disabledSamplesLeaveExistingOwnerCatalogExactlyUntouched() {
        var owner = bundled("achievements.yml");
        String before = owner.saveToString();
        var disabled = bundled("suite-achievements.yml");
        disabled.set("achievements.invalid", "ignored while disabled");
        assertSame(owner, merge(owner, disabled, bundled("gui.yml")));
        assertEquals(before, owner.saveToString());
    }

    @Test void everyBundledExampleFitsTheBundledCatalogWithoutChangingOwnerDefinitions() {
        var owner = bundled("achievements.yml"); var samples = bundled("suite-achievements.yml");
        String before = owner.saveToString(); samples.set("enabled", true);
        var merged = merge(owner, samples, bundled("gui.yml"));
        assertEquals(owner.getConfigurationSection("achievements").getKeys(false).size() + 12,
                merged.getConfigurationSection("achievements").getKeys(false).size());
        assertEquals(before, owner.saveToString());
        for (String path : owner.getKeys(true)) if (!owner.isConfigurationSection(path)) assertEquals(owner.get(path), merged.get(path), path);
        var profile = new dev.erissos.betteradvancements.model.PlayerProfile(java.util.UUID.randomUUID());
        var completed = new dev.erissos.betteradvancements.model.PlayerAchievementProgress(); completed.complete();
        profile.getAdvancementProgress().put("suite_animal_registered", completed);
        assertEquals(1, profile.getCompletedAdvancements(merged.getConfigurationSection("achievements").getKeys(false)));
        assertEquals(0, profile.getCompletedAdvancements(owner.getConfigurationSection("achievements").getKeys(false)));
        assertSame(completed, profile.getAdvancementProgress().get("suite_animal_registered"));
    }

    @Test void duplicateIdsOccupiedSlotsNavigationAndUnsupportedPagesFailBeforeMerge() {
        var owner = bundled("achievements.yml"); var gui = bundled("gui.yml");
        var duplicate = example(); duplicate.set("achievements.t1_welcome_home", duplicate.get("achievements.test"));
        assertThrows(IllegalArgumentException.class, () -> merge(owner, duplicate, gui));
        for (int slot : new int[]{10, 4, 45, 9, 17}) {
            var samples = example(); samples.set("achievements.test.gui.slot", slot);
            assertThrows(IllegalArgumentException.class, () -> merge(owner, samples, gui));
        }
        var page = example(); page.set("achievements.test.gui.page", 1);
        assertThrows(IllegalArgumentException.class, () -> merge(owner, page, gui));
        var customNavigation = bundled("gui.yml"); customNavigation.set("tier.navigation.back.slot", 11);
        assertThrows(IllegalArgumentException.class, () -> merge(owner, example(), customNavigation));
    }

    @Test void invalidRewardsSourceTargetsAndDependencyCyclesPreserveOwnerSnapshot() {
        var owner = bundled("achievements.yml"); var gui = bundled("gui.yml"); String before = owner.saveToString();
        for (var mutation : java.util.List.of(
                Map.entry("trigger.conditions.source", "DynamicBounty"), Map.entry("trigger.target", 0),
                Map.entry("trigger.type", "MOB_KILL"), Map.entry("points", -1),
                Map.entry("dependencies", java.util.List.of("test")), Map.entry("dependencies", java.util.List.of("missing")),
                Map.entry("rewards.first.type", "ITEM"))) {
            var samples = example(); samples.set("achievements.test." + mutation.getKey(), mutation.getValue());
            assertThrows(IllegalArgumentException.class, () -> merge(owner, samples, gui));
            assertEquals(before, owner.saveToString());
        }
        var invalidIcon = example(); invalidIcon.set("achievements.test.icon", "WATER");
        assertThrows(IllegalArgumentException.class, () -> merge(owner, invalidIcon, gui));
        var invalidReward = example(); invalidReward.set("achievements.test.rewards.first.type", "ITEM");
        invalidReward.set("achievements.test.rewards.first.amount", 1); invalidReward.set("achievements.test.rewards.first.value", "WATER");
        assertThrows(IllegalArgumentException.class, () -> merge(owner, invalidReward, gui));
        invalidReward.set("achievements.test.rewards.first.value", "TORCH");
        assertEquals("TORCH", merge(owner, invalidReward, gui).getString("achievements.test.rewards.first.value"));
    }

    private static org.bukkit.configuration.file.FileConfiguration merge(org.bukkit.configuration.file.FileConfiguration owner,
            org.bukkit.configuration.file.FileConfiguration examples, org.bukkit.configuration.file.FileConfiguration gui) {
        return SuiteAchievementCatalog.merge(owner, examples, gui, KNOWN_ITEMS::contains);
    }

    private static YamlConfiguration example() {
        var yaml = new YamlConfiguration(); yaml.set("enabled", true);
        String root = "achievements.test.";
        yaml.set(root + "title", "New companion"); yaml.set(root + "description", "Register your first stable animal.");
        yaml.set(root + "tier", "TIER_1"); yaml.set(root + "icon", "SADDLE"); yaml.set(root + "gui.slot", 11);
        yaml.set(root + "trigger.type", "CUSTOM"); yaml.set(root + "trigger.target", 1);
        yaml.set(root + "trigger.conditions.source", "StableMan"); yaml.set(root + "trigger.conditions.event", "animal_registered");
        return yaml;
    }

    private static YamlConfiguration bundled(String name) {
        var stream = SuiteAchievementCatalogTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var yaml = new YamlConfiguration(); yaml.load(reader); return yaml;
        } catch (Exception failure) { throw new AssertionError(name, failure); }
    }
}
