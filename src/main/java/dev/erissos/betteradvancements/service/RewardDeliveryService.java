package dev.erissos.betteradvancements.service;

import dev.erissos.betteradvancements.util.AtomicFiles;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Durable reward mailbox; full stacks are delivered only when they fit. */
public final class RewardDeliveryService {
    private final JavaPlugin plugin;
    public RewardDeliveryService(JavaPlugin plugin) { this.plugin = plugin; }
    public void initialize() {
        File[] files=new File(plugin.getDataFolder(),"rewards").listFiles((d,n)->n.endsWith(".yml"));
        if (files!=null) for (File file:files) {
            YamlConfiguration data=load(file); boolean changed=false;
            for (String id:data.getKeys(false)) if ("DELIVERING".equals(data.getString(id+".state"))) { data.set(id+".state","REVIEW"); changed=true; }
            if (changed) { save(file,data); plugin.getLogger().warning("Reward delivery requires review: "+file.getName()); }
        }
    }
    public java.util.List<String> review(UUID player) {
        var data=load(new File(plugin.getDataFolder(),"rewards/"+player+".yml"));
        return data.getKeys(false).stream().filter(id -> "REVIEW".equals(data.getString(id+".state"))).toList();
    }
    public void reconcile(UUID player,String id,boolean delivered) {
        File file=new File(plugin.getDataFolder(),"rewards/"+player+".yml");
        var data=load(file);
        if (!"REVIEW".equals(data.getString(id+".state"))) throw new IllegalArgumentException("No pending reward review");
        if (delivered) finish(data,id); else data.set(id+".state","PENDING");
        save(file,data);
    }
    private YamlConfiguration load(File file) {
        YamlConfiguration data=new YamlConfiguration(); if (!file.exists()) return data;
        try { data.load(file); return data; } catch (Exception failure) { throw new IllegalStateException("Invalid mailbox; original preserved",failure); }
    }

    public void give(Player player, ItemStack reward) {
        give(player,reward,null);
    }
    public void give(Player player, ItemStack reward,String source) {
        File file = file(player);
        YamlConfiguration data = load(file);
        String id = rewardId(data,source);
        if (data.contains(id)) { claim(player); return; }
        if (source!=null) data.set(id+".source",source);
        data.set(id + ".item", reward.clone()); data.set(id + ".state", "PENDING");
        save(file, data);
        claim(player);
    }
    public int claim(Player player) {
        File file = file(player);
        YamlConfiguration data = load(file);
        int delivered = 0;
        for (String id : List.copyOf(data.getKeys(false))) {
            if (!"PENDING".equals(data.getString(id + ".state"))) continue;
            if (data.contains(id+".money")) {
                var hook=((dev.erissos.betteradvancements.BetterAdvancementsPlugin)plugin).getVaultHook();
                if (!hook.hasEconomy()) continue;
                data.set(id+".state","DELIVERING"); save(file,data);
                boolean accepted;
                try { accepted=hook.deposit(player,data.getDouble(id+".money")); }
                catch (RuntimeException failure) {
                    data.set(id+".state","REVIEW"); save(file,data);
                    plugin.getLogger().severe("Money reward needs review: "+player.getUniqueId()+" / "+id+": "+failure.getMessage());
                    continue;
                }
                if (!accepted) { data.set(id+".state","PENDING"); save(file,data); continue; }
                finish(data,id); save(file,data); delivered++; continue;
            }
            ItemStack item = data.getItemStack(id + ".item");
            if (item == null || !fits(player, item)) continue;
            // A crash in the inventory/write gap must not silently replay the same reward.
            data.set(id + ".state", "DELIVERING"); save(file, data);
            var leftovers = player.getInventory().addItem(item.clone());
            if (!leftovers.isEmpty()) throw new IllegalStateException("Reward inventory changed after capacity check");
            finish(data,id); save(file, data); delivered++;
        }
        return delivered;
    }
    public void giveMoney(Player player,double amount) {
        giveMoney(player,amount,null);
    }
    public void giveMoney(Player player,double amount,String source) {
        if (!Double.isFinite(amount) || amount<0 || amount>1_000_000_000_000D) throw new IllegalArgumentException("Invalid reward amount");
        File file=file(player); var data=load(file); String id=rewardId(data,source);
        if (data.contains(id)) { claim(player); return; }
        if (source!=null) data.set(id+".source",source);
        data.set(id+".money",amount); data.set(id+".state","PENDING"); save(file,data);
        claim(player);
    }
    private String rewardId(YamlConfiguration data,String source) {
        return source==null ? UUID.randomUUID().toString() : UUID.nameUUIDFromBytes((data.getString("_epoch","initial")+"|"+source).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }
    private void finish(YamlConfiguration data,String id) {
        if (data.contains(id+".source")) {
            data.set(id+".state","DONE");data.set(id+".item",null);data.set(id+".money",null);
        } else data.set(id,null);
    }
    public void resetEpoch(UUID player) {
        File file=new File(plugin.getDataFolder(),"rewards/"+player+".yml");var data=load(file);
        data.set("_epoch",UUID.randomUUID().toString());save(file,data);
    }
    public static boolean fits(Player player, ItemStack item) {
        int capacity = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) capacity += Math.min(item.getMaxStackSize(), player.getInventory().getMaxStackSize());
            else if (slot.isSimilar(item)) capacity += Math.max(0, Math.min(slot.getMaxStackSize(), player.getInventory().getMaxStackSize()) - slot.getAmount());
            if (capacity >= item.getAmount()) return true;
        }
        return false;
    }
    private File file(Player player) { return new File(plugin.getDataFolder(), "rewards/" + player.getUniqueId() + ".yml"); }
    private void save(File file, YamlConfiguration data) {
        try { AtomicFiles.write(file.toPath(), data.saveToString()); }
        catch (IOException failure) { throw new IllegalStateException("Could not save reward mailbox " + file, failure); }
    }
}
