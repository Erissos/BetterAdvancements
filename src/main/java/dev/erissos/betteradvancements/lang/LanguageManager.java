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
        this.defaultLanguage = resolveLanguage(configManager.getMainConfig().getString("general.default-language", "en"));
        if (defaultLanguage == null) defaultLanguage = "en";
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean hasLanguage(String locale) {
        return resolveLanguage(locale) != null;
    }

    public String resolveLanguage(String code) {
        if (code == null) return null;
        String normalized = code.trim().replace('-', '_').toLowerCase(Locale.ROOT);
        if (languages.containsKey(normalized)) return normalized;
        String base = normalized.split("_", 2)[0];
        return normalized.contains("_") && languages.containsKey(base) ? base : null;
    }

    public String getDisplayName(String locale) {
        String normalized = locale == null || locale.isBlank()
                ? defaultLanguage
                : locale.toLowerCase(Locale.ROOT);
        Locale displayLocale = Locale.forLanguageTag(normalized.replace('_', '-'));
        String displayName = displayLocale.getDisplayLanguage(displayLocale);
        if (displayName == null || displayName.isBlank()) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(displayName.charAt(0)) + displayName.substring(1);
    }

    public String getMessage(String locale, String key) {
        String selectedLocale = resolveLanguage(locale);
        FileConfiguration configuration = languages.get(selectedLocale);
        if (configuration != null && configuration.contains(key)) {
            return configuration.getString(key);
        }

        FileConfiguration fallback = languages.get(defaultLanguage);
        if (fallback != null && fallback.contains(key)) {
            return fallback.getString(key);
        }

        return key;
    }

    public String getMessage(Player player, String key) {
        return getMessage(getLocale(player), key);
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
            String selected = plugin.getPlayerDataManager().getCachedProfile(player.getUniqueId())
                    .map(profile -> resolveLanguage(profile.getLanguage())).orElse(defaultLanguage);
            return selected == null ? defaultLanguage : selected;
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

    /** Preserve explicitly customized display names, otherwise use the native game's selected-language name. */
    public String nativeName(CommandSender viewer, String identifier, String fallback) {
        FileConfiguration selected = languages.get(getLocale(viewer));
        if (selected == null) return fallback;
        String path = "values." + identifier.toLowerCase(Locale.ROOT);
        String value = selected.getString(path);
        String bundled = selected.getDefaults() == null ? null : selected.getDefaults().getString(path);
        return value != null && !value.equals(bundled) ? value : fallback;
    }

    public String text(CommandSender viewer, String path, String fallback) {
        if (getLocale(viewer).equals("en") && path.startsWith("gui.")) return fallback;
        FileConfiguration selected = languages.get(getLocale(viewer));
        return selected != null && selected.isString(path) ? selected.getString(path) : fallback;
    }

    public java.util.List<String> lines(CommandSender viewer, String path, java.util.List<String> fallback) {
        if (getLocale(viewer).equals("en") && path.startsWith("gui.")) return fallback;
        FileConfiguration selected = languages.get(getLocale(viewer));
        return selected != null && selected.isList(path) ? selected.getStringList(path) : fallback;
    }

    public String content(CommandSender viewer, String type, String id, String field, String fallback) {
        String path="content."+type+"."+id+".";
        String source=text(viewer,path+"source-"+field,fallback);
        return source.equals(fallback) ? text(viewer,path+field,fallback) : fallback;
    }
}
