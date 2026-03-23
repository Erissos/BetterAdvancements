package dev.erissos.betteradvancements.lang;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.integration.PlaceholderHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LanguageManager {

    private final BetterAdvancementsPlugin plugin;
    private final ConfigManager configManager;
    private final PlaceholderHook placeholderHook;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String defaultLanguage = "en";

    public LanguageManager(BetterAdvancementsPlugin plugin, ConfigManager configManager, PlaceholderHook placeholderHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.placeholderHook = placeholderHook;
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
        return configuration == null ? key : configuration.getString(key, key);
    }

    public String getMessage(Player player, String key) {
        String selectedLocale = plugin.getPlayerDataManager().getCachedProfile(player.getUniqueId())
                .map(profile -> profile.getLanguage().toLowerCase(Locale.ROOT))
                .orElse(defaultLanguage);
        return getMessage(selectedLocale, key);
    }

    public Component getComponent(String locale, String key, Map<String, String> placeholders) {
        return deserialize(null, getMessage(locale, key), placeholders);
    }

    public Component getComponent(CommandSender sender, String locale, String key, Map<String, String> placeholders) {
        return deserialize(sender, getMessage(locale, key), placeholders);
    }

    public Component deserialize(CommandSender sender, String rawMessage, Map<String, String> placeholders) {
        String message = applyExternalPlaceholders(sender, rawMessage);
        TagResolver.Builder resolver = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolver.resolver(Placeholder.unparsed(entry.getKey(), entry.getValue()));
        }
        return miniMessage.deserialize(message == null ? "" : message, resolver.build());
    }

    public String getLocale(CommandSender sender) {
        if (sender instanceof Player player) {
            return plugin.getPlayerDataManager().getOrCreate(player.getUniqueId()).getLanguage().toLowerCase(Locale.ROOT);
        }
        return defaultLanguage;
    }

    private String applyExternalPlaceholders(CommandSender sender, String input) {
        if (sender instanceof Player player && placeholderHook.isAvailable()) {
            return placeholderHook.apply(player, input);
        }
        return input;
    }

    public Map<String, FileConfiguration> getLanguages() {
        return languages;
    }
}