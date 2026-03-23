package dev.erissos.betteradvancements.gui;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.config.ConfigManager;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.manager.AchievementManager;
import dev.erissos.betteradvancements.manager.ChallengeManager;
import dev.erissos.betteradvancements.manager.LeaderboardManager;
import dev.erissos.betteradvancements.model.BetterAdvancement;
import dev.erissos.betteradvancements.model.LeaderboardEntry;
import dev.erissos.betteradvancements.model.PlayerAchievementProgress;
import dev.erissos.betteradvancements.model.PlayerProfile;
import dev.erissos.betteradvancements.model.Tier;
import dev.erissos.betteradvancements.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class GUIManager implements Listener {

    private final ConfigManager configManager;
    private final AchievementManager achievementManager;
    private final ChallengeManager challengeManager;
    private final LeaderboardManager leaderboardManager;
    private final PlayerDataManager playerDataManager;

    public GUIManager(BetterAdvancementsPlugin plugin, ConfigManager configManager, AchievementManager achievementManager, ChallengeManager challengeManager, LeaderboardManager leaderboardManager, PlayerDataManager playerDataManager, dev.erissos.betteradvancements.lang.LanguageManager languageManager) {
        this.configManager = configManager;
        this.achievementManager = achievementManager;
        this.challengeManager = challengeManager;
        this.leaderboardManager = leaderboardManager;
        this.playerDataManager = playerDataManager;
    }

    public void reload() {
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.MAIN, null, false), 54, ItemUtils.component(configManager.getGuiConfig().getString("main.title", "&6Better Advancements")));
        decorate(inventory);
        for (Tier tier : Tier.values()) {
            int slot = configManager.getGuiConfig().getInt("main.tiers." + tier.name() + ".slot", 10 + tier.ordinal() * 2);
            int completion = achievementManager.getTierCompletion(playerDataManager.getOrCreate(player.getUniqueId()), tier);
            inventory.setItem(slot, ItemUtils.create(
                    configManager.getGuiConfig().getString("main.tiers." + tier.name() + ".icon", "BOOK"),
                    configManager.getGuiConfig().getString("main.tiers." + tier.name() + ".name", "&e" + tier.name()),
                    List.of("&7Completion: &f" + completion + "%", "&8Click to open tier"),
                    completion >= 100
            ));
        }
        inventory.setItem(49, ItemUtils.create("NETHER_STAR", "&bStatistics", List.of("&7Open personal metrics"), false));
        inventory.setItem(53, ItemUtils.create("PLAYER_HEAD", "&aLeaderboard", List.of("&7View global ranking"), false));
        inventory.setItem(45, ItemUtils.create("CLOCK", "&6Challenges", buildChallengeLore(), false));
        player.openInventory(inventory);
    }

    public void openTierMenu(Player player, Tier tier) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.TIER, tier, false), 54, ItemUtils.component("&6" + tier.getDisplayKey() + " Tier"));
        decorate(inventory);
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        for (BetterAdvancement advancement : achievementManager.getByTier(tier)) {
            int slot = advancement.guiPosition().getOrDefault("slot", 0);
            boolean unlocked = achievementManager.isUnlocked(profile, advancement);
            PlayerAchievementProgress progress = profile.getAdvancementProgress().get(advancement.id());
            boolean completed = progress != null && progress.isCompleted();
            String displayTitle = advancement.hidden() && !completed ? "&8???" : advancement.title();
            String displayDescription = advancement.hidden() && !completed ? "&7Hidden achievement" : advancement.description();
            List<String> lore = new ArrayList<>();
            lore.add("&7" + displayDescription);
            lore.add("&8Category: &f" + advancement.category());
            lore.add("&8Rarity: &f" + advancement.rarity());
            lore.add("&8Points: &f" + advancement.points());
            lore.add("&8Progress: &f" + (progress == null ? 0 : progress.getProgress()) + "/" + advancement.trigger().target());
            lore.add(completed ? "&aCompleted" : unlocked ? "&eUnlocked" : "&cLocked");
            inventory.setItem(slot, ItemUtils.create(completed ? "LIME_STAINED_GLASS_PANE" : unlocked ? advancement.icon() : "GRAY_STAINED_GLASS_PANE", displayTitle, lore, completed));
        }
        for (String slotText : configManager.getGuiConfig().getStringList("paths." + tier.name())) {
            int slot = Integer.parseInt(slotText);
            if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.GRAY_STAINED_GLASS_PANE) {
                inventory.setItem(slot, ItemUtils.create("ARROW", "&8Path", List.of("&7Progression route"), false));
            }
        }
        inventory.setItem(49, ItemUtils.create("BARRIER", "&cBack", List.of("&7Return to main menu"), false));
        player.openInventory(inventory);
    }

    public void openStats(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.STATS, null, false), 45, ItemUtils.component("&bYour Statistics"));
        decorate(inventory);
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        inventory.setItem(13, ItemUtils.create("KNOWLEDGE_BOOK", "&6Profile Summary", List.of(
                "&7Completed: &f" + profile.getCompletedAdvancements() + "/" + achievementManager.getAdvancements().size(),
                "&7Progression: &f" + String.format("%.2f", achievementManager.getProgressPercent(profile)) + "%",
                "&7Points: &f" + profile.getPoints(),
                "&7Rare unlocks: &f" + achievementManager.getRareCompletions(profile)
        ), true));
        inventory.setItem(20, ItemUtils.create("DIAMOND_SWORD", "&cCombat", List.of("&7Completions: &f" + achievementManager.getPlaystyleInsights(profile).get("combat")), false));
        inventory.setItem(21, ItemUtils.create("COMPASS", "&aExploration", List.of("&7Completions: &f" + achievementManager.getPlaystyleInsights(profile).get("exploration")), false));
        inventory.setItem(22, ItemUtils.create("BRICKS", "&6Builder", List.of("&7Completions: &f" + achievementManager.getPlaystyleInsights(profile).get("builder")), false));
        inventory.setItem(23, ItemUtils.create("WHEAT", "&eFarming", List.of("&7Completions: &f" + achievementManager.getPlaystyleInsights(profile).get("farming")), false));
        inventory.setItem(24, ItemUtils.create("ENCHANTING_TABLE", "&dMagic", List.of("&7Completions: &f" + achievementManager.getPlaystyleInsights(profile).get("magic")), false));
        inventory.setItem(40, ItemUtils.create("BARRIER", "&cBack", List.of("&7Return to main menu"), false));
        player.openInventory(inventory);
    }

    public void openLeaderboard(Player player, boolean session) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.LEADERBOARD, null, session), 54, ItemUtils.component(session ? "&6Session Leaderboard" : "&aGlobal Leaderboard"));
        decorate(inventory);
        List<LeaderboardEntry> entries = session ? leaderboardManager.getSessionLeaderboard(10) : leaderboardManager.getGlobalLeaderboard(10);
        int slot = 10;
        for (LeaderboardEntry entry : entries) {
            inventory.setItem(slot, ItemUtils.create("PLAYER_HEAD", "&e" + entry.name(), List.of(
                    "&7Completed: &f" + entry.completed(),
                    "&7Progress: &f" + String.format("%.2f", entry.progression()) + "%",
                    "&7Points: &f" + entry.points(),
                    "&7Highest tier: &f" + entry.highestTier()
            ), slot == 10));
            slot++;
            if (slot == 17) {
                slot = 19;
            }
        }
        inventory.setItem(45, ItemUtils.create("PAPER", "&aGlobal", List.of("&7Switch to global ranking"), !session));
        inventory.setItem(53, ItemUtils.create("CLOCK", "&6Session", List.of("&7Switch to session ranking"), session));
        inventory.setItem(49, ItemUtils.create("BARRIER", "&cBack", List.of("&7Return to main menu"), false));
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
            case TIER -> handleTierClick(player, event.getSlot());
            case STATS -> {
                if (event.getSlot() == 40) {
                    openMainMenu(player);
                }
            }
            case LEADERBOARD -> handleLeaderboardClick(player, event.getSlot());
        }
    }

    private void handleMainClick(Player player, int slot) {
        for (Tier tier : Tier.values()) {
            if (slot == configManager.getGuiConfig().getInt("main.tiers." + tier.name() + ".slot", 10 + tier.ordinal() * 2)) {
                openTierMenu(player, tier);
                return;
            }
        }
        if (slot == 49) {
            openStats(player);
        }
        if (slot == 53) {
            openLeaderboard(player, false);
        }
    }

    private void handleTierClick(Player player, int slot) {
        if (slot == 49) {
            openMainMenu(player);
        }
    }

    private void handleLeaderboardClick(Player player, int slot) {
        if (slot == 45) {
            openLeaderboard(player, false);
        }
        if (slot == 53) {
            openLeaderboard(player, true);
        }
        if (slot == 49) {
            openMainMenu(player);
        }
    }

    private void decorate(Inventory inventory) {
        FileConfiguration gui = configManager.getGuiConfig();
        ItemStack filler = ItemUtils.create(gui.getString("layout.filler-material", "BLACK_STAINED_GLASS_PANE"), gui.getString("layout.filler-name", " "), List.of(), false);
        for (int slot : gui.getIntegerList("layout.filler-slots")) {
            inventory.setItem(slot, filler);
        }
    }

    private List<String> buildChallengeLore() {
        List<String> lore = new ArrayList<>();
        challengeManager.getActiveChallenges().forEach(challenge -> lore.add("&7" + challenge.title() + " &8(" + challenge.type() + ")"));
        if (lore.isEmpty()) {
            lore.add("&7No active challenges");
        }
        return lore;
    }
}