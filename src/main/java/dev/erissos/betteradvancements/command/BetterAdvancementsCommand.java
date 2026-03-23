package dev.erissos.betteradvancements.command;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.gui.GUIManager;
import dev.erissos.betteradvancements.lang.LanguageManager;
import dev.erissos.betteradvancements.manager.AchievementManager;
import dev.erissos.betteradvancements.manager.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BetterAdvancementsCommand implements CommandExecutor, TabCompleter {

    private final BetterAdvancementsPlugin plugin;
    private final AchievementManager achievementManager;
    private final PlayerDataManager playerDataManager;
    private final GUIManager guiManager;
    private final LeaderboardManager leaderboardManager;
    private final LanguageManager languageManager;

    public BetterAdvancementsCommand(BetterAdvancementsPlugin plugin, ConfigManager configManager, AchievementManager achievementManager, PlayerDataManager playerDataManager, GUIManager guiManager, LeaderboardManager leaderboardManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.achievementManager = achievementManager;
        this.playerDataManager = playerDataManager;
        this.guiManager = guiManager;
        this.leaderboardManager = leaderboardManager;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String locale = sender instanceof Player player
                ? playerDataManager.getOrCreate(player.getUniqueId()).getLanguage()
                : languageManager.getDefaultLanguage();

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command is player-only.");
                return true;
            }
            guiManager.openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "stats" -> {
                if (sender instanceof Player player) {
                    guiManager.openStats(player);
                }
                return true;
            }
            case "leaderboard" -> {
                if (sender instanceof Player player) {
                    guiManager.openLeaderboard(player, args.length > 1 && args[1].equalsIgnoreCase("session"));
                }
                return true;
            }
            case "language" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    return true;
                }
                String requested = args[1].toLowerCase(Locale.ROOT);
                if (!languageManager.hasLanguage(requested)) {
                    sender.sendMessage(languageManager.format(locale, "command.invalid-language", Map.of("language", requested)));
                    return true;
                }
                playerDataManager.getOrCreate(player.getUniqueId()).setLanguage(requested);
                playerDataManager.saveProfile(player.getUniqueId());
                sender.sendMessage(languageManager.format(requested, "command.language-set", Map.of("language", requested)));
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("ba.admin")) {
                    sender.sendMessage(languageManager.format(locale, "command.no-permission", Map.of()));
                    return true;
                }
                plugin.reloadPlugin();
                leaderboardManager.refresh();
                sender.sendMessage(languageManager.format(locale, "command.reload", Map.of()));
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("ba.admin") || args.length < 3) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(languageManager.format(locale, "command.player-not-found", Map.of("player", args[1])));
                    return true;
                }
                boolean result = achievementManager.forceGrant(target.getUniqueId(), args[2]);
                sender.sendMessage(languageManager.format(locale, result ? "command.give-success" : "command.give-failed", Map.of("player", target.getName(), "achievement", args[2])));
                return true;
            }
            case "reset" -> {
                if (!sender.hasPermission("ba.admin") || args.length < 2) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(languageManager.format(locale, "command.player-not-found", Map.of("player", args[1])));
                    return true;
                }
                playerDataManager.resetProfile(target.getUniqueId());
                playerDataManager.loadProfile(target);
                sender.sendMessage(languageManager.format(locale, "command.reset", Map.of("player", target.getName())));
                return true;
            }
            default -> {
                sender.sendMessage(languageManager.format(locale, "command.usage", Map.of()));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("menu", "stats", "leaderboard", "language", "reload", "give", "reset");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            return List.of("global", "session");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("language")) {
            return new ArrayList<>(languageManager.getLanguages().keySet());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return achievementManager.getAdvancements().stream().map(dev.erissos.betteradvancements.model.BetterAdvancement::id).toList();
        }
        return List.of();
    }
}