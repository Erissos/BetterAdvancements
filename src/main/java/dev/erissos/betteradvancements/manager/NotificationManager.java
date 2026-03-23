package dev.erissos.betteradvancements.manager;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.lang.LanguageManager;
import dev.erissos.betteradvancements.model.BetterAdvancement;
import dev.erissos.betteradvancements.util.ItemUtils;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

public final class NotificationManager {

    private final BetterAdvancementsPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;

    public NotificationManager(BetterAdvancementsPlugin plugin, ConfigManager configManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageManager = languageManager;
    }

    public void notifyCompletion(Player player, BetterAdvancement advancement, LanguageManager languageManager, String locale) {
        Map<String, String> placeholders = Map.of(
                "title", advancement.title(),
                "description", advancement.description(),
                "tier", advancement.tier().name(),
                "points", String.valueOf(advancement.points())
        );

        if (configManager.getMainConfig().getBoolean("notifications.chat", true)) {
            player.sendMessage(this.languageManager.format(locale, "notifications.chat", placeholders));
        }
        if (configManager.getMainConfig().getBoolean("notifications.title", true)) {
            player.showTitle(Title.title(
                    ItemUtils.component(this.languageManager.format(locale, "notifications.title-main", placeholders)),
                    ItemUtils.component(this.languageManager.format(locale, "notifications.title-sub", placeholders)),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
        }
        if (configManager.getMainConfig().getBoolean("notifications.action-bar", true)) {
            player.sendActionBar(ItemUtils.component(this.languageManager.format(locale, "notifications.action-bar-message", placeholders)));
        }
        if (configManager.getMainConfig().getBoolean("notifications.boss-bar", true)) {
            BossBar bossBar = Bukkit.createBossBar(this.languageManager.format(locale, "notifications.boss-bar-message", placeholders), BarColor.GREEN, BarStyle.SOLID);
            bossBar.addPlayer(player);
            bossBar.setProgress(1.0D);
            Bukkit.getScheduler().runTaskLater(plugin, bossBar::removeAll, 80L);
        }
        String soundName = configManager.getMainConfig().getString("notifications.sound", "UI_TOAST_CHALLENGE_COMPLETE");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0F, 1.0F);
        } catch (IllegalArgumentException ignored) {
        }
    }
}