package dev.erissos.betteradvancements.lang;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LanguageManager {

    private final BetterAdvancementsPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private String defaultLanguage = "en";

    public LanguageManager(BetterAdvancementsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void load() {
        this.languages.clear();
        this.languages.putAll(configManager.getLanguageConfigs());
        this.defaultLanguage = configManager.getMainConfig().getString("general.default-language", "en").toLowerCase(Locale.ROOT);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean hasLanguage(String locale) {
        return languages.containsKey(locale.toLowerCase(Locale.ROOT));
    }

    public String getMessage(String locale, String key) {
        FileConfiguration configuration = languages.getOrDefault(locale.toLowerCase(Locale.ROOT), languages.get(defaultLanguage));
        String raw = configuration == null ? key : configuration.getString(key, key);
        return raw.replace('&', '§');
    }

    public String getMessage(Player player, String key) {
        String selectedLocale = plugin.getPlayerDataManager().getCachedProfile(player.getUniqueId())
            .map(profile -> profile.getLanguage().toLowerCase(Locale.ROOT))
            .orElse(defaultLanguage);
        return getMessage(selectedLocale, key);
    }

    public String format(String locale, String key, Map<String, String> placeholders) {
        String message = getMessage(locale, key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String format(CommandSender sender, String locale, String key, Map<String, String> placeholders) {
        return format(locale, key, placeholders);
    }

    public Map<String, FileConfiguration> getLanguages() {
        return languages;
    }
}