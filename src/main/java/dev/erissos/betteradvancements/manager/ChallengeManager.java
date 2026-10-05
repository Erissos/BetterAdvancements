package dev.erissos.betteradvancements.manager;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.integration.VaultHook;
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
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChallengeManager {

    private final BetterAdvancementsPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final VaultHook vaultHook;
    private final SeasonManager seasonManager;
    private final Map<String, ChallengeDefinition> challenges = new ConcurrentHashMap<>();
    private String activeDailyChallenge;
    private String activeWeeklyChallenge;
    private int rotationTaskId = -1;

    public ChallengeManager(BetterAdvancementsPlugin plugin, ConfigManager configManager, PlayerDataManager playerDataManager, VaultHook vaultHook, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.vaultHook = vaultHook;
        this.seasonManager = seasonManager;
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
            try {
            if (challengeSection.getInt("trigger.target",1)<1) throw new IllegalArgumentException("trigger.target must be positive");
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
            for (RewardDefinition reward:rewards) if (reward.amount()<0 || (reward.type()==RewardType.ITEM && (Material.matchMaterial(reward.value())==null || Material.matchMaterial(reward.value()).isAir() || reward.amount()==0))) throw new IllegalArgumentException("Invalid reward");
            if (challengeSection.getInt("points-reward",10)<0) throw new IllegalArgumentException("points-reward must not be negative");
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
                    rewards,
                    challengeSection.getBoolean("repeatable", true),
                    challengeSection.getString("repeat-window", challengeSection.getString("type", "daily"))
            ));
            } catch (RuntimeException invalid) { plugin.getLogger().warning("Skipping invalid ChallengeManager definition "+id+": "+invalid.getMessage()); }
        }
        rotate(false);
    }

    public void start() {
        rotate(false);
        if (rotationTaskId != -1) {
            return;
        }
        rotationTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> rotate(true), 20L * 60L, 20L * 60L);
    }

    public void stop() {
        if (rotationTaskId != -1) {
            Bukkit.getScheduler().cancelTask(rotationTaskId);
            rotationTaskId = -1;
        }
    }

    public void rotate(boolean announce) {
        String previousDaily = activeDailyChallenge;
        String previousWeekly = activeWeeklyChallenge;
        List<String> daily = challenges.values().stream().filter(challenge -> challenge.type().equalsIgnoreCase("daily")).map(ChallengeDefinition::id).sorted().toList();
        List<String> weekly = challenges.values().stream().filter(challenge -> challenge.type().equalsIgnoreCase("weekly")).map(ChallengeDefinition::id).sorted().toList();
        if (!daily.isEmpty()) {
            activeDailyChallenge = daily.get(LocalDate.now().getDayOfYear() % daily.size());
        }
        if (!weekly.isEmpty()) {
            int week = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
            activeWeeklyChallenge = weekly.get(week % weekly.size());
        }
        if (announce
            && configManager.getMainConfig().getBoolean("challenges.broadcast-rotation", true)
            && (!equalsNullable(previousDaily, activeDailyChallenge) || !equalsNullable(previousWeekly, activeWeeklyChallenge))) {
            String rotationMessage = configManager.getMainConfig().getString("messages.challenge-rotation-broadcast", "");
            if (rotationMessage != null && !rotationMessage.isBlank()) {
                for (Player viewer : Bukkit.getOnlinePlayers()) viewer.sendMessage(plugin.getLanguageManager().getComponent(viewer,plugin.getLanguageManager().getLocale(viewer),"notifications.challenge-rotation",Map.of()));
            }
        }
    }

    public void handleTrigger(Player player, TriggerType triggerType, Map<String, String> context) {
        if (!plugin.getIntegrations().allows(player, player.getLocation(), dev.desperis.integration.IntegrationService.Action.PROGRESS)) return;
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        boolean changed = false;
        for (ChallengeDefinition challenge : getActiveChallenges()) {
            if (challenge.trigger().type() != triggerType) {
                continue;
            }
            if (!matches(challenge.trigger(), context)) {
                continue;
            }
            PlayerChallengeProgress progress = profile.getChallengeProgress().computeIfAbsent(challenge.id(), ignored -> new PlayerChallengeProgress());
            String cycleKey = currentCycleKey(challenge);
            if (challenge.repeatable() && !cycleKey.equals(progress.getCompletedCycleKey())) {
                progress.reset();
                progress.setCompletedCycleKey(cycleKey);
            }
            if (progress.isCompleted()) {
                continue;
            }
            int amount = Integer.parseInt(context.getOrDefault("amount", "1"));
            if (amount<=0) continue;
            progress.setProgress((int)Math.min(challenge.trigger().target(), (long)progress.getProgress() + amount));
            changed = true;
            if (progress.getProgress() >= challenge.trigger().target()) {
                progress.complete(cycleKey);
                profile.addPoints(challenge.pointsReward());
                seasonManager.addSeasonPoints(player, profile, challenge.pointsReward());
                int rewardIndex=0;
                for (RewardDefinition reward : challenge.rewards()) {
                    String receipt="challenge:"+challenge.id()+":prestige:"+profile.getPrestigeLevel()+":cycle:"+(challenge.repeatable()?cycleKey:"once")+":reward:"+rewardIndex++;
                    switch (reward.type()) {
                        case COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.value().replace("{player}", player.getName()));
                        case MONEY -> plugin.getRewardDelivery().giveMoney(player, reward.amount(),receipt);
                        case XP -> player.giveExp(reward.amount());
                        case POINTS -> profile.addPoints(reward.amount());
                        case ITEM -> {
                            Material material = Material.matchMaterial(reward.value());
                            if (material != null) {
                                plugin.getRewardDelivery().give(player, new ItemStack(material, Math.max(1, reward.amount())),receipt);
                            }
                        }
                        case BROADCAST -> Bukkit.broadcast(dev.erissos.betteradvancements.util.ItemUtils.component(
                                reward.value().replace("{player}", player.getName())
                        ));
                        case TITLE -> {
                            String[] titleParts = reward.value().split("\\|", 2);
                            String mainTitle = titleParts.length > 0 ? titleParts[0] : "";
                            String subTitle = titleParts.length > 1 ? titleParts[1] : "";
                            player.showTitle(net.kyori.adventure.title.Title.title(
                                dev.erissos.betteradvancements.util.ItemUtils.component(mainTitle.replace("{player}", player.getName())),
                                dev.erissos.betteradvancements.util.ItemUtils.component(subTitle.replace("{player}", player.getName()))
                            ));
                        }
                        case SOUND -> {
                            org.bukkit.Sound sound = dev.erissos.betteradvancements.util.SoundResolver.parse(reward.value(), null);
                            if (sound != null) {
                                player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
                            }
                        }
                    }
                }
                changed = true;
            }
        }
        if (changed) {
            playerDataManager.setLastKnownName(player.getUniqueId(), player.getName());
            playerDataManager.saveProfile(player.getUniqueId());
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

    private String currentCycleKey(ChallengeDefinition challenge) {
        String zoneId = configManager.getSeasonConfig().getString("season.timezone", ZoneId.systemDefault().getId());
        ZoneId zone;
        try { zone=ZoneId.of(zoneId); }
        catch (java.time.DateTimeException invalid) { zone=ZoneId.of("UTC"); }
        LocalDate now = LocalDate.now(zone);
        String window = challenge.repeatWindow() == null ? "none" : challenge.repeatWindow().toLowerCase(Locale.ROOT);
        return switch (window) {
            case "daily" -> now.toString();
            case "weekly" -> now.get(WeekFields.ISO.weekBasedYear()) + "-W" + now.get(WeekFields.ISO.weekOfWeekBasedYear());
            case "monthly" -> now.getYear() + "-M" + now.getMonthValue();
            default -> seasonManager.currentSeasonId();
        };
    }

    private boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
