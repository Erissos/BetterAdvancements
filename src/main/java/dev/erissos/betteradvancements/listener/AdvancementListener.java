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
    private final Map<UUID, Double> distances = new HashMap<>();
    private final Map<UUID, Map<org.bukkit.Material, Integer>> crafts = new HashMap<>();
    private final Map<UUID, EntityDeathEvent> pendingDeaths = new HashMap<>();

    public AdvancementListener(BetterAdvancementsPlugin plugin, AchievementManager achievementManager, ChallengeManager challengeManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.achievementManager = achievementManager;
        this.challengeManager = challengeManager;
        this.playerDataManager = playerDataManager;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> Bukkit.getOnlinePlayers().forEach(player -> trigger(player, TriggerType.PLAYTIME, Map.of("amount", "1"))), 20L * 60L, 20L * 60L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.loadProfile(event.getPlayer()).thenRun(() -> {
            if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    plugin.getRewardDelivery().claim(event.getPlayer());
                    trigger(event.getPlayer(), TriggerType.JOIN, Map.of("amount", "1"));
                }
            });
        }).exceptionally(failure -> { plugin.getLogger().severe("Profile load failed: " + failure.getMessage()); return null; });
        lastLocations.put(event.getPlayer().getUniqueId(), event.getPlayer().getLocation().clone());
    }

    @EventHandler(priority=org.bukkit.event.EventPriority.MONITOR,ignoreCancelled=true)
    public void onTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        distances.remove(event.getPlayer().getUniqueId());
        if (event.getTo()!=null) lastLocations.put(event.getPlayer().getUniqueId(),event.getTo().clone());
    }

    @EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        distances.remove(event.getPlayer().getUniqueId()); lastLocations.put(event.getPlayer().getUniqueId(),event.getPlayer().getLocation().clone());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastLocations.remove(event.getPlayer().getUniqueId());
        distances.remove(event.getPlayer().getUniqueId()); crafts.remove(event.getPlayer().getUniqueId());
        playerDataManager.release(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getSuiteIntegrations() != null
                && dev.erissos.betteradvancements.integration.SuiteHooks.enabled(plugin.getConfig(), "betteradvancements-ignore-synthetic-progress", true)
                && plugin.getSuiteIntegrations().isSynthetic(event)) return;
        if (!plugin.getIntegrations().allows(event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getType(), dev.desperis.integration.IntegrationService.Action.BREAK)
                || !allowedProgress(event.getPlayer(), event.getBlock().getLocation())) return;
        trigger(event.getPlayer(), TriggerType.BLOCK_BREAK, Map.of("material", event.getBlock().getType().name(), "amount", "1"));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player && event.getCurrentItem() != null) {
            org.bukkit.Location location = event.getInventory().getLocation();
            if (!allowedProgress(player, location == null ? player.getLocation() : location)) return;
            org.bukkit.Material material = event.getCurrentItem().getType();
            UUID id = player.getUniqueId();
            boolean scheduled = crafts.containsKey(id);
            crafts.computeIfAbsent(id, unused -> new HashMap<>()).putIfAbsent(material, countItems(player, material));
            if (!scheduled) Bukkit.getScheduler().runTask(plugin, () -> {
                var pending = crafts.remove(id);
                if (pending == null || !player.isOnline()) return;
                pending.forEach((type, before) -> {
                    int produced = countItems(player, type) - before;
                    if (produced > 0) trigger(player, TriggerType.ITEM_CRAFT, Map.of("item", type.name(), "amount", String.valueOf(produced)));
                });
            });
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getLastDamageCause() != null && event.getEntity().getLastDamageCause().isCancelled()) return;
        Player killer = event.getEntity().getKiller();
        if (!livePlayer(killer) || dev.erissos.betteradvancements.integration.SuiteHooks.isNpc(event.getEntity())) return;
        if (!(event.getEntity() instanceof Player) && plugin.getSuiteIntegrations() != null
                && plugin.getSuiteIntegrations().isManaged(event.getEntity())
                && !dev.erissos.betteradvancements.integration.SuiteHooks.enabled(plugin.getConfig(), "betteradvancements-count-managed-mob-kills", false)) return;
        if (!allowedProgress(killer, event.getEntity().getLocation())) return;
        if (event.getEntity() instanceof Player target && (!livePlayer(target) || !plugin.getIntegrations().allowsPvP(killer, target))) return;
        UUID entityId = event.getEntity().getUniqueId();
        // A later listener can cancel Paper's death event for a resurrection. Count only its final result.
        if (pendingDeaths.putIfAbsent(entityId, event) != null) return;
        var deathLocation = event.getEntity().getLocation().clone();
        var damage = event.getEntity().getLastDamageCause();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!pendingDeaths.remove(entityId, event) || event.isCancelled() || (damage != null && damage.isCancelled())
                    || !livePlayer(killer) || !allowedProgress(killer, deathLocation)) return;
            if (!(event.getEntity() instanceof Player) && event.getEntity().isValid() && !event.getEntity().isDead()) return;
            if (event.getEntity() instanceof Player target) {
                if (!plugin.getIntegrations().allowsPvP(killer, target)) return;
                trigger(killer, TriggerType.PLAYER_KILL, Map.of("entity", "PLAYER", "amount", "1"));
            } else {
                trigger(killer, TriggerType.MOB_KILL, Map.of("entity", event.getEntity().getType().name(), "amount", "1"));
            }
        });
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        UUID id = event.getPlayer().getUniqueId();
        if (event instanceof org.bukkit.event.player.PlayerTeleportEvent || event.getFrom().getWorld() != event.getTo().getWorld()) {
            lastLocations.put(id, event.getTo().clone()); distances.remove(id); return;
        }
        if (event.getFrom().distanceSquared(event.getTo()) == 0) {
            return;
        }
        org.bukkit.Location previous = lastLocations.put(id, event.getTo().clone());
        if (!allowedProgress(event.getPlayer(), event.getFrom()) || !allowedProgress(event.getPlayer(), event.getTo())) { distances.remove(id); return; }
        if (previous == null) previous = event.getFrom();
        if (previous.getWorld() != event.getTo().getWorld()) { distances.remove(id); return; }
        if (previous != null) {
            double movement = previous.distance(event.getTo());
            if (movement > 8 || event.getPlayer().isFlying() || event.getPlayer().isInsideVehicle()) { distances.remove(id); return; }
            double total = distances.getOrDefault(id, 0.0) + movement;
            int blocks = (int) Math.floor(total + 1.0e-9);
            distances.put(id, Math.max(0, total - blocks));
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

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getCaught() != null && allowedProgress(event.getPlayer(), event.getCaught().getLocation())) {
            trigger(event.getPlayer(), TriggerType.FISH, Map.of("amount", "1"));
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!allowedProgress(event.getEnchanter(), event.getEnchantBlock().getLocation())) return;
        trigger(event.getEnchanter(), TriggerType.ENCHANT, Map.of("amount", "1"));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onExtract(FurnaceExtractEvent event) {
        if (!allowedProgress(event.getPlayer(), event.getBlock().getLocation())) return;
        trigger(event.getPlayer(), TriggerType.SMELT, Map.of("item", event.getItemType().name(), "amount", String.valueOf(event.getItemAmount())));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player && allowedProgress(player, event.getEntity().getLocation())) {
            trigger(player, TriggerType.BREED, Map.of("entity", event.getEntityType().name(), "amount", "1"));
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player && allowedProgress(player, event.getEntity().getLocation())) {
            trigger(player, TriggerType.TAME, Map.of("entity", event.getEntityType().name(), "amount", "1"));
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        trigger(event.getPlayer(), TriggerType.ITEM_CONSUME, Map.of("item", event.getItem().getType().name(), "amount", "1"));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().split(" ")[0].replace("/", "").toUpperCase(java.util.Locale.ROOT);
        trigger(event.getPlayer(), TriggerType.COMMAND, Map.of("command", command, "amount", "1"));
    }

    private boolean allowedProgress(Player player, org.bukkit.Location location) {
        return plugin.getIntegrations().allows(player, location, dev.desperis.integration.IntegrationService.Action.PROGRESS);
    }

    private boolean livePlayer(Player player) {
        return player != null && player.isOnline() && Bukkit.getPlayer(player.getUniqueId()) == player
                && !dev.erissos.betteradvancements.integration.SuiteHooks.isNpc(player);
    }

    private void trigger(Player player, TriggerType type, Map<String, String> context) {
        Map<String, String> mutableContext = new HashMap<>(context);
        achievementManager.handleTrigger(player, type, mutableContext);
        challengeManager.handleTrigger(player, type, mutableContext);
    }

    private int countItems(Player player, org.bukkit.Material material) {
        int amount = 0;
        for (var item : player.getInventory().getStorageContents()) if (item != null && item.getType() == material) amount += item.getAmount();
        var cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() == material) amount += cursor.getAmount();
        return amount;
    }
}
