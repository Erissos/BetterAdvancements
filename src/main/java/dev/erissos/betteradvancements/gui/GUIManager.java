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
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.MAIN, null, false), 54, ItemUtils.component(configManager.getGuiConfig().getString("main.title", "<gradient:#f59e0b:#fb7185>Better Advancements</gradient>")));
        decorate(inventory);
        for (Tier tier : Tier.values()) {
            int slot = configManager.getGuiConfig().getInt("main.tiers." + tier.name() + ".slot", 10 + tier.ordinal() * 2);
            int completion = achievementManager.getTierCompletion(playerDataManager.getOrCreate(player.getUniqueId()), tier);
            inventory.setItem(slot, ItemUtils.create(
                    configManager.getGuiConfig().getString("main.tiers." + tier.name() + ".icon", "BOOK"),
                    configManager.getGuiConfig().getString("main.tiers." + tier.name() + ".name", "<#facc15>" + tier.name() + "</#facc15>"),
                    List.of("<gray>Completion: <white>" + completion + "%</white></gray>", "<dark_gray>Click to open tier</dark_gray>"),
                    completion >= 100
            ));
        }
        inventory.setItem(49, ItemUtils.create("NETHER_STAR", "<#7dd3fc>Statistics</#7dd3fc>", List.of("<gray>Open personal metrics</gray>"), false));
        inventory.setItem(53, ItemUtils.create("PLAYER_HEAD", "<#50fa7b>Leaderboard</#50fa7b>", List.of("<gray>View global ranking</gray>"), false));
        inventory.setItem(45, ItemUtils.create("CLOCK", "<gradient:#f59e0b:#fb7185>Challenges</gradient>", buildChallengeLore(), false));
        player.openInventory(inventory);
    }

    public void openTierMenu(Player player, Tier tier) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.TIER, tier, false), 54, ItemUtils.component("<gradient:#f59e0b:#fb7185>" + tier.getDisplayKey() + " Tier</gradient>"));
        decorate(inventory);
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        for (BetterAdvancement advancement : achievementManager.getByTier(tier)) {
            int slot = advancement.guiPosition().getOrDefault("slot", 0);
            boolean unlocked = achievementManager.isUnlocked(profile, advancement);
            PlayerAchievementProgress progress = profile.getAdvancementProgress().get(advancement.id());
            boolean completed = progress != null && progress.isCompleted();
            String displayTitle = advancement.hidden() && !completed ? "<dark_gray>???</dark_gray>" : advancement.title();
            String displayDescription = advancement.hidden() && !completed ? "<gray>Hidden achievement</gray>" : advancement.description();
            List<String> lore = new ArrayList<>();
            lore.add(displayDescription);
            lore.add("<dark_gray>Category: <white>" + advancement.category() + "</white></dark_gray>");
            lore.add("<dark_gray>Rarity: <white>" + advancement.rarity() + "</white></dark_gray>");
            lore.add("<dark_gray>Points: <white>" + advancement.points() + "</white></dark_gray>");
            lore.add("<dark_gray>Progress: <white>" + (progress == null ? 0 : progress.getProgress()) + "/" + advancement.trigger().target() + "</white></dark_gray>");
            lore.add(completed ? "<#50fa7b>Completed</#50fa7b>" : unlocked ? "<#facc15>Unlocked</#facc15>" : "<#ff6b6b>Locked</#ff6b6b>");
            inventory.setItem(slot, ItemUtils.create(completed ? "LIME_STAINED_GLASS_PANE" : unlocked ? advancement.icon() : "GRAY_STAINED_GLASS_PANE", displayTitle, lore, completed));
        }
        for (String slotText : configManager.getGuiConfig().getStringList("paths." + tier.name())) {
            int slot = Integer.parseInt(slotText);
            if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.GRAY_STAINED_GLASS_PANE) {
                inventory.setItem(slot, ItemUtils.create("ARROW", "<dark_gray>Path</dark_gray>", List.of("<gray>Progression route</gray>"), false));
            }
        }
        inventory.setItem(49, ItemUtils.create("BARRIER", "<#ff6b6b>Back</#ff6b6b>", List.of("<gray>Return to main menu</gray>"), false));
        player.openInventory(inventory);
    }

    public void openStats(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.STATS, null, false), 45, ItemUtils.component("<#7dd3fc>Your Statistics</#7dd3fc>"));
        decorate(inventory);
        PlayerProfile profile = playerDataManager.getOrCreate(player.getUniqueId());
        inventory.setItem(13, ItemUtils.create("KNOWLEDGE_BOOK", "<gradient:#f59e0b:#fb7185>Profile Summary</gradient>", List.of(
                "<gray>Completed: <white>" + profile.getCompletedAdvancements() + "/" + achievementManager.getAdvancements().size() + "</white></gray>",
                "<gray>Progression: <white>" + String.format("%.2f", achievementManager.getProgressPercent(profile)) + "%</white></gray>",
                "<gray>Points: <white>" + profile.getPoints() + "</white></gray>",
                "<gray>Rare unlocks: <white>" + achievementManager.getRareCompletions(profile) + "</white></gray>"
        ), true));
        inventory.setItem(20, ItemUtils.create("DIAMOND_SWORD", "<#ff6b6b>Combat</#ff6b6b>", List.of("<gray>Completions: <white>" + achievementManager.getPlaystyleInsights(profile).get("combat") + "</white></gray>"), false));
        inventory.setItem(21, ItemUtils.create("COMPASS", "<#50fa7b>Exploration</#50fa7b>", List.of("<gray>Completions: <white>" + achievementManager.getPlaystyleInsights(profile).get("exploration") + "</white></gray>"), false));
        inventory.setItem(22, ItemUtils.create("BRICKS", "<#f59e0b>Builder</#f59e0b>", List.of("<gray>Completions: <white>" + achievementManager.getPlaystyleInsights(profile).get("builder") + "</white></gray>"), false));
        inventory.setItem(23, ItemUtils.create("WHEAT", "<#facc15>Farming</#facc15>", List.of("<gray>Completions: <white>" + achievementManager.getPlaystyleInsights(profile).get("farming") + "</white></gray>"), false));
        inventory.setItem(24, ItemUtils.create("ENCHANTING_TABLE", "<#c084fc>Magic</#c084fc>", List.of("<gray>Completions: <white>" + achievementManager.getPlaystyleInsights(profile).get("magic") + "</white></gray>"), false));
        inventory.setItem(40, ItemUtils.create("BARRIER", "<#ff6b6b>Back</#ff6b6b>", List.of("<gray>Return to main menu</gray>"), false));
        player.openInventory(inventory);
    }

    public void openLeaderboard(Player player, boolean session) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MenuType.LEADERBOARD, null, session), 54, ItemUtils.component(session ? "<gradient:#f59e0b:#fb7185>Session Leaderboard</gradient>" : "<#50fa7b>Global Leaderboard</#50fa7b>"));
        decorate(inventory);
        List<LeaderboardEntry> entries = session ? leaderboardManager.getSessionLeaderboard(10) : leaderboardManager.getGlobalLeaderboard(10);
        int slot = 10;
        for (LeaderboardEntry entry : entries) {
            inventory.setItem(slot, ItemUtils.create("PLAYER_HEAD", "<#facc15>" + entry.name() + "</#facc15>", List.of(
                    "<gray>Completed: <white>" + entry.completed() + "</white></gray>",
                    "<gray>Progress: <white>" + String.format("%.2f", entry.progression()) + "%</white></gray>",
                    "<gray>Points: <white>" + entry.points() + "</white></gray>",
                    "<gray>Highest tier: <white>" + entry.highestTier() + "</white></gray>"
            ), slot == 10));
            slot++;
            if (slot == 17) {
                slot = 19;
            }
        }
        inventory.setItem(45, ItemUtils.create("PAPER", "<#50fa7b>Global</#50fa7b>", List.of("<gray>Switch to global ranking</gray>"), !session));
        inventory.setItem(53, ItemUtils.create("CLOCK", "<#f59e0b>Session</#f59e0b>", List.of("<gray>Switch to session ranking</gray>"), session));
        inventory.setItem(49, ItemUtils.create("BARRIER", "<#ff6b6b>Back</#ff6b6b>", List.of("<gray>Return to main menu</gray>"), false));
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
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private List<String> buildChallengeLore() {
        List<String> lore = new ArrayList<>();
        challengeManager.getActiveChallenges().forEach(challenge -> lore.add("<gray>" + challenge.title() + " <dark_gray>(" + challenge.type() + ")</dark_gray></gray>"));
        if (lore.isEmpty()) {
            lore.add("<gray>No active challenges</gray>");
        }
        return lore;
    }
}