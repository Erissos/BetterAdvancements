package dev.erissos.betteradvancements.manager;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.model.ChallengeDefinition;
import dev.erissos.betteradvancements.model.PlayerChallengeProgress;
import dev.erissos.betteradvancements.model.PlayerProfile;
import dev.erissos.betteradvancements.model.RewardDefinition;
import dev.erissos.betteradvancements.model.RewardType;
import dev.erissos.betteradvancements.model.TriggerDefinition;
import dev.erissos.betteradvancements.model.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChallengeManager {

    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final Map<String, ChallengeDefinition> challenges = new ConcurrentHashMap<>();
    private String activeDailyChallenge;
    private String activeWeeklyChallenge;

    public ChallengeManager(BetterAdvancementsPlugin plugin, ConfigManager configManager, PlayerDataManager playerDataManager) {
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    public void load() {
        challenges.clear();
        ConfigurationSection section = configManager.getChallengesConfig().getConfigurationSection("challenges");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection challengeSection = section.getConfigurationSection(id);
            if (challengeSection == null) {
                continue;
            }
            Map<String, String> conditions = new HashMap<>();
            ConfigurationSection conditionSection = challengeSection.getConfigurationSection("trigger.conditions");
            if (conditionSection != null) {
                for (String key : conditionSection.getKeys(false)) {
                    conditions.put(key, String.valueOf(conditionSection.get(key)));
                }
            }
            List<RewardDefinition> rewards = new ArrayList<>();
            ConfigurationSection rewardSection = challengeSection.getConfigurationSection("rewards");
            if (rewardSection != null) {
                for (String rewardId : rewardSection.getKeys(false)) {
                    ConfigurationSection reward = rewardSection.getConfigurationSection(rewardId);
                    if (reward != null) {
                        rewards.add(new RewardDefinition(
                                RewardType.valueOf(reward.getString("type", "POINTS").toUpperCase(Locale.ROOT)),
                                reward.getString("value", ""),
                                reward.getInt("amount", 0)
                        ));
                    }
                }
            }
            challenges.put(id, new ChallengeDefinition(
                    id,
                    challengeSection.getString("type", "daily"),
                    challengeSection.getString("title", id),
                    challengeSection.getString("description", ""),
                    new TriggerDefinition(
                            TriggerType.fromString(challengeSection.getString("trigger.type", "CUSTOM")),
                            challengeSection.getInt("trigger.target", 1),
                            conditions
                    ),
                    challengeSection.getInt("points-reward", 10),
                    rewards
            ));
        }
        rotate();
    }

    public void rotate() {
        List<String> daily = challenges.values().stream().filter(challenge -> challenge.type().equalsIgnoreCase("daily")).map(ChallengeDefinition::id).sorted().toList();
        List<String> weekly = challenges.values().stream().filter(challenge -> challenge.type().equalsIgnoreCase("weekly")).map(ChallengeDefinition::id).sorted().toList();
        if (!daily.isEmpty()) {
            activeDailyChallenge = daily.get(LocalDate.now().getDayOfYear() % daily.size());
        }
        if (!weekly.isEmpty()) {
            int week = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
            activeWeeklyChallenge = weekly.get(week % weekly.size());
        }
    }

    public void handleTrigger(Player player, TriggerType triggerType, Map<String, String> context) {
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        for (ChallengeDefinition challenge : getActiveChallenges()) {
            if (challenge.trigger().type() != triggerType) {
                continue;
            }
            if (!matches(challenge.trigger(), context)) {
                continue;
            }
            PlayerChallengeProgress progress = profile.getChallengeProgress().computeIfAbsent(challenge.id(), ignored -> new PlayerChallengeProgress());
            if (progress.isCompleted()) {
                continue;
            }
            int amount = Integer.parseInt(context.getOrDefault("amount", "1"));
            progress.setProgress(Math.min(challenge.trigger().target(), progress.getProgress() + amount));
            if (progress.getProgress() >= challenge.trigger().target()) {
                progress.complete();
                profile.addPoints(challenge.pointsReward());
                for (RewardDefinition reward : challenge.rewards()) {
                    switch (reward.type()) {
                        case COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.value().replace("{player}", player.getName()));
                        case XP -> player.giveExp(reward.amount());
                        case POINTS -> profile.addPoints(reward.amount());
                        case ITEM -> {
                            Material material = Material.matchMaterial(reward.value());
                            if (material != null) {
                                player.getInventory().addItem(new ItemStack(material, Math.max(1, reward.amount())));
                            }
                        }
                        case MONEY -> {
                        }
                    }
                }
            }
        }
    }

    public Collection<ChallengeDefinition> getActiveChallenges() {
        List<ChallengeDefinition> result = new ArrayList<>();
        if (activeDailyChallenge != null && challenges.containsKey(activeDailyChallenge)) {
            result.add(challenges.get(activeDailyChallenge));
        }
        if (activeWeeklyChallenge != null && challenges.containsKey(activeWeeklyChallenge)) {
            result.add(challenges.get(activeWeeklyChallenge));
        }
        return result;
    }

    private boolean matches(TriggerDefinition trigger, Map<String, String> context) {
        for (Map.Entry<String, String> entry : trigger.conditions().entrySet()) {
            if (!entry.getValue().equalsIgnoreCase(context.getOrDefault(entry.getKey(), ""))) {
                return false;
            }
        }
        return true;
    }
}