package dev.erissos.betteradvancements.integration;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Optional examples merge in memory only, without rewriting an administrator's achievement catalog. */
public final class SuiteAchievementCatalog {
    private SuiteAchievementCatalog() {}

    public static FileConfiguration merge(FileConfiguration owner, FileConfiguration examples, FileConfiguration gui) {
        return merge(owner, examples, gui, material -> !material.isAir() && material.isItem());
    }

    /** Tests inject material policy only; production always queries the actual Paper item registry. */
    static FileConfiguration merge(FileConfiguration owner, FileConfiguration examples, FileConfiguration gui, Predicate<Material> validItem) {
        java.util.Objects.requireNonNull(validItem, "validItem");
        if (examples.contains("enabled") && !examples.isBoolean("enabled")) fail("enabled must be a boolean");
        if (!examples.getBoolean("enabled", false)) return owner;
        ConfigurationSection additions = examples.getConfigurationSection("achievements");
        if (additions == null) fail("enabled catalog needs achievements");
        Map<String, String> tiers = tierAliases(gui);
        Set<String> occupied = new HashSet<>();
        Set<Integer> blocked = new HashSet<>(gui.getIntegerList("tier.blocked-slots"));
        ConfigurationSection tierUi = gui.getConfigurationSection("tier");
        if (tierUi != null) for (String path : tierUi.getKeys(true)) if (path.endsWith(".slot")) blocked.add(tierUi.getInt(path));
        ConfigurationSection original = owner.getConfigurationSection("achievements");
        if (original != null) for (String id : original.getKeys(false)) {
            ConfigurationSection node = original.getConfigurationSection(id);
            if (node != null) occupied.add(resolveTier(tiers, node.getString("tier", "TIER_1")) + ":" + node.getInt("gui.slot", 0));
        }
        Set<String> allIds = new HashSet<>(additions.getKeys(false));
        if (original != null) allIds.addAll(original.getKeys(false));
        for (String id : additions.getKeys(false)) {
            if (!id.matches("[a-zA-Z0-9_-]{1,80}") || (original != null && original.contains(id))) fail("duplicate or invalid id: " + id);
            ConfigurationSection node = additions.getConfigurationSection(id);
            if (node == null) fail("achievement is not a section: " + id);
            if (!node.getString("trigger.type", "").equalsIgnoreCase("CUSTOM")) fail(id + " must use CUSTOM");
            if (!SuiteMilestoneContext.knownPair(node.getString("trigger.conditions.source"), node.getString("trigger.conditions.event"))) fail(id + " has an unknown source/event pair");
            if (!node.isInt("trigger.target") || node.getInt("trigger.target") < 1) fail(id + " needs a positive integer target");
            if (node.contains("points") && (!node.isInt("points") || node.getInt("points") < 0)) fail(id + " points must be a nonnegative integer");
            if (!node.isString("title") || !node.isString("description")) fail(id + " needs title and description text");
            Material icon = Material.matchMaterial(node.getString("icon", "BOOK"));
            if (icon == null || !validItem.test(icon)) fail(id + " has an invalid icon");
            String tier = resolveTier(tiers, node.getString("tier", "TIER_1"));
            if (!node.isInt("gui.slot")) fail(id + " needs an integer gui.slot");
            int slot = node.getInt("gui.slot");
            if ((node.contains("gui.page") && !node.isInt("gui.page")) || node.getInt("gui.page", 0) != 0) fail(id + " uses an unsupported page; tier menus have one page");
            if (slot < 10 || slot > 43 || slot % 9 == 0 || slot % 9 == 8 || blocked.contains(slot)) fail(id + " overlaps a frame/navigation slot");
            if (!occupied.add(tier + ":" + slot)) fail(id + " overlaps another node in " + tier + " slot " + slot);
            if (node.contains("dependencies") && (!node.isList("dependencies")
                    || node.getList("dependencies").stream().anyMatch(value -> !(value instanceof String)))) fail(id + " dependencies must be a text list");
            for (String dependency : node.getStringList("dependencies")) if (!allIds.contains(dependency)) fail(id + " has a missing dependency: " + dependency);
            validateRewards(node, id, validItem);
        }
        YamlConfiguration merged = new YamlConfiguration();
        try { merged.loadFromString(owner.saveToString()); }
        catch (org.bukkit.configuration.InvalidConfigurationException invalid) { throw new IllegalStateException("Cannot snapshot achievement catalog", invalid); }
        for (String id : additions.getKeys(false)) {
            ConfigurationSection node = additions.getConfigurationSection(id);
            for (String path : node.getKeys(true)) if (!node.isConfigurationSection(path)) merged.set("achievements." + id + "." + path, node.get(path));
        }
        for (String id : additions.getKeys(false)) validateDependencies(merged.getConfigurationSection("achievements"), id, new HashSet<>());
        return merged;
    }

    private static void validateRewards(ConfigurationSection node, String id, Predicate<Material> validItem) {
        ConfigurationSection rewards = node.getConfigurationSection("rewards");
        if (rewards == null) return;
        for (String key : rewards.getKeys(false)) {
            ConfigurationSection reward = rewards.getConfigurationSection(key);
            if (reward == null) fail(id + " has a malformed reward");
            String type = reward.getString("type", "POINTS").toUpperCase(Locale.ROOT);
            try { dev.erissos.betteradvancements.model.RewardType.valueOf(type); }
            catch (RuntimeException invalid) { fail(id + " has an unknown reward type"); }
            if (reward.contains("amount") && (!reward.isInt("amount") || reward.getInt("amount") < 0)) fail(id + " has a negative/noninteger reward amount");
            if (type.equals("ITEM")) {
                Material item = Material.matchMaterial(reward.getString("value", ""));
                if (item == null || !validItem.test(item) || reward.getInt("amount") < 1) fail(id + " has an invalid item reward");
            }
        }
    }

    private static void validateDependencies(ConfigurationSection nodes, String id, Set<String> route) {
        if (!route.add(id)) fail("cyclic dependency through " + id);
        ConfigurationSection node = nodes.getConfigurationSection(id);
        if (node == null) fail("missing dependency " + id);
        for (String dependency : node.getStringList("dependencies")) validateDependencies(nodes, dependency, route);
        route.remove(id);
    }

    private static Map<String, String> tierAliases(FileConfiguration gui) {
        Map<String, String> aliases = new HashMap<>();
        ConfigurationSection section = gui.getConfigurationSection("main.tiers");
        if (section == null) {
            List<String> defaults = List.of("novice", "skilled", "expert", "master", "mythic");
            for (int index = 0; index < defaults.size(); index++) {
                String name = defaults.get(index);
                aliases.put(normalize(name), name); aliases.put("TIER_" + (index + 1), name); aliases.put(Integer.toString(index + 1), name);
            }
            return aliases;
        }
        List<String> order = section.getStringList("order");
        if (order.isEmpty()) order = section.getKeys(false).stream().filter(key -> section.isConfigurationSection(key)).toList();
        int nextWeight = 1;
        for (String key : order) {
            ConfigurationSection tier = section.getConfigurationSection(key);
            if (tier == null) continue;
            int weight = Math.max(1, tier.getInt("weight", nextWeight));
            aliases.put(normalize(key), key); aliases.put(normalize(tier.getString("display-name", key)), key);
            aliases.put("TIER_" + weight, key); aliases.put(Integer.toString(weight), key);
            for (String alias : tier.getStringList("aliases")) aliases.put(normalize(alias), key);
            nextWeight = weight + 1;
        }
        if (aliases.isEmpty()) return tierAliases(new YamlConfiguration());
        return aliases;
    }

    private static String resolveTier(Map<String, String> aliases, String input) {
        String value = aliases.get(normalize(input));
        if (value == null) fail("unknown tier: " + input);
        return value;
    }

    private static String normalize(String value) { return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT); }
    private static void fail(String reason) { throw new IllegalArgumentException("suite-achievements.yml: " + reason); }
}
