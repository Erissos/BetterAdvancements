package dev.erissos.betteradvancements.integration;

import dev.erissos.betteradvancements.BetterAdvancementsPlugin;
import org.bukkit.Bukkit;
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
        for (var provider : plugin.getServer().getServicesManager().getRegistrations(Object.class)) {
            Object candidate = provider.getProvider();
            if (candidate.getClass().getName().equals("net.milkbowl.vault.economy.plugins.Economy_ServerEconomy")) {
                this.economy = candidate;
                resolveDepositMethod(candidate);
                return;
            }
            for (Class<?> iface : candidate.getClass().getInterfaces()) {
                if (iface.getName().equals("net.milkbowl.vault.economy.Economy")) {
                    this.economy = candidate;
                    resolveDepositMethod(candidate);
                    return;
                }
            }
        }
    }

    private void resolveDepositMethod(Object candidate) {
        for (Method method : candidate.getClass().getMethods()) {
            if (method.getName().equals("depositPlayer") && method.getParameterCount() == 2) {
                this.depositMethod = method;
                break;
            }
        }
    }

    public boolean hasEconomy() {
        return economy != null && depositMethod != null;
    }

    public void deposit(Player player, double amount) {
        if (economy != null && depositMethod != null) {
            try {
                depositMethod.invoke(economy, player, amount);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}