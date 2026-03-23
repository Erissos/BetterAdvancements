package dev.erissos.betteradvancements.listener;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import dev.erissos.betteradvancements.data.PlayerDataManager;
import dev.erissos.betteradvancements.manager.AchievementManager;
import dev.erissos.betteradvancements.manager.ChallengeManager;
import dev.erissos.betteradvancements.model.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvancementListener implements Listener {

    private final BetterAdvancementsPlugin plugin;
    private final AchievementManager achievementManager;
    private final ChallengeManager challengeManager;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, org.bukkit.Location> lastLocations = new ConcurrentHashMap<>();

    public AdvancementListener(BetterAdvancementsPlugin plugin, AchievementManager achievementManager, ChallengeManager challengeManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.achievementManager = achievementManager;
        this.challengeManager = challengeManager;
        this.playerDataManager = playerDataManager;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> Bukkit.getOnlinePlayers().forEach(player -> trigger(player, TriggerType.PLAYTIME, Map.of("amount", "1"))), 20L * 60L, 20L * 60L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.loadProfile(event.getPlayer()).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> trigger(event.getPlayer(), TriggerType.JOIN, Map.of("amount", "1"))));
        lastLocations.put(event.getPlayer().getUniqueId(), event.getPlayer().getLocation().clone());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastLocations.remove(event.getPlayer().getUniqueId());
        playerDataManager.saveProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        trigger(event.getPlayer(), TriggerType.BLOCK_BREAK, Map.of("material", event.getBlock().getType().name(), "amount", "1"));
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player && event.getCurrentItem() != null) {
            trigger(player, TriggerType.ITEM_CRAFT, Map.of("item", event.getCurrentItem().getType().name(), "amount", String.valueOf(Math.max(1, event.getCurrentItem().getAmount()))));
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            trigger(killer, TriggerType.PLAYER_KILL, Map.of("entity", "PLAYER", "amount", "1"));
            return;
        }
        trigger(killer, TriggerType.MOB_KILL, Map.of("entity", event.getEntity().getType().name(), "amount", "1"));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().distanceSquared(event.getTo()) == 0) {
            return;
        }
        org.bukkit.Location previous = lastLocations.put(event.getPlayer().getUniqueId(), event.getTo().clone());
        if (previous != null) {
            int blocks = (int) previous.distance(event.getTo());
            if (blocks > 0) {
                trigger(event.getPlayer(), TriggerType.DISTANCE_WALK, Map.of("amount", String.valueOf(blocks)));
            }
            String previousBiome = previous.getBlock().getBiome().name();
            String newBiome = event.getTo().getBlock().getBiome().name();
            if (!previousBiome.equals(newBiome)) {
                trigger(event.getPlayer(), TriggerType.EXPLORE_BIOME, Map.of("biome", newBiome, "amount", "1"));
            }
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getCaught() != null) {
            trigger(event.getPlayer(), TriggerType.FISH, Map.of("amount", "1"));
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        trigger(event.getEnchanter(), TriggerType.ENCHANT, Map.of("amount", "1"));
    }

    @EventHandler
    public void onExtract(FurnaceExtractEvent event) {
        trigger(event.getPlayer(), TriggerType.SMELT, Map.of("item", event.getItemType().name(), "amount", String.valueOf(event.getItemAmount())));
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            trigger(player, TriggerType.BREED, Map.of("entity", event.getEntityType().name(), "amount", "1"));
        }
    }

    @EventHandler
    public void onTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) {
            trigger(player, TriggerType.TAME, Map.of("entity", event.getEntityType().name(), "amount", "1"));
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        trigger(event.getPlayer(), TriggerType.ITEM_CONSUME, Map.of("item", event.getItem().getType().name(), "amount", "1"));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().split(" ")[0].replace("/", "").toUpperCase();
        trigger(event.getPlayer(), TriggerType.COMMAND, Map.of("command", command, "amount", "1"));
    }

    private void trigger(Player player, TriggerType type, Map<String, String> context) {
        Map<String, String> mutableContext = new HashMap<>(context);
        achievementManager.handleTrigger(player, type, mutableContext);
        challengeManager.handleTrigger(player, type, mutableContext);
    }
}