package dev.erissos.betteradvancements.gui;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.lang.LanguageManager;
import dev.erissos.betteradvancements.manager.AchievementManager;
import dev.erissos.betteradvancements.manager.ChallengeManager;
import dev.erissos.betteradvancements.manager.LeaderboardManager;
import dev.erissos.betteradvancements.manager.SeasonManager;
import dev.erissos.betteradvancements.model.BetterAdvancement;
import dev.erissos.betteradvancements.model.ChallengeDefinition;
import dev.erissos.betteradvancements.model.LeaderboardEntry;
import dev.erissos.betteradvancements.model.PlayerAchievementProgress;
import dev.erissos.betteradvancements.model.PlayerChallengeProgress;
import dev.erissos.betteradvancements.model.PlayerProfile;
import dev.erissos.betteradvancements.model.RewardDefinition;
import dev.erissos.betteradvancements.model.Tier;
import dev.erissos.betteradvancements.model.TriggerType;
import dev.erissos.betteradvancements.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GUIManager implements Listener {

    private static final int MENU_SIZE = 54;
    private static final List<Integer> DEFAULT_MAIN_TIER_SLOTS = List.of(19, 20, 22, 24, 25);
    private static final List<Integer> DEFAULT_STATS_TIER_SLOTS = List.of(19, 20, 21, 22, 23);
    private static final DateTimeFormatter SESSION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());

    private final ConfigManager configManager;
    private final AchievementManager achievementManager;
    private final ChallengeManager challengeManager;
    private final LeaderboardManager leaderboardManager;
    private final PlayerDataManager playerDataManager;
    private final SeasonManager seasonManager;
    private final LanguageManager languageManager;

    public GUIManager(BetterAdvancementsPlugin plugin, ConfigManager configManager, AchievementManager achievementManager, ChallengeManager challengeManager, LeaderboardManager leaderboardManager, PlayerDataManager playerDataManager, SeasonManager seasonManager, LanguageManager languageManager) {
        this.configManager = configManager;
        this.achievementManager = achievementManager;
        this.challengeManager = challengeManager;
        this.leaderboardManager = leaderboardManager;
        this.playerDataManager = playerDataManager;
        this.seasonManager = seasonManager;
        this.languageManager = languageManager;
    }

    public void reload() {
    }

    public void openMainMenu(Player player) {
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.MAIN, null, false), MENU_SIZE, ItemUtils.component(text("main.title")));

        paintFrame(inventory, "main.frame");
        applyFillerGroups(inventory, "main.fillers");
        fillEmptySlots(inventory, "main.background");

        inventory.setItem(intValue("main.profile.slot"), createPlayerHeadCard(
                player.getUniqueId(),
                text("main.profile.title"),
                lines("main.profile.lore", placeholders(
                        "player", player.getName(),
                        "completion", formatPercent(achievementManager.getProgressPercent(profile)),
                        "points", profile.getPoints(),
                        "highest_tier", formatTierName(getHighestCompletedTier(profile))
                )),
                boolValue("main.profile.glow")
        ));

        BetterAdvancement nextTarget = findNextTarget(profile);
        boolean hasTarget = nextTarget != null;
        String nextPath = hasTarget ? "main.next-target.active" : "main.next-target.empty";
        String configuredNextMaterial = text(nextPath + ".material");
        String nextTargetMaterial = hasTarget
            ? configuredNextMaterial.isBlank() ? nextTarget.icon() : configuredNextMaterial
            : configuredNextMaterial;
        inventory.setItem(intValue("main.next-target.slot"), ItemUtils.create(
            nextTargetMaterial,
                text(nextPath + ".title"),
                hasTarget
                        ? lines(nextPath + ".lore", placeholders(
                        "target", nextTarget.title(),
                        "tier", formatTierName(nextTarget.tier()),
                        "category", capitalize(nextTarget.category()),
                        "goal", nextTarget.trigger().target()
                ))
                        : lines(nextPath + ".lore"),
                boolValue(nextPath + ".glow")
        ));

        inventory.setItem(intValue("main.challenge.slot"), ItemUtils.create(
                text("main.challenge.material"),
                text("main.challenge.title"),
                buildChallengeSpotlightLore(),
                boolValue("main.challenge.glow")
        ));

        for (Tier tier : Tier.values()) {
            int completion = achievementManager.getTierCompletion(profile, tier);
            int slot = mainTierSlot(tier);
            if (slot < 0) {
                continue;
            }
            List<String> lore = new ArrayList<>(lines("main.tiers." + tier.name() + ".lore", placeholders(
                "completion", completion,
                "unlocked", getUnlockedCount(profile, tier),
                "total", achievementManager.getByTier(tier).size(),
                "rare", getRareTierCount(profile, tier),
                "tier_name", formatTierName(tier)
            )));
            lore.addAll(buildMainTierStateLore(profile, tier, completion));
            inventory.setItem(slot, ItemUtils.create(
                    text("main.tiers." + tier.name() + ".icon"),
                    text("main.tiers." + tier.name() + ".name"),
                lore,
                    completion >= 100
            ));
        }

        inventory.setItem(intValue("main.stats.slot"), ItemUtils.create(
                text("main.stats.material"),
                text("main.stats.title"),
                lines("main.stats.lore"),
                boolValue("main.stats.glow")
        ));
        inventory.setItem(intValue("main.leaderboard.slot"), ItemUtils.create(
                text("main.leaderboard.material"),
                text("main.leaderboard.title"),
                lines("main.leaderboard.lore"),
                boolValue("main.leaderboard.glow")
        ));
        inventory.setItem(intValue("main.categories.slot"), ItemUtils.create(
            text("main.categories.material"),
            text("main.categories.title"),
            lines("main.categories.lore", placeholders(
                "categories", orderedCategories().size(),
                "completed", profile.getCompletedAdvancements()
            )),
            boolValue("main.categories.glow")
        ));
        inventory.setItem(intValue("main.season.slot"), ItemUtils.create(
            text("main.season.material"),
            text("main.season.title"),
            lines("main.season.lore", placeholders(
                "season", seasonManager.currentSeasonId(),
                "season_points", profile.getSeasonPoints(),
                "reward_count", seasonManager.getRewardThresholds().size()
            )),
            boolValue("main.season.glow")
        ));
        inventory.setItem(intValue("main.prestige.slot"), ItemUtils.create(
            text("main.prestige.material"),
            text("main.prestige.title"),
            lines("main.prestige.lore", placeholders(
                "prestige", profile.getPrestigeLevel(),
                "completed", profile.getCompletedAdvancements(),
                "total", achievementManager.getAdvancements().size(),
                "bonus", configManager.getMainConfig().getInt("prestige.bonus-points", 250)
            )),
            boolValue("main.prestige.glow")
        ));
            inventory.setItem(intValue("main.settings.slot"), ItemUtils.create(
                text("main.settings.material"),
                text("main.settings.title"),
                lines("main.settings.lore", placeholders(
                    "language", languageManager.getDisplayName(profile.getLanguage()),
                    "chat", settingState(profile.isChatNotificationsEnabled()),
                    "title_state", settingState(profile.isTitleNotificationsEnabled())
                )),
                boolValue("main.settings.glow")
            ));

        player.openInventory(inventory);
    }

            public void openCategoryMenu(Player player) {
            PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
            Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.CATEGORY, null, false), MENU_SIZE, ItemUtils.component(text("category-menu.title")));

            paintFrame(inventory, "category-menu.frame");
            applyFillerGroups(inventory, "category-menu.fillers");
            fillEmptySlots(inventory, "category-menu.background");

            inventory.setItem(intValue("category-menu.profile.slot"), createPlayerHeadCard(
                player.getUniqueId(),
                text("category-menu.profile.title"),
                lines("category-menu.profile.lore", placeholders(
                    "categories", orderedCategories().size(),
                    "completed", profile.getCompletedAdvancements(),
                    "total", achievementManager.getAdvancements().size(),
                    "rare", achievementManager.getRareCompletions(profile)
                )),
                boolValue("category-menu.profile.glow")
            ));

            List<String> categories = orderedCategories();
            List<Integer> slots = intList("category-menu.auto-slots");
            for (int index = 0; index < Math.min(categories.size(), slots.size()); index++) {
                inventory.setItem(slots.get(index), createCategorySummaryCard(profile, categories.get(index)));
            }

            inventory.setItem(intValue("category-menu.stats.slot"), ItemUtils.create(
                text("category-menu.stats.material"),
                text("category-menu.stats.title"),
                lines("category-menu.stats.lore"),
                boolValue("category-menu.stats.glow")
            ));
            inventory.setItem(intValue("category-menu.settings.slot"), ItemUtils.create(
                text("category-menu.settings.material"),
                text("category-menu.settings.title"),
                lines("category-menu.settings.lore"),
                boolValue("category-menu.settings.glow")
            ));
            inventory.setItem(intValue("category-menu.back.slot"), ItemUtils.create(
                text("category-menu.back.material"),
                text("category-menu.back.title"),
                lines("category-menu.back.lore"),
                boolValue("category-menu.back.glow")
            ));

            player.openInventory(inventory);
            }

    public void openTierMenu(Player player, Tier tier) {
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.TIER, tier, false), MENU_SIZE, ItemUtils.component(text("tier.title", placeholders("tier_name", formatTierName(tier)))));

        paintFrame(inventory, "tier.frame");
        fillEmptySlots(inventory, "tier.background");
        inventory.setItem(intValue("tier.overview.slot"), ItemUtils.create(
                text("main.tiers." + tier.name() + ".icon"),
                text("tier.overview.title", placeholders("tier_name", formatTierName(tier))),
                lines("tier.overview.lore", placeholders(
                        "tier_name", formatTierName(tier),
                        "completion", achievementManager.getTierCompletion(profile, tier),
                        "total", achievementManager.getByTier(tier).size(),
                        "unlocked", getUnlockedCount(profile, tier),
                        "rare", getRareTierCount(profile, tier)
                )),
                boolValue("tier.overview.glow")
        ));

        List<BetterAdvancement> tierAdvancements = achievementManager.getByTier(tier);
        Set<Integer> blockedSlots = new HashSet<>(intList("tier.blocked-slots"));
        tierAdvancements.stream().map(advancement -> advancement.guiPosition().getOrDefault("slot", 0)).forEach(blockedSlots::add);

        for (BetterAdvancement advancement : tierAdvancements) {
            int slot = advancement.guiPosition().getOrDefault("slot", 0);
            PlayerAchievementProgress progress = profile.getAdvancementProgress().get(advancement.id());
            boolean completed = progress != null && progress.isCompleted();
            boolean unlocked = achievementManager.isUnlocked(profile, advancement);
            boolean hidden = advancement.hidden() && !completed;
            String title = advancement.hidden() && !completed ? text("tier.node.hidden-title") : advancement.title();
            String description = advancement.hidden() && !completed ? text("tier.node.hidden-description") : advancement.description();

            List<String> lore = new ArrayList<>(lines("tier.node.lore", placeholders(
                    "description", description,
                    "category", capitalize(advancement.category()),
                    "rarity", capitalize(advancement.rarity()),
                    "objective", formatObjective(advancement),
                    "rewards", formatRewards(advancement),
                    "points", advancement.points(),
                    "progress", progress == null ? 0 : progress.getProgress(),
                    "target", advancement.trigger().target()
            )));
            if (!advancement.dependencies().isEmpty()) {
                lore.add(text("tier.node.requires-format", placeholders("requirements", joinTitles(advancement.dependencies()))));
            }
            List<String> unlocks = tierAdvancements.stream()
                    .filter(candidate -> candidate.dependencies().contains(advancement.id()))
                    .map(BetterAdvancement::title)
                    .toList();
            if (!unlocks.isEmpty()) {
                lore.add(text("tier.node.unlocks-format", placeholders("unlocks", String.join(", ", unlocks))));
            }
            lore.add(text(completed ? "tier.node.state.completed" : unlocked ? "tier.node.state.unlocked" : "tier.node.state.locked"));

                String material = hidden ? text("tier.node.materials.hidden") : advancement.icon();
            inventory.setItem(slot, ItemUtils.create(material, title, lore, completed));
        }

        drawDependencyPaths(inventory, tierAdvancements, profile, blockedSlots);
        fillTierNavigation(inventory, profile, tier);
        player.openInventory(inventory);
    }

    public void openStats(Player player) {
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        Map<String, Long> insights = achievementManager.getPlaystyleInsights(profile);
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.STATS, null, false), MENU_SIZE, ItemUtils.component(text("stats.title")));

        paintFrame(inventory, "stats.frame");
        fillEmptySlots(inventory, "stats.background");
        inventory.setItem(intValue("stats.profile.slot"), createPlayerHeadCard(
                player.getUniqueId(),
                text("stats.profile.title"),
                lines("stats.profile.lore", placeholders(
                        "completed", profile.getCompletedAdvancements(),
                        "total", achievementManager.getAdvancements().size(),
                        "progression", formatPercent(achievementManager.getProgressPercent(profile)),
                        "points", profile.getPoints(),
                        "rare", achievementManager.getRareCompletions(profile)
                )),
                boolValue("stats.profile.glow")
        ));

        for (Tier tier : Tier.values()) {
            String tierPath = "stats.tiers." + tier.name();
            int slot = statsTierSlot(tier);
            if (slot < 0) {
                continue;
            }
            inventory.setItem(slot, ItemUtils.create(
                    text("main.tiers." + tier.name() + ".icon"),
                    text("main.tiers." + tier.name() + ".name"),
                    lines(tierPath + ".lore", placeholders(
                            "completion", achievementManager.getTierCompletion(profile, tier),
                            "unlocked", getUnlockedCount(profile, tier)
                    )),
                    achievementManager.getTierCompletion(profile, tier) >= 100
            ));
        }

        ConfigurationSection categories = section("stats.categories");
        if (categories != null) {
            for (String key : categories.getKeys(false)) {
                String categoryPath = "stats.categories." + key;
                inventory.setItem(intValue(categoryPath + ".slot"), ItemUtils.create(
                        text(categoryPath + ".material"),
                        text(categoryPath + ".title"),
                        lines(categoryPath + ".lore", placeholders("value", insights.getOrDefault(key, 0L))),
                        boolValue(categoryPath + ".glow")
                ));
            }
        }

        inventory.setItem(intValue("stats.rare.slot"), ItemUtils.create(
                text("stats.rare.material"),
                text("stats.rare.title"),
                lines("stats.rare.lore", placeholders("rare", achievementManager.getRareCompletions(profile))),
                boolValue("stats.rare.glow")
        ));
        inventory.setItem(intValue("stats.session.slot"), ItemUtils.create(
                text("stats.session.material"),
                text("stats.session.title"),
                lines("stats.session.lore", placeholders(
                        "session_completions", profile.getSessionCompletions(),
                "session_join", formatSessionJoin(profile.getSessionJoinMillis())
                )),
                boolValue("stats.session.glow")
        ));
        inventory.setItem(intValue("stats.objective.slot"), ItemUtils.create(
                text("stats.objective.material"),
                text("stats.objective.title"),
                buildCurrentObjectiveLore(profile),
                boolValue("stats.objective.glow")
        ));
        inventory.setItem(intValue("stats.back.slot"), ItemUtils.create(
                text("stats.back.material"),
                text("stats.back.title"),
                lines("stats.back.lore"),
                boolValue("stats.back.glow")
        ));

        player.openInventory(inventory);
    }

        public void openSettings(Player player) {
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.SETTINGS, null, false), MENU_SIZE, ItemUtils.component(text("settings-menu.title")));

        paintFrame(inventory, "settings-menu.frame");
        applyFillerGroups(inventory, "settings-menu.fillers");
        fillEmptySlots(inventory, "settings-menu.background");

        inventory.setItem(intValue("settings-menu.profile.slot"), createPlayerHeadCard(
            player.getUniqueId(),
            text("settings-menu.profile.title"),
            lines("settings-menu.profile.lore", placeholders(
                "language", languageManager.getDisplayName(profile.getLanguage()),
                "chat", settingState(profile.isChatNotificationsEnabled()),
                "title_state", settingState(profile.isTitleNotificationsEnabled()),
                "action_bar", settingState(profile.isActionBarNotificationsEnabled()),
                "boss_bar", settingState(profile.isBossBarNotificationsEnabled()),
                "sound", settingState(profile.isSoundNotificationsEnabled())
            )),
            boolValue("settings-menu.profile.glow")
        ));

        inventory.setItem(intValue("settings-menu.language.slot"), ItemUtils.create(
            text("settings-menu.language.material"),
            text("settings-menu.language.title"),
            lines("settings-menu.language.lore", placeholders(
                "language", languageManager.getDisplayName(profile.getLanguage()),
                "count", languageManager.getLanguages().size()
            )),
            boolValue("settings-menu.language.glow")
        ));
        inventory.setItem(intValue("settings-menu.toggles.chat.slot"), createSettingsToggleCard("settings-menu.toggles.chat", profile.isChatNotificationsEnabled()));
        inventory.setItem(intValue("settings-menu.toggles.title.slot"), createSettingsToggleCard("settings-menu.toggles.title", profile.isTitleNotificationsEnabled()));
        inventory.setItem(intValue("settings-menu.toggles.action-bar.slot"), createSettingsToggleCard("settings-menu.toggles.action-bar", profile.isActionBarNotificationsEnabled()));
        inventory.setItem(intValue("settings-menu.toggles.boss-bar.slot"), createSettingsToggleCard("settings-menu.toggles.boss-bar", profile.isBossBarNotificationsEnabled()));
        inventory.setItem(intValue("settings-menu.toggles.sound.slot"), createSettingsToggleCard("settings-menu.toggles.sound", profile.isSoundNotificationsEnabled()));
        inventory.setItem(intValue("settings-menu.categories.slot"), ItemUtils.create(
            text("settings-menu.categories.material"),
            text("settings-menu.categories.title"),
            lines("settings-menu.categories.lore"),
            boolValue("settings-menu.categories.glow")
        ));
        inventory.setItem(intValue("settings-menu.back.slot"), ItemUtils.create(
            text("settings-menu.back.material"),
            text("settings-menu.back.title"),
            lines("settings-menu.back.lore"),
            boolValue("settings-menu.back.glow")
        ));

        player.openInventory(inventory);
        }

    public void openLeaderboard(Player player, boolean session) {
        List<LeaderboardEntry> entries = session ? leaderboardManager.getSessionLeaderboard(10) : leaderboardManager.getGlobalLeaderboard(10);
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.LEADERBOARD, null, session), MENU_SIZE, ItemUtils.component(text(session ? "leaderboard.title.session" : "leaderboard.title.global")));

        paintFrame(inventory, "leaderboard.frame");
        fillEmptySlots(inventory, "leaderboard.background");
        String headerPath = session ? "leaderboard.header.session" : "leaderboard.header.global";
        inventory.setItem(intValue("leaderboard.header.slot"), ItemUtils.create(
                text(headerPath + ".material"),
                text(headerPath + ".title"),
                lines(headerPath + ".lore", placeholders(
                        "entries", entries.size(),
                        "mode", session ? text("leaderboard.mode.session") : text("leaderboard.mode.global")
                )),
                boolValue(headerPath + ".glow")
        ));

        if (entries.isEmpty()) {
            inventory.setItem(intValue("leaderboard.empty.slot"), ItemUtils.create(
                    text("leaderboard.empty.material"),
                    text("leaderboard.empty.title"),
                    lines("leaderboard.empty.lore"),
                    boolValue("leaderboard.empty.glow")
            ));
        } else {
            List<Integer> podiumSlots = intList("leaderboard.podium-slots");
            List<Integer> listSlots = intList("leaderboard.list-slots");
            for (int index = 0; index < Math.min(3, entries.size()) && index < podiumSlots.size(); index++) {
                inventory.setItem(podiumSlots.get(index), createLeaderboardCard(entries.get(index), index + 1, true));
            }
            for (int index = 3; index < Math.min(entries.size(), listSlots.size() + 3); index++) {
                inventory.setItem(listSlots.get(index - 3), createLeaderboardCard(entries.get(index), index + 1, false));
            }
        }

        inventory.setItem(intValue("leaderboard.footer.global.slot"), ItemUtils.create(
                text("leaderboard.footer.global.material"),
                text("leaderboard.footer.global.title"),
                lines("leaderboard.footer.global.lore"),
                !session
        ));
        inventory.setItem(intValue("leaderboard.footer.back.slot"), ItemUtils.create(
                text("leaderboard.footer.back.material"),
                text("leaderboard.footer.back.title"),
                lines("leaderboard.footer.back.lore"),
                boolValue("leaderboard.footer.back.glow")
        ));
        inventory.setItem(intValue("leaderboard.footer.session.slot"), ItemUtils.create(
                text("leaderboard.footer.session.material"),
                text("leaderboard.footer.session.title"),
                lines("leaderboard.footer.session.lore"),
                session
        ));
        inventory.setItem(intValue("leaderboard.footer.notes.slot"), ItemUtils.create(
                text("leaderboard.footer.notes.material"),
                text("leaderboard.footer.notes.title"),
                lines("leaderboard.footer.notes.lore"),
                boolValue("leaderboard.footer.notes.glow")
        ));

        player.openInventory(inventory);
    }

    public void openChallenges(Player player) {
    PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
    Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.CHALLENGES, null, false), MENU_SIZE, ItemUtils.component(text("challenges.title")));

    paintFrame(inventory, "challenges.frame");
    applyFillerGroups(inventory, "challenges.fillers");
    fillEmptySlots(inventory, "challenges.background");

    inventory.setItem(intValue("challenges.profile.slot"), createPlayerHeadCard(
        player.getUniqueId(),
        text("challenges.profile.title"),
        lines("challenges.profile.lore", placeholders(
            "active", challengeManager.getActiveChallenges().size(),
            "completed", profile.getCompletedChallenges(),
            "season_points", profile.getSeasonPoints()
        )),
        boolValue("challenges.profile.glow")
    ));

    ChallengeDefinition daily = findActiveChallenge("daily");
    ChallengeDefinition weekly = findActiveChallenge("weekly");
    inventory.setItem(intValue("challenges.cards.daily.slot"), createChallengeCard("challenges.cards.daily", daily, profile));
    inventory.setItem(intValue("challenges.cards.weekly.slot"), createChallengeCard("challenges.cards.weekly", weekly, profile));

    inventory.setItem(intValue("challenges.summary.slot"), ItemUtils.create(
        text("challenges.summary.material"),
        text("challenges.summary.title"),
        lines("challenges.summary.lore", placeholders(
            "active", challengeManager.getActiveChallenges().size(),
            "completed", profile.getCompletedChallenges(),
            "points", profile.getPoints()
        )),
        boolValue("challenges.summary.glow")
    ));
    inventory.setItem(intValue("challenges.season.slot"), ItemUtils.create(
        text("challenges.season.material"),
        text("challenges.season.title"),
        lines("challenges.season.lore"),
        boolValue("challenges.season.glow")
    ));
    inventory.setItem(intValue("challenges.prestige.slot"), ItemUtils.create(
        text("challenges.prestige.material"),
        text("challenges.prestige.title"),
        lines("challenges.prestige.lore"),
        boolValue("challenges.prestige.glow")
    ));
    inventory.setItem(intValue("challenges.back.slot"), ItemUtils.create(
        text("challenges.back.material"),
        text("challenges.back.title"),
        lines("challenges.back.lore"),
        boolValue("challenges.back.glow")
    ));

    player.openInventory(inventory);
    }

    public void openSeason(Player player) {
    PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
    Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.SEASON, null, false), MENU_SIZE, ItemUtils.component(text("season-menu.title")));

    paintFrame(inventory, "season-menu.frame");
    applyFillerGroups(inventory, "season-menu.fillers");
    fillEmptySlots(inventory, "season-menu.background");

    inventory.setItem(intValue("season-menu.profile.slot"), createPlayerHeadCard(
        player.getUniqueId(),
        text("season-menu.profile.title"),
        lines("season-menu.profile.lore", placeholders(
            "season", seasonManager.currentSeasonId(),
            "season_points", profile.getSeasonPoints(),
            "points", profile.getPoints(),
            "claimed", profile.getClaimedSeasonRewards().size(),
            "reward_count", seasonManager.getRewardThresholds().size(),
            "prestige", profile.getPrestigeLevel()
        )),
        boolValue("season-menu.profile.glow")
    ));

    List<Integer> rewardSlots = intList("season-menu.rewards.auto-slots");
    List<Integer> thresholds = seasonManager.getRewardThresholds();
    for (int index = 0; index < Math.min(rewardSlots.size(), thresholds.size()); index++) {
        int threshold = thresholds.get(index);
        inventory.setItem(rewardSlots.get(index), createSeasonRewardCard(profile, threshold));
    }
    if (thresholds.isEmpty()) {
        inventory.setItem(intValue("season-menu.rewards.empty.slot"), ItemUtils.create(
            text("season-menu.rewards.empty.material"),
            text("season-menu.rewards.empty.title"),
            lines("season-menu.rewards.empty.lore"),
            boolValue("season-menu.rewards.empty.glow")
        ));
    }

    inventory.setItem(intValue("season-menu.leaderboard.slot"), ItemUtils.create(
        text("season-menu.leaderboard.material"),
        text("season-menu.leaderboard.title"),
        buildSeasonLeaderboardLore(),
        boolValue("season-menu.leaderboard.glow")
    ));
    inventory.setItem(intValue("season-menu.progress.slot"), ItemUtils.create(
        text("season-menu.progress.material"),
        text("season-menu.progress.title"),
        lines("season-menu.progress.lore", placeholders(
            "next_threshold", nextSeasonThreshold(profile.getSeasonPoints()),
            "reward_count", thresholds.size(),
            "claimed", profile.getClaimedSeasonRewards().size()
        )),
        boolValue("season-menu.progress.glow")
    ));
    inventory.setItem(intValue("season-menu.challenges.slot"), ItemUtils.create(
        text("season-menu.challenges.material"),
        text("season-menu.challenges.title"),
        lines("season-menu.challenges.lore"),
        boolValue("season-menu.challenges.glow")
    ));
    inventory.setItem(intValue("season-menu.prestige.slot"), ItemUtils.create(
        text("season-menu.prestige.material"),
        text("season-menu.prestige.title"),
        lines("season-menu.prestige.lore"),
        boolValue("season-menu.prestige.glow")
    ));
    inventory.setItem(intValue("season-menu.back.slot"), ItemUtils.create(
        text("season-menu.back.material"),
        text("season-menu.back.title"),
        lines("season-menu.back.lore"),
        boolValue("season-menu.back.glow")
    ));

    player.openInventory(inventory);
    }

    public void openPrestige(Player player) {
    PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
    int total = achievementManager.getAdvancements().size();
    int completed = profile.getCompletedAdvancements();
    boolean eligible = completed >= total && total > 0;

    Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.PRESTIGE, null, false), MENU_SIZE, ItemUtils.component(text("prestige-menu.title")));

    paintFrame(inventory, "prestige-menu.frame");
    applyFillerGroups(inventory, "prestige-menu.fillers");
    fillEmptySlots(inventory, "prestige-menu.background");

    inventory.setItem(intValue("prestige-menu.profile.slot"), createPlayerHeadCard(
        player.getUniqueId(),
        text("prestige-menu.profile.title"),
        lines("prestige-menu.profile.lore", placeholders(
            "prestige", profile.getPrestigeLevel(),
            "completed", completed,
            "total", total,
            "season_points", profile.getSeasonPoints(),
            "bonus", configManager.getMainConfig().getInt("prestige.bonus-points", 250)
        )),
        boolValue("prestige-menu.profile.glow")
    ));
    inventory.setItem(intValue("prestige-menu.readiness.slot"), ItemUtils.create(
        text("prestige-menu.readiness.material"),
        text("prestige-menu.readiness.title"),
        lines("prestige-menu.readiness.lore", placeholders(
            "completed", completed,
            "total", total,
            "progress", total == 0 ? 0 : formatPercent((completed * 100.0D) / total),
            "status", text(eligible ? "prestige-menu.status.ready" : "prestige-menu.status.locked")
        )),
        boolValue("prestige-menu.readiness.glow")
    ));
    inventory.setItem(intValue("prestige-menu.impact.slot"), ItemUtils.create(
        text("prestige-menu.impact.material"),
        text("prestige-menu.impact.title"),
        lines("prestige-menu.impact.lore", placeholders(
            "challenge_reset", configManager.getMainConfig().getBoolean("prestige.reset-challenges", true) ? text("prestige-menu.values.enabled") : text("prestige-menu.values.disabled"),
            "bonus", configManager.getMainConfig().getInt("prestige.bonus-points", 250)
        )),
        boolValue("prestige-menu.impact.glow")
    ));
    inventory.setItem(intValue("prestige-menu.rewards.slot"), ItemUtils.create(
        text("prestige-menu.rewards.material"),
        text("prestige-menu.rewards.title"),
        lines("prestige-menu.rewards.lore", placeholders(
            "bonus", configManager.getMainConfig().getInt("prestige.bonus-points", 250),
            "next_prestige", profile.getPrestigeLevel() + 1
        )),
        boolValue("prestige-menu.rewards.glow")
    ));

    String actionPath = eligible ? "prestige-menu.action.ready" : "prestige-menu.action.locked";
    inventory.setItem(intValue("prestige-menu.action.slot"), ItemUtils.create(
        text(actionPath + ".material"),
        text(actionPath + ".title"),
        lines(actionPath + ".lore", placeholders(
            "completed", completed,
            "total", total,
            "next_prestige", profile.getPrestigeLevel() + 1,
            "bonus", configManager.getMainConfig().getInt("prestige.bonus-points", 250)
        )),
        boolValue(actionPath + ".glow")
    ));
    inventory.setItem(intValue("prestige-menu.season.slot"), ItemUtils.create(
        text("prestige-menu.season.material"),
        text("prestige-menu.season.title"),
        lines("prestige-menu.season.lore"),
        boolValue("prestige-menu.season.glow")
    ));
    inventory.setItem(intValue("prestige-menu.challenges.slot"), ItemUtils.create(
        text("prestige-menu.challenges.material"),
        text("prestige-menu.challenges.title"),
        lines("prestige-menu.challenges.lore"),
        boolValue("prestige-menu.challenges.glow")
    ));
    inventory.setItem(intValue("prestige-menu.back.slot"), ItemUtils.create(
        text("prestige-menu.back.material"),
        text("prestige-menu.back.title"),
        lines("prestige-menu.back.lore"),
        boolValue("prestige-menu.back.glow")
    ));

    player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }

        switch (holder.type()) {
            case MAIN -> handleMainClick(player, event.getSlot());
            case TIER -> handleTierClick(player, holder.tier(), event.getSlot());
            case CATEGORY -> handleCategoryClick(player, event.getSlot());
            case STATS -> {
                if (event.getSlot() == intValue("stats.back.slot")) {
                    openMainMenu(player);
                }
            }
            case LEADERBOARD -> handleLeaderboardClick(player, event.getSlot());
            case CHALLENGES -> handleChallengesClick(player, event.getSlot());
            case SEASON -> handleSeasonClick(player, event.getSlot());
            case PRESTIGE -> handlePrestigeClick(player, event.getSlot());
            case SETTINGS -> handleSettingsClick(player, event.getSlot());
        }
    }

    private void handleMainClick(Player player, int slot) {
        for (Tier tier : Tier.values()) {
            if (slot == mainTierSlot(tier) && mainTierSlot(tier) >= 0) {
                openTierMenu(player, tier);
                return;
            }
        }
        if (slot == intValue("main.next-target.slot")) {
            BetterAdvancement nextTarget = findNextTarget(playerDataManager.getOrCreate(player.getUniqueId()));
            if (nextTarget != null) {
                openTierMenu(player, nextTarget.tier());
                return;
            }
        }
        if (slot == intValue("main.stats.slot")) {
            openStats(player);
            return;
        }
        if (slot == intValue("main.challenge.slot")) {
            openChallenges(player);
            return;
        }
        if (slot == intValue("main.categories.slot")) {
            openCategoryMenu(player);
            return;
        }
        if (slot == intValue("main.leaderboard.slot")) {
            openLeaderboard(player, false);
            return;
        }
        if (slot == intValue("main.season.slot")) {
            openSeason(player);
            return;
        }
        if (slot == intValue("main.prestige.slot")) {
            openPrestige(player);
            return;
        }
        if (slot == intValue("main.settings.slot")) {
            openSettings(player);
            return;
        }
    }

    private void handleCategoryClick(Player player, int slot) {
        if (slot == intValue("category-menu.stats.slot")) {
            openStats(player);
            return;
        }
        if (slot == intValue("category-menu.settings.slot")) {
            openSettings(player);
            return;
        }
        if (slot == intValue("category-menu.back.slot")) {
            openMainMenu(player);
            return;
        }

        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        List<String> categories = orderedCategories();
        List<Integer> slots = intList("category-menu.auto-slots");
        for (int index = 0; index < Math.min(categories.size(), slots.size()); index++) {
            if (slot != slots.get(index)) {
                continue;
            }
            BetterAdvancement target = findNextTarget(profile, categories.get(index));
            if (target != null) {
                openTierMenu(player, target.tier());
            }
            return;
        }
    }

    private void handleSettingsClick(Player player, int slot) {
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        boolean changed = false;

        if (slot == intValue("settings-menu.language.slot")) {
            String nextLanguage = cycleLanguage(player, profile);
            player.sendMessage(languageManager.getComponent(player, nextLanguage, "command.language-set", Map.of(
                    "language", languageManager.getDisplayName(nextLanguage)
            )));
            openSettings(player);
            return;
        }
        if (slot == intValue("settings-menu.toggles.chat.slot")) {
            profile.setChatNotificationsEnabled(!profile.isChatNotificationsEnabled());
            changed = true;
        } else if (slot == intValue("settings-menu.toggles.title.slot")) {
            profile.setTitleNotificationsEnabled(!profile.isTitleNotificationsEnabled());
            changed = true;
        } else if (slot == intValue("settings-menu.toggles.action-bar.slot")) {
            profile.setActionBarNotificationsEnabled(!profile.isActionBarNotificationsEnabled());
            changed = true;
        } else if (slot == intValue("settings-menu.toggles.boss-bar.slot")) {
            profile.setBossBarNotificationsEnabled(!profile.isBossBarNotificationsEnabled());
            changed = true;
        } else if (slot == intValue("settings-menu.toggles.sound.slot")) {
            profile.setSoundNotificationsEnabled(!profile.isSoundNotificationsEnabled());
            changed = true;
        } else if (slot == intValue("settings-menu.categories.slot")) {
            openCategoryMenu(player);
            return;
        } else if (slot == intValue("settings-menu.back.slot")) {
            openMainMenu(player);
            return;
        }

        if (changed) {
            playerDataManager.setLastKnownName(player.getUniqueId(), player.getName());
            playerDataManager.saveProfile(player.getUniqueId());
            openSettings(player);
        }
    }

    private void handleTierClick(Player player, Tier tier, int slot) {
        if (slot == intValue("tier.navigation.previous.slot")) {
            Tier previous = previousTier(tier);
            if (previous != null) {
                openTierMenu(player, previous);
            }
            return;
        }
        if (slot == intValue("tier.navigation.back.slot")) {
            openMainMenu(player);
            return;
        }
        if (slot == intValue("tier.navigation.next.slot")) {
            Tier next = nextTier(tier);
            if (next != null) {
                openTierMenu(player, next);
            }
        }
    }

    private void handleLeaderboardClick(Player player, int slot) {
        if (slot == intValue("leaderboard.footer.global.slot")) {
            openLeaderboard(player, false);
            return;
        }
        if (slot == intValue("leaderboard.footer.back.slot")) {
            openMainMenu(player);
            return;
        }
        if (slot == intValue("leaderboard.footer.session.slot")) {
            openLeaderboard(player, true);
        }
    }

    private void handleChallengesClick(Player player, int slot) {
        if (slot == intValue("challenges.season.slot")) {
            openSeason(player);
            return;
        }
        if (slot == intValue("challenges.prestige.slot")) {
            openPrestige(player);
            return;
        }
        if (slot == intValue("challenges.back.slot")) {
            openMainMenu(player);
        }
    }

    private void handleSeasonClick(Player player, int slot) {
        if (slot == intValue("season-menu.challenges.slot")) {
            openChallenges(player);
            return;
        }
        if (slot == intValue("season-menu.prestige.slot")) {
            openPrestige(player);
            return;
        }
        if (slot == intValue("season-menu.back.slot")) {
            openMainMenu(player);
        }
    }

    private void handlePrestigeClick(Player player, int slot) {
        if (slot == intValue("prestige-menu.action.slot")) {
            attemptPrestige(player);
            openPrestige(player);
            return;
        }
        if (slot == intValue("prestige-menu.season.slot")) {
            openSeason(player);
            return;
        }
        if (slot == intValue("prestige-menu.challenges.slot")) {
            openChallenges(player);
            return;
        }
        if (slot == intValue("prestige-menu.back.slot")) {
            openMainMenu(player);
        }
    }

    private void paintFrame(Inventory inventory, String path) {
        ItemStack border = ItemUtils.create(text(path + ".material"), text(path + ".name"), List.of(), false);
        int rows = inventory.getSize() / 9;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inventory.setItem(slot, border);
            }
        }
    }

    private void applyFillerGroups(Inventory inventory, String path) {
        ConfigurationSection section = section(path);
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String groupPath = path + "." + key;
            ItemStack item = ItemUtils.create(text(groupPath + ".material"), text(groupPath + ".name"), List.of(), false);
            for (int slot : intList(groupPath + ".slots")) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }

    private void fillEmptySlots(Inventory inventory, String path) {
        ConfigurationSection background = section(path);
        if (background == null) {
            return;
        }
        ItemStack item = ItemUtils.create(text(path + ".material"), text(path + ".name"), List.of(), boolValue(path + ".glow"));
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, item);
            }
        }
    }

    private void fillTierNavigation(Inventory inventory, PlayerProfile profile, Tier tier) {
        Tier previous = previousTier(tier);
        Tier next = nextTier(tier);

        String previousPath = previous == null ? "tier.navigation.previous.empty" : "tier.navigation.previous.active";
        inventory.setItem(intValue("tier.navigation.previous.slot"), ItemUtils.create(
                text(previousPath + ".material"),
                text(previousPath + ".title", placeholders("tier_name", previous == null ? formatTierName(tier) : formatTierName(previous))),
                lines(previousPath + ".lore", placeholders("tier_name", previous == null ? formatTierName(tier) : formatTierName(previous))),
                boolValue(previousPath + ".glow")
        ));

        inventory.setItem(intValue("tier.navigation.legend-completed.slot"), ItemUtils.create(
                text("tier.navigation.legend-completed.material"),
                text("tier.navigation.legend-completed.title"),
                lines("tier.navigation.legend-completed.lore"),
                boolValue("tier.navigation.legend-completed.glow")
        ));
        inventory.setItem(intValue("tier.navigation.legend-unlocked.slot"), ItemUtils.create(
                text("tier.navigation.legend-unlocked.material"),
                text("tier.navigation.legend-unlocked.title"),
                lines("tier.navigation.legend-unlocked.lore"),
                boolValue("tier.navigation.legend-unlocked.glow")
        ));
        inventory.setItem(intValue("tier.navigation.legend-locked.slot"), ItemUtils.create(
                text("tier.navigation.legend-locked.material"),
                text("tier.navigation.legend-locked.title"),
                lines("tier.navigation.legend-locked.lore"),
                boolValue("tier.navigation.legend-locked.glow")
        ));
        inventory.setItem(intValue("tier.navigation.back.slot"), ItemUtils.create(
                text("tier.navigation.back.material"),
                text("tier.navigation.back.title"),
                lines("tier.navigation.back.lore"),
                boolValue("tier.navigation.back.glow")
        ));
        inventory.setItem(intValue("tier.navigation.horizontal.slot"), createConnectorCard(
                text("tier.navigation.horizontal.title"),
                text("tier.navigation.horizontal.detail"),
                1,
                0,
                false
        ));
        inventory.setItem(intValue("tier.navigation.vertical.slot"), createConnectorCard(
                text("tier.navigation.vertical.title"),
                text("tier.navigation.vertical.detail"),
                0,
                1,
                false
        ));
        inventory.setItem(intValue("tier.navigation.focus.slot"), ItemUtils.create(
                text("tier.navigation.focus.material"),
                text("tier.navigation.focus.title"),
                buildTierFocusLore(profile, tier),
                boolValue("tier.navigation.focus.glow")
        ));

        String nextPath = next == null ? "tier.navigation.next.empty" : "tier.navigation.next.active";
        inventory.setItem(intValue("tier.navigation.next.slot"), ItemUtils.create(
                text(nextPath + ".material"),
                text(nextPath + ".title", placeholders("tier_name", next == null ? formatTierName(tier) : formatTierName(next))),
                lines(nextPath + ".lore", placeholders("tier_name", next == null ? formatTierName(tier) : formatTierName(next))),
                boolValue(nextPath + ".glow")
        ));
    }

    private void drawDependencyPaths(Inventory inventory, List<BetterAdvancement> tierAdvancements, PlayerProfile profile, Set<Integer> blockedSlots) {
        for (BetterAdvancement advancement : tierAdvancements) {
            for (String dependencyId : advancement.dependencies()) {
                BetterAdvancement dependency = tierAdvancements.stream()
                        .filter(candidate -> candidate.id().equalsIgnoreCase(dependencyId))
                        .findFirst()
                        .orElse(null);
                if (dependency == null) {
                    continue;
                }
                PlayerAchievementProgress dependencyProgress = profile.getAdvancementProgress().get(dependency.id());
                boolean completed = dependencyProgress != null && dependencyProgress.isCompleted();
                drawRoute(inventory, dependency.guiPosition().getOrDefault("slot", 0), advancement.guiPosition().getOrDefault("slot", 0), completed, blockedSlots);
            }
        }
    }

    private void drawRoute(Inventory inventory, int sourceSlot, int targetSlot, boolean completed, Set<Integer> blockedSlots) {
        int row = sourceSlot / 9;
        int col = sourceSlot % 9;
        int targetRow = targetSlot / 9;
        int targetCol = targetSlot % 9;

        while (col != targetCol) {
            int nextCol = col + Integer.compare(targetCol, col);
            int pathSlot = row * 9 + nextCol;
            if (pathSlot == targetSlot) {
                break;
            }
            placeConnector(inventory, pathSlot, Integer.compare(nextCol, col), 0, completed, blockedSlots);
            col = nextCol;
        }
        while (row != targetRow) {
            int nextRow = row + Integer.compare(targetRow, row);
            int pathSlot = nextRow * 9 + col;
            if (pathSlot == targetSlot) {
                break;
            }
            placeConnector(inventory, pathSlot, 0, Integer.compare(nextRow, row), completed, blockedSlots);
            row = nextRow;
        }
    }

    private void placeConnector(Inventory inventory, int slot, int deltaCol, int deltaRow, boolean completed, Set<Integer> blockedSlots) {
        if (slot < 0 || slot >= inventory.getSize() || blockedSlots.contains(slot)) {
            return;
        }
        inventory.setItem(slot, createConnectorCard(
                text(completed ? "tier.connectors.title-completed" : "tier.connectors.title-locked", placeholders("direction", connectorSymbol(deltaCol, deltaRow))),
                text(completed ? "tier.connectors.detail-completed" : "tier.connectors.detail-locked"),
                deltaCol,
                deltaRow,
                completed
        ));
    }

    private ItemStack createConnectorCard(String title, String detail, int deltaCol, int deltaRow, boolean completed) {
        return ItemUtils.create(connectorMaterial(deltaCol, deltaRow, completed), title, List.of(detail, text("tier.connectors.direction-lore", placeholders("direction", connectorSymbol(deltaCol, deltaRow)))), false);
    }

    private ItemStack createPlayerHeadCard(UUID uniqueId, String title, List<String> lore, boolean glow) {
        ItemStack item = ItemUtils.create("PLAYER_HEAD", title, lore, glow);
        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
            skullMeta.setOwningPlayer(offlinePlayer);
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    private ItemStack createLeaderboardCard(LeaderboardEntry entry, int rank, boolean podium) {
        String badge = text("leaderboard.cards.badges." + rank);
        List<String> lore = new ArrayList<>(lines("leaderboard.cards.lore", placeholders(
                "completed", entry.completed(),
                "progression", formatPercent(entry.progression()),
                "points", entry.points(),
                "highest_tier", formatTierName(weightToTier(entry.highestTier()))
        )));
        if (podium) {
            lore.addAll(lines("leaderboard.cards.podium-extra"));
        }
        return createPlayerHeadCard(entry.uniqueId(), text("leaderboard.cards.title", placeholders("badge", badge, "player", entry.name())), lore, podium);
    }

    private List<String> buildChallengeSpotlightLore() {
        Collection<ChallengeDefinition> challenges = challengeManager.getActiveChallenges();
        if (challenges.isEmpty()) {
            return lines("main.challenge.empty-lore");
        }
        List<String> lore = new ArrayList<>();
        for (ChallengeDefinition challenge : challenges) {
            lore.add(text("main.challenge.entry-title", placeholders(
                    "type", capitalize(challenge.type()),
                    "title", challenge.title()
            )));
            lore.add(text("main.challenge.entry-description", placeholders("description", challenge.description())));
            lore.add(text("main.challenge.entry-reward", placeholders("reward", challenge.pointsReward())));
        }
        lore.addAll(lines("main.challenge.footer-lore"));
        return lore;
    }

    private ItemStack createChallengeCard(String path, ChallengeDefinition challenge, PlayerProfile profile) {
        if (challenge == null) {
            return ItemUtils.create(
                    text(path + ".empty.material"),
                    text(path + ".empty.title"),
                    lines(path + ".empty.lore"),
                    boolValue(path + ".empty.glow")
            );
        }

        PlayerChallengeProgress progress = profile.getChallengeProgress().get(challenge.id());
        boolean completed = progress != null && progress.isCompleted();
        int current = progress == null ? 0 : progress.getProgress();
        return ItemUtils.create(
                text(path + ".active.material"),
                text(path + ".active.title", placeholders("title", challenge.title())),
                lines(path + ".active.lore", placeholders(
                        "title", challenge.title(),
                        "description", challenge.description(),
                        "type", capitalize(challenge.type()),
                        "progress", current,
                        "target", challenge.trigger().target(),
                        "status", text(completed ? "challenges.status.completed" : "challenges.status.active"),
                        "objective", formatChallengeObjective(challenge),
                        "points", challenge.pointsReward(),
                        "rewards", formatChallengeRewards(challenge)
                )),
                completed || boolValue(path + ".active.glow")
        );
    }

    private ItemStack createSeasonRewardCard(PlayerProfile profile, int threshold) {
        boolean claimed = profile.getClaimedSeasonRewards().contains(threshold);
        boolean ready = profile.getSeasonPoints() >= threshold;
        String status = claimed
                ? text("season-menu.status.claimed")
                : ready ? text("season-menu.status.ready") : text("season-menu.status.locked");

        return ItemUtils.create(
                text("season-menu.rewards.card.material"),
                text("season-menu.rewards.card.title", placeholders("threshold", threshold)),
                lines("season-menu.rewards.card.lore", placeholders(
                        "threshold", threshold,
                        "status", status,
                        "season_points", profile.getSeasonPoints(),
                        "commands", seasonManager.getRewardCommands(threshold).size()
                )),
                claimed || ready
        );
    }

            private ItemStack createCategorySummaryCard(PlayerProfile profile, String category) {
            String categoryPath = section("category-menu.categories." + category) == null
                ? "category-menu.categories.default"
                : "category-menu.categories." + category;
            long total = achievementManager.getAdvancements().stream()
                .filter(advancement -> advancement.category().equalsIgnoreCase(category))
                .count();
            long completed = achievementManager.getAdvancements().stream()
                .filter(advancement -> advancement.category().equalsIgnoreCase(category))
                .filter(advancement -> isCompleted(profile, advancement))
                .count();
            long unlocked = achievementManager.getAdvancements().stream()
                .filter(advancement -> advancement.category().equalsIgnoreCase(category))
                .filter(advancement -> achievementManager.isUnlocked(profile, advancement))
                .count();
            long rare = achievementManager.getAdvancements().stream()
                .filter(advancement -> advancement.category().equalsIgnoreCase(category))
                .filter(advancement -> advancement.rarity().equalsIgnoreCase("legendary") || advancement.rarity().equalsIgnoreCase("mythic"))
                .filter(advancement -> isCompleted(profile, advancement))
                .count();
            BetterAdvancement focus = findNextTarget(profile, category);

            return ItemUtils.create(
                text(categoryPath + ".material"),
                text(categoryPath + ".title", placeholders("category", capitalize(category))),
                lines(categoryPath + ".lore", placeholders(
                    "category", capitalize(category),
                    "completed", completed,
                    "total", total,
                    "unlocked", unlocked,
                    "rare", rare,
                    "focus", focus == null ? text("category-menu.values.none") : focus.title(),
                    "tier", focus == null ? text("category-menu.values.none") : formatTierName(focus.tier())
                )),
                completed > 0 || boolValue(categoryPath + ".glow")
            );
            }

            private ItemStack createSettingsToggleCard(String path, boolean enabled) {
            String statePath = enabled ? path + ".enabled" : path + ".disabled";
            return ItemUtils.create(
                text(path + ".material"),
                text(statePath + ".title"),
                lines(statePath + ".lore", placeholders("state", settingState(enabled))),
                enabled
            );
            }

    private List<String> buildSeasonLeaderboardLore() {
        List<LeaderboardEntry> entries = leaderboardManager.getSeasonLeaderboard(3);
        if (entries.isEmpty()) {
            return lines("season-menu.leaderboard.empty-lore");
        }

        List<String> lore = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            LeaderboardEntry entry = entries.get(index);
            lore.add(text("season-menu.leaderboard.entry", placeholders(
                    "rank", index + 1,
                    "player", entry.name(),
                    "points", entry.points()
            )));
        }
        lore.addAll(lines("season-menu.leaderboard.footer-lore"));
        return lore;
    }

    private Integer nextSeasonThreshold(int seasonPoints) {
        return seasonManager.getRewardThresholds().stream()
                .filter(threshold -> threshold > seasonPoints)
                .findFirst()
                .orElse(0);
    }

    private List<String> orderedCategories() {
        List<String> ordered = new ArrayList<>(config().getStringList("category-menu.order"));
        for (BetterAdvancement advancement : achievementManager.getAdvancements()) {
            String category = advancement.category().toLowerCase(Locale.ROOT);
            if (!ordered.contains(category)) {
                ordered.add(category);
            }
        }
        return ordered;
    }

    private String cycleLanguage(Player player, PlayerProfile profile) {
        List<String> languages = new ArrayList<>(languageManager.getLanguages().keySet());
        if (languages.isEmpty()) {
            return profile.getLanguage();
        }
        languages.sort(String::compareToIgnoreCase);
        String current = profile.getLanguage().toLowerCase(Locale.ROOT);
        int currentIndex = languages.indexOf(current);
        String next = languages.get((currentIndex + 1 + languages.size()) % languages.size());
        profile.setLanguage(next);
        playerDataManager.setLastKnownName(player.getUniqueId(), player.getName());
        playerDataManager.saveProfile(player.getUniqueId());
        return next;
    }

    private String settingState(boolean enabled) {
        return text(enabled ? "settings-menu.states.enabled" : "settings-menu.states.disabled");
    }

    private ChallengeDefinition findActiveChallenge(String type) {
        return challengeManager.getActiveChallenges().stream()
                .filter(challenge -> challenge.type().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

    private String formatChallengeObjective(ChallengeDefinition challenge) {
        Map<String, String> conditions = challenge.trigger().conditions();
        String subject = switch (challenge.trigger().type()) {
            case BLOCK_BREAK -> capitalize(conditions.getOrDefault("material", text("texts.objective.subject-default.blocks")));
            case ITEM_CRAFT, ITEM_CONSUME, SMELT -> capitalize(conditions.getOrDefault("item", text("texts.objective.subject-default.items")));
            case MOB_KILL, PLAYER_KILL, BREED, TAME -> capitalize(conditions.getOrDefault("entity", text("texts.objective.subject-default.targets")));
            case EXPLORE_BIOME -> capitalize(conditions.getOrDefault("biome", text("texts.objective.subject-default.biomes")));
            case COMMAND -> "/" + conditions.getOrDefault("command", text("texts.objective.subject-default.command"));
            case JOIN -> text("texts.objective.subject-default.server-joins");
            case DISTANCE_WALK -> text("texts.objective.subject-default.blocks-travelled");
            case FISH -> text("texts.objective.subject-default.fish-caught");
            case ENCHANT -> text("texts.objective.subject-default.enchants");
            case PLAYTIME -> text("texts.objective.subject-default.minutes-played");
            case CUSTOM -> text("texts.objective.subject-default.custom");
        };

        return switch (challenge.trigger().type()) {
            case JOIN -> text("texts.objective.templates.join");
            case BLOCK_BREAK -> text("texts.objective.templates.break", placeholders("target", challenge.trigger().target(), "subject", subject));
            case ITEM_CRAFT -> text("texts.objective.templates.craft", placeholders("target", challenge.trigger().target(), "subject", subject));
            case MOB_KILL -> text("texts.objective.templates.defeat", placeholders("target", challenge.trigger().target(), "subject", subject));
            case PLAYER_KILL -> text(challenge.trigger().target() == 1 ? "texts.objective.templates.defeat-player-single" : "texts.objective.templates.defeat-player-multi", placeholders("target", challenge.trigger().target(), "subject", subject));
            case EXPLORE_BIOME -> text("texts.objective.templates.discover", placeholders("target", challenge.trigger().target(), "subject", subject));
            case DISTANCE_WALK -> text("texts.objective.templates.travel", placeholders("target", challenge.trigger().target(), "subject", subject));
            case FISH -> text("texts.objective.templates.fish", placeholders("target", challenge.trigger().target(), "subject", subject));
            case ENCHANT -> text(challenge.trigger().target() == 1 ? "texts.objective.templates.enchant-single" : "texts.objective.templates.enchant-multi", placeholders("target", challenge.trigger().target(), "subject", subject));
            case SMELT -> text("texts.objective.templates.smelt", placeholders("target", challenge.trigger().target(), "subject", subject));
            case BREED -> text("texts.objective.templates.breed", placeholders("target", challenge.trigger().target(), "subject", subject));
            case TAME -> text("texts.objective.templates.tame", placeholders("target", challenge.trigger().target(), "subject", subject));
            case PLAYTIME -> text("texts.objective.templates.playtime", placeholders("target", challenge.trigger().target(), "subject", subject));
            case ITEM_CONSUME -> text("texts.objective.templates.consume", placeholders("target", challenge.trigger().target(), "subject", subject));
            case COMMAND -> text("texts.objective.templates.command", placeholders("target", challenge.trigger().target(), "subject", subject));
            case CUSTOM -> text("texts.objective.templates.custom", placeholders("target", challenge.trigger().target(), "subject", subject));
        };
    }

    private String formatChallengeRewards(ChallengeDefinition challenge) {
        List<String> rewards = new ArrayList<>();
        rewards.add(text("texts.rewards.templates.points", placeholders("amount", challenge.pointsReward())));
        rewards.addAll(challenge.rewards().stream().map(this::formatReward).toList());
        return rewards.stream()
                .reduce((left, right) -> left + text("texts.general.list-separator") + right)
                .orElse(text("texts.rewards.none"));
    }

    private boolean attemptPrestige(Player player) {
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        int total = achievementManager.getAdvancements().size();
        int completed = profile.getCompletedAdvancements();
        String locale = languageManager.getLocale(player);
        if (completed < total) {
            player.sendMessage(languageManager.getComponent(player, locale, "command.prestige-requirements", Map.of(
                "completed", String.valueOf(completed),
                "total", String.valueOf(total)
            )));
            return false;
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
        player.sendMessage(languageManager.getComponent(player, locale, "command.prestige-success", Map.of(
            "prestige", String.valueOf(profile.getPrestigeLevel())
        )));
        return true;
    }

    private List<String> buildTierFocusLore(PlayerProfile profile, Tier tier) {
        BetterAdvancement nextTarget = achievementManager.getByTier(tier).stream()
                .filter(advancement -> !isCompleted(profile, advancement))
                .filter(advancement -> achievementManager.isUnlocked(profile, advancement))
                .min(Comparator.comparingInt(advancement -> advancement.guiPosition().getOrDefault("slot", 0)))
                .orElse(null);
        if (nextTarget == null) {
            return lines("tier.navigation.focus.empty-lore", placeholders("tier_name", formatTierName(tier)));
        }
        return lines("tier.navigation.focus.active-lore", placeholders(
                "target", nextTarget.title(),
                "goal", nextTarget.trigger().target(),
                "category", capitalize(nextTarget.category()),
                "tier_name", formatTierName(tier)
        ));
    }

    private List<String> buildCurrentObjectiveLore(PlayerProfile profile) {
        BetterAdvancement nextTarget = findNextTarget(profile);
        if (nextTarget == null) {
            return lines("stats.objective.empty-lore");
        }
        return lines("stats.objective.active-lore", placeholders(
                "target", nextTarget.title(),
                "tier", formatTierName(nextTarget.tier()),
                "goal", nextTarget.trigger().target()
        ));
    }

    private BetterAdvancement findNextTarget(PlayerProfile profile) {
        return achievementManager.getAdvancements().stream()
                .sorted(Comparator.comparingInt((BetterAdvancement advancement) -> advancement.tier().getWeight())
                        .thenComparingInt(advancement -> advancement.guiPosition().getOrDefault("slot", 0)))
                .filter(advancement -> !isCompleted(profile, advancement))
                .filter(advancement -> achievementManager.isUnlocked(profile, advancement))
                .findFirst()
                .orElseGet(() -> achievementManager.getAdvancements().stream()
                        .sorted(Comparator.comparingInt((BetterAdvancement advancement) -> advancement.tier().getWeight())
                                .thenComparingInt(advancement -> advancement.guiPosition().getOrDefault("slot", 0)))
                        .filter(advancement -> !isCompleted(profile, advancement))
                        .findFirst()
                        .orElse(null));
    }

                private BetterAdvancement findNextTarget(PlayerProfile profile, String category) {
                return achievementManager.getAdvancements().stream()
                    .sorted(Comparator.comparingInt((BetterAdvancement advancement) -> advancement.tier().getWeight())
                        .thenComparingInt(advancement -> advancement.guiPosition().getOrDefault("slot", 0)))
                    .filter(advancement -> advancement.category().equalsIgnoreCase(category))
                    .filter(advancement -> !isCompleted(profile, advancement))
                    .filter(advancement -> achievementManager.isUnlocked(profile, advancement))
                    .findFirst()
                    .orElseGet(() -> achievementManager.getAdvancements().stream()
                        .sorted(Comparator.comparingInt((BetterAdvancement advancement) -> advancement.tier().getWeight())
                            .thenComparingInt(advancement -> advancement.guiPosition().getOrDefault("slot", 0)))
                        .filter(advancement -> advancement.category().equalsIgnoreCase(category))
                        .findFirst()
                        .orElse(null));
                }

    private boolean isCompleted(PlayerProfile profile, BetterAdvancement advancement) {
        PlayerAchievementProgress progress = profile.getAdvancementProgress().get(advancement.id());
        return progress != null && progress.isCompleted();
    }

    private int getUnlockedCount(PlayerProfile profile, Tier tier) {
        return (int) achievementManager.getByTier(tier).stream()
                .filter(advancement -> achievementManager.isUnlocked(profile, advancement))
                .count();
    }

    private long getRareTierCount(PlayerProfile profile, Tier tier) {
        return achievementManager.getByTier(tier).stream()
                .filter(advancement -> advancement.rarity().equalsIgnoreCase("legendary") || advancement.rarity().equalsIgnoreCase("mythic"))
                .filter(advancement -> isCompleted(profile, advancement))
                .count();
    }

    private List<String> buildMainTierStateLore(PlayerProfile profile, Tier tier, int completion) {
        if (completion >= 100) {
            return lines("main.tiers.state.completed", placeholders("tier_name", formatTierName(tier)));
        }
        Tier previous = previousTier(tier);
        if (previous != null && achievementManager.getTierCompletion(profile, previous) < 100) {
            return lines("main.tiers.state.locked", placeholders(
                    "tier_name", formatTierName(tier),
                    "previous_tier", formatTierName(previous)
            ));
        }
        return lines("main.tiers.state.available", placeholders("tier_name", formatTierName(tier)));
    }

    private Tier getHighestCompletedTier(PlayerProfile profile) {
        return achievementManager.getAdvancements().stream()
                .filter(advancement -> isCompleted(profile, advancement))
                .map(BetterAdvancement::tier)
                .max(Comparator.comparingInt(Tier::getWeight))
                .orElse(Tier.first());
    }

    private String joinTitles(List<String> dependencyIds) {
        return dependencyIds.stream()
                .map(id -> achievementManager.getAdvancement(id)
                        .map(BetterAdvancement::title)
                        .orElse(capitalize(id)))
            .reduce((left, right) -> left + text("texts.general.list-separator") + right)
            .orElse(text("texts.general.none"));
    }

    private String formatObjective(BetterAdvancement advancement) {
        Map<String, String> conditions = advancement.trigger().conditions();
        String subject = switch (advancement.trigger().type()) {
            case BLOCK_BREAK -> capitalize(conditions.getOrDefault("material", text("texts.objective.subject-default.blocks")));
            case ITEM_CRAFT, ITEM_CONSUME, SMELT -> capitalize(conditions.getOrDefault("item", text("texts.objective.subject-default.items")));
            case MOB_KILL, PLAYER_KILL, BREED, TAME -> capitalize(conditions.getOrDefault("entity", text("texts.objective.subject-default.targets")));
            case EXPLORE_BIOME -> capitalize(conditions.getOrDefault("biome", text("texts.objective.subject-default.biomes")));
            case COMMAND -> "/" + conditions.getOrDefault("command", text("texts.objective.subject-default.command"));
            case JOIN -> text("texts.objective.subject-default.server-joins");
            case DISTANCE_WALK -> text("texts.objective.subject-default.blocks-travelled");
            case FISH -> text("texts.objective.subject-default.fish-caught");
            case ENCHANT -> text("texts.objective.subject-default.enchants");
            case PLAYTIME -> text("texts.objective.subject-default.minutes-played");
            case CUSTOM -> text("texts.objective.subject-default.custom");
        };

        return switch (advancement.trigger().type()) {
            case JOIN -> text("texts.objective.templates.join");
            case BLOCK_BREAK -> text("texts.objective.templates.break", placeholders("target", advancement.trigger().target(), "subject", subject));
            case ITEM_CRAFT -> text("texts.objective.templates.craft", placeholders("target", advancement.trigger().target(), "subject", subject));
            case MOB_KILL -> text("texts.objective.templates.defeat", placeholders("target", advancement.trigger().target(), "subject", subject));
            case PLAYER_KILL -> text(advancement.trigger().target() == 1 ? "texts.objective.templates.defeat-player-single" : "texts.objective.templates.defeat-player-multi", placeholders("target", advancement.trigger().target(), "subject", subject));
            case EXPLORE_BIOME -> text("texts.objective.templates.discover", placeholders("target", advancement.trigger().target(), "subject", subject));
            case DISTANCE_WALK -> text("texts.objective.templates.travel", placeholders("target", advancement.trigger().target(), "subject", subject));
            case FISH -> text("texts.objective.templates.fish", placeholders("target", advancement.trigger().target(), "subject", subject));
            case ENCHANT -> text(advancement.trigger().target() == 1 ? "texts.objective.templates.enchant-single" : "texts.objective.templates.enchant-multi", placeholders("target", advancement.trigger().target(), "subject", subject));
            case SMELT -> text("texts.objective.templates.smelt", placeholders("target", advancement.trigger().target(), "subject", subject));
            case BREED -> text("texts.objective.templates.breed", placeholders("target", advancement.trigger().target(), "subject", subject));
            case TAME -> text("texts.objective.templates.tame", placeholders("target", advancement.trigger().target(), "subject", subject));
            case PLAYTIME -> text("texts.objective.templates.playtime", placeholders("target", advancement.trigger().target(), "subject", subject));
            case ITEM_CONSUME -> text("texts.objective.templates.consume", placeholders("target", advancement.trigger().target(), "subject", subject));
            case COMMAND -> text("texts.objective.templates.command", placeholders("target", advancement.trigger().target(), "subject", subject));
            case CUSTOM -> text("texts.objective.templates.custom", placeholders("target", advancement.trigger().target(), "subject", subject));
        };
    }

    private String formatRewards(BetterAdvancement advancement) {
        if (advancement.rewards().isEmpty()) {
            return text("texts.rewards.none");
        }
        return advancement.rewards().stream()
                .map(this::formatReward)
                .reduce((left, right) -> left + text("texts.general.list-separator") + right)
                .orElse(text("texts.rewards.none"));
    }

    private String formatReward(RewardDefinition reward) {
        return switch (reward.type()) {
            case POINTS -> text("texts.rewards.templates.points", placeholders("amount", reward.amount(), "value", reward.value()));
            case XP -> text("texts.rewards.templates.xp", placeholders("amount", reward.amount(), "value", reward.value()));
            case MONEY -> text("texts.rewards.templates.money", placeholders("amount", reward.amount(), "value", reward.value()));
            case ITEM -> text("texts.rewards.templates.item", placeholders("amount", reward.amount(), "value", capitalize(reward.value())));
            case COMMAND -> reward.value().isBlank()
                    ? text("texts.rewards.templates.command-empty", placeholders("amount", reward.amount(), "value", reward.value()))
                    : text("texts.rewards.templates.command", placeholders("amount", reward.amount(), "value", reward.value()));
            case BROADCAST -> text("texts.rewards.templates.broadcast", placeholders("amount", reward.amount(), "value", reward.value()));
            case TITLE -> text("texts.rewards.templates.title", placeholders("amount", reward.amount(), "value", reward.value()));
            case SOUND -> text("texts.rewards.templates.sound", placeholders("amount", reward.amount(), "value", reward.value()));
        };
    }

    private Tier previousTier(Tier tier) {
        Tier[] tiers = Tier.values();
        for (int index = 0; index < tiers.length; index++) {
            if (tiers[index].equals(tier)) {
                return index > 0 ? tiers[index - 1] : null;
            }
        }
        return null;
    }

    private Tier nextTier(Tier tier) {
        Tier[] tiers = Tier.values();
        for (int index = 0; index < tiers.length; index++) {
            if (tiers[index].equals(tier)) {
                return index + 1 < tiers.length ? tiers[index + 1] : null;
            }
        }
        return null;
    }

    private Tier weightToTier(int weight) {
        for (Tier tier : Tier.values()) {
            if (tier.getWeight() == weight) {
                return tier;
            }
        }
        return Tier.first();
    }

    private String formatTierName(Tier tier) {
        return tier == null ? text("texts.general.unknown") : tier.getDisplayKey();
    }

    private int mainTierSlot(Tier tier) {
        int index = tierIndex(tier);
        List<Integer> configuredSlots = intList("main.tiers.auto-slots");
        if (index < configuredSlots.size()) {
            return configuredSlots.get(index);
        }
        if (configuredSlots.isEmpty() && index >= 0 && index < DEFAULT_MAIN_TIER_SLOTS.size()) {
            return DEFAULT_MAIN_TIER_SLOTS.get(index);
        }
        return -1;
    }

    private int statsTierSlot(Tier tier) {
        int index = tierIndex(tier);
        List<Integer> configuredSlots = intList("stats.tiers.auto-slots");
        if (index < configuredSlots.size()) {
            return configuredSlots.get(index);
        }
        if (configuredSlots.isEmpty() && index >= 0 && index < DEFAULT_STATS_TIER_SLOTS.size()) {
            return DEFAULT_STATS_TIER_SLOTS.get(index);
        }
        return -1;
    }

    private int tierIndex(Tier target) {
        Tier[] tiers = Tier.values();
        for (int index = 0; index < tiers.length; index++) {
            if (tiers[index].equals(target)) {
                return index;
            }
        }
        return -1;
    }

    private String connectorMaterial(int deltaCol, int deltaRow, boolean completed) {
        if (completed) {
            return text("tier.connectors.materials.completed");
        }
        if (deltaRow != 0) {
            return text("tier.connectors.materials.vertical");
        }
        return deltaCol < 0 ? text("tier.connectors.materials.horizontal-back") : text("tier.connectors.materials.horizontal-forward");
    }

    private String connectorSymbol(int deltaCol, int deltaRow) {
        if (deltaCol > 0) {
            return text("tier.connectors.symbols.right");
        }
        if (deltaCol < 0) {
            return text("tier.connectors.symbols.left");
        }
        if (deltaRow > 0) {
            return text("tier.connectors.symbols.down");
        }
        if (deltaRow < 0) {
            return text("tier.connectors.symbols.up");
        }
        return text("tier.connectors.symbols.point");
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String formatSessionJoin(long epochMillis) {
        if (epochMillis <= 0L) {
            return text("texts.general.unknown");
        }
        return SESSION_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return text("texts.general.unknown");
        }
        String[] parts = value.replace('_', ' ').toLowerCase(Locale.ROOT).trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? text("texts.general.unknown") : builder.toString();
    }

    private String text(String path) {
        return text(path, Map.of());
    }

    private String text(String path, Map<String, ?> placeholders) {
        return applyPlaceholders(config().getString(path, ""), placeholders);
    }

    private List<String> lines(String path) {
        return lines(path, Map.of());
    }

    private List<String> lines(String path, Map<String, ?> placeholders) {
        List<String> configured = config().getStringList(path);
        List<String> resolved = new ArrayList<>();
        for (String line : configured) {
            resolved.add(applyPlaceholders(line, placeholders));
        }
        return resolved;
    }

    private int intValue(String path) {
        return config().getInt(path);
    }

    private boolean boolValue(String path) {
        return config().getBoolean(path);
    }

    private List<Integer> intList(String path) {
        return config().getIntegerList(path);
    }

    private ConfigurationSection section(String path) {
        return config().getConfigurationSection(path);
    }

    private FileConfiguration config() {
        return configManager.getGuiConfig();
    }

    private String applyPlaceholders(String value, Map<String, ?> placeholders) {
        String resolved = value == null ? "" : value;
        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return resolved;
    }

    private Map<String, Object> placeholders(Object... values) {
        Map<String, Object> placeholders = new HashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            placeholders.put(String.valueOf(values[index]), values[index + 1]);
        }
        return placeholders;
    }
}