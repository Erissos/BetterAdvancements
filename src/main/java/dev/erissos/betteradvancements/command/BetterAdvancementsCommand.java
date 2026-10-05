package dev.erissos.betteradvancements.command;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.gui.GUIManager;
import dev.erissos.betteradvancements.lang.LanguageManager;
import dev.erissos.betteradvancements.manager.AchievementManager;
import dev.erissos.betteradvancements.manager.ChallengeManager;
import dev.erissos.betteradvancements.manager.LeaderboardManager;
import dev.erissos.betteradvancements.manager.SeasonManager;
import dev.erissos.betteradvancements.model.ChallengeDefinition;
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
    private final ConfigManager configManager;
    private final AchievementManager achievementManager;
    private final PlayerDataManager playerDataManager;
    private final GUIManager guiManager;
    private final LeaderboardManager leaderboardManager;
    private final LanguageManager languageManager;
    private final ChallengeManager challengeManager;
    private final SeasonManager seasonManager;

    public BetterAdvancementsCommand(BetterAdvancementsPlugin plugin, ConfigManager configManager, AchievementManager achievementManager, PlayerDataManager playerDataManager, GUIManager guiManager, LeaderboardManager leaderboardManager, LanguageManager languageManager, ChallengeManager challengeManager, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.achievementManager = achievementManager;
        this.playerDataManager = playerDataManager;
        this.guiManager = guiManager;
        this.leaderboardManager = leaderboardManager;
        this.languageManager = languageManager;
        this.challengeManager = challengeManager;
        this.seasonManager = seasonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try { return execute(sender,command,label,args); }
        catch (RuntimeException failure) {
            plugin.getLogger().warning("Command failed: "+failure.getMessage());
            sender.sendMessage(languageManager.getComponent(sender,languageManager.getLocale(sender),"command.storage-error",Map.of()));
            return true;
        }
    }

    private boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length>0 && args[0].equalsIgnoreCase("rewards")) {
            if (!sender.hasPermission("ba.admin")) return true;
            if (args.length==2) sender.sendMessage(plugin.getRewardDelivery().review(java.util.UUID.fromString(args[1])).toString());
            else if (args.length==4 && (args[3].equalsIgnoreCase("applied") || args[3].equalsIgnoreCase("retry"))) {
                plugin.getRewardDelivery().reconcile(java.util.UUID.fromString(args[1]),args[2],args[3].equalsIgnoreCase("applied")); sender.sendMessage("Reward resolved.");
            } else sender.sendMessage("/ba rewards <player UUID> [reward id applied|retry]");
            return true;
        }

        if (sender instanceof Player player) playerDataManager.getOrCreate(player.getUniqueId());
        String locale = languageManager.getLocale(sender);

        if (sender instanceof Player && !sender.hasPermission("ba.use")) {
            sender.sendMessage(languageManager.getComponent(sender, locale, "command.no-permission", Map.of()));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-only", Map.of()));
                return true;
            }
            guiManager.openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "claim" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-only", Map.of())); return true;
                }
                int count = plugin.getRewardDelivery().claim(player);
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.reward-claim", Map.of("count", String.valueOf(count))));
                return true;
            }
            case "help" -> {
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.general-help", Map.of("command", label)));
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.usage", Map.of()));
                return true;
            }
            case "stats" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-only", Map.of()));
                    return true;
                }
                guiManager.openStats(player);
                return true;
            }
            case "leaderboard" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-only", Map.of()));
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("season")) {
                    int rank = 1;
                    for (var entry : leaderboardManager.getSeasonLeaderboard(10)) {
                        sender.sendMessage(languageManager.getComponent(sender, locale, "command.season-leader-line", Map.of(
                                "rank", String.valueOf(rank++),
                                "player", entry.name(),
                                "points", String.valueOf(entry.points())
                        )));
                    }
                    return true;
                }
                guiManager.openLeaderboard(player, args.length > 1 && args[1].equalsIgnoreCase("session"));
                return true;
            }
            case "language", "lang", "locale" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-only", Map.of()));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.language-info", Map.of(
                            "language", locale, "languages", String.join(", ", languageManager.getLanguages().keySet().stream().sorted().toList()))));
                    return true;
                }
                String requested = languageManager.resolveLanguage(args[1]);
                if (requested == null) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.invalid-language", Map.of("language", args[1])));
                    return true;
                }
                playerDataManager.setLastKnownName(player.getUniqueId(), player.getName());
                try {
                    plugin.setPersonalLanguage(player.getUniqueId(), requested);
                } catch (RuntimeException failure) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.storage-error", Map.of())); return true;
                }
                sender.sendMessage(languageManager.getComponent(sender, requested, "command.language-set", Map.of("language", languageManager.getDisplayName(requested))));
                guiManager.refreshLanguage(player);
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("ba.reload") && !sender.hasPermission("ba.admin")) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.no-permission", Map.of()));
                    return true;
                }
                plugin.reloadPlugin();
                leaderboardManager.refresh();
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.reload", Map.of()));
                return true;
            }
            case "season" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-only", Map.of()));
                    return true;
                }
                if (args.length == 1 || (args.length > 1 && args[1].equalsIgnoreCase("menu"))) {
                    guiManager.openSeason(player);
                    return true;
                }
                var profile = playerDataManager.getOrCreate(player.getUniqueId());
                if (args.length > 1 && args[1].equalsIgnoreCase("rewards")) {
                    for (Integer threshold : seasonManager.getRewardThresholds()) {
                        boolean claimed = profile.getClaimedSeasonRewards().contains(threshold);
                        String status = languageManager.getMessage(locale, claimed ? "command.status.claimed" : "command.status.available");
                        sender.sendMessage(languageManager.getComponent(sender, locale, "command.season-reward-line", Map.of(
                                "threshold", String.valueOf(threshold),
                                "status", status
                        )));
                    }
                    return true;
                }
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.season-info", Map.of(
                        "season", seasonManager.currentSeasonId(),
                        "season_points", String.valueOf(profile.getSeasonPoints()),
                        "points", String.valueOf(profile.getPoints()),
                        "prestige", String.valueOf(profile.getPrestigeLevel())
                )));
                return true;
            }
            case "prestige" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-only", Map.of()));
                    return true;
                }
                if (args.length == 1 || (args.length > 1 && args[1].equalsIgnoreCase("menu"))) {
                    guiManager.openPrestige(player);
                    return true;
                }
                var profile = playerDataManager.getOrCreate(player.getUniqueId());
                int total = achievementManager.getAdvancements().size();
                int completed = achievementManager.getCompletedCount(profile);
                if (completed < total) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.prestige-requirements", Map.of(
                            "completed", String.valueOf(completed),
                            "total", String.valueOf(total)
                    )));
                    return true;
                }

                profile.incrementPrestige();
                profile.getAdvancementProgress().clear();
                if (configManager.getMainConfig().getBoolean("prestige.reset-challenges", true)) {
                    profile.getChallengeProgress().clear();
                }
                int bonusPoints = configManager.getMainConfig().getInt("prestige.bonus-points", 250);
                profile.addPoints(bonusPoints);
                profile.addSeasonPoints(bonusPoints);
                playerDataManager.setLastKnownName(player.getUniqueId(), player.getName());
                playerDataManager.saveProfile(player.getUniqueId());
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.prestige-success", Map.of(
                        "prestige", String.valueOf(profile.getPrestigeLevel())
                )));
                return true;
            }
            case "challenges" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-only", Map.of()));
                    return true;
                }
                if (args.length == 1 || (args.length > 1 && args[1].equalsIgnoreCase("menu"))) {
                    guiManager.openChallenges(player);
                    return true;
                }
                for (ChallengeDefinition challenge : challengeManager.getActiveChallenges()) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.challenge-line", Map.of(
                            "title", languageManager.content(sender,"challenges",challenge.id(),"title",challenge.title()),
                            "type", languageManager.text(sender,"gui.texts.challenges.type-labels."+challenge.type(),challenge.type().toUpperCase(Locale.ROOT)),
                            "target", String.valueOf(challenge.trigger().target())
                    )));
                }
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("ba.admin") || args.length < 3) {
                    if (!sender.hasPermission("ba.admin")) {
                        sender.sendMessage(languageManager.getComponent(sender, locale, "command.no-permission", Map.of()));
                    } else {
                        sender.sendMessage(languageManager.getComponent(sender, locale, "command.usage", Map.of()));
                    }
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-not-found", Map.of("player", args[1])));
                    return true;
                }
                boolean result = achievementManager.forceGrant(target.getUniqueId(), args[2]);
                sender.sendMessage(languageManager.getComponent(sender, locale, result ? "command.give-success" : "command.give-failed", Map.of("player", target.getName(), "achievement", args[2])));
                return true;
            }
            case "reset" -> {
                if (!sender.hasPermission("ba.admin") || args.length < 2) {
                    if (!sender.hasPermission("ba.admin")) {
                        sender.sendMessage(languageManager.getComponent(sender, locale, "command.no-permission", Map.of()));
                    } else {
                        sender.sendMessage(languageManager.getComponent(sender, locale, "command.usage", Map.of()));
                    }
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.player-not-found", Map.of("player", args[1])));
                    return true;
                }
                try {
                    playerDataManager.resetProfile(target.getUniqueId()).join();
                    playerDataManager.loadProfile(target).join();
                } catch (RuntimeException failure) {
                    sender.sendMessage(languageManager.getComponent(sender, locale, "command.storage-error", Map.of())); return true;
                }
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.reset", Map.of("player", target.getName())));
                return true;
            }
            default -> {
                sender.sendMessage(languageManager.getComponent(sender, locale, "command.usage", Map.of()));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("ba.use")) return List.of();
        List<String> suggestions = suggestions(sender, args);
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }

    private List<String> suggestions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>(List.of("menu", "help", "stats", "leaderboard", "challenges", "season", "prestige", "language", "claim"));
            if (sender.hasPermission("ba.admin") || sender.hasPermission("ba.reload")) result.add("reload");
            if (sender.hasPermission("ba.admin")) result.addAll(List.of("give", "reset"));
            return result;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            return List.of("global", "session", "season");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("season")) {
            return List.of("menu", "rewards");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("challenges")) {
            return List.of("menu", "list");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("prestige")) {
            return List.of("menu", "confirm");
        }
        if (args.length == 2 && List.of("language", "lang", "locale").contains(args[0].toLowerCase(Locale.ROOT))) {
            return new ArrayList<>(languageManager.getLanguages().keySet());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return achievementManager.getAdvancements().stream().map(dev.erissos.betteradvancements.model.BetterAdvancement::id).toList();
        }
        return List.of();
    }
}
