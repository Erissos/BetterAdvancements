package dev.erissos.betteradvancements;

import dev.erissos.betteradvancements.api.BetterAdvancementsAPI;
import dev.erissos.betteradvancements.command.BetterAdvancementsCommand;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.gui.GUIManager;
import dev.erissos.betteradvancements.integration.PlaceholderHook;
import dev.erissos.betteradvancements.integration.VaultHook;
import dev.erissos.betteradvancements.lang.LanguageManager;
import dev.erissos.betteradvancements.listener.AdvancementListener;
import dev.erissos.betteradvancements.manager.AchievementManager;
import dev.erissos.betteradvancements.manager.ChallengeManager;
import dev.erissos.betteradvancements.manager.LeaderboardManager;
import dev.erissos.betteradvancements.manager.NotificationManager;
import dev.erissos.betteradvancements.service.BetterAdvancementsService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterAdvancementsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private PlayerDataManager playerDataManager;
    private PlaceholderHook placeholderHook;
    private VaultHook vaultHook;
    private AchievementManager achievementManager;
    private ChallengeManager challengeManager;
    private LeaderboardManager leaderboardManager;
    private NotificationManager notificationManager;
    private GUIManager guiManager;
    private BetterAdvancementsService apiService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.configManager.bootstrap();

        this.placeholderHook = new PlaceholderHook();
        this.vaultHook = new VaultHook(this);
        this.playerDataManager = new PlayerDataManager(this, configManager);
        this.playerDataManager.start();
        this.languageManager = new LanguageManager(this, configManager, placeholderHook);
        this.languageManager.load();

        this.achievementManager = new AchievementManager(this, configManager, playerDataManager, languageManager, vaultHook);
        this.challengeManager = new ChallengeManager(this, configManager, playerDataManager);
        this.leaderboardManager = new LeaderboardManager(this, playerDataManager, achievementManager);
        this.notificationManager = new NotificationManager(this, configManager, languageManager);
        this.guiManager = new GUIManager(this, configManager, achievementManager, challengeManager, leaderboardManager, playerDataManager, languageManager);
        this.apiService = new BetterAdvancementsService(achievementManager, playerDataManager, leaderboardManager);

        this.achievementManager.load();
        this.challengeManager.load();
        this.leaderboardManager.start();

        Bukkit.getPluginManager().registerEvents(new AdvancementListener(this, achievementManager, challengeManager, playerDataManager), this);
        Bukkit.getPluginManager().registerEvents(guiManager, this);

        BetterAdvancementsCommand commandExecutor = new BetterAdvancementsCommand(this, configManager, achievementManager, playerDataManager, guiManager, leaderboardManager, languageManager);
        PluginCommand command = getCommand("ba");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        getServer().getServicesManager().register(BetterAdvancementsAPI.class, apiService, this, org.bukkit.plugin.ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (leaderboardManager != null) {
            leaderboardManager.stop();
        }
        if (playerDataManager != null) {
            playerDataManager.shutdown();
        }
    }

    public void reloadPlugin() {
        configManager.reloadAll();
        languageManager.load();
        achievementManager.load();
        challengeManager.load();
        guiManager.reload();
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }
}