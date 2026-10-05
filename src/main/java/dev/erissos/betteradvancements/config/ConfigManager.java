package dev.erissos.betteradvancements.config;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private FileConfiguration seasonConfig;
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
        ensureResource("season.yml");

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
        // Validate every input before replacing the active configuration on reload.
        dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(),"config.yml"));
        dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(),"database.yml"));
        dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(),"gui.yml"));
        dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(),"achievements.yml"));
        dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(),"challenges.yml"));
        dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(),"season.yml"));
        File[] languageFiles=new File(plugin.getDataFolder(),"lang").listFiles((directory,name) -> name.endsWith(".yml"));
        if (languageFiles!=null) for (File languageFile:languageFiles) dev.erissos.betteradvancements.util.StrictYaml.load(languageFile);

        plugin.reloadConfig();
        this.mainConfig = plugin.getConfig();
        this.databaseConfig = dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(), "database.yml"));
        this.guiConfig = dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(), "gui.yml"));
        this.achievementsConfig = dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(), "achievements.yml"));
        this.challengesConfig = dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(), "challenges.yml"));
        this.seasonConfig = dev.erissos.betteradvancements.util.StrictYaml.load(new File(plugin.getDataFolder(), "season.yml"));
        this.languageConfigs.clear();
        File langFolder = new File(plugin.getDataFolder(), "lang");
        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String key = file.getName().replace(".yml", "").toLowerCase(java.util.Locale.ROOT);
                YamlConfiguration language = dev.erissos.betteradvancements.util.StrictYaml.load(file);
                InputStream resource = plugin.getResource("lang/" + file.getName());
                if (resource != null) {
                    try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                        language.setDefaults(YamlConfiguration.loadConfiguration(reader));
                    } catch (IOException exception) {
                        throw new IllegalStateException("Cannot load language defaults", exception);
                    }
                }
                if (key.equals("tr")) upgradeLegacyTurkish(language);
                languageConfigs.put(key, language);
            }
        }
    }

    private void upgradeLegacyTurkish(YamlConfiguration language) {
        InputStream resource=plugin.getResource("lang-legacy/tr.yml");
        if (resource==null) return;
        try (var reader=new InputStreamReader(resource,StandardCharsets.UTF_8)) {
            var legacy=YamlConfiguration.loadConfiguration(reader);
            for (String path:legacy.getKeys(true)) if (legacy.isString(path)
                    && legacy.getString(path).equals(language.getString(path))
                    && language.getDefaults()!=null && language.getDefaults().isString(path)) {
                language.set(path,language.getDefaults().getString(path));
            }
        } catch (IOException failure) { throw new IllegalStateException("Cannot load legacy Turkish defaults",failure); }
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

    public FileConfiguration getSeasonConfig() {
        return seasonConfig;
    }

    public Map<String, FileConfiguration> getLanguageConfigs() {
        return languageConfigs;
    }
}
