package dev.erissos.betteradvancements.manager;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.lang.LanguageManager;
import dev.erissos.betteradvancements.model.BetterAdvancement;
import dev.erissos.betteradvancements.model.PlayerProfile;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
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

    public void notifyCompletion(Player player, BetterAdvancement advancement, String locale) {
        PlayerProfile profile = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId());
        Map<String, String> placeholders = Map.of(
                "title", languageManager.content(player,"advancements",advancement.id(),"title",advancement.title()),
                "description", languageManager.content(player,"advancements",advancement.id(),"description",advancement.description()),
            "tier", languageManager.text(player,"gui.main.tiers."+advancement.tier().name()+".display-name",advancement.tier().getDisplayKey()),
                "points", String.valueOf(advancement.points())
        );

        if (configManager.getMainConfig().getBoolean("notifications.chat", true) && profile.isChatNotificationsEnabled()) {
            player.sendMessage(this.languageManager.getComponent(player, locale, "notifications.chat", placeholders));
        }
        if (configManager.getMainConfig().getBoolean("notifications.title", true) && profile.isTitleNotificationsEnabled()) {
            player.showTitle(Title.title(
                    this.languageManager.getComponent(player, locale, "notifications.title-main", placeholders),
                    this.languageManager.getComponent(player, locale, "notifications.title-sub", placeholders),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
        }
        if (configManager.getMainConfig().getBoolean("notifications.action-bar", true) && profile.isActionBarNotificationsEnabled()) {
            player.sendActionBar(this.languageManager.getComponent(player, locale, "notifications.action-bar-message", placeholders));
        }
        if (configManager.getMainConfig().getBoolean("notifications.boss-bar", true) && profile.isBossBarNotificationsEnabled()) {
            Component bossBarTitle = this.languageManager.getComponent(player, locale, "notifications.boss-bar-message", placeholders);
            BossBar bossBar = BossBar.bossBar(bossBarTitle, 1.0F, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
            player.showBossBar(bossBar);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.hideBossBar(bossBar), 80L);
        }
        String soundName = configManager.getMainConfig().getString("notifications.sound", "UI_TOAST_CHALLENGE_COMPLETE");
        if (profile.isSoundNotificationsEnabled()) {
            Sound sound = dev.erissos.betteradvancements.util.SoundResolver.parse(soundName, null);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
            }
        }
    }
}
