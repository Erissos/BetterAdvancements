package dev.erissos.betteradvancements.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class Tier {

    private static final List<String> RESERVED_MAIN_TIER_KEYS = List.of("order", "auto-slots", "connector-slots", "connector-open", "connector-locked");
    private static final Map<String, Tier> REGISTRY = new LinkedHashMap<>();
    private static final Map<String, Tier> LOOKUP = new LinkedHashMap<>();

    private final String key;
    private final int weight;
    private final String displayKey;

    static {
        loadDefaults();
    }

    private Tier(String key, int weight, String displayKey) {
        this.key = key;
        this.weight = weight;
        this.displayKey = displayKey;
    }

    public String name() {
        return key;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    public static synchronized void loadFromConfig(FileConfiguration guiConfig) {
        ConfigurationSection tiersSection = guiConfig.getConfigurationSection("main.tiers");
        if (tiersSection == null) {
            loadDefaults();
            return;
        }

        List<String> configuredOrder = tiersSection.getStringList("order");
        List<String> orderedKeys = new ArrayList<>();
        if (!configuredOrder.isEmpty()) {
            orderedKeys.addAll(configuredOrder);
        } else {
            for (String key : tiersSection.getKeys(false)) {
                if (!RESERVED_MAIN_TIER_KEYS.contains(key)) {
                    orderedKeys.add(key);
                }
            }
        }

        if (orderedKeys.isEmpty()) {
            loadDefaults();
            return;
        }

        REGISTRY.clear();
        LOOKUP.clear();

        int nextWeight = 1;
        for (String key : orderedKeys) {
            ConfigurationSection section = tiersSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            int weight = Math.max(1, section.getInt("weight", nextWeight));
            String displayName = section.getString("display-name", prettify(key));
            Tier tier = new Tier(key, weight, displayName);
            register(tier, buildAliases(section, key, displayName, weight));
            nextWeight = weight + 1;
        }

        if (REGISTRY.isEmpty()) {
            loadDefaults();
        }
    }

    public static synchronized void loadDefaults() {
        REGISTRY.clear();
        LOOKUP.clear();
        register(new Tier("novice", 1, "Novice"), List.of("TIER_1", "novice", "1"));
        register(new Tier("skilled", 2, "Skilled"), List.of("TIER_2", "skilled", "2"));
        register(new Tier("expert", 3, "Expert"), List.of("TIER_3", "expert", "3"));
        register(new Tier("master", 4, "Master"), List.of("TIER_4", "master", "4"));
        register(new Tier("mythic", 5, "Mythic"), List.of("TIER_5", "mythic", "5"));
    }

    public static Tier fromString(String value) {
        if (value == null || value.isBlank()) {
            return first();
        }
        Tier tier = LOOKUP.get(normalize(value));
        if (tier != null) {
            return tier;
        }
        throw new IllegalArgumentException("Unknown tier: " + value);
    }

    public static Tier first() {
        return REGISTRY.values().stream().findFirst().orElse(new Tier("novice", 1, "Novice"));
    }

    public static Tier[] values() {
        Collection<Tier> tiers = REGISTRY.values();
        return tiers.toArray(new Tier[0]);
    }

    private static List<String> buildAliases(ConfigurationSection section, String key, String displayName, int weight) {
        List<String> aliases = new ArrayList<>(section.getStringList("aliases"));
        aliases.add(key);
        aliases.add(displayName);
        aliases.add(String.valueOf(weight));
        aliases.add("TIER_" + weight);
        return aliases;
    }

    private static void register(Tier tier, List<String> aliases) {
        REGISTRY.put(normalize(tier.key), tier);
        LOOKUP.put(normalize(tier.key), tier);
        for (String alias : aliases) {
            LOOKUP.put(normalize(alias), tier);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private static String prettify(String value) {
        String[] parts = value.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return builder.isEmpty() ? value : builder.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Tier tier)) {
            return false;
        }
        return normalize(key).equals(normalize(tier.key));
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalize(key));
    }

    @Override
    public String toString() {
        return key;
    }
}