package dev.erissos.betteradvancements.manager;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.integration.VaultHook;
import dev.erissos.betteradvancements.lang.LanguageManager;
import dev.erissos.betteradvancements.model.BetterAdvancement;
import dev.erissos.betteradvancements.model.PlayerAchievementProgress;
import dev.erissos.betteradvancements.model.PlayerProfile;
import dev.erissos.betteradvancements.model.RewardDefinition;
import dev.erissos.betteradvancements.model.RewardType;
import dev.erissos.betteradvancements.model.Tier;
import dev.erissos.betteradvancements.model.TriggerDefinition;
import dev.erissos.betteradvancements.model.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AchievementManager {

    private final BetterAdvancementsPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final LanguageManager languageManager;
    private final VaultHook vaultHook;
    private final Map<String, BetterAdvancement> advancements = new ConcurrentHashMap<>();

    public AchievementManager(BetterAdvancementsPlugin plugin, ConfigManager configManager, PlayerDataManager playerDataManager, LanguageManager languageManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.languageManager = languageManager;
        this.vaultHook = vaultHook;
    }

    public void load() {
        advancements.clear();
        ConfigurationSection section = configManager.getAchievementsConfig().getConfigurationSection("achievements");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection achievementSection = section.getConfigurationSection(id);
            if (achievementSection == null) {
                continue;
            }

            List<RewardDefinition> rewards = new ArrayList<>();
            ConfigurationSection rewardsSection = achievementSection.getConfigurationSection("rewards");
            if (rewardsSection != null) {
                for (String rewardId : rewardsSection.getKeys(false)) {
                    ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(rewardId);
                    if (rewardSection == null) {
                        continue;
                    }
                    rewards.add(new RewardDefinition(
                            RewardType.valueOf(rewardSection.getString("type", "POINTS").toUpperCase(Locale.ROOT)),
                            rewardSection.getString("value", ""),
                            rewardSection.getInt("amount", 0)
                    ));
                }
            }

            Map<String, String> conditions = new HashMap<>();
            ConfigurationSection conditionSection = achievementSection.getConfigurationSection("trigger.conditions");
            if (conditionSection != null) {
                for (String key : conditionSection.getKeys(false)) {
                    conditions.put(key, String.valueOf(conditionSection.get(key)));
                }
            }

            Map<String, Integer> guiPosition = new HashMap<>();
            guiPosition.put("slot", achievementSection.getInt("gui.slot", 0));
            guiPosition.put("page", achievementSection.getInt("gui.page", 0));

            BetterAdvancement advancement = new BetterAdvancement(
                    id,
                    achievementSection.getString("title", id),
                    achievementSection.getString("description", ""),
                    Tier.fromString(achievementSection.getString("tier", "TIER_1")),
                    achievementSection.getString("category", "general"),
                    achievementSection.getString("icon", "BOOK"),
                    achievementSection.getBoolean("hidden", false),
                    achievementSection.getInt("points", 1),
                    achievementSection.getString("rarity", "common"),
                    new TriggerDefinition(
                            TriggerType.fromString(achievementSection.getString("trigger.type", "CUSTOM")),
                            achievementSection.getInt("trigger.target", 1),
                            conditions
                    ),
                    achievementSection.getStringList("dependencies"),
                    rewards,
                    guiPosition
            );
            advancements.put(id, advancement);
        }
    }

    public Collection<BetterAdvancement> getAdvancements() {
        return Collections.unmodifiableCollection(advancements.values());
    }

    public Optional<BetterAdvancement> getAdvancement(String id) {
        return Optional.ofNullable(advancements.get(id));
    }

    public List<BetterAdvancement> getByTier(Tier tier) {
        return advancements.values().stream()
                .filter(advancement -> advancement.tier() == tier)
                .sorted(Comparator.comparingInt(advancement -> advancement.guiPosition().getOrDefault("slot", 0)))
                .toList();
    }

    public boolean handleTrigger(Player player, TriggerType triggerType, Map<String, String> context) {
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        boolean changed = false;
        for (BetterAdvancement advancement : advancements.values()) {
            if (advancement.trigger().type() != triggerType) {
                continue;
            }
            if (!hasDependencies(profile, advancement)) {
                continue;
            }
            if (!matchesConditions(advancement.trigger(), context)) {
                continue;
            }
            PlayerAchievementProgress progress = profile.getAdvancementProgress().computeIfAbsent(advancement.id(), ignored -> new PlayerAchievementProgress());
            if (progress.isCompleted()) {
                continue;
            }

            int amount = Integer.parseInt(context.getOrDefault("amount", "1"));
            progress.setProgress(Math.min(advancement.trigger().target(), progress.getProgress() + amount));
            if (progress.getProgress() >= advancement.trigger().target()) {
                complete(player, profile, advancement, progress);
            }
            changed = true;
        }
        if (changed) {
            playerDataManager.setLastKnownName(player.getUniqueId(), player.getName());
            playerDataManager.saveProfile(player.getUniqueId());
        }
        return changed;
    }

    public boolean forceGrant(UUID uniqueId, String advancementId) {
        BetterAdvancement advancement = advancements.get(advancementId);
        if (advancement == null) {
            return false;
        }
        PlayerProfile profile = playerDataManager.getOrCreate(uniqueId);
        PlayerAchievementProgress progress = profile.getAdvancementProgress().computeIfAbsent(advancementId, ignored -> new PlayerAchievementProgress());
        if (progress.isCompleted()) {
            return false;
        }
        progress.setProgress(advancement.trigger().target());
        Player player = Bukkit.getPlayer(uniqueId);
        if (player != null) {
            complete(player, profile, advancement, progress);
            playerDataManager.saveProfile(uniqueId);
        } else {
            progress.complete();
            profile.incrementSessionCompletions();
            profile.addPoints(advancement.points());
            playerDataManager.saveProfile(uniqueId);
        }
        return true;
    }

    public boolean hasDependencies(PlayerProfile profile, BetterAdvancement advancement) {
        return advancement.dependencies().stream()
                .map(profile.getAdvancementProgress()::get)
                .filter(Objects::nonNull)
                .allMatch(PlayerAchievementProgress::isCompleted)
                && profile.getAdvancementProgress().keySet().containsAll(advancement.dependencies());
    }

    public boolean isUnlocked(PlayerProfile profile, BetterAdvancement advancement) {
        return advancement.dependencies().isEmpty() || hasDependencies(profile, advancement);
    }

    public double getProgressPercent(PlayerProfile profile) {
        if (advancements.isEmpty()) {
            return 0.0D;
        }
        return (profile.getCompletedAdvancements() * 100.0D) / advancements.size();
    }

    public int getTierCompletion(PlayerProfile profile, Tier tier) {
        List<BetterAdvancement> tierAdvancements = getByTier(tier);
        if (tierAdvancements.isEmpty()) {
            return 0;
        }
        int completed = (int) tierAdvancements.stream()
                .map(advancement -> profile.getAdvancementProgress().get(advancement.id()))
                .filter(Objects::nonNull)
                .filter(PlayerAchievementProgress::isCompleted)
                .count();
        return (int) ((completed * 100.0D) / tierAdvancements.size());
    }

    public long getRareCompletions(PlayerProfile profile) {
        return advancements.values().stream()
                .filter(advancement -> advancement.rarity().equalsIgnoreCase("legendary") || advancement.rarity().equalsIgnoreCase("mythic"))
                .map(advancement -> profile.getAdvancementProgress().get(advancement.id()))
                .filter(Objects::nonNull)
                .filter(PlayerAchievementProgress::isCompleted)
                .count();
    }

    public Map<String, Long> getPlaystyleInsights(PlayerProfile profile) {
        Map<String, Long> insights = new HashMap<>();
        for (String category : List.of("combat", "exploration", "builder", "farming", "magic")) {
            long count = advancements.values().stream()
                    .filter(advancement -> advancement.category().equalsIgnoreCase(category))
                    .map(advancement -> profile.getAdvancementProgress().get(advancement.id()))
                    .filter(Objects::nonNull)
                    .filter(PlayerAchievementProgress::isCompleted)
                    .count();
            insights.put(category, count);
        }
        return insights;
    }

    private boolean matchesConditions(TriggerDefinition trigger, Map<String, String> context) {
        for (Map.Entry<String, String> entry : trigger.conditions().entrySet()) {
            String actual = context.get(entry.getKey());
            if (actual == null) {
                return false;
            }
            if (!entry.getValue().equalsIgnoreCase(actual)) {
                return false;
            }
        }
        return true;
    }

    private void complete(Player player, PlayerProfile profile, BetterAdvancement advancement, PlayerAchievementProgress progress) {
        progress.complete();
        profile.incrementSessionCompletions();
        profile.addPoints(advancement.points());
        applyRewards(player, profile, advancement);
        plugin.getNotificationManager().notifyCompletion(player, advancement, profile.getLanguage());
    }

    private void applyRewards(Player player, PlayerProfile profile, BetterAdvancement advancement) {
        for (RewardDefinition reward : advancement.rewards()) {
            switch (reward.type()) {
                case COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.value().replace("{player}", player.getName()));
                case MONEY -> vaultHook.deposit(player, reward.amount());
                case XP -> player.giveExp(reward.amount());
                case POINTS -> profile.addPoints(reward.amount());
                case ITEM -> {
                    Material material = Material.matchMaterial(reward.value());
                    if (material != null) {
                        player.getInventory().addItem(new ItemStack(material, Math.max(1, reward.amount())));
                    }
                }
            }
        }
    }
}