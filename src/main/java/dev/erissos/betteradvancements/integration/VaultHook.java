package dev.erissos.betteradvancements.integration;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class VaultHook {

    private final BetterAdvancementsPlugin plugin;
    private Object economy;
    private Method depositMethod;

    public VaultHook(BetterAdvancementsPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        for (Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
            if (!service.getName().equals("net.milkbowl.vault.economy.Economy")) continue;
            var registration=Bukkit.getServicesManager().getRegistration(service);
            if (registration==null) continue;
            try {
                depositMethod=service.getMethod("depositPlayer",OfflinePlayer.class,double.class);
                economy=registration.getProvider();
            } catch (ReflectiveOperationException failure) {
                plugin.getLogger().warning("Vault economy binding failed: "+failure.getMessage());
            }
            return;
        }
    }

    public boolean hasEconomy() {
        economy=null; depositMethod=null; setup();
        return economy != null && depositMethod != null;
    }

    public boolean deposit(Player player, double amount) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Vault reward must run on the server thread");
        if (!Double.isFinite(amount) || amount<0 || amount>1_000_000_000_000D) throw new IllegalArgumentException("Invalid reward amount");
        if (!hasEconomy()) return false;
        try {
            Object response=depositMethod.invoke(economy,player,amount);
            if (response==null) throw new IllegalStateException("Vault returned no payment result");
            return Boolean.TRUE.equals(response.getClass().getMethod("transactionSuccess").invoke(response));
        } catch (java.lang.reflect.InvocationTargetException failure) {
            throw new IllegalStateException("Vault reward outcome needs review",failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not determine Vault reward outcome",failure);
        }
    }
}
