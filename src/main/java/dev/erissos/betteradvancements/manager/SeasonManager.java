package dev.erissos.betteradvancements.manager;

import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SeasonManager {

    private final ConfigManager configManager;
    private dev.erissos.betteradvancements.lang.LanguageManager languageManager;
    public void setLanguageManager(dev.erissos.betteradvancements.lang.LanguageManager value) { languageManager=value; }
    private final DateTimeFormatter monthlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

    public SeasonManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public String currentSeasonId() {
        FileConfiguration config = configManager.getSeasonConfig();
        String mode = config.getString("season.mode", "monthly");
        if (mode == null || mode.equalsIgnoreCase("monthly")) {
            String zone = config.getString("season.timezone", ZoneId.systemDefault().getId());
            try { return LocalDate.now(ZoneId.of(zone)).format(monthlyFormatter); }
            catch (java.time.DateTimeException invalid) { return LocalDate.now(ZoneId.of("UTC")).format(monthlyFormatter); }
        }

        String fixedSeason = config.getString("season.fixed-id", "default");
        return fixedSeason == null || fixedSeason.isBlank() ? "default" : fixedSeason;
    }

    public void addSeasonPoints(Player player, PlayerProfile profile, int amount) {
        if (amount <= 0) {
            return;
        }

        profile.addSeasonPoints(amount);
        String playerName = player.getName();
        for (Integer threshold : getRewardThresholds()) {
            if (profile.getSeasonPoints() < threshold || profile.getClaimedSeasonRewards().contains(threshold)) {
                continue;
            }
            profile.getClaimedSeasonRewards().add(threshold);
            for (String command : getRewardCommands(threshold)) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", playerName));
            }
            if (configManager.getSeasonConfig().getBoolean("season.reward-message", true)) {
                String rewardMessage = configManager.getMainConfig().getString("messages.season-reward-unlocked", "");
                if (rewardMessage != null && !rewardMessage.isBlank()) {
                    player.sendMessage(dev.erissos.betteradvancements.util.ItemUtils.component(
                            (languageManager == null ? rewardMessage : languageManager.text(player,"notifications.season-reward-unlocked",rewardMessage)).replace("<threshold>", String.valueOf(threshold)).replace("{threshold}", String.valueOf(threshold))
                    ));
                }
            }
        }
    }

    public List<Integer> getRewardThresholds() {
        ConfigurationSection rewards = configManager.getSeasonConfig().getConfigurationSection("season.rewards");
        if (rewards == null) {
            return List.of();
        }

        List<Integer> thresholds = new ArrayList<>();
        for (String key : rewards.getKeys(false)) {
            try {
                thresholds.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
            }
        }
        thresholds.sort(Comparator.naturalOrder());
        return thresholds;
    }

    public List<String> getRewardCommands(int threshold) {
        String basePath = "season.rewards." + threshold;
        List<String> commands = configManager.getSeasonConfig().getStringList(basePath + ".commands");
        if (!commands.isEmpty()) {
            return commands;
        }
        return configManager.getSeasonConfig().getStringList(basePath);
    }
}
