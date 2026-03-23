package dev.erissos.betteradvancements.config;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigManager {

    private final BetterAdvancementsPlugin plugin;
    private FileConfiguration mainConfig;
    private FileConfiguration databaseConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration achievementsConfig;
    private FileConfiguration challengesConfig;
    private final Map<String, FileConfiguration> languageConfigs = new LinkedHashMap<>();

    public ConfigManager(BetterAdvancementsPlugin plugin) {
        this.plugin = plugin;
    }

    public void bootstrap() {
        ensureResource("config.yml");
        ensureResource("database.yml");
        ensureResource("gui.yml");
        ensureResource("achievements.yml");
        ensureResource("challenges.yml");

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            throw new IllegalStateException("Could not create lang folder");
        }
        for (String locale : new String[]{"en", "tr", "ru", "de", "es", "it", "fr", "sk", "cs", "zh", "ro"}) {
            ensureResource("lang/" + locale + ".yml");
        }

        reloadAll();
    }

    public void reloadAll() {
        plugin.reloadConfig();
        this.mainConfig = plugin.getConfig();
        this.databaseConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "database.yml"));
        this.guiConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
        this.achievementsConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "achievements.yml"));
        this.challengesConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "challenges.yml"));
        this.languageConfigs.clear();
        File langFolder = new File(plugin.getDataFolder(), "lang");
        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String key = file.getName().replace(".yml", "").toLowerCase();
                languageConfigs.put(key, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    private void ensureResource(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (target.exists()) {
            return;
        }

        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create folder for " + resourcePath);
        }

        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bundled resource: " + resourcePath);
            }
            Files.copy(inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to copy resource " + resourcePath, exception);
        }
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public FileConfiguration getDatabaseConfig() {
        return databaseConfig;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public FileConfiguration getAchievementsConfig() {
        return achievementsConfig;
    }

    public FileConfiguration getChallengesConfig() {
        return challengesConfig;
    }

    public Map<String, FileConfiguration> getLanguageConfigs() {
        return languageConfigs;
    }
}